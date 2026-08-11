package uz.alphazet.hoopla.ui.cart

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.UserData
import uz.alphazet.data.models.cart.CartData
import uz.alphazet.data.models.cart.CartItemData
import uz.alphazet.data.models.order.CheckOutInfo
import uz.alphazet.data.models.order.PaymentRequiredExceptionData
import uz.alphazet.domain.R
import uz.alphazet.domain.ui.BaseActivity
import uz.alphazet.domain.ui.BaseFragment
import uz.alphazet.domain.ui.showMessageDF
import uz.alphazet.domain.ui.showRequestDF
import uz.alphazet.domain.utils.formatToPrice
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.intentToBrowser
import uz.alphazet.domain.utils.visible
import uz.alphazet.domain.viewbinding.viewBinding
import uz.alphazet.hoopla.databinding.ScreenCartBinding
import uz.alphazet.hoopla.ui.MainActivity
import uz.alphazet.hoopla.ui.auth.AuthActivity
import uz.alphazet.hoopla.ui.cart.InputCartPromoBD.Companion.showInputCartPromoBD
import uz.alphazet.hoopla.ui.order.SelectCashbackSummaBD.Companion.showSelectCashbackSummaBD
import uz.alphazet.hoopla.ui.profile.payment.PaymentServicesActivity

/**
 * The server-side cart for one shop.
 *
 * Nothing here is computed locally except the cashback the customer is choosing to spend — every
 * add, remove, quantity change, promocode and comment round-trips and the screen re-renders from
 * the cart the server returns. That is deliberate: the server owns the totals and is the only
 * thing that can price a promocode.
 *
 * It is a top-level destination — the Cart tab — so it carries no back arrow and leans on
 * [MainActivity] for the status bar and for where to go next.
 */
class CartScreen : BaseFragment(uz.alphazet.hoopla.R.layout.screen_cart) {

    private val binding by viewBinding(ScreenCartBinding::bind)
    private val viewModel: CartVM by viewModel()

    private val tabHost: MainActivity? get() = activity as? MainActivity

    /**
     * The cafe's name, which the cart response does not carry. Resolved from the shop the cart
     * itself names, so this screen needs nothing handed to it.
     */
    private var fetchedShopName: String? = null

    private val adapter = CartItemAdapter()

    /** The latest cart from the server — the source of every figure on screen. */
    private var cart: CartData? = null
    private var userData: UserData? = null

    /** Cashback the customer has chosen to put towards this order; 0 when unused. */
    private var usingCashBack = 0.0

    /** The comment as the server last accepted it, so an unchanged field is never re-sent. */
    private var savedComment: String? = null

    /**
     * True while a cart mutation is out. `BaseRepo.handleFlow` emits only its terminal result —
     * never [UIResource.Loading] — so `collect`'s `onLoading` never fires and cannot be used to
     * disable controls.
     */
    private var isMutating = false

    /** True between tapping checkout and its response, so a second tap cannot order twice. */
    private var isCheckingOut = false

    /** True once a cart has rendered, so later refreshes never blank the screen out. */
    private var hasLoaded = false

    /**
     * Quantities the customer has stepped to but the server has not confirmed. The stepper has
     * to answer the tap immediately — waiting a round-trip per tap reads as a broken button —
     * so the row renders from here and the PATCH is debounced onto the settled value.
     */
    private val pendingQuantities = mutableMapOf<Int, Int>()
    private val quantityJobs = mutableMapOf<Int, Job>()

    /**
     * Lines the customer has removed but can still undo. The DELETE is held back until the
     * snackbar goes away un-actioned; the row disappears immediately either way.
     */
    private val pendingRemovals = mutableSetOf<Int>()

    /** Orders cart responses so a slow earlier one cannot overwrite a newer change. */
    private var mutationSeq = 0

    private val authListener =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

        }

    private val subscriptionListener =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

        }

    override fun initialize() {
        setUpChrome()

        binding.cartRv.adapter = adapter

        adapter.setOnQuantityChangeListener(::onQuantityStepped)
        adapter.setOnRemoveClickListener(::requestRemove)
        attachSwipeToRemove()

        binding.swipeRefresh.setOnRefreshListener { viewModel.getCart() }
        binding.browseMenu.setOnClickListener { leaveForMenu() }

        binding.promoAddRow.setOnClickListener { openPromoDialog() }
        binding.promoAppliedRow.setOnClickListener { openPromoDialog() }
        binding.removePromo.setOnClickListener { mutate { viewModel.removePromo() } }

        binding.retry.setOnClickListener {
            binding.errorState.gone()
            viewModel.getCart()
        }

        // Saved as soon as the customer leaves the field; onPause covers leaving the screen
        // without ever blurring it.
        binding.commentInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) persistCommentIfChanged { }
        }

        binding.checkout.setOnClickListener { onCheckoutClicked() }

        launch { viewModel.cartFlow.collectLatest(::collectCart) }
        launch { viewModel.userDataFlow.collectLatest(::collectUserData) }
        launch { viewModel.drinkImagesFlow.collectLatest { adapter.drinkImages = it } }
        launch {
            viewModel.shopNameFlow.collectLatest { name ->
                fetchedShopName = name
                renderShopName()
            }
        }

        // The cart itself is loaded by onResume, which runs on the way in as well as on the
        // way back — fetching here too would just double every launch. The shop follows from
        // whichever one the cart turns out to belong to.
        viewModel.getUser()
    }

    private fun setUpChrome() {
        binding.toolbar.inflateMenu(uz.alphazet.hoopla.R.menu.menu_cart)
        binding.toolbar.menu.findItem(uz.alphazet.hoopla.R.id.action_clear_cart)?.isVisible = false
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                uz.alphazet.hoopla.R.id.action_clear_cart -> {
                    confirmClearCart()
                    true
                }

                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Unconditional: items can be added from the order screen while this screen is in the
        // back stack, and a first load that failed (expired session, flaky network) must get
        // another chance rather than leaving the screen stuck on its failure state forever.
        viewModel.getCart()
    }

    /**
     * Tabs are shown and hidden rather than recreated, so returning to the Cart tab never fires
     * [onResume]. Without this the cart would still show whatever it held when last left.
     */
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) viewModel.getCart()
    }

    /**
     * The note is cart-scoped and only matters at checkout, but losing it on a back-press would
     * be silent. Saving here covers navigating away; [onCheckoutClicked] covers ordering with an
     * unsaved edit. A process kill can still drop it — it is never held anywhere but the server.
     */
    override fun onPause() {
        super.onPause()

        val typed = binding.commentInput.text?.toString()?.trim().orEmpty()
        if (typed != savedComment?.trim().orEmpty()) {
            savedComment = typed
            viewModel.setCommentInBackground(typed)
        }

        // The customer already sees these applied, so they have to be committed rather than
        // waiting out a debounce or an undo window this screen will not be around for.
        quantityJobs.values.forEach { it.cancel() }
        quantityJobs.clear()
        pendingQuantities.forEach { (id, quantity) ->
            viewModel.updateItemQuantityInBackground(id, quantity)
        }
        pendingQuantities.clear()

        pendingRemovals.forEach { viewModel.removeItemInBackground(it) }
        pendingRemovals.clear()
    }

    // ------------------------------------------------------------------ navigation

    /** An empty cart is a dead end, so it offers the way back to somewhere with drinks. */
    private fun leaveForMenu() {
        tabHost?.navigateToHomeScreen()
    }

    /** An order now exists; the cart is spent, so hand the customer to where they can watch it. */
    private fun onOrderPlaced() {
        viewModel.getCart()
        tabHost?.navigateToOrdersScreen()
    }

    // ------------------------------------------------------------------ rendering

    private fun collectCart(t: UIResource<CartData>) = t.collect(
        onError = { throwable ->
            // Leave something the customer can act on instead of an empty shell, then hand the
            // throwable to the usual typed handling (401 re-auth, 402 top-up, ...).
            if (cart == null) showLoadFailure(throwable)
            checkErrors(throwable)
        }
    ) { data -> render(data) }

    /**
     * Runs a cart mutation, dropping taps that arrive while one is already out.
     *
     * Every mutating call answers with the whole cart. The sequence number makes the newest
     * change the one that wins: a debounced quantity PATCH runs outside this gate, so without
     * it a slow earlier response could land last and paint a cart the customer has already
     * moved on from.
     */
    private fun mutate(block: suspend () -> SharedFlow<UIResource<CartData>>) {
        if (isMutating) return
        isMutating = true
        val seq = ++mutationSeq
        launch {
            block().collectLatest { resource ->
                resource.collect(
                    onError = { throwable ->
                        isMutating = false
                        checkErrors(throwable)
                    }
                ) { data ->
                    isMutating = false
                    acceptCart(data, seq)
                }
            }
        }
    }

    /** Renders a server cart unless a later change has already superseded it. */
    private fun acceptCart(data: CartData?, seq: Int) {
        if (seq != mutationSeq) return
        render(data)
    }

    // ------------------------------------------------------------------ quantity

    /**
     * Answers the tap straight away and sends the settled quantity once the customer stops
     * stepping. Three quick taps on "+" are one PATCH of +3, not three racing PATCHes whose
     * responses could land out of order.
     */
    private fun onQuantityStepped(item: CartItemData, quantity: Int) {
        val id = item.id ?: return

        // Stepping below one drops the line, and that route offers an undo.
        if (quantity <= 0) {
            requestRemove(item)
            return
        }

        pendingQuantities[id] = quantity
        renderItems()

        quantityJobs[id]?.cancel()
        quantityJobs[id] = launch {
            delay(QUANTITY_DEBOUNCE_MS)
            val seq = ++mutationSeq
            viewModel.updateItemQuantity(id, quantity).collectLatest { resource ->
                resource.collect(
                    onError = { throwable ->
                        // The server never took it, so the optimistic number has to go back.
                        pendingQuantities.remove(id)
                        renderItems()
                        checkErrors(throwable)
                    }
                ) { data ->
                    pendingQuantities.remove(id)
                    acceptCart(data, seq)
                }
            }
        }
    }

    // ------------------------------------------------------------------ removal

    /**
     * Hides the line and only commits the DELETE once the undo window closes. Re-adding is not
     * possible after the fact — the cart echoes modifiers back without the ids needed to send
     * them again — so the deletion is deferred rather than reversed.
     */
    private fun requestRemove(item: CartItemData) {
        val id = item.id ?: return
        if (!pendingRemovals.add(id)) return

        // A queued quantity change for a line being dropped is moot.
        quantityJobs.remove(id)?.cancel()
        pendingQuantities.remove(id)
        renderItems()

        Snackbar.make(binding.root, getString(R.string.cart_item_removed), Snackbar.LENGTH_LONG)
            .setAnchorView(if (binding.checkout.isVisible) binding.checkout else null)
            .setAction(getString(R.string.cart_undo)) {
                pendingRemovals.remove(id)
                renderItems()
            }
            .addCallback(object : Snackbar.Callback() {
                override fun onDismissed(bar: Snackbar?, event: Int) {
                    // The undo already put the line back; anything else commits it.
                    if (!pendingRemovals.contains(id)) return
                    pendingRemovals.remove(id)
                    commitRemoval(id)
                }
            })
            .show()
    }

    /**
     * Sends the DELETE the undo window has now committed to. Deliberately not routed through
     * [mutate]: that gate drops calls made while another mutation is out, and the customer has
     * already been told this line is gone — it cannot be the one that gets silently skipped.
     */
    private fun commitRemoval(id: Int) {
        val seq = ++mutationSeq
        launch {
            viewModel.removeItem(id).collectLatest { resource ->
                resource.collect(
                    onError = { throwable ->
                        // The line is still on the server, so put the screen back in step
                        // with it rather than leaving it hidden.
                        checkErrors(throwable)
                        viewModel.getCart()
                    }
                ) { data -> acceptCart(data, seq) }
            }
        }
    }

    /** Swiping a row aside removes it, with the same undo as the delete button. */
    private fun attachSwipeToRemove() {
        val callback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.absoluteAdapterPosition
                val item = adapter.currentList.getOrNull(position)
                if (item == null) {
                    adapter.notifyItemChanged(position)
                    return
                }
                requestRemove(item)
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(binding.cartRv)
    }

    private fun showLoadFailure(throwable: Throwable) {
        binding.swipeRefresh.gone()
        binding.checkout.gone()
        binding.emptyState.gone()
        binding.errorMessage.text =
            throwable.message?.takeIf { it.isNotBlank() } ?: getString(R.string.cart_load_failed)
        binding.errorState.visible()
    }

    /**
     * The list as the customer currently sees it: the server's lines with any un-sent quantity
     * step applied and any line awaiting its undo window taken out.
     */
    private fun visibleItems(): List<CartItemData> =
        cart?.items.orEmpty()
            .filter { it.id !in pendingRemovals }
            .map { item ->
                val pending = pendingQuantities[item.id] ?: return@map item
                // Only the line's own arithmetic is echoed. The cart subtotal and total stay
                // exactly as the server last stated them — a promocode can make them anything.
                item.copy(
                    quantity = pending,
                    lineTotal = (item.unitPrice ?: 0.0) * pending
                )
            }

    /** Re-renders just the list and the controls that depend on it being non-empty. */
    private fun renderItems() {
        val items = visibleItems()
        val isEmpty = items.isEmpty()

        // The tab's badge sits right under this screen; it has to move with the list, including
        // for edits that have not reached the server yet.
        tabHost?.onCartUpdated(cart?.copy(items = items))

        adapter.submitList(items)
        binding.emptyState.isVisible = isEmpty
        binding.swipeRefresh.isVisible = !isEmpty
        binding.checkout.isVisible = !isEmpty
        binding.toolbar.menu.findItem(uz.alphazet.hoopla.R.id.action_clear_cart)?.isVisible =
            !isEmpty
        // Drinks, not lines — two cappuccinos on one line read as "2 items" here and in the
        // shop screen's badge and cart bar, which quote the same plural.
        val units = items.sumOf { it.quantity ?: 0 }
        binding.itemCount.text =
            resources.getQuantityString(R.plurals.cart_items_count, units, units)

        // While an edit is un-sent the summary still shows the server's last word, which no
        // longer matches the list above it. Fading it says "this is catching up" rather than
        // quietly stating a figure that is about to change.
        val settled = pendingQuantities.isEmpty() && pendingRemovals.isEmpty()
        binding.summaryContainer.alpha = if (settled) 1f else PROVISIONAL_ALPHA
    }

    private fun render(data: CartData?) {
        cart = data
        hasLoaded = true

        binding.errorState.gone()
        binding.progress.gone()
        binding.swipeRefresh.isRefreshing = false

        // The tab does not know the shop until the cart names it; this is a no-op afterwards.
        data?.shopId?.let { viewModel.loadShopContext(it) }

        renderItems()
        if (visibleItems().isEmpty()) return

        renderShopName()

        val subtotal = data?.subtotal ?: 0.0
        val promoDiscount = data?.promoDiscount ?: 0.0

        binding.subtotal.text = subtotal.formatToPrice().plus(" UZS")

        if (promoDiscount > 0.0) {
            binding.promoDiscount.text = "-".plus(promoDiscount.formatToPrice()).plus(" UZS")
            binding.promoDiscountContainer.visible()
        } else {
            binding.promoDiscountContainer.gone()
        }

        val promoCode = data?.promoCode
        if (promoCode.isNullOrBlank()) {
            binding.promoAppliedRow.gone()
            binding.promoAddRow.visible()
        } else {
            binding.promoAddRow.gone()
            binding.promoAppliedRow.visible()
            binding.promoAppliedCode.text = promoCode
            binding.promoAppliedDiscount.text =
                "-".plus(promoDiscount.formatToPrice()).plus(" UZS")
        }

        // A discount can shrink the bill below the cashback that was already selected.
        val payableBeforeCashback = data?.total ?: 0.0
        if (usingCashBack > payableBeforeCashback) {
            usingCashBack = payableBeforeCashback.coerceAtLeast(0.0)
        }
        updateCashbackRow()
        renderTotals()

        // Re-filling the field mid-edit would fight the customer's cursor.
        savedComment = data?.comment
        if (!binding.commentInput.hasFocus()) {
            val current = binding.commentInput.text?.toString().orEmpty()
            val incoming = data?.comment.orEmpty()
            if (current != incoming) binding.commentInput.setText(incoming)
        }
    }

    private fun renderShopName() {
        binding.shopName.text = fetchedShopName
        binding.shopName.isVisible = !fetchedShopName.isNullOrBlank()
    }

    private fun collectUserData(t: UIResource<UserData>) = t.collect { data ->
        userData = data

        val balance = data?.balance ?: 0.0
        binding.useCashback.isEnabled = balance > 0.0
        binding.cashbackAvailable.text =
            getString(R.string.cashback_available, balance.formatToPrice().plus(" UZS"))

        binding.useCashback.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) openCashbackSelector() else clearCashback()
        }
        binding.cashbackChange.setOnClickListener { openCashbackSelector() }
    }

    // ------------------------------------------------------------------ promocode

    private fun openPromoDialog() {
        // The sheet already applied the code server-side, so its cart is the newest word.
        requireActivity().showInputCartPromoBD(cart?.promoCode) { data ->
            acceptCart(data, ++mutationSeq)
        }
    }

    // ------------------------------------------------------------------ cashback

    /** Cashback can only cover what is left after the promocode. */
    private fun openCashbackSelector() {
        val host = activity as? BaseActivity ?: return
        val maxLimit = (cart?.total ?: 0.0).coerceAtLeast(0.0)
        host.showSelectCashbackSummaBD(
            null, userData, maxLimit, usingCashBack,
            // The sheet only reports from its "use"/"reset" buttons; swiping it away would
            // otherwise leave the switch on while no cashback is actually being spent.
            onDismissed = { updateCashbackRow() }
        ) { summa ->
            usingCashBack = summa
            updateCashbackRow()
            renderTotals()
        }
    }

    private fun clearCashback() {
        usingCashBack = 0.0
        updateCashbackRow()
        renderTotals()
    }

    private fun updateCashbackRow() {
        if (usingCashBack > 0) {
            binding.cashbackUsing.text =
                getString(R.string.cashback_using, usingCashBack.formatToPrice().plus(" UZS"))
            binding.cashbackAmountRow.visible()
        } else {
            binding.useCashback.isChecked = false
            binding.cashbackAmountRow.gone()
        }
    }

    /** The server's total less the cashback the customer is spending. */
    private fun payableTotal(): Double =
        ((cart?.total ?: 0.0) - usingCashBack).coerceAtLeast(0.0)

    /**
     * Puts the payable figure both in the summary and on the button. Carrying the amount on the
     * primary action is what lets someone commit without scrolling back up to check it.
     */
    private fun renderTotals() {
        val payable = payableTotal().formatToPrice().plus(" UZS")
        binding.totalSumma.text = payable
        binding.checkout.text = getString(R.string.cart_checkout_with_total, payable)
    }

    // ------------------------------------------------------------------ comment

    /**
     * Sends the note when it differs from what the server last accepted, then runs [andThen].
     * When nothing changed [andThen] runs immediately — no wasted round-trip.
     */
    private fun persistCommentIfChanged(andThen: () -> Unit) {
        val typed = binding.commentInput.text?.toString()?.trim().orEmpty()
        if (typed == savedComment?.trim().orEmpty()) {
            andThen()
            return
        }
        val seq = ++mutationSeq
        launch {
            viewModel.setComment(typed).collectLatest { resource ->
                resource.collect(
                    // A note that would not save is not worth cancelling an order over, and
                    // stopping here silently would look to the customer like the tap was missed.
                    onError = { throwable ->
                        showErrorMessage(throwable.message)
                        andThen()
                    }
                ) { data ->
                    acceptCart(data, seq)
                    andThen()
                }
            }
        }
    }

    // ------------------------------------------------------------------ clear

    private fun confirmClearCart() {
        showRequestDF(
            getString(R.string.cart_clear),
            getString(R.string.cart_clear_question),
            getString(R.string.yes),
            getString(R.string.no)
        ) {
            launch {
                viewModel.clearCart().collectLatest { resource ->
                    // Clearing answers empty, so the cart is re-read rather than rendered.
                    resource.collect { viewModel.getCart() }
                }
            }
        }
    }

    // ------------------------------------------------------------------ checkout

    private fun onCheckoutClicked() {
        // The flush and the comment save each put a round-trip between the tap and the order,
        // and the button is still live during them — without this guard a second tap orders
        // the cart twice.
        if (isCheckingOut || visibleItems().isEmpty()) return
        isCheckingOut = true
        morphCheckoutButton(true)

        flushPendingEdits { persistCommentIfChanged { checkout() } }
    }

    /**
     * Sends anything the customer can see but the server has not been told yet — a quantity
     * still inside its debounce, a line still inside its undo window — and only then continues.
     *
     * Ordering without this would bill the cart the server still holds, which is not the cart
     * on screen: a line the customer just removed would arrive in their order.
     */
    private fun flushPendingEdits(andThen: () -> Unit) {
        val quantities = pendingQuantities.toMap()
        val removals = pendingRemovals.toList()
        if (quantities.isEmpty() && removals.isEmpty()) {
            andThen()
            return
        }

        quantityJobs.values.forEach { it.cancel() }
        quantityJobs.clear()
        pendingQuantities.clear()
        pendingRemovals.clear()

        launch {
            val failed = buildList {
                quantities.forEach { (id, quantity) ->
                    add(viewModel.updateItemQuantity(id, quantity).firstOrNull())
                }
                removals.forEach { add(viewModel.removeItem(itemId = it).firstOrNull()) }
            }.filterIsInstance<UIResource.Error>()

            if (failed.isNotEmpty()) {
                // The server still holds what the screen says was changed. Ordering now would
                // bill the cart the customer thinks they edited, so stop and resync instead.
                onCheckoutFinished()
                viewModel.getCart()
                checkErrors(failed.first().throwable)
                return@launch
            }

            andThen()
        }
    }

    private fun checkout() {
        launch {
            viewModel.checkout(usingCashBack > 0, usingCashBack).collectLatest(::collectCheckout)
        }
    }

    /** Re-arms the button after a checkout that did not navigate away. */
    private fun onCheckoutFinished() {
        isCheckingOut = false
        morphCheckoutButton(false)
    }

    /**
     * A 200 here means the order needed no payment — cashback covered it. Anything that needs
     * paying arrives as a 402 and is handled by [onPaymentException], exactly as the
     * single-item checkout does.
     */
    private fun collectCheckout(t: UIResource<CheckOutInfo>) = t.collect(
        onError = { throwable ->
            // The overrides below leave the screen where that is right; anything else keeps the
            // customer here, so the button has to come back.
            onCheckoutFinished()
            checkErrors(throwable)
        }
    ) { data ->
        val url = data?.checkout_url?.takeIf { it.isNotBlank() }
        if (url != null) {
            requireContext().intentToBrowser(url)
            onOrderPlaced()
            return@collect
        }

        showMessageDF(
            getString(R.string.order_received_),
            fetchedShopName?.let { getString(R.string.cart_order_received_message, it) }.orEmpty(),
            "OK"
        ) {
            onOrderPlaced()
        }
    }

    override fun onPaymentException(
        errorData: PaymentRequiredExceptionData?,
        message: String?,
        code: Int
    ) {
        super.onPaymentException(errorData, message, code)
        requireContext().intentToBrowser(errorData?.checkoutUrl ?: "")
        onOrderPlaced()
    }

    override fun onPreconditionRequiredException(message: String?, code: Int) {
        super.onPreconditionRequiredException(message, code)
        subscriptionListener.launch(Intent(requireContext(), PaymentServicesActivity::class.java))
    }

    override fun onUnauthorizedException(message: String?, code: Int) {
        super.onUnauthorizedException(message, code)
        authListener.launch(Intent(requireContext(), AuthActivity::class.java))
    }

    // ------------------------------------------------------------------ loading

    /** Checkout morphs its own button instead of dimming the whole screen. */
    private fun morphCheckoutButton(isLoading: Boolean) {
        if (isLoading) binding.checkout.startAnimation() else binding.checkout.revertAnimation()
    }

    /**
     * Only the very first load gets a spinner in place of content. Once a cart is on screen a
     * refresh must not replace it with a blank page — the pull-to-refresh indicator carries
     * that, and a background refresh from [onResume] shows nothing at all.
     */
    override fun showLoading() {
        if (!hasLoaded) binding.progress.visible()
    }

    override fun hideLoading() {
        binding.progress.gone()
        binding.swipeRefresh.isRefreshing = false
    }

    private companion object {
        /**
         * Long enough to swallow a burst of taps on the stepper, short enough that the summary
         * catches up while the customer is still looking at the line they changed.
         */
        const val QUANTITY_DEBOUNCE_MS = 500L

        /** Fade for a summary that has not caught up with an un-sent edit yet. */
        const val PROVISIONAL_ALPHA = 0.45f
    }

}

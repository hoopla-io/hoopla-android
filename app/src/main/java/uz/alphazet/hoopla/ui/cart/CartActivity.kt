package uz.alphazet.hoopla.ui.cart

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.UserData
import uz.alphazet.data.models.cart.CartData
import uz.alphazet.data.models.order.CheckOutInfo
import uz.alphazet.data.models.order.PaymentRequiredExceptionData
import uz.alphazet.domain.R
import uz.alphazet.domain.ui.BaseActivity
import uz.alphazet.domain.ui.showMessageDF
import uz.alphazet.domain.ui.showRequestDF
import uz.alphazet.domain.utils.formatToPrice
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.intentToBrowser
import uz.alphazet.domain.utils.visible
import uz.alphazet.hoopla.databinding.ScreenCartBinding
import uz.alphazet.hoopla.ui.auth.AuthActivity
import uz.alphazet.hoopla.ui.cart.InputCartPromoBD.Companion.showInputCartPromoBD
import uz.alphazet.hoopla.ui.order.OrderActivity.Companion.RESULT_ORDER_CREATED
import uz.alphazet.hoopla.ui.order.SelectCashbackSummaBD.Companion.showSelectCashbackSummaBD
import uz.alphazet.hoopla.ui.profile.payment.PaymentServicesActivity
import uz.alphazet.hoopla.ui.shop_details.ShopDetailActivity.Companion.SHOP_ID
import uz.alphazet.hoopla.ui.shop_details.ShopDetailActivity.Companion.SHOP_NAME

/**
 * The server-side cart for one shop.
 *
 * Nothing here is computed locally except the cashback the customer is choosing to spend — every
 * add, remove, quantity change, promocode and comment round-trips and the screen re-renders from
 * the cart the server returns. That is deliberate: the server owns the totals and is the only
 * thing that can price a promocode.
 */
class CartActivity : BaseActivity() {

    private lateinit var binding: ScreenCartBinding
    private val viewModel: CartVM by viewModel()

    /**
     * The cart response carries no shop name, so the screen that opened the cart passes its
     * own along with the id it belongs to. The cart is per customer and may have been built at
     * a different cafe, so the name is only used once [CartData.shopId] confirms it matches.
     */
    private val shopId by lazy { intent.getIntExtra(SHOP_ID, -1) }
    private val shopName by lazy { intent.getStringExtra(SHOP_NAME) }

    /** [shopName] once the cart has confirmed it belongs to that shop; null otherwise. */
    private var resolvedShopName: String? = null

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
     * disable controls. Without this, a customer tapping "+" three times inside one round-trip
     * would send three PATCHes all computed from the same stale quantity.
     */
    private var isMutating = false

    /** True between tapping checkout and its response, so a second tap cannot order twice. */
    private var isCheckingOut = false

    private val authListener =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

        }

    private val subscriptionListener =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ScreenCartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.cartRv.adapter = adapter

        binding.toolbar.setNavigationOnClickListener { finish() }
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

        adapter.setOnQuantityChangeListener { item, quantity ->
            val id = item.id ?: return@setOnQuantityChangeListener
            mutate { viewModel.updateItemQuantity(id, quantity) }
        }

        adapter.setOnRemoveClickListener { item ->
            val id = item.id ?: return@setOnRemoveClickListener
            mutate { viewModel.removeItem(id) }
        }

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

        // The cart itself is loaded by onResume, which runs on the way in as well as on the
        // way back — fetching here too would just double every launch.
        viewModel.getUser()
    }

    override fun onResume() {
        super.onResume()
        // Unconditional: items can be added from the order screen while this activity is in the
        // back stack, and a first load that failed (expired session, flaky network) must get
        // another chance rather than leaving the screen stuck on its failure state forever.
        viewModel.getCart()
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
    }

    override fun updateStatusBarViewHeight() {
        launch {
            val statusBarHeight = getStatusBarHeight()
            binding.statusBarView.layoutParams.height = statusBarHeight
            binding.statusBarView.requestLayout()
        }
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
     * Every mutating call answers with the whole cart, so they all land here. The in-flight flag
     * is cleared on both outcomes — `handleFlow` never emits Loading, so nothing else would.
     */
    private fun collectMutation(t: UIResource<CartData>) = t.collect(
        onError = { throwable ->
            isMutating = false
            checkErrors(throwable)
        }
    ) { data ->
        isMutating = false
        render(data)
    }

    /** Runs a cart mutation, dropping taps that arrive while one is already out. */
    private fun mutate(block: suspend () -> SharedFlow<UIResource<CartData>>) {
        if (isMutating) return
        isMutating = true
        launch { block().collectLatest(::collectMutation) }
    }

    private fun showLoadFailure(throwable: Throwable) {
        binding.nestedScroll.gone()
        binding.checkout.gone()
        binding.emptyState.gone()
        binding.errorMessage.text =
            throwable.message?.takeIf { it.isNotBlank() } ?: getString(R.string.cart_load_failed)
        binding.errorState.visible()
    }

    private fun render(data: CartData?) {
        cart = data

        val items = data?.items.orEmpty()
        val isEmpty = items.isEmpty()

        binding.errorState.gone()
        binding.emptyState.isVisible = isEmpty
        binding.nestedScroll.isVisible = !isEmpty
        binding.checkout.isVisible = !isEmpty
        binding.toolbar.menu.findItem(uz.alphazet.hoopla.R.id.action_clear_cart)?.isVisible =
            !isEmpty

        adapter.submitList(items)
        if (isEmpty) return

        resolvedShopName = shopName?.takeIf { shopId != -1 && data?.shopId == shopId }
        binding.shopName.text = resolvedShopName
        binding.shopName.isVisible = !resolvedShopName.isNullOrBlank()

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

        binding.totalSumma.text =
            (payableBeforeCashback - usingCashBack).coerceAtLeast(0.0).formatToPrice().plus(" UZS")

        // Re-filling the field mid-edit would fight the customer's cursor.
        savedComment = data?.comment
        if (!binding.commentInput.hasFocus()) {
            val current = binding.commentInput.text?.toString().orEmpty()
            val incoming = data?.comment.orEmpty()
            if (current != incoming) binding.commentInput.setText(incoming)
        }
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
        showInputCartPromoBD(cart?.promoCode) { data -> render(data) }
    }

    // ------------------------------------------------------------------ cashback

    /** Cashback can only cover what is left after the promocode. */
    private fun openCashbackSelector() {
        val maxLimit = (cart?.total ?: 0.0).coerceAtLeast(0.0)
        showSelectCashbackSummaBD(
            null, userData, maxLimit, usingCashBack,
            // The sheet only reports from its "use"/"reset" buttons; swiping it away would
            // otherwise leave the switch on while no cashback is actually being spent.
            onDismissed = { updateCashbackRow() }
        ) { summa ->
            usingCashBack = summa
            updateCashbackRow()
            binding.totalSumma.text = payableTotal().formatToPrice().plus(" UZS")
        }
    }

    private fun clearCashback() {
        usingCashBack = 0.0
        updateCashbackRow()
        binding.totalSumma.text = payableTotal().formatToPrice().plus(" UZS")
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
                    render(data)
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
        // The comment save puts a round-trip between the tap and the order, and the button is
        // still live during it — without this guard a second tap orders the cart twice.
        if (isCheckingOut || cart?.items.isNullOrEmpty()) return
        isCheckingOut = true
        morphCheckoutButton(true)

        persistCommentIfChanged { checkout() }
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
            // The overrides below finish the screen where that is right; anything else leaves
            // the customer here, so the button has to come back.
            onCheckoutFinished()
            checkErrors(throwable)
        }
    ) { data ->
        val url = data?.checkout_url?.takeIf { it.isNotBlank() }
        if (url != null) {
            intentToBrowser(url)
            setResult(RESULT_ORDER_CREATED)
            finish()
            return@collect
        }

        showMessageDF(
            getString(R.string.order_received_),
            resolvedShopName?.let { getString(R.string.cart_order_received_message, it) }.orEmpty(),
            "OK"
        ) {
            setResult(RESULT_ORDER_CREATED)
            finish()
        }
    }

    override fun onPaymentException(
        errorData: PaymentRequiredExceptionData?,
        message: String?,
        code: Int
    ) {
        super.onPaymentException(errorData, message, code)
        intentToBrowser(errorData?.checkoutUrl ?: "")
        setResult(RESULT_ORDER_CREATED)
        finish()
    }

    override fun onPreconditionRequiredException(message: String?, code: Int) {
        super.onPreconditionRequiredException(message, code)
        val intent1 = Intent(this, PaymentServicesActivity::class.java)
        subscriptionListener.launch(intent1)
    }

    override fun onUnauthorizedException(message: String?, code: Int) {
        super.onUnauthorizedException(message, code)
        val intent1 = Intent(this, AuthActivity::class.java)
        authListener.launch(intent1)
    }

    // ------------------------------------------------------------------ loading

    /** Checkout morphs its own button instead of dimming the whole screen. */
    private fun morphCheckoutButton(isLoading: Boolean) {
        if (isLoading) binding.checkout.startAnimation() else binding.checkout.revertAnimation()
    }

    override fun showLoading() {
        binding.progress.visible()
    }

    override fun hideLoading() {
        binding.progress.gone()
    }

}

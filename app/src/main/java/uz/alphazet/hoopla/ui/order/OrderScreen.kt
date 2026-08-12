package uz.alphazet.hoopla.ui.order

import android.content.Intent
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.DrinkItemData
import uz.alphazet.data.models.ShopData
import uz.alphazet.data.models.cart.CartData
import uz.alphazet.data.models.order.ModifierGroupData
import uz.alphazet.data.models.order.ModifierItemData
import uz.alphazet.data.models.order.OrderDetails
import uz.alphazet.data.models.order.PaymentRequiredExceptionData
import uz.alphazet.domain.R
import uz.alphazet.domain.network.ConflictException
import uz.alphazet.domain.ui.BaseFragment
import uz.alphazet.domain.ui.showRequestDF
import uz.alphazet.domain.utils.Constants
import uz.alphazet.domain.utils.formatToPrice
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.visible
import uz.alphazet.domain.viewbinding.viewBinding
import uz.alphazet.hoopla.databinding.ScreenOrderBinding
import uz.alphazet.hoopla.ui.auth.AuthActivity
import uz.alphazet.hoopla.ui.cart.CartVM
import uz.alphazet.hoopla.ui.mainActivity
import uz.alphazet.hoopla.ui.navigateTo
import uz.alphazet.hoopla.ui.popScreen
import uz.alphazet.hoopla.ui.profile.payment.PaymentServicesScreen
import uz.alphazet.hoopla.ui.profile.subscriptions.SubscriptionsScreen
import uz.alphazet.hoopla.ui.views.showTopPill
import uz.alphazet.hoopla.ui.shop_details.ShopDetailScreen.Companion.DRINK_DATA
import uz.alphazet.hoopla.ui.shop_details.ShopDetailScreen.Companion.SHOP_ID

class OrderScreen : BaseFragment(uz.alphazet.hoopla.R.layout.screen_order) {

    private val binding by viewBinding(ScreenOrderBinding::bind)
    private val viewModel: OrderVM by viewModel()
    private val cartViewModel: CartVM by viewModel()

    private val shopId by lazy { arguments?.getInt(SHOP_ID, -1) ?: -1 }
    private val drinkData by lazy { arguments?.getParcelable<DrinkItemData>(DRINK_DATA) }

    private val sizeAdapter = SizeAdapter()
    private val sugarAdapter = SizeAdapter()
    private val milkAdapter = ModificationAdapter()
    private val syrupAdapter = ModificationAdapter()

    /** Dynamic modifier-group list (new flow); active only when the API returns modifierGroups. */
    private val groupAdapter = ModifierGroupAdapter { onModifierSelectionChanged() }
    private var usingGroups = false

    /**
     * The selection of the add-to-cart request currently in flight. It is what tells a 409
     * apart from any other conflict, so it is read-and-cleared as soon as one arrives.
     */
    private var pendingCartModifiers: ArrayList<ModifierItemData>? = null

    /**
     * Guards the single CTA while an add is in flight. A [CircularProgressButton]'s own
     * `isEnabled` is unreliable mid-morph, so the flag is tracked separately — and it is what
     * stops [renderTotal] overwriting the spinner with a price.
     */
    private var isAdding = false

    private val authListener =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

        }

    override fun initialize() {
        launch {
            viewModel.validateOrder(shopId, drinkData?.id ?: -1).collectLatest(::collectDetail)
        }

        binding.sizes.adapter = sizeAdapter
        binding.sugars.adapter = sugarAdapter
        binding.milkTypes.adapter = milkAdapter
        binding.syrups.adapter = syrupAdapter
        binding.modifierGroupsRv.adapter = groupAdapter

        binding.toolbar.setNavigationOnClickListener { popScreen() }

        // One action, whichever modifier flow is running: everything is ordered via the cart.
        binding.order.setOnClickListener { onAddToCartClicked() }

        binding.qtyMinus.setOnClickListener { stepQuantity(-1) }
        binding.qtyPlus.setOnClickListener { stepQuantity(1) }
        renderQuantity()

        sizeAdapter.setOnItemClickListener { item ->
            sizeAdapter.selectItem(item.modificationId ?: "")
            renderTotal()
        }
        sugarAdapter.setOnItemClickListener { item ->
            sugarAdapter.selectItem(item.modificationId ?: "")
            renderTotal()
        }
        milkAdapter.setOnItemClickListener { item ->
            milkAdapter.selectItem(item.modificationId ?: "")
            renderTotal()
        }
        syrupAdapter.setOnItemClickListener { item ->
            syrupAdapter.selectItem(item.modificationId ?: "")
            renderTotal()
        }
    }

    private fun stepQuantity(delta: Int) {
        viewModel.stepQuantity(delta)
        renderQuantity()
    }

    /** The stepper floors at one — dropping a line is the cart's job, not this screen's. */
    private fun renderQuantity() {
        binding.qtyValue.text = viewModel.quantity.toString()
        val canDecrease = viewModel.quantity > OrderVM.MIN_QUANTITY
        binding.qtyMinus.isEnabled = canDecrease
        binding.qtyMinus.alpha = if (canDecrease) 1f else DISABLED_ALPHA
        renderTotal()
    }

    /** What this selection, at this quantity, will cost. */
    private fun renderTotal() {
        if (isAdding) return // the button is showing a spinner; its text is restored on revert
        val total = calculatePrice() * viewModel.quantity
        binding.order.text =
            getString(R.string.order_add_with_total, total.formatToPrice().plus(" UZS"))
    }

    private fun collectDetail(t: UIResource<OrderDetails>) = t.collect { data ->
        binding.image.load(data?.drink?.imageUrl)
        binding.name.text = data?.drink?.name
        binding.drinkPrice.text = (data?.drink?.amount ?: 0.0).formatToPrice().plus(" UZS")
        binding.cafeName.text = getString(R.string.label_by_shop, data?.shop?.name)
        viewModel.defaultPrice = data?.drink?.amount

        val groups = data?.modifierGroups
        if (data != null && !groups.isNullOrEmpty()) {
            setupGroups(groups)
        } else {
            setupLegacy(data)
        }
    }

    /** New flow: render the named modifier groups with their min/max rules. */
    private fun setupGroups(groups: List<ModifierGroupData>) {
        usingGroups = true

        // Hide the legacy fixed sections; the dynamic group list replaces them.
        listOf(
            binding.sizeTitle, binding.sizes,
            binding.sugarTitle, binding.sugars,
            binding.milkTitle, binding.milkTypes,
            binding.syrupTitle, binding.syrups
        ).forEach { it.gone() }
        binding.modifierGroupsRv.visible()

        groupAdapter.setGroups(groups)
        renderTotal()
    }

    /** Legacy flow: the fixed size/sugar/milk/syrup sections read from the modifications map. */
    private fun setupLegacy(data: OrderDetails?) {
        usingGroups = false
        binding.modifierGroupsRv.gone()

        initModification(
            binding.sugarTitle,
            binding.sugars,
            sugarAdapter,
            data?.modifications?.sugar
        )
        initModification(binding.sizeTitle, binding.sizes, sizeAdapter, data?.modifications?.size)

        val milkItems = data?.modifications?.milk
        if (milkItems.isNullOrEmpty()) {
            binding.milkTypes.gone()
            binding.milkTitle.gone()
        } else {
            milkAdapter.submitList(milkItems)
            milkAdapter.selectItem(milkItems.firstOrNull()?.modificationId ?: "")
        }

        val syrupItems = data?.modifications?.syrup
        if (syrupItems.isNullOrEmpty()) {
            binding.syrups.gone()
            binding.syrupTitle.gone()
        } else {
            syrupAdapter.submitList(syrupItems)
            syrupAdapter.selectItem(syrupItems.firstOrNull()?.modificationId ?: "")
        }

        renderTotal()
    }

    /** The fixed size/sugar/milk/syrup selections, as the API expects them. */
    private fun buildLegacyModifiers(): ArrayList<ModifierItemData> {
        val modifiers = ArrayList<ModifierItemData>()

        listOfNotNull(
            sizeAdapter.getSelectedItem(),
            sugarAdapter.getSelectedItem(),
            milkAdapter.getSelectedItem(),
            syrupAdapter.getSelectedItem()
        ).forEach { item ->
            modifiers.add(
                ModifierItemData(
                    item.modificationId ?: "",
                    item.modificationGroupId,
                    item.modificationKey ?: "",
                    item.modificationPrice ?: 0.0,
                    item.modificationName
                )
            )
        }

        return modifiers
    }

    // ---------------------------------------------------------------- add to cart

    /**
     * Puts this drink in the shop's cart instead of ordering it on its own. The two flows ship
     * side by side, so nothing about the order button changes.
     */
    private fun onAddToCartClicked() {
        if (usingGroups) {
            val unmet = groupAdapter.firstUnsatisfiedGroup()
            if (unmet != null) {
                showErrorMessage(
                    getString(
                        R.string.modifier_select_at_least,
                        unmet.minSelect,
                        unmet.name?.takeIf { it.isNotBlank() } ?: unmet.key
                    )
                )
                return
            }
        }

        val modifiers = if (usingGroups) groupAdapter.buildModifiers() else buildLegacyModifiers()
        sendAddToCart(modifiers)
    }

    /**
     * `BaseRepo.handleFlow` emits only its terminal result, never [UIResource.Loading], so
     * `collect`'s `onLoading` cannot disable this button. It is disabled here instead and
     * re-armed on both outcomes — otherwise a double tap adds the drink twice, and the second
     * 409 would arrive after [pendingCartModifiers] had already been consumed.
     */
    private fun sendAddToCart(modifiers: ArrayList<ModifierItemData>) {
        if (isAdding) return
        pendingCartModifiers = modifiers
        isAdding = true
        binding.order.isEnabled = false
        binding.order.startAnimation()
        launch {
            cartViewModel.addItem(shopId, drinkData?.id ?: -1, viewModel.quantity, modifiers)
                .collectLatest(::collectAddToCart)
        }
    }

    /** Re-arms the CTA and puts the price back where the spinner was. */
    private fun rearmAddToCart() {
        if (!isAdding) return
        isAdding = false
        binding.order.revertAnimation()
        binding.order.isEnabled = true
        renderTotal()
    }

    private fun collectAddToCart(t: UIResource<CartData>) = t.collect(
        onError = { throwable ->
            rearmAddToCart()
            // A conflict is the one error that still needs the selection, to retry it after
            // clearing; anything else must not leave it armed for an unrelated 409 to pick up.
            if (throwable !is ConflictException) pendingCartModifiers = null
            // A failed add can still follow a successful clear-and-retry, which emptied the
            // cart server-side. Nothing else re-counts it now that this screen never leaves
            // the host, so the badge would keep showing the drinks that were just discarded.
            mainActivity?.refreshCartCount()
            checkErrors(throwable)
        }
    ) { data ->
        // Reverted before leaving rather than after: the morph needs the button still attached,
        // and it keeps the screen consistent if the pop turns out to be a no-op.
        rearmAddToCart()
        pendingCartModifiers = null
        // The host badges the cart tab straight from this response — its own onResume refresh
        // no longer fires now that this screen lives inside MainActivity.
        mainActivity?.onCartUpdated(data)
        // The pill hangs off the activity's content frame, not this fragment's view, so it stays
        // up over the menu the customer lands back on.
        showTopPill(R.string.cart_item_added)
        // The drink is in the cart; there is nothing left to do here. Going back also puts the
        // menu card that was just tapped in reach, now showing its stepper.
        popScreen()
    }

    /**
     * A 409 on add-to-cart means the cart already holds another shop's drinks — the only signal
     * the API gives for that. Emptying someone's cart is destructive, so it is offered rather
     * than done, and the original selection is retried afterwards.
     *
     * Any other 409 (nothing was being added) falls through to the default toast.
     */
    override fun onConflictException(message: String?, code: Int) {
        val modifiers = pendingCartModifiers
        pendingCartModifiers = null

        if (modifiers == null) {
            super.onConflictException(message, code)
            return
        }

        showRequestDF(
            getString(R.string.cart_different_shop_title),
            message?.takeIf { it.isNotBlank() }
                ?: getString(R.string.cart_different_shop_message),
            getString(R.string.cart_clear_and_add),
            getString(R.string.cancel)
        ) {
            launch {
                cartViewModel.clearCart().collectLatest { resource ->
                    resource.collect(
                        // Without this the customer approves "clear and add" and, if the clear
                        // fails, sees nothing happen at all.
                        onError = { throwable -> checkErrors(throwable) }
                    ) { sendAddToCart(modifiers) }
                }
            }
        }
    }

    private fun initModification(
        title: View,
        recyclerView: RecyclerView,
        adapter: SizeAdapter,
        modifications: List<OrderDetails.ModificationItem?>?
    ) {
        if (!modifications.isNullOrEmpty()) {
            val spanCount = if (modifications.size > 2) 2 else modifications.size
            (recyclerView.layoutManager as GridLayoutManager).spanCount = spanCount
            adapter.submitList(modifications)
            adapter.selectItem(modifications.firstOrNull()?.modificationId ?: "")
        } else {
            recyclerView.gone()
            title.gone()
        }
    }

    /** Re-prices the CTA whenever a modifier-group selection changes. */
    private fun onModifierSelectionChanged() {
        renderTotal()
    }

    private fun calculatePrice(): Double {
        if (usingGroups) {
            return (viewModel.defaultPrice ?: 0.0) + groupAdapter.totalModifierPrice()
        }

        var price = viewModel.defaultPrice ?: 0.0
        val size = sizeAdapter.getSelectedItem()
        val sugar = sugarAdapter.getSelectedItem()
        val milk = milkAdapter.getSelectedItem()
        val syrup = syrupAdapter.getSelectedItem()

        if (size != null) price += size.modificationPrice ?: 0.0
        if (sugar != null) price += sugar.modificationPrice ?: 0.0
        if (milk != null) price += milk.modificationPrice ?: 0.0
        if (syrup != null) price += syrup.modificationPrice ?: 0.0

        return price
    }

    override fun onPaymentException(
        errorData: PaymentRequiredExceptionData?,
        message: String?,
        code: Int
    ) {
        super.onPaymentException(errorData, message, code)
        navigateTo(SubscriptionsScreen())
    }

    override fun onPreconditionRequiredException(message: String?, code: Int) {
        super.onPreconditionRequiredException(message, code)
        navigateTo(PaymentServicesScreen())
    }

    override fun onUnauthorizedException(message: String?, code: Int) {
        super.onUnauthorizedException(message, code)
        val intent1 = Intent(requireContext(), AuthActivity::class.java)
        authListener.launch(intent1)
    }

    companion object {
        /** How far the stepper's minus fades once quantity is at its floor. */
        private const val DISABLED_ALPHA = 0.35f

        fun newInstance(
            shopId: Int,
            drink: DrinkItemData?,
            workingHours: ArrayList<ShopData.WorkHour>?,
        ) = OrderScreen().apply {
            arguments = bundleOf(
                SHOP_ID to shopId,
                DRINK_DATA to drink,
                Constants.WORKING_HOURS to workingHours,
            )
        }
    }

}

package uz.alphazet.hoopla.ui.order

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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
import uz.alphazet.data.models.order.OrderInfoData
import uz.alphazet.data.models.order.PaymentRequiredExceptionData
import uz.alphazet.domain.R
import uz.alphazet.domain.network.ConflictException
import uz.alphazet.domain.ui.BaseActivity
import uz.alphazet.domain.ui.showMessageDF
import uz.alphazet.domain.ui.showRequestDF
import uz.alphazet.domain.utils.Constants
import uz.alphazet.domain.utils.formatToPrice
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.visible
import uz.alphazet.hoopla.databinding.ScreenOrderBinding
import uz.alphazet.hoopla.ui.auth.AuthActivity
import uz.alphazet.hoopla.ui.cart.CartVM
import uz.alphazet.hoopla.ui.profile.payment.PaymentServicesActivity
import uz.alphazet.hoopla.ui.profile.subscriptions.SubscriptionActivity
import uz.alphazet.hoopla.ui.shop_details.ShopDetailActivity.Companion.DRINK_DATA
import uz.alphazet.hoopla.ui.shop_details.ShopDetailActivity.Companion.SHOP_ID
import uz.alphazet.hoopla.ui.shop_details.ShopDetailActivity.Companion.SHOP_NAME

class OrderActivity : BaseActivity() {

    private lateinit var binding: ScreenOrderBinding
    private val viewModel: OrderVM by viewModel()
    private val cartViewModel: CartVM by viewModel()

    private val shopId by lazy { intent.getIntExtra(SHOP_ID, -1) }
    private val shopName by lazy { intent.getStringExtra(SHOP_NAME) }
    private val drinkData by lazy { intent.getParcelableExtra<DrinkItemData>(DRINK_DATA) }

    /** Passed straight through to checkout, where it constrains the pickup-time picker. */
    private val workingHours by lazy {
        intent.getParcelableArrayListExtra<ShopData.WorkHour>(Constants.WORKING_HOURS)
    }

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

    private val authListener =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

        }

    private val subscriptionListener =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

        }

    private val checkoutListener =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == OrderActivity2.Companion.RESULT_ORDER_CREATED) {
                setResult(result.resultCode)
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ScreenOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        launch {
            viewModel.validateOrder(shopId, drinkData?.id ?: -1).collectLatest(::collectDetail)
        }

        binding.sizes.adapter = sizeAdapter
        binding.sugars.adapter = sugarAdapter
        binding.milkTypes.adapter = milkAdapter
        binding.syrups.adapter = syrupAdapter
        binding.modifierGroupsRv.adapter = groupAdapter

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.addToCart.setOnClickListener { onAddToCartClicked() }

        sizeAdapter.setOnItemClickListener { item ->
            sizeAdapter.selectItem(item.modificationId ?: "")
            val price = calculatePrice()
            binding.order.text = price.formatToPrice().plus(" UZS")
        }
        sugarAdapter.setOnItemClickListener { item ->
            sugarAdapter.selectItem(item.modificationId ?: "")
            val price = calculatePrice()
            binding.order.text = price.formatToPrice().plus(" UZS")
        }
        milkAdapter.setOnItemClickListener { item ->
            milkAdapter.selectItem(item.modificationId ?: "")
            val price = calculatePrice()
            binding.order.text = price.formatToPrice().plus(" UZS")
        }
        syrupAdapter.setOnItemClickListener { item ->
            syrupAdapter.selectItem(item.modificationId ?: "")
            val price = calculatePrice()
            binding.order.text = price.formatToPrice().plus(" UZS")
        }

    }

    override fun updateStatusBarViewHeight() {
        launch {
            val statusBarHeight = getStatusBarHeight()
            binding.statusBarView.layoutParams.height = statusBarHeight
            binding.statusBarView.requestLayout()
        }
    }

    private fun collectDetail(t: UIResource<OrderDetails>) = t.collect { data ->
        binding.image.load(data?.drink?.imageUrl)
        binding.name.text = data?.drink?.name
        binding.cafeName.text = getString(R.string.label_by_shop, data?.shop?.name)
        viewModel.defaultPrice = data?.drink?.amount

        val groups = data?.modifierGroups
        if (data != null && !groups.isNullOrEmpty()) {
            setupGroups(data, groups)
        } else {
            setupLegacy(data)
        }
    }

    /** New flow: render the named modifier groups with their min/max rules. */
    private fun setupGroups(data: OrderDetails, groups: List<ModifierGroupData>) {
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
        binding.order.text = calculatePrice().formatToPrice().plus(" UZS")

        binding.order.setOnClickListener {
            val unmet = groupAdapter.firstUnsatisfiedGroup()
            if (unmet != null) {
                showErrorMessage(
                    getString(
                        R.string.modifier_select_at_least,
                        unmet.minSelect,
                        unmet.name?.takeIf { it.isNotBlank() } ?: unmet.key
                    )
                )
                return@setOnClickListener
            }

            val modifiers = groupAdapter.buildModifiers()
            val intent1 = Intent(this, CheckoutActivity::class.java)
            intent1.putExtra(Constants.DATA, data)
            intent1.putExtra(Constants.MODIFIERS, modifiers)
            intent1.putExtra(Constants.COMMENT, binding.commentInput.text?.toString()?.trim())
            intent1.putParcelableArrayListExtra(Constants.WORKING_HOURS, workingHours)
            checkoutListener.launch(intent1)
        }
    }

    /** Legacy flow: the fixed size/sugar/milk/syrup sections read from the modifications map. */
    private fun setupLegacy(data: OrderDetails?) {
        usingGroups = false
        binding.modifierGroupsRv.gone()
        binding.order.text = data?.drink?.amount?.formatToPrice().plus(" UZS")

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

        val price = calculatePrice()
        binding.order.text = price.formatToPrice().plus(" UZS")

        binding.order.setOnClickListener {
            val modifiers = buildLegacyModifiers()

            val intent1 = Intent(this, CheckoutActivity::class.java)
            intent1.putExtra(Constants.DATA, data)
            intent1.putExtra(Constants.MODIFIERS, modifiers)
            intent1.putExtra(Constants.COMMENT, binding.commentInput.text?.toString()?.trim())
            intent1.putParcelableArrayListExtra(Constants.WORKING_HOURS, workingHours)
            checkoutListener.launch(intent1)
        }

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
        if (!binding.addToCart.isEnabled) return
        pendingCartModifiers = modifiers
        binding.addToCart.isEnabled = false
        launch {
            cartViewModel.addItem(shopId, drinkData?.id ?: -1, 1, modifiers)
                .collectLatest(::collectAddToCart)
        }
    }

    private fun collectAddToCart(t: UIResource<CartData>) = t.collect(
        onError = { throwable ->
            binding.addToCart.isEnabled = true
            // A conflict is the one error that still needs the selection, to retry it after
            // clearing; anything else must not leave it armed for an unrelated 409 to pick up.
            if (throwable !is ConflictException) pendingCartModifiers = null
            checkErrors(throwable)
        }
    ) {
        binding.addToCart.isEnabled = true
        pendingCartModifiers = null
        Toast.makeText(this, getString(R.string.cart_item_added), Toast.LENGTH_SHORT).show()
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

    private fun collectData(t: UIResource<OrderInfoData>) = t.collect { data ->
        showMessageDF(
            getString(R.string.order_received_),
            getString(R.string.label_order_received_, data?.drinkName ?: "", data?.shopName ?: ""),
            "OK"
        ) {
            setResult(RESULT_ORDER_CREATED)
            finish()
        }
    }

    /** Re-prices the order button whenever a modifier-group selection changes. */
    private fun onModifierSelectionChanged() {
        binding.order.text = calculatePrice().formatToPrice().plus(" UZS")
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

    override fun showLoading() {
        binding.order.startAnimation()
    }

    override fun hideLoading() {
        binding.order.revertAnimation()
    }

    override fun onPaymentException(
        errorData: PaymentRequiredExceptionData?,
        message: String?,
        code: Int
    ) {
        super.onPaymentException(errorData, message, code)
        val intent1 = Intent(this, SubscriptionActivity::class.java)
        subscriptionListener.launch(intent1)
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

    companion object {
        const val RESULT_ORDER_CREATED = 203
    }

}
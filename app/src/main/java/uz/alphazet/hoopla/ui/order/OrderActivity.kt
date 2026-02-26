package uz.alphazet.hoopla.ui.order

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.DrinkItemData
import uz.alphazet.data.models.order.ModifierItemData
import uz.alphazet.data.models.order.OrderDetails
import uz.alphazet.data.models.order.OrderInfoData
import uz.alphazet.data.models.order.PaymentRequiredExceptionData
import uz.alphazet.domain.R
import uz.alphazet.domain.ui.BaseActivity
import uz.alphazet.domain.ui.showMessageDF
import uz.alphazet.domain.utils.Constants
import uz.alphazet.domain.utils.formatToPrice
import uz.alphazet.domain.utils.gone
import uz.alphazet.hoopla.databinding.ScreenOrderBinding
import uz.alphazet.hoopla.ui.auth.AuthActivity
import uz.alphazet.hoopla.ui.profile.payment.PaymentServicesActivity
import uz.alphazet.hoopla.ui.profile.subscriptions.SubscriptionActivity
import uz.alphazet.hoopla.ui.shop_details.ShopDetailActivity.Companion.DRINK_DATA
import uz.alphazet.hoopla.ui.shop_details.ShopDetailActivity.Companion.SHOP_ID
import uz.alphazet.hoopla.ui.shop_details.ShopDetailActivity.Companion.SHOP_NAME

class OrderActivity : BaseActivity() {

    private lateinit var binding: ScreenOrderBinding
    private val viewModel: OrderVM by viewModel()

    private val shopId by lazy { intent.getIntExtra(SHOP_ID, -1) }
    private val shopName by lazy { intent.getStringExtra(SHOP_NAME) }
    private val drinkData by lazy { intent.getParcelableExtra<DrinkItemData>(DRINK_DATA) }

    private val sizeAdapter = SizeAdapter()
    private val sugarAdapter = SizeAdapter()
    private val milkAdapter = ModificationAdapter()
    private val syrupAdapter = ModificationAdapter()

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

        binding.toolbar.setNavigationOnClickListener { finish() }

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
//            milkAdapter.selectItem(milkItems.firstOrNull()?.modificationId ?: "")
        }

        val syrupItems = data?.modifications?.syrup
        if (syrupItems.isNullOrEmpty()) {
            binding.syrups.gone()
            binding.syrupTitle.gone()
        } else {
            syrupAdapter.submitList(syrupItems)
//            syrupAdapter.selectItem(syrupItems.firstOrNull()?.modificationId ?: "")
        }

        val price = calculatePrice()
        binding.order.text = price.formatToPrice().plus(" UZS")

        binding.order.setOnClickListener {
            val modifiers = ArrayList<ModifierItemData>()

            val size = sizeAdapter.getSelectedItem()
            if (size != null) {
                modifiers.add(
                    ModifierItemData(
                        size.modificationId ?: "",
                        size.modificationGroupId,
                        size.modificationKey ?: "",
                        size.modificationPrice ?: 0.0,
                        size.modificationName
                    )
                )
            }

            val sugar = sugarAdapter.getSelectedItem()
            if (sugar != null) {
                modifiers.add(
                    ModifierItemData(
                        sugar.modificationId ?: "",
                        sugar.modificationGroupId,
                        sugar.modificationKey ?: "",
                        sugar.modificationPrice ?: 0.0,
                        sugar.modificationName
                    )
                )
            }

            val milk = milkAdapter.getSelectedItem()
            if (milk != null) {
                modifiers.add(
                    ModifierItemData(
                        milk.modificationId ?: "",
                        milk.modificationGroupId,
                        milk.modificationKey ?: "",
                        milk.modificationPrice ?: 0.0,
                        milk.modificationName
                    )
                )
            }

            val syrup = syrupAdapter.getSelectedItem()
            if (syrup != null) {
                modifiers.add(
                    ModifierItemData(
                        syrup.modificationId ?: "",
                        syrup.modificationGroupId,
                        syrup.modificationKey ?: "",
                        syrup.modificationPrice ?: 0.0,
                        syrup.modificationName
                    )
                )
            }

            val intent1 = Intent(this, CheckoutActivity::class.java)
            intent1.putExtra(Constants.DATA, data)
            intent1.putExtra(Constants.MODIFIERS, modifiers)
            checkoutListener.launch(intent1)
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

    private fun calculatePrice(): Double {
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
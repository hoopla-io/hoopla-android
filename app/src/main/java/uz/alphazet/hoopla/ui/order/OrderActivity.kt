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
import uz.alphazet.domain.R
import uz.alphazet.domain.ui.BaseActivity
import uz.alphazet.domain.ui.showMessageDF
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
    private val milkAdapter = SizeAdapter()
    private val syrupAdapter = SizeAdapter()

    private val authListener =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

        }

    private val subscriptionListener =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

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

        binding.order.setOnClickListener {
            val modifiers = ArrayList<ModifierItemData>()

            val size = sizeAdapter.getSelectedItemItem()
            if (size != null) {
                modifiers.add(
                    ModifierItemData(
                        size.modificationId ?: "",
                        size.modificationGroupId,
                        size.modificationKey ?: "",
                        size.modificationPrice ?: 0.0
                    )
                )
            }

            val sugar = sugarAdapter.getSelectedItemItem()
            if (sugar != null) {
                modifiers.add(
                    ModifierItemData(
                        sugar.modificationId ?: "",
                        sugar.modificationGroupId,
                        sugar.modificationKey ?: "",
                        sugar.modificationPrice ?: 0.0
                    )
                )
            }

            launch {
                viewModel.createOrder(shopId, drinkData?.id ?: -1, modifiers)
                    .collectLatest(::collectData)
            }
        }

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
//        binding.time.text = (System.currentTimeMillis() / 1000L).getDateDMMMMYYYYHHmm()
//        binding.shopName.text = data?.shop?.name
//        binding.drinksName.text = data?.drink?.name
        viewModel.defaultPrice = data?.drink?.amount
        binding.order.text = data?.drink?.amount?.formatToPrice().plus(" UZS")

        initModification(
            binding.sugarTitle,
            binding.sugars,
            sugarAdapter,
            data?.modifications?.sugar
        )
        initModification(binding.sizeTitle, binding.sizes, sizeAdapter, data?.modifications?.size)
        initModification(
            binding.milkTitle,
            binding.milkTypes,
            milkAdapter,
            data?.modifications?.milk
        )
        initModification(
            binding.syrupTitle,
            binding.syrups,
            syrupAdapter,
            data?.modifications?.syrup
        )

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
        val size = sizeAdapter.getSelectedItemItem()
        val sugar = sugarAdapter.getSelectedItemItem()

        if (size != null) price += size.modificationPrice ?: 0.0
        if (sugar != null) price += sugar.modificationPrice ?: 0.0

        return price
    }

    override fun showLoading() {
        binding.order.startAnimation()
    }

    override fun hideLoading() {
        binding.order.revertAnimation()
    }

    override fun onPaymentException(message: String?, code: Int) {
        super.onPaymentException(message, code)
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
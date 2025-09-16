package uz.alphazet.hoopla.ui.order

import android.content.Intent
import android.os.Bundle
import android.widget.RadioButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.children
import coil3.load
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.DrinkItemData
import uz.alphazet.data.models.order.OrderDetails
import uz.alphazet.data.models.order.OrderInfoData
import uz.alphazet.domain.R
import uz.alphazet.domain.ui.BaseActivity
import uz.alphazet.domain.ui.showMessageDF
import uz.alphazet.domain.utils.formatToPrice
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.visible
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

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.order.setOnClickListener {
            var addOnId: String? = null
            if (binding.addOnRg.childCount > 0) {
                val buttonId = binding.addOnRg.checkedRadioButtonId
                binding.addOnRg.children.forEach { item ->
                    if (item.id == buttonId) {
                        addOnId = (item as RadioButton).tag.toString()
                    }
                }
            }
            launch {
                viewModel.createOrder(shopId, drinkData?.id ?: -1, addOnId)
                    .collectLatest(::collectData)
            }
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
        binding.toolbar.title = data?.drink?.name
//        binding.time.text = (System.currentTimeMillis() / 1000L).getDateDMMMMYYYYHHmm()
        binding.shopName.text = data?.shop?.name
        binding.drinksName.text = data?.drink?.name
        binding.price.text = data?.drink?.amount?.formatToPrice().plus(" UZS")

        val addOns = data?.addOns
        binding.addOnRg.removeAllViews()

        if (addOns.isNullOrEmpty()) {
            binding.addOnRg.gone()
            binding.addOnTitle.gone()
        } else {
            binding.addOnRg.visible()
            binding.addOnTitle.visible()

            addOns.forEachIndexed { index, item ->
                val radioButton =
                    layoutInflater.inflate(
                        uz.alphazet.hoopla.R.layout.item_add_on,
                        null
                    ) as? RadioButton
                radioButton?.id = index
                radioButton?.text = item?.addOn
                radioButton?.tag = item?.vendorAddOnId
                binding.addOnRg.addView(radioButton)
            }

            (binding.addOnRg.children.firstOrNull() as? RadioButton?)?.isChecked = true

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
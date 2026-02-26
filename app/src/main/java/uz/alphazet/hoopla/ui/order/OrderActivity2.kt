package uz.alphazet.hoopla.ui.order

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatButton
import coil3.load
import coil3.request.transformations
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
import uz.alphazet.domain.utils.BlurTransformation
import uz.alphazet.domain.utils.formatToPrice
import uz.alphazet.hoopla.databinding.ScreenOrder2Binding
import uz.alphazet.hoopla.ui.auth.AuthActivity
import uz.alphazet.hoopla.ui.profile.payment.PaymentServicesActivity
import uz.alphazet.hoopla.ui.profile.subscriptions.SubscriptionActivity
import uz.alphazet.hoopla.ui.shop_details.ShopDetailActivity.Companion.DRINK_DATA
import uz.alphazet.hoopla.ui.shop_details.ShopDetailActivity.Companion.SHOP_ID
import uz.alphazet.hoopla.ui.shop_details.ShopDetailActivity.Companion.SHOP_NAME

class OrderActivity2 : BaseActivity() {

    private lateinit var binding: ScreenOrder2Binding
    private val viewModel: OrderVM by viewModel()

    private val shopId by lazy { intent.getIntExtra(SHOP_ID, -1) }
    private val shopName by lazy { intent.getStringExtra(SHOP_NAME) }
    private val drinkData by lazy { intent.getParcelableExtra<DrinkItemData>(DRINK_DATA) }

    private val adapter = ModificationTypeAdapter()

    private val authListener =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

        }

    private val subscriptionListener =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ScreenOrder2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        launch {
            viewModel.validateOrder(shopId, drinkData?.id ?: -1).collectLatest(::collectDetail)
        }

        binding.modificationsRv.adapter = adapter

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.order.setOnClickListener {
            val modifiers = ArrayList<ModifierItemData>()

            launch {
                viewModel.createOrder(shopId, drinkData?.id ?: -1, modifiers)
                    .collectLatest(::collectData)
            }
        }

        binding.sizeSelector.setOnOptionSelectedListener { item ->
            val summa = (viewModel.defaultPrice ?: 0.0) + (item.modificationPrice ?: 0.0)
            binding.order.text = summa.formatToPrice().plus(" UZS")
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
        binding.bgImage.load(data?.drink?.imageUrl) {
            transformations(BlurTransformation(binding.root.context, 12F, 10F))
        }
        binding.toolbar.title = data?.drink?.name
        viewModel.defaultPrice = data?.drink?.amount
        binding.order.text = data?.drink?.amount?.formatToPrice().plus(" UZS")

        val modificationTypes = ArrayList<ModificationType>()
        modificationTypes.add(
            ModificationType(
                "Sugar",
                R.drawable.sugar_cube,
                data?.modifications?.sugar
            )
        )
        modificationTypes.add(
            ModificationType(
                "Milk",
                R.drawable.ic_milk,
                data?.modifications?.milk
            )
        )
        modificationTypes.add(
            ModificationType(
                "Syrup",
                R.drawable.ic_syrup,
                data?.modifications?.syrup
            )
        )

        adapter.submitList(modificationTypes)

        val sizes = ArrayList<OrderDetails.ModificationItem>()
        sizes.add(OrderDetails.ModificationItem("s", "s", "s", "s", 0.0))
        sizes.add(OrderDetails.ModificationItem("m", "m", "m", "m", 5000.0))
        sizes.add(OrderDetails.ModificationItem("l", "l", "l", "l", 8000.0))

        binding.sizeSelector.setOptions(sizes)

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

    private fun animateButtonText(button: AppCompatButton, newText: String) {
        // Eski text chiqib ketadi
        button.animate()
            .alpha(0f)
            .translationY(-10f)
            .setDuration(150)
            .withEndAction {
                button.text = newText

                // Yangi text kirib keladi
                button.translationY = 10f
                button.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(150)
                    .start()
            }
            .start()
    }


    companion object {
        const val RESULT_ORDER_CREATED = 203
    }

}
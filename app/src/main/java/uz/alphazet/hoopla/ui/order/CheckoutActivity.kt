package uz.alphazet.hoopla.ui.order

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import coil3.load
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.UserData
import uz.alphazet.data.models.order.CheckOutInfo
import uz.alphazet.data.models.order.ModifierItemData
import uz.alphazet.data.models.order.OrderDetails
import uz.alphazet.data.models.order.PaymentRequiredExceptionData
import uz.alphazet.domain.R
import uz.alphazet.domain.ui.BaseActivity
import uz.alphazet.domain.ui.showMessageDF
import uz.alphazet.domain.utils.Constants
import uz.alphazet.domain.utils.formatToPrice
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.intentToBrowser
import uz.alphazet.domain.utils.visible
import uz.alphazet.hoopla.databinding.ScreenCheckoutBinding
import uz.alphazet.hoopla.ui.auth.AuthActivity
import uz.alphazet.hoopla.ui.order.OrderActivity2.Companion.RESULT_ORDER_CREATED
import uz.alphazet.hoopla.ui.order.SelectCashbackSummaBD.Companion.showSelectCashbackSummaBD
import uz.alphazet.hoopla.ui.profile.payment.PaymentServicesActivity

class CheckoutActivity : BaseActivity() {

    private lateinit var binding: ScreenCheckoutBinding
    private val viewModel: OrderVM by viewModel()
    private val orderData by lazy { intent.getParcelableExtra<OrderDetails>(Constants.DATA) }
    private val modifiers by lazy { intent.getParcelableArrayListExtra<ModifierItemData>(Constants.MODIFIERS) }
    private val comment by lazy { intent.getStringExtra(Constants.COMMENT) }

    private val adapter = SummaInfoAdapter()
    private var usingCashBack = 0.0

    private val authListener =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

        }

    private val subscriptionListener =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ScreenCheckoutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.image.load(orderData?.drink?.imageUrl)
        binding.name.text = orderData?.drink?.name
        binding.name2.text = orderData?.drink?.name
        binding.cafeName.text = getString(R.string.label_by_shop, orderData?.shop?.name)
        binding.drinkSumma.text = orderData?.drink?.amount?.formatToPrice().plus(" UZS")
//        binding.used.text = usingCashBack.formatToPrice().plus(" UZS")

        binding.infoRv.adapter = adapter

        adapter.submitList(modifiers)

        binding.totalSumma.text = calculatePrice().formatToPrice().plus(" UZS")

        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.order.setOnClickListener {
            launch {
                modifiers?.let { list ->
                    viewModel.createOrderRahmat(
                        orderData?.shop?.id ?: -1,
                        orderData?.drink?.id ?: -1,
                        list,
                        usingCashBack > 0,
                        usingCashBack,
                        comment
                    ).collectLatest(::collectData)
                }
            }
        }

        launch {
            viewModel.userDataFlow.collectLatest(::collectUserData)
        }
    }

    private fun collectData(t: UIResource<CheckOutInfo>) = t.collect { data ->
        showMessageDF(
            getString(R.string.order_received_),
            getString(
                R.string.label_order_received_,
                orderData?.drink?.name ?: "",
                orderData?.shop?.name ?: ""
            ),
            "OK"
        ) {
            setResult(RESULT_ORDER_CREATED)
            finish()
        }
    }

    private fun collectUserData(t: UIResource<UserData>) = t.collect { userData ->
        binding.useCashback.isEnabled = (userData?.balance ?: 0.0) > 0.0
        binding.available.text = userData?.balance?.formatToPrice().plus(" UZS")

        binding.useCashback.setOnCheckedChangeListener { view, isChecked ->
            if (isChecked) {
                orderData?.let { data ->
                    var maxLimit = orderData?.drink?.amount ?: 0.0

                    modifiers?.forEach { (_, _, _, modifierPrice, _) ->
                        maxLimit += modifierPrice
                    }
                    showSelectCashbackSummaBD(data, userData, maxLimit, usingCashBack) { summa ->
                        usingCashBack = summa
                        binding.totalSumma.text = calculatePrice().formatToPrice().plus(" UZS")
                    }
                }
            } else {
                usingCashBack = 0.0
                binding.totalSumma.text = calculatePrice().formatToPrice().plus(" UZS")
            }
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

    override fun updateStatusBarViewHeight() {
        launch {
            val statusBarHeight = getStatusBarHeight()
            binding.statusBarView.layoutParams.height = statusBarHeight
            binding.statusBarView.requestLayout()
        }
    }

    private fun calculatePrice(): Double {
        var price = orderData?.drink?.amount ?: 0.0

        modifiers?.forEach { (_, _, _, modifierPrice, _) ->
            price += modifierPrice
        }

        if (usingCashBack > 0) {
            price -= usingCashBack
            binding.usedCashback.text = "-".plus(usingCashBack.formatToPrice()).plus(" UZS")
            binding.usedCashbackContainer.visible()
        } else {
            binding.useCashback.isChecked = false
            binding.usedCashbackContainer.gone()
        }

        updateEarnCashback(price)

        return price
    }

    /**
     * Shows how much cashback this order earns: [CheckoutActivity]'s payable total
     * multiplied by the shop's [OrderDetails.cashbackPercent]. Hidden when the shop
     * gives no cashback.
     */
    private fun updateEarnCashback(total: Double) {
        val percent = orderData?.cashbackPercent ?: 0f
        if (percent <= 0f) {
            binding.earnCashbackContainer.gone()
            return
        }
        val earned = total * percent / 100.0
        binding.earnCashbackLabel.text = getString(R.string.label_earn_cashback, percent.formatPercent())
        binding.earnCashback.text = "+".plus(earned.formatToPrice()).plus(" UZS")
        binding.earnCashbackContainer.visible()
    }

    private fun Float.formatPercent(): String =
        if (this % 1f == 0f) toInt().toString() else toString()

    override fun showLoading() {
        binding.order.startAnimation()
    }

    override fun hideLoading() {
        binding.order.revertAnimation()
    }

}
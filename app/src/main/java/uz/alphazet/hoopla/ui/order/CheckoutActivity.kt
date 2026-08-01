package uz.alphazet.hoopla.ui.order

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import coil3.load
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.ShopData
import uz.alphazet.data.models.UserData
import uz.alphazet.data.models.order.CheckOutInfo
import uz.alphazet.data.models.order.ModifierItemData
import uz.alphazet.data.models.order.OrderDetails
import uz.alphazet.data.models.order.PaymentRequiredExceptionData
import uz.alphazet.data.models.order.PromocodeData
import uz.alphazet.domain.R
import uz.alphazet.domain.ui.BaseActivity
import uz.alphazet.domain.ui.showMessageDF
import uz.alphazet.domain.utils.Constants
import uz.alphazet.domain.utils.PickupTime
import uz.alphazet.domain.utils.formatToPrice
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.intentToBrowser
import uz.alphazet.domain.utils.setTextColorRes
import uz.alphazet.domain.utils.visible
import uz.alphazet.hoopla.databinding.ScreenCheckoutBinding
import uz.alphazet.hoopla.ui.auth.AuthActivity
import uz.alphazet.hoopla.ui.order.InputPromocodeBD.Companion.showInputPromocodeBD
import uz.alphazet.hoopla.ui.order.OrderActivity2.Companion.RESULT_ORDER_CREATED
import uz.alphazet.hoopla.ui.order.SelectCashbackSummaBD.Companion.showSelectCashbackSummaBD
import uz.alphazet.hoopla.ui.order.SelectPickupTimeBD.Companion.showSelectPickupTimeBD
import uz.alphazet.hoopla.ui.profile.payment.PaymentServicesActivity

class CheckoutActivity : BaseActivity() {

    private lateinit var binding: ScreenCheckoutBinding
    private val viewModel: OrderVM by viewModel()
    private val orderData by lazy { intent.getParcelableExtra<OrderDetails>(Constants.DATA) }
    private val modifiers by lazy { intent.getParcelableArrayListExtra<ModifierItemData>(Constants.MODIFIERS) }
    private val comment by lazy { intent.getStringExtra(Constants.COMMENT) }
    private val workingHours by lazy {
        intent.getParcelableArrayListExtra<ShopData.WorkHour>(Constants.WORKING_HOURS)
    }

    private val adapter = SummaInfoAdapter()
    private var usingCashBack = 0.0

    /** The promocode the customer validated and applied; null when none. */
    private var appliedPromo: PromocodeData? = null

    /**
     * The scheduled pickup instant in epoch millis, or null for the default ASAP order —
     * which is how every order behaved before scheduled pickup existed.
     */
    private var pickupAtMillis: Long? = null

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

        binding.promoAddRow.setOnClickListener { openPromoDialog() }
        binding.promoAppliedRow.setOnClickListener { openPromoDialog() }
        binding.removePromo.setOnClickListener { removePromo() }

        binding.pickupRow.setOnClickListener { openPickupTimeDialog() }
        updatePickupRow()

        binding.order.setOnClickListener {
            launch {
                modifiers?.let { list ->
                    // The picker may have been open a while; re-check the lead time so an
                    // obviously stale slot is caught here instead of by a server round-trip.
                    val pickup = pickupAtMillis
                    if (pickup != null && !PickupTime.isSelectable(pickup, workingHours)) {
                        onPickupTimeRejected(getString(R.string.pickup_time_unavailable))
                        return@launch
                    }

                    viewModel.createOrderRahmat(
                        orderData?.shop?.id ?: -1,
                        orderData?.drink?.id ?: -1,
                        list,
                        usingCashBack > 0,
                        usingCashBack,
                        comment,
                        appliedPromo?.code,
                        pickup?.let { PickupTime.format(it) }
                    ).collectLatest(::collectData)
                }
            }
        }

        launch {
            viewModel.userDataFlow.collectLatest(::collectUserData)
        }
    }

    /** Opens the pickup-time picker; the sheet reports null when the customer keeps ASAP. */
    private fun openPickupTimeDialog() {
        showSelectPickupTimeBD(workingHours, pickupAtMillis) { millis ->
            pickupAtMillis = millis
            updatePickupRow()
        }
    }

    /**
     * Mirrors the current choice into the control beside the order button. Labels are the
     * compact variants — it shares that row with the primary action.
     */
    private fun updatePickupRow() {
        val millis = pickupAtMillis
        binding.pickupValue.text = when {
            millis == null -> getString(R.string.pickup_asap_short)
            PickupTime.dayOffset(millis) <= 0 ->
                getString(R.string.pickup_today_at, PickupTime.formatForDisplay(millis))

            else -> getString(R.string.pickup_tomorrow_at, PickupTime.formatForDisplay(millis))
        }

        // The row stays quiet on the default and only picks up colour once the customer has
        // actually scheduled something — that is the part worth noticing before paying.
        binding.pickupValue.setTextColorRes(
            if (millis == null) R.color.grey_400 else R.color.primary_300
        )
    }

    /**
     * A scheduled time the shop can no longer honour. Every reason the backend gives is
     * customer-actionable — pick another time — so the message is shown as-is and the picker
     * is reopened rather than dropping the customer back on a screen that just failed.
     */
    private fun onPickupTimeRejected(message: String?) {
        pickupAtMillis = null
        updatePickupRow()
        showMessageDF(
            getString(R.string.pickup_time_title),
            // A blank reason would leave the dialog with nothing but a title.
            message?.takeIf { it.isNotBlank() } ?: getString(R.string.pickup_time_unavailable),
            "OK"
        ) {
            openPickupTimeDialog()
        }
    }

    /**
     * The backend answers 409 when the pickup time is too soon, too far ahead, or outside the
     * shop's hours. Hours and pause state can change between populating the picker and
     * checking out, so this stays reachable even with the client-side constraints.
     */
    override fun onConflictException(message: String?, code: Int) {
        if (pickupAtMillis != null) onPickupTimeRejected(message)
        else super.onConflictException(message, code)
    }

    /** Opens the promocode input dialog (pre-filled when a code is already applied). */
    private fun openPromoDialog() {
        showInputPromocodeBD(
            orderData?.shop?.id ?: -1,
            orderData?.drink?.id ?: -1,
            modifiers ?: arrayListOf(),
            appliedPromo?.code
        ) { data -> applyPromo(data) }
    }

    /** Switches the promocode card into its "applied" state and re-prices the order. */
    private fun applyPromo(data: PromocodeData) {
        appliedPromo = data

        // A freshly applied discount may now exceed the bill — trim the cashback to fit.
        val afterPromo = subtotal() - (data.discountAmount ?: 0.0)
        if (usingCashBack > afterPromo) {
            usingCashBack = afterPromo.coerceAtLeast(0.0)
            updateCashbackRow()
        }

        binding.promoAddRow.gone()
        binding.promoAppliedRow.visible()
        binding.promoAppliedCode.text = data.code
        binding.promoAppliedDiscount.text =
            "-".plus((data.discountAmount ?: 0.0).formatToPrice()).plus(" UZS")

        binding.totalSumma.text = calculatePrice().formatToPrice().plus(" UZS")
    }

    private fun removePromo() {
        appliedPromo = null
        binding.promoAppliedRow.gone()
        binding.promoAddRow.visible()
        binding.totalSumma.text = calculatePrice().formatToPrice().plus(" UZS")
    }

    private fun collectData(t: UIResource<CheckOutInfo>) = t.collect { data ->
        val received = getString(
            R.string.label_order_received_,
            orderData?.drink?.name ?: "",
            orderData?.shop?.name ?: ""
        )

        // An ASAP order and a 17:30 order look identical at this point, so confirm the
        // scheduled time back. The instant the backend echoed wins over the local choice.
        val pickup = PickupTime.parse(data?.pickup_at) ?: pickupAtMillis
        val message =
            if (pickup == null) received
            else received + "\n\n" + getString(
                R.string.pickup_at_value,
                PickupTime.formatDayTimeForDisplay(pickup)
            )

        showMessageDF(getString(R.string.order_received_), message, "OK") {
            setResult(RESULT_ORDER_CREATED)
            finish()
        }
    }

    private fun collectUserData(t: UIResource<UserData>) = t.collect { userData ->
        val balance = userData?.balance ?: 0.0
        binding.useCashback.isEnabled = balance > 0.0
        binding.cashbackAvailable.text =
            getString(R.string.cashback_available, balance.formatToPrice().plus(" UZS"))

        binding.useCashback.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) openCashbackSelector(userData) else clearCashback()
        }
        binding.cashbackChange.setOnClickListener { openCashbackSelector(userData) }
    }

    /** Opens the amount picker; cashback can only cover what's left after the promocode. */
    private fun openCashbackSelector(userData: UserData?) {
        val data = orderData ?: return
        val maxLimit = (subtotal() - (appliedPromo?.discountAmount ?: 0.0)).coerceAtLeast(0.0)
        showSelectCashbackSummaBD(data, userData, maxLimit, usingCashBack) { summa ->
            usingCashBack = summa
            updateCashbackRow()
            binding.totalSumma.text = calculatePrice().formatToPrice().plus(" UZS")
        }
    }

    private fun clearCashback() {
        usingCashBack = 0.0
        updateCashbackRow()
        binding.totalSumma.text = calculatePrice().formatToPrice().plus(" UZS")
    }

    /** Shows or hides the "using X cashback" row inside the cashback card. */
    private fun updateCashbackRow() {
        if (usingCashBack > 0) {
            binding.cashbackUsing.text =
                getString(R.string.cashback_using, usingCashBack.formatToPrice().plus(" UZS"))
            binding.cashbackAmountRow.visible()
        } else {
            binding.cashbackAmountRow.gone()
        }
    }

    /** Drink price plus every selected modifier — before promocode and cashback. */
    private fun subtotal(): Double {
        var sum = orderData?.drink?.amount ?: 0.0
        modifiers?.forEach { (_, _, _, modifierPrice, _) ->
            sum += modifierPrice
        }
        return sum
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
        var price = subtotal()

        // The promocode reduces the order total first.
        val promoDiscount = appliedPromo?.discountAmount ?: 0.0
        if (promoDiscount > 0) {
            price -= promoDiscount
            binding.promoDiscount.text = "-".plus(promoDiscount.formatToPrice()).plus(" UZS")
            binding.promoDiscountContainer.visible()
        } else {
            binding.promoDiscountContainer.gone()
        }
        if (price < 0) price = 0.0

        // Then cashback comes off what's left.
        if (usingCashBack > 0) {
            price -= usingCashBack
            binding.usedCashback.text = "-".plus(usingCashBack.formatToPrice()).plus(" UZS")
            binding.usedCashbackContainer.visible()
        } else {
            binding.useCashback.isChecked = false
            binding.usedCashbackContainer.gone()
        }
        if (price < 0) price = 0.0

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
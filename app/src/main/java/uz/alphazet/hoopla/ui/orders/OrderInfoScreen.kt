package uz.alphazet.hoopla.ui.orders

import android.os.Bundle
import androidx.core.graphics.Insets
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import coil3.load
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.FeedbackDetail
import uz.alphazet.data.models.order.ModifierItemData
import uz.alphazet.data.models.order.OrderInfo
import uz.alphazet.data.models.order.OrderStatus
import uz.alphazet.domain.R
import uz.alphazet.domain.ui.BaseActivity
import uz.alphazet.domain.ui.showMessageDF
import uz.alphazet.domain.ui.showRequestDF
import uz.alphazet.domain.utils.Constants
import uz.alphazet.domain.utils.formatToPrice
import uz.alphazet.domain.utils.getDateDDMMMMYYYYHHmm
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.intentToBrowser
import uz.alphazet.domain.utils.setTextColorRes
import uz.alphazet.domain.utils.setTextStringRes
import uz.alphazet.domain.utils.visible
import uz.alphazet.hoopla.databinding.ScreenOrderInfoBinding
import uz.alphazet.hoopla.ui.home.FeedbackBD.Companion.showFeedbackBD
import uz.alphazet.hoopla.ui.order.SummaInfoAdapter
import uz.alphazet.hoopla.ui.orders.OrderQrCodeBD.Companion.showOrderQrCodeBD

class OrderInfoScreen : BaseActivity(), SwipeRefreshLayout.OnRefreshListener {

    private lateinit var binding: ScreenOrderInfoBinding
    private val viewModel: OrdersVM by viewModel()
    private val adapter = SummaInfoAdapter()
    private val orderId by lazy { intent.getIntExtra(Constants.ID, -1) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ScreenOrderInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.getOrderInfo(orderId)

        launch {
            viewModel.orderInfoFlow.collectLatest(::collectData)
        }
        binding.infoRv.adapter = adapter
        binding.swipeRefreshLayout.setOnRefreshListener(this)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    override fun onRefresh() {
        viewModel.getOrderInfo(orderId)
    }

    override fun showLoading() {
        binding.swipeRefreshLayout.isRefreshing = true
    }

    override fun hideLoading() {
        binding.swipeRefreshLayout.isRefreshing = false
    }

    private suspend fun collectData(t: UIResource<OrderInfo>) = t.collect { orderInfo ->
        if (orderInfo == null) return@collect
        binding.toolbar.title = orderInfo.purchasedAtUnix?.getDateDDMMMMYYYYHHmm()
        binding.image.load(orderInfo.drinkImageUrl)
        binding.shopName.text = getString(R.string.label_by_shop, orderInfo.shopName)
        binding.name.text = orderInfo.drinkName
        binding.totalSumma.text = orderInfo.productPrice?.formatToPrice().plus(" UZS")

        binding.orderNumber.text = orderInfo.id.toString()

        if (orderInfo.comment.isNullOrBlank()) {
            binding.commentContainer.gone()
        } else {
            binding.commentContainer.visible()
            binding.comment.text = orderInfo.comment
        }

        if ((orderInfo.cashbackUsed ?: 0.0) > 0) {
            binding.usedCashbackContainer.visible()
            binding.usedCashback.text =
                "-".plus(orderInfo.cashbackEarned?.formatToPrice()).plus(" UZS")
        } else
            binding.usedCashbackContainer.gone()

        if ((orderInfo.cashbackEarned ?: 0.0) > 0) {
            binding.earnedCashbackContainer.visible()
            binding.earnedCashback.text =
                "+".plus(orderInfo.cashbackEarned?.formatToPrice()).plus(" UZS")
        } else
            binding.earnedCashbackContainer.gone()

        val modifiers = ArrayList<ModifierItemData>()
        orderInfo.items?.forEach { item ->
            modifiers.add(
                ModifierItemData(
                    item?.itemType ?: "",
                    item?.itemType ?: "",
                    item?.itemType ?: "",
                    item?.price ?: 0.0,
                    item?.name
                )
            )
        }

        adapter.submitList(modifiers)

        when (orderInfo.orderStatus) {
            OrderStatus.PendingPayment -> {
                binding.status.setTextStringRes(R.string.pending_payment)
                binding.status.setTextColorRes(R.color.purple_80)
            }

            OrderStatus.Paid -> {
                binding.status.setTextStringRes(R.string.completed)
                binding.status.setTextColorRes(R.color.green_80)
            }

            OrderStatus.Pending -> {
                binding.status.setTextStringRes(R.string.pending)
                binding.status.setTextColorRes(R.color.purple_80)
            }

            OrderStatus.Completed -> {
                binding.status.setTextStringRes(R.string.completed)
                binding.status.setTextColorRes(R.color.green_80)
            }

            OrderStatus.PaymentFailed -> {
                binding.status.setTextStringRes(R.string.payment_failed)
                binding.status.setTextColorRes(R.color.error_300)
            }

            OrderStatus.PaymentExpired -> {
                binding.status.setTextStringRes(R.string.payment_expired)
                binding.status.setTextColorRes(R.color.error_300)
            }

            OrderStatus.Cancelled -> {
                binding.status.setTextStringRes(R.string.cancelled)
                binding.status.setTextColorRes(R.color.error_300)
            }

            OrderStatus.Error -> {
                binding.status.setTextStringRes(R.string.error)
                binding.status.setTextColorRes(R.color.error_300)
            }

            "created" -> {
                binding.status.setTextStringRes(R.string.created)
                binding.status.setTextColorRes(R.color.purple_80)
            }

            "preparing" -> {
                binding.status.setTextStringRes(R.string.preparing)
                binding.status.setTextColorRes(R.color.blue_80)
            }
        }

        binding.check.isVisible = !orderInfo.fiscalLink.isNullOrEmpty()

        binding.check.setOnClickListener {
            intentToBrowser(orderInfo.fiscalLink ?: "")
        }

        if (orderInfo.orderStatus == OrderStatus.Completed && orderInfo.hasFeedback != true) {
            binding.rate.visible()
            binding.rate.setOnClickListener {
                showFeedbackBD(orderInfo.toFeedbackDetail()) {
                    binding.rate.gone()
                }
            }
        } else {
            binding.rate.gone()
        }

        val pickupReady = when (orderInfo.orderStatus) {
            OrderStatus.Paid,
            OrderStatus.Pending,
            OrderStatus.Completed,
            "created",
            "preparing" -> true

            else -> false
        }

        if (pickupReady) {
            binding.pickupQr.visible()
            binding.pickupQr.setOnClickListener {
                showOrderQrCodeBD(orderId)
            }
        } else {
            binding.pickupQr.gone()
        }

        if (orderInfo.orderStatus == OrderStatus.PendingPayment) {
            binding.cancelOrder.visible()
            binding.continuePayment.visible()
            binding.cancelOrder.setOnClickListener {
                showRequestDF(
                    getString(R.string.cancel_order),
                    getString(R.string.do_u_want_to_cancel_your_order),
                    getString(R.string.yes_cancel),
                    getString(R.string.no_keep)
                ) {
                    launch {
                        viewModel.cancelOrder(orderId).collectLatest(::collectCancelOrder)
                    }
                }
            }
            binding.continuePayment.setOnClickListener {
                intentToBrowser(orderInfo.checkoutUrl ?: "")
            }
        } else {
            binding.cancelOrder.gone()
            binding.continuePayment.gone()
        }
    }

    private fun OrderInfo.toFeedbackDetail() = FeedbackDetail(
        cashbackEarned = cashbackEarned,
        cashbackUsed = cashbackUsed,
        drinkName = drinkName,
        id = id,
        orderStatus = orderStatus,
        partnerName = shopName,
        productPrice = productPrice,
        purchasedAt = purchasedAt,
        purchasedAtUnix = purchasedAtUnix,
        shopName = shopName,
        drinkImage = drinkImageUrl
    )

    private fun collectCancelOrder(t: UIResource<Any>) = t.collect {
        showMessageDF(getString(R.string.order_cancelled), "", "OK") {
            finish()
        }
    }

    override fun onApplySystemBarInsets(systemBars: Insets) {
        binding.root.updatePadding(bottom = systemBars.bottom)
    }

}
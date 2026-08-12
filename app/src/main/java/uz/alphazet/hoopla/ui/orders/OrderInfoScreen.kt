package uz.alphazet.hoopla.ui.orders

import androidx.core.os.bundleOf
import androidx.core.view.isVisible
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
import uz.alphazet.domain.ui.BaseFragment
import uz.alphazet.domain.ui.showMessageDF
import uz.alphazet.domain.ui.showRequestDF
import uz.alphazet.domain.utils.Constants
import uz.alphazet.domain.utils.PickupTime
import uz.alphazet.domain.utils.formatToPrice
import uz.alphazet.domain.utils.getDateDDMMMMYYYYHHmm
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.intentToBrowser
import uz.alphazet.domain.utils.setTextColorRes
import uz.alphazet.domain.utils.setTextStringRes
import uz.alphazet.domain.utils.visible
import uz.alphazet.domain.viewbinding.viewBinding
import uz.alphazet.hoopla.databinding.ScreenOrderInfoBinding
import uz.alphazet.hoopla.ui.home.FeedbackBD.Companion.showFeedbackBD
import uz.alphazet.hoopla.ui.order.SummaInfoAdapter
import uz.alphazet.hoopla.ui.orders.OrderQrCodeBD.Companion.showOrderQrCodeBD
import uz.alphazet.hoopla.ui.popScreen

class OrderInfoScreen : BaseFragment(uz.alphazet.hoopla.R.layout.screen_order_info),
    SwipeRefreshLayout.OnRefreshListener {

    private val binding by viewBinding(ScreenOrderInfoBinding::bind)
    private val viewModel: OrdersVM by viewModel()
    private val adapter = SummaInfoAdapter()
    private val orderId by lazy { arguments?.getInt(Constants.ID, -1) ?: -1 }

    override fun initialize() {
        viewModel.getOrderInfo(orderId)

        launch {
            viewModel.orderInfoFlow.collectLatest(::collectData)
        }
        binding.infoRv.adapter = adapter
        binding.swipeRefreshLayout.setOnRefreshListener(this)
        binding.toolbar.setNavigationOnClickListener { popScreen() }
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

        // Scheduled pickup: shown alongside the "ordered at" stamp in the toolbar so the
        // customer can tell at a glance this isn't an ASAP order.
        val pickupAt = PickupTime.parse(orderInfo.pickupAt)
        if (pickupAt == null) {
            binding.pickupContainer.gone()
        } else {
            binding.pickupContainer.visible()
            binding.pickupTime.text = PickupTime.formatDayTimeForDisplay(pickupAt)
        }

        if (orderInfo.comment.isNullOrBlank()) {
            binding.commentContainer.gone()
        } else {
            binding.commentContainer.visible()
            binding.comment.text = orderInfo.comment
        }

        if ((orderInfo.promoDiscount ?: 0.0) > 0) {
            binding.promoContainer.visible()
            binding.promoDiscount.text =
                "-".plus(orderInfo.promoDiscount?.formatToPrice()).plus(" UZS")
        } else {
            binding.promoContainer.gone()
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
            requireContext().intentToBrowser(orderInfo.fiscalLink ?: "")
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
                requireContext().intentToBrowser(orderInfo.checkoutUrl ?: "")
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
            popScreen()
        }
    }

    companion object {
        fun newInstance(orderId: Int) = OrderInfoScreen().apply {
            arguments = bundleOf(Constants.ID to orderId)
        }
    }

}

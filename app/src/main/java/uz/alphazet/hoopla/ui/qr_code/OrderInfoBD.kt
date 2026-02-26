package uz.alphazet.hoopla.ui.qr_code

import androidx.core.view.isVisible
import uz.alphazet.data.models.order.OrderItemData
import uz.alphazet.data.models.order.OrderStatus
import uz.alphazet.domain.ui.BaseBottomSheetDF
import uz.alphazet.domain.ui.BaseFragment
import uz.alphazet.domain.utils.formatToPrice
import uz.alphazet.domain.utils.getDateDDMMMMYYYYHHmm
import uz.alphazet.domain.utils.intentToBrowser
import uz.alphazet.domain.utils.setBackgroundTintColor
import uz.alphazet.domain.utils.setDrawableStart
import uz.alphazet.domain.utils.setTextColorRes
import uz.alphazet.domain.utils.setTextStringRes
import uz.alphazet.domain.viewbinding.viewBinding
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.DialogOrderInfoBinding

class OrderInfoBD(val orderInfo: OrderItemData) : BaseBottomSheetDF(R.layout.dialog_order_info) {

    private val binding by viewBinding(DialogOrderInfoBinding::bind)

    override fun initialize() {
        binding.title.text = "#${orderInfo.id}, ".plus(orderInfo.drinkName)
        binding.shopName.text = orderInfo.shopName
        binding.price.text = orderInfo.productPrice?.formatToPrice().plus(" UZS")

        binding.time.text = orderInfo.purchasedAtUnix?.getDateDDMMMMYYYYHHmm()

        when (orderInfo.orderStatus) {
            OrderStatus.PendingPayment -> {
                binding.status.setTextStringRes(uz.alphazet.domain.R.string.pending_payment)
                binding.status.setBackgroundTintColor(uz.alphazet.domain.R.color.purple_30)
                binding.status.setTextColorRes(uz.alphazet.domain.R.color.purple_80)
                binding.status.setDrawableStart(uz.alphazet.domain.R.drawable.ic_clock_pending)
            }

            OrderStatus.Paid -> {
                binding.status.setTextStringRes(uz.alphazet.domain.R.string.completed)
                binding.status.setBackgroundTintColor(uz.alphazet.domain.R.color.green_20)
                binding.status.setTextColorRes(uz.alphazet.domain.R.color.green_80)
                binding.status.setDrawableStart(uz.alphazet.domain.R.drawable.ic_accept)
            }

            OrderStatus.Pending -> {
                binding.status.setTextStringRes(uz.alphazet.domain.R.string.pending)
                binding.status.setBackgroundTintColor(uz.alphazet.domain.R.color.purple_30)
                binding.status.setTextColorRes(uz.alphazet.domain.R.color.purple_80)
                binding.status.setDrawableStart(uz.alphazet.domain.R.drawable.ic_clock_pending)
            }

            OrderStatus.Completed -> {
                binding.status.setTextStringRes(uz.alphazet.domain.R.string.completed)
                binding.status.setBackgroundTintColor(uz.alphazet.domain.R.color.green_20)
                binding.status.setTextColorRes(uz.alphazet.domain.R.color.green_80)
                binding.status.setDrawableStart(uz.alphazet.domain.R.drawable.ic_accept)
            }

            OrderStatus.PaymentFailed -> {
                binding.status.setTextStringRes(uz.alphazet.domain.R.string.payment_failed)
                binding.status.setBackgroundTintColor(uz.alphazet.domain.R.color.error_50)
                binding.status.setTextColorRes(uz.alphazet.domain.R.color.error_300)
                binding.status.setDrawableStart(uz.alphazet.domain.R.drawable.ic_cancel)
            }

            OrderStatus.PaymentExpired -> {
                binding.status.setTextStringRes(uz.alphazet.domain.R.string.payment_expired)
                binding.status.setBackgroundTintColor(uz.alphazet.domain.R.color.error_50)
                binding.status.setTextColorRes(uz.alphazet.domain.R.color.error_300)
                binding.status.setDrawableStart(uz.alphazet.domain.R.drawable.ic_cancel)
            }

            OrderStatus.Cancelled -> {
                binding.status.setTextStringRes(uz.alphazet.domain.R.string.cancelled)
                binding.status.setBackgroundTintColor(uz.alphazet.domain.R.color.error_50)
                binding.status.setTextColorRes(uz.alphazet.domain.R.color.error_300)
                binding.status.setDrawableStart(uz.alphazet.domain.R.drawable.ic_cancel)
            }

            OrderStatus.Error -> {
                binding.status.setTextStringRes(uz.alphazet.domain.R.string.error)
                binding.status.setBackgroundTintColor(uz.alphazet.domain.R.color.error_50)
                binding.status.setTextColorRes(uz.alphazet.domain.R.color.error_300)
                binding.status.setDrawableStart(uz.alphazet.domain.R.drawable.ic_cancel)
            }

            "created" -> {
                binding.status.setTextStringRes(uz.alphazet.domain.R.string.created)
                binding.status.setBackgroundTintColor(uz.alphazet.domain.R.color.purple_30)
                binding.status.setTextColorRes(uz.alphazet.domain.R.color.purple_80)
                binding.status.setDrawableStart(uz.alphazet.domain.R.drawable.ic_clock_pending)
            }

            "preparing" -> {
                binding.status.setTextStringRes(uz.alphazet.domain.R.string.preparing)
                binding.status.setBackgroundTintColor(uz.alphazet.domain.R.color.blue_20)
                binding.status.setTextColorRes(uz.alphazet.domain.R.color.blue_80)
                binding.status.setDrawableStart(uz.alphazet.domain.R.drawable.ic_clock_pending)
            }
        }

        binding.check.isVisible = !orderInfo.fiscalLink.isNullOrEmpty()

        binding.check.setOnClickListener {
            requireContext().intentToBrowser(orderInfo.fiscalLink ?: "")
        }

        binding.close.setOnClickListener { dismiss() }
    }

    companion object {
        private const val TAG = "OrderInfoBD"

        fun BaseFragment.showOrderInfoBD(
            orderInfo: OrderItemData
        ) {
            val current = childFragmentManager.findFragmentByTag(TAG)
            if (current == null) {
                OrderInfoBD(orderInfo).show(
                    childFragmentManager,
                    TAG
                )
            }
        }
    }

}
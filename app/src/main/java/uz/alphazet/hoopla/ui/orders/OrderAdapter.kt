package uz.alphazet.hoopla.ui.orders

import android.view.View
import coil3.load
import uz.alphazet.data.models.order.OrderItemData
import uz.alphazet.data.models.order.OrderStatus
import uz.alphazet.domain.rv.BasePagingAdapter
import uz.alphazet.domain.rv.BaseVH
import uz.alphazet.domain.utils.formatToPrice
import uz.alphazet.domain.utils.getDateDDMMMMYYYYHHmm
import uz.alphazet.domain.utils.setTextColorRes
import uz.alphazet.domain.utils.setTextStringRes
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ItemOrderBinding

/**
 * Shared presentational binding for an order row ([R.layout.item_order]).
 * Used by the paged history ([OrderAdapter]) and the non-paged active-orders
 * list ([ActiveOrderAdapter]) so status/cashback rendering stays in one place.
 */
fun ItemOrderBinding.bindOrderItem(itemData: OrderItemData) {
    image.load(itemData.shopIconUrl)
    drinkName.text = itemData.drinkName
    price.text = itemData.productPrice?.formatToPrice().plus(" UZS")
    time.text = itemData.purchasedAtUnix?.getDateDDMMMMYYYYHHmm()

    when (itemData.orderStatus) {
        OrderStatus.PendingPayment -> {
            status.setTextStringRes(uz.alphazet.domain.R.string.pending_payment)
            status.setTextColorRes(uz.alphazet.domain.R.color.purple_80)
        }

        OrderStatus.Paid -> {
            status.setTextStringRes(uz.alphazet.domain.R.string.completed)
            status.setTextColorRes(uz.alphazet.domain.R.color.green_80)
        }

        OrderStatus.Pending -> {
            status.setTextStringRes(uz.alphazet.domain.R.string.pending)
            status.setTextColorRes(uz.alphazet.domain.R.color.purple_80)
        }

        OrderStatus.Completed -> {
            status.setTextStringRes(uz.alphazet.domain.R.string.completed)
            status.setTextColorRes(uz.alphazet.domain.R.color.green_80)
        }

        OrderStatus.PaymentFailed -> {
            status.setTextStringRes(uz.alphazet.domain.R.string.payment_failed)
            status.setTextColorRes(uz.alphazet.domain.R.color.error_300)
        }

        OrderStatus.PaymentExpired -> {
            status.setTextStringRes(uz.alphazet.domain.R.string.payment_expired)
            status.setTextColorRes(uz.alphazet.domain.R.color.error_300)
        }

        OrderStatus.Cancelled -> {
            status.setTextStringRes(uz.alphazet.domain.R.string.cancelled)
            status.setTextColorRes(uz.alphazet.domain.R.color.error_300)
        }

        OrderStatus.Error -> {
            status.setTextStringRes(uz.alphazet.domain.R.string.error)
            status.setTextColorRes(uz.alphazet.domain.R.color.error_300)
        }

        "created" -> {
            status.setTextStringRes(uz.alphazet.domain.R.string.created)
            status.setTextColorRes(uz.alphazet.domain.R.color.purple_80)
        }

        "preparing" -> {
            status.setTextStringRes(uz.alphazet.domain.R.string.preparing)
            status.setTextColorRes(uz.alphazet.domain.R.color.blue_80)
        }
    }

    if ((itemData.cashbackEarned ?: 0.0) > 0) {
        status.text = itemData.cashbackEarned?.formatToPrice().plus(" UZS")
        status.setTextColorRes(uz.alphazet.domain.R.color.green_300)
    }
}

class OrderAdapter : BasePagingAdapter<OrderItemData>() {

    override fun onCreateViewHolder(view: View, viewType: Int): BaseVH {
        return VH(ItemOrderBinding.bind(view))
    }

    override fun getItemViewType(position: Int): Int = R.layout.item_order

    inner class VH(private val binding: ItemOrderBinding) : BaseVH(binding.root) {
        override fun bind(position: Int) {
            val itemData = getItem(absoluteAdapterPosition) ?: return
            binding.bindOrderItem(itemData)
        }
    }

}

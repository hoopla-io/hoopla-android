package uz.alphazet.hoopla.ui.orders

import android.view.View
import androidx.annotation.StringRes
import coil3.load
import uz.alphazet.data.models.order.OrderItemData
import uz.alphazet.data.models.order.OrderStatus
import uz.alphazet.domain.rv.BaseAdapter
import uz.alphazet.domain.rv.BaseVH
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ItemActiveOrderBinding

class ActiveOrderAdapter : BaseAdapter<OrderItemData>() {

    override fun onCreateViewHolder(view: View): BaseVH = VH(ItemActiveOrderBinding.bind(view))

    override fun getItemViewType(position: Int): Int = R.layout.item_active_order

    inner class VH(private val binding: ItemActiveOrderBinding) : BaseVH(binding.root) {
        override fun bind(position: Int) {
            val item = getItem(absoluteAdapterPosition) ?: return

            binding.image.load(item.shopIconUrl)
            binding.drinkName.text = item.drinkName
            binding.subtitle.text = item.shopName
            binding.status.setText(item.orderStatus.statusLabelRes())
        }
    }
}

@StringRes
private fun String?.statusLabelRes(): Int = when (this) {
    OrderStatus.PendingPayment -> uz.alphazet.domain.R.string.pending_payment
    "created" -> uz.alphazet.domain.R.string.created
    OrderStatus.Paid -> uz.alphazet.domain.R.string.paid
    OrderStatus.Pending -> uz.alphazet.domain.R.string.pending
    "preparing" -> uz.alphazet.domain.R.string.preparing
    OrderStatus.Completed -> uz.alphazet.domain.R.string.completed
    else -> uz.alphazet.domain.R.string.pending
}

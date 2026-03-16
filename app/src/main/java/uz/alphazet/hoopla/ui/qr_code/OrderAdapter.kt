package uz.alphazet.hoopla.ui.qr_code

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

class OrderAdapter : BasePagingAdapter<OrderItemData>() {

    override fun onCreateViewHolder(view: View, viewType: Int): BaseVH {
        return VH(ItemOrderBinding.bind(view))
    }

    override fun getItemViewType(position: Int): Int = R.layout.item_order

    inner class VH(private val binding: ItemOrderBinding) : BaseVH(binding.root) {
        override fun bind(position: Int) {
            val itemData = getItem(absoluteAdapterPosition) ?: return

            binding.image.load(itemData.shopIconUrl)
            binding.drinkName.text = itemData.drinkName
            binding.price.text = itemData.productPrice?.formatToPrice().plus(" UZS")
            binding.time.text = itemData.purchasedAtUnix?.getDateDDMMMMYYYYHHmm()

            when (itemData.orderStatus) {
                OrderStatus.PendingPayment -> {
                    binding.status.setTextStringRes(uz.alphazet.domain.R.string.pending_payment)
                    binding.status.setTextColorRes(uz.alphazet.domain.R.color.purple_80)
                }

                OrderStatus.Paid -> {
                    binding.status.setTextStringRes(uz.alphazet.domain.R.string.completed)
                    binding.status.setTextColorRes(uz.alphazet.domain.R.color.green_80)
                }

                OrderStatus.Pending -> {
                    binding.status.setTextStringRes(uz.alphazet.domain.R.string.pending)
                    binding.status.setTextColorRes(uz.alphazet.domain.R.color.purple_80)
                }

                OrderStatus.Completed -> {
                    binding.status.setTextStringRes(uz.alphazet.domain.R.string.completed)
                    binding.status.setTextColorRes(uz.alphazet.domain.R.color.green_80)
                }

                OrderStatus.PaymentFailed -> {
                    binding.status.setTextStringRes(uz.alphazet.domain.R.string.payment_failed)
                    binding.status.setTextColorRes(uz.alphazet.domain.R.color.error_300)
                }

                OrderStatus.PaymentExpired -> {
                    binding.status.setTextStringRes(uz.alphazet.domain.R.string.payment_expired)
                    binding.status.setTextColorRes(uz.alphazet.domain.R.color.error_300)
                }

                OrderStatus.Cancelled -> {
                    binding.status.setTextStringRes(uz.alphazet.domain.R.string.cancelled)
                    binding.status.setTextColorRes(uz.alphazet.domain.R.color.error_300)
                }

                OrderStatus.Error -> {
                    binding.status.setTextStringRes(uz.alphazet.domain.R.string.error)
                    binding.status.setTextColorRes(uz.alphazet.domain.R.color.error_300)
                }

                "created" -> {
                    binding.status.setTextStringRes(uz.alphazet.domain.R.string.created)
                    binding.status.setTextColorRes(uz.alphazet.domain.R.color.purple_80)
                }

                "preparing" -> {
                    binding.status.setTextStringRes(uz.alphazet.domain.R.string.preparing)
                    binding.status.setTextColorRes(uz.alphazet.domain.R.color.blue_80)
                }
            }

            if ((itemData.cashbackEarned ?: 0.0) > 0) {
                binding.status.text = itemData.cashbackEarned?.formatToPrice().plus(" UZS")
                binding.status.setTextColorRes(uz.alphazet.domain.R.color.green_300)
            }

        }
    }

}
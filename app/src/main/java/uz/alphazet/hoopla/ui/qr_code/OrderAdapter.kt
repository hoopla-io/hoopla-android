package uz.alphazet.hoopla.ui.qr_code

import android.view.View
import uz.alphazet.data.models.OrderItemData
import uz.alphazet.domain.rv.BaseAdapter
import uz.alphazet.domain.rv.BaseVH
import uz.alphazet.domain.utils.getDateDDMMMMYYYYHHmm
import uz.alphazet.domain.utils.setBackgroundTintColor
import uz.alphazet.domain.utils.setDrawableStart
import uz.alphazet.domain.utils.setTextColorRes
import uz.alphazet.domain.utils.setTextStringRes
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ItemOrderBinding

class OrderAdapter : BaseAdapter<OrderItemData>() {

    override fun onCreateViewHolder(view: View): BaseVH {
        return VH(ItemOrderBinding.bind(view))
    }

    override fun getItemViewType(position: Int): Int = R.layout.item_order

    inner class VH(private val binding: ItemOrderBinding) : BaseVH(binding.root) {
        override fun bind(position: Int) {
            val itemData = getItem(absoluteAdapterPosition) ?: return

            binding.drinkName.text = "#${itemData.id}, ".plus(itemData.drinkName)
            binding.shopName.text = itemData.shopName

            binding.time.text = itemData.purchasedAtUnix?.getDateDDMMMMYYYYHHmm()

            when (itemData.orderStatus) {
                "created" -> {
                    binding.status.setTextStringRes(uz.alphazet.domain.R.string.created)
                    binding.status.setBackgroundTintColor(uz.alphazet.domain.R.color.purple_30)
                    binding.status.setTextColorRes(uz.alphazet.domain.R.color.purple_80)
                    binding.status.setDrawableStart(uz.alphazet.domain.R.drawable.ic_clock_pending)
                }

                "pending" -> {
                    binding.status.setTextStringRes(uz.alphazet.domain.R.string.pending)
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

                "completed" -> {
                    binding.status.setTextStringRes(uz.alphazet.domain.R.string.completed)
                    binding.status.setBackgroundTintColor(uz.alphazet.domain.R.color.green_20)
                    binding.status.setTextColorRes(uz.alphazet.domain.R.color.green_80)
                    binding.status.setDrawableStart(uz.alphazet.domain.R.drawable.ic_accept)
                }


                "canceled" -> {
                    binding.status.setTextStringRes(uz.alphazet.domain.R.string.cancelled)
                    binding.status.setBackgroundTintColor(uz.alphazet.domain.R.color.error_50)
                    binding.status.setTextColorRes(uz.alphazet.domain.R.color.error_300)
                    binding.status.setDrawableStart(uz.alphazet.domain.R.drawable.ic_cancel)
                }
            }

        }
    }

}
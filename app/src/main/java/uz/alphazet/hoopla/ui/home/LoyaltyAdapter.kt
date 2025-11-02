package uz.alphazet.hoopla.ui.home

import android.view.View
import uz.alphazet.data.models.LoyaltyItemData
import uz.alphazet.domain.rv.BaseAdapter
import uz.alphazet.domain.rv.BaseVH
import uz.alphazet.domain.utils.setBackgroundTintColor
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ItemLoyaltyBinding

class LoyaltyAdapter : BaseAdapter<LoyaltyItemData>() {
    override fun onCreateViewHolder(view: View): BaseVH {
        return VH(ItemLoyaltyBinding.bind(view))
    }

    override fun getItemViewType(position: Int): Int = R.layout.item_loyalty

    inner class VH(private val binding: ItemLoyaltyBinding) : BaseVH(binding.root) {
        override fun bind(position: Int) {
            val itemData = getItem(absoluteAdapterPosition) ?: return

            if (itemData.isFree)
                binding.name.text =
                    itemView.context.getString(uz.alphazet.domain.R.string.free)
            else
                binding.name.text =
                    itemView.context.getString(
                        uz.alphazet.domain.R.string.label_cup_,
                        itemData.drinkIndex.toString()
                    )

            if (itemData.isFilled) {
                binding.image.setBackgroundTintColor(uz.alphazet.domain.R.color.primary_300)
            } else {
                binding.image.setBackgroundTintColor(uz.alphazet.domain.R.color.grey_400)
            }
        }
    }


}
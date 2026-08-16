package uz.alphazet.hoopla.ui.shop_details

import android.view.View
import coil3.load
import uz.alphazet.data.models.DrinkItemData
import uz.alphazet.domain.rv.BaseAdapter
import uz.alphazet.domain.rv.BaseVH
import uz.alphazet.domain.utils.formatToPrice
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.visible
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ItemDrinkVerticalBinding

class DrinksAdapter : BaseAdapter<DrinkItemData>() {

    override fun onCreateViewHolder(view: View): BaseVH {
        return VH(ItemDrinkVerticalBinding.bind(view))
    }

    override fun getItemViewType(position: Int): Int = R.layout.item_drink_vertical

    var isClickable = true

    inner class VH(private val binding: ItemDrinkVerticalBinding) : BaseVH(binding.root) {

        override fun bind(position: Int) {
            val itemData = getItem(absoluteAdapterPosition) ?: return

            binding.name.text = itemData.name
            binding.image.load(itemData.pictureUrl)

            val description = itemData.description
            if (description.isNullOrBlank()) {
                binding.description.gone()
            } else {
                binding.description.text = description
                binding.description.visible()
            }

            binding.price.text = itemData.productPrice?.formatToPrice().plus(" UZS")

        }
    }


}
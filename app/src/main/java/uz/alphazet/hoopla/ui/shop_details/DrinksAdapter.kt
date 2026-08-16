package uz.alphazet.hoopla.ui.shop_details

import android.view.View
import coil3.load
import uz.alphazet.data.models.DrinkItemData
import uz.alphazet.domain.rv.BaseAdapter
import uz.alphazet.domain.rv.BaseVH
import uz.alphazet.domain.utils.formatToPrice
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

            binding.price.text = itemData.productPrice?.formatToPrice().plus(" UZS")

            val ratingCount = itemData.ratingCount ?: 0
            val ratingAvg = itemData.ratingAvg
            if (ratingCount > 0 && ratingAvg != null) {
                binding.rating.visibility = View.VISIBLE
                binding.rating.text = "%.1f (%d)".format(ratingAvg, ratingCount)
            } else {
                binding.rating.visibility = View.GONE
            }
        }
    }


}
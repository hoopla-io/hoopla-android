package uz.i_tv.qahvazor.ui.shop_details

import android.view.View
import coil3.load
import uz.i_tv.data.models.DrinkItemData
import uz.i_tv.domain.rv.BaseAdapter
import uz.i_tv.domain.rv.BaseVH
import uz.i_tv.qahvazor.R
import uz.i_tv.qahvazor.databinding.ItemDrinkBinding

class DrinksAdapter : BaseAdapter<DrinkItemData>() {

    override fun onCreateViewHolder(view: View): BaseVH {
        return VH(ItemDrinkBinding.bind(view))
    }

    override fun getItemViewType(position: Int): Int = R.layout.item_drink

    inner class VH(private val binding: ItemDrinkBinding) : BaseVH(binding.root) {
        override fun bind(position: Int) {
            val itemData = getItem(absoluteAdapterPosition) ?: return

            binding.name.text = itemData.name
            binding.image.load(itemData.pictureUrl)

        }
    }


}
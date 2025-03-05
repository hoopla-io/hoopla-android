package uz.alphazet.hoopla.ui.partner_details

import android.view.View
import coil3.load
import uz.alphazet.data.models.ShopItemData
import uz.alphazet.domain.rv.BaseAdapter
import uz.alphazet.domain.rv.BaseVH
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ItemShopBinding

class ShopsAdapter : BaseAdapter<ShopItemData>() {

    override fun onCreateViewHolder(view: View): BaseVH {
        return VH(ItemShopBinding.bind(view))
    }

    override fun getItemViewType(position: Int): Int = R.layout.item_shop

    inner class VH(private val binding: ItemShopBinding) : BaseVH(binding.root) {
        override fun bind(position: Int) {
            val itemData = getItem(absoluteAdapterPosition) ?: return

            binding.name.text = itemData.name
            binding.image.load(itemData.pictureUrl)

        }
    }


}
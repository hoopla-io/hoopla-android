package uz.i_tv.qahvazor.ui.home

import android.view.View
import coil3.load
import uz.i_tv.data.models.ShopItemData
import uz.i_tv.domain.rv.BaseAdapter
import uz.i_tv.domain.rv.BaseVH
import uz.i_tv.domain.utils.formatDistance
import uz.i_tv.qahvazor.R
import uz.i_tv.qahvazor.databinding.ItemCompanyBinding

class NearShopAdapter : BaseAdapter<ShopItemData>() {

    override fun onCreateViewHolder(view: View): BaseVH {
        return VH(ItemCompanyBinding.bind(view))
    }

    override fun getItemViewType(position: Int): Int = R.layout.item_company

    inner class VH(private val binding: ItemCompanyBinding) : BaseVH(binding.root) {
        override fun bind(position: Int) {
            val itemData = getItem(absoluteAdapterPosition) ?: return

            binding.name.text = itemData.name
            binding.distance.text = itemData.distance?.formatDistance()
            binding.image.load(itemData.pictureUrl)
        }
    }


}
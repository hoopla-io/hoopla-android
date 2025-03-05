package uz.i_tv.qahvazor.ui.home

import android.view.View
import coil3.load
import uz.i_tv.data.models.ShopItemData
import uz.i_tv.domain.rv.BaseAdapter
import uz.i_tv.domain.rv.BaseVH
import uz.i_tv.domain.utils.formatDistance
import uz.i_tv.qahvazor.R
import uz.i_tv.qahvazor.databinding.ItemPartnerBinding

class NearShopAdapter : BaseAdapter<ShopItemData>() {

    override fun onCreateViewHolder(view: View): BaseVH {
        return VH(ItemPartnerBinding.bind(view))
    }

    override fun getItemViewType(position: Int): Int = R.layout.item_partner

    inner class VH(private val binding: ItemPartnerBinding) : BaseVH(binding.root) {
        override fun bind(position: Int) {
            val itemData = getItem(absoluteAdapterPosition) ?: return

            binding.name.text = itemData.name
            binding.distance.text = itemView.context.formatDistance(itemData.distance ?: -1.0)
            binding.image.load(itemData.pictureUrl)
        }
    }


}
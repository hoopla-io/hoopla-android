package uz.alphazet.hoopla.ui.search

import android.view.View
import coil3.load
import uz.alphazet.data.models.PartnerItemData
import uz.alphazet.domain.rv.BaseAdapter
import uz.alphazet.domain.rv.BaseVH
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ItemBrandBinding

class PartnerAdapter : BaseAdapter<PartnerItemData>() {

    override fun onCreateViewHolder(view: View): BaseVH {
        return VH(ItemBrandBinding.bind(view))
    }

    override fun getItemViewType(position: Int): Int = R.layout.item_brand

    inner class VH(private val binding: ItemBrandBinding) : BaseVH(binding.root) {
        override fun bind(position: Int) {
            val itemData = getItem(absoluteAdapterPosition) ?: return

            binding.name.text = itemData.name
            binding.logo.load(itemData.logoUrl)
        }
    }

}
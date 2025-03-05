package uz.alphazet.hoopla.ui.profile.subscriptions

import android.view.View
import uz.alphazet.data.models.FeatureItemData
import uz.alphazet.domain.rv.BaseAdapter
import uz.alphazet.domain.rv.BaseVH
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ItemFeatureBinding

class FeatureAdapter : BaseAdapter<FeatureItemData>() {
    override fun onCreateViewHolder(view: View): BaseVH {
        return VH(ItemFeatureBinding.bind(view))
    }

    override fun getItemViewType(position: Int): Int = R.layout.item_feature

    inner class VH(private val binding: ItemFeatureBinding) : BaseVH(binding.root) {
        override fun bind(position: Int) {
            val itemData = getItem(absoluteAdapterPosition) ?: return

            binding.feature.text = itemData.feature
        }
    }


}
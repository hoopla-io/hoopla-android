package uz.i_tv.qahvazor.ui.profile.subscriptions

import android.view.View
import uz.i_tv.data.models.FeatureItemData
import uz.i_tv.domain.rv.BaseAdapter
import uz.i_tv.domain.rv.BaseVH
import uz.i_tv.qahvazor.R
import uz.i_tv.qahvazor.databinding.ItemFeatureBinding

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
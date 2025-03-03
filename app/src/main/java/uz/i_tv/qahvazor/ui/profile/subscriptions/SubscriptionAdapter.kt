package uz.i_tv.qahvazor.ui.profile.subscriptions

import android.view.View
import uz.i_tv.data.models.SubscriptionItemData
import uz.i_tv.domain.rv.BaseAdapter
import uz.i_tv.domain.rv.BaseVH
import uz.i_tv.domain.utils.formatToPrice
import uz.i_tv.qahvazor.R
import uz.i_tv.qahvazor.databinding.ItemSubscriptionBinding

class SubscriptionAdapter : BaseAdapter<SubscriptionItemData>() {

    override fun onCreateViewHolder(view: View): BaseVH {
        return VH(ItemSubscriptionBinding.bind(view))
    }

    override fun getItemViewType(position: Int): Int = R.layout.item_subscription

    inner class VH(private val binding: ItemSubscriptionBinding) : BaseVH(binding.root) {

        private val featureAdapter = FeatureAdapter()

        init {
            binding.featureRv.adapter = featureAdapter
        }

        override fun bind(position: Int) {
            val itemData = getItem(absoluteAdapterPosition) ?: return

            binding.name.text = itemData.name
            binding.price.text = itemData.price?.formatToPrice()
                .plus(" ${itemData.currency}")
                .plus(" / ${itemData.days} days")

            featureAdapter.submitList(itemData.features)
        }
    }


}
package uz.alphazet.hoopla.ui.profile.subscriptions

import android.view.View
import uz.alphazet.data.models.SubscriptionItemData
import uz.alphazet.domain.rv.BaseAdapter
import uz.alphazet.domain.rv.BaseVH
import uz.alphazet.domain.utils.formatToPrice
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ItemSubscriptionBinding

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
            binding.price.text = itemView.context.getString(
                uz.alphazet.domain.R.string.label_price_currency_days,
                itemData.price?.formatToPrice(),
                itemData.currency, itemData.days.toString()
            )

            featureAdapter.submitList(itemData.features)
        }
    }


}
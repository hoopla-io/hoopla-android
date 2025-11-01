package uz.alphazet.hoopla.ui.shop_details

import android.view.View
import uz.alphazet.data.models.PartnerData
import uz.alphazet.data.models.UrlTypes.URL_TYPE_FACEBOOK
import uz.alphazet.data.models.UrlTypes.URL_TYPE_INSTAGRAM
import uz.alphazet.data.models.UrlTypes.URL_TYPE_WEB
import uz.alphazet.domain.rv.BaseAdapter
import uz.alphazet.domain.rv.BaseVH
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ItemUrlBinding

class UrlAdapter : BaseAdapter<PartnerData.UrlData>() {
    override fun onCreateViewHolder(view: View): BaseVH {
        return VH(ItemUrlBinding.bind(view))
    }

    override fun getItemViewType(position: Int): Int = R.layout.item_url

    inner class VH(private val binding: ItemUrlBinding) : BaseVH(binding.root) {
        override fun bind(position: Int) {
            val itemData = getItem(absoluteAdapterPosition) ?: return

            when (itemData.urlType) {
                URL_TYPE_WEB -> {
                    binding.root.setImageResource(uz.alphazet.domain.R.drawable.ic_world)
                }

                URL_TYPE_INSTAGRAM -> {
                    binding.root.setImageResource(uz.alphazet.domain.R.drawable.ic_instagram)
                }

                URL_TYPE_FACEBOOK -> {
                    binding.root.setImageResource(uz.alphazet.domain.R.drawable.ic_facebook)
                }

            }
        }
    }

}
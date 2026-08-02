package uz.alphazet.hoopla.ui.home

import android.view.View
import androidx.core.view.isVisible
import coil3.load
import uz.alphazet.data.models.ShopItemData
import uz.alphazet.domain.rv.BaseAdapter
import uz.alphazet.domain.rv.BaseVH
import uz.alphazet.domain.utils.formatDistance
import uz.alphazet.domain.utils.formatRating
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ItemPartnerBinding

class NearShopAdapter(showDistance: Boolean = true) : BaseAdapter<ShopItemData>() {

    var showDistance: Boolean = showDistance
        set(value) {
            if (field == value) return
            field = value
            notifyItemRangeChanged(0, itemCount)
        }

    override fun onCreateViewHolder(view: View): BaseVH {
        return VH(ItemPartnerBinding.bind(view))
    }

    override fun getItemViewType(position: Int): Int = R.layout.item_partner

    inner class VH(private val binding: ItemPartnerBinding) : BaseVH(binding.root) {
        override fun bind(position: Int) {
            val itemData = getItem(absoluteAdapterPosition) ?: return

            binding.name.text = itemData.name
            binding.distance.isVisible = showDistance
            binding.distance.text = itemView.context.formatDistance(itemData.distance ?: -1.0)
            binding.image.load(itemData.pictureUrl)

            // Shop rating: "★ 4.7". Hidden only if the field is somehow absent.
            val rating = itemData.rating
            binding.ratingGroup.isVisible = rating != null
            if (rating != null) binding.rating.text = rating.formatRating()

            // Live cashier-pause state: shop is paused only when explicitly false.
            val paused = itemData.acceptingOrders == false
            binding.pausedScrim.isVisible = paused
            binding.pausedLabel.isVisible = paused
        }
    }


}
package uz.alphazet.hoopla.ui.home

import android.content.res.ColorStateList
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import coil3.load
import uz.alphazet.data.models.ShopItemData
import uz.alphazet.domain.rv.BaseAdapter
import uz.alphazet.domain.rv.BaseVH
import uz.alphazet.domain.utils.formatDistanceShort
import uz.alphazet.domain.utils.formatRating
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ItemPartnerBinding
import uz.alphazet.domain.R as DomainR

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
            val context = itemView.context

            binding.name.text = itemData.name
            binding.image.load(itemData.pictureUrl)

            // Partner logo badge; collapsed when the shop has no logo so the
            // title starts at the card padding instead of next to an empty circle.
            val logoUrl = itemData.logoUrl
            binding.logo.isVisible = !logoUrl.isNullOrBlank()
            if (!logoUrl.isNullOrBlank()) binding.logo.load(logoUrl)

            // Distance pill — hidden when the list is not location-based, or when
            // the shop came back without a distance.
            val distance = itemData.distance
            binding.distanceGroup.isVisible = showDistance && distance != null
            if (distance != null) binding.distance.text = context.formatDistanceShort(distance)

            // Shop rating: "★ 4.7". Hidden only if the field is somehow absent.
            val rating = itemData.rating
            binding.ratingGroup.isVisible = rating != null
            if (rating != null) binding.rating.text = rating.formatRating()

            // Live cashier-pause state: shop is paused only when explicitly false.
            val paused = itemData.acceptingOrders == false
            binding.pausedScrim.isVisible = paused
            binding.pausedLabel.isVisible = paused

            val statusColor = ContextCompat.getColor(
                context,
                if (paused) DomainR.color.error_300 else DomainR.color.green_70
            )
            binding.status.setText(
                if (paused) DomainR.string.shop_status_closed else DomainR.string.shop_status_open
            )
            binding.status.setTextColor(statusColor)
            binding.statusDot.backgroundTintList = ColorStateList.valueOf(statusColor)
        }
    }


}

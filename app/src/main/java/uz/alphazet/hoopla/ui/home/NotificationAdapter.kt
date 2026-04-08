package uz.alphazet.hoopla.ui.home

import android.view.View
import coil3.load
import uz.alphazet.data.models.NotificationItemData
import uz.alphazet.domain.rv.BasePagingAdapter
import uz.alphazet.domain.rv.BaseVH
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.visible
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ItemNotificationBinding

class NotificationAdapter : BasePagingAdapter<NotificationItemData>() {

    override fun onCreateViewHolder(view: View, viewType: Int): BaseVH {
        return VH(ItemNotificationBinding.bind(view))
    }

    override fun getItemViewType(position: Int): Int = R.layout.item_notification

    inner class VH(private val binding: ItemNotificationBinding) : BaseVH(binding.root) {
        override fun bind(position: Int) {
            val itemData = getItem(absoluteAdapterPosition) ?: return

            binding.image.load(itemData.files?.imageUrl)
            binding.name.text = itemData.notificationTitle
            binding.desc.text = itemData.notificationDescription

            if (itemData.isNew == true) {
                binding.newIndicator.visible()
            } else {
                binding.newIndicator.gone()
            }
        }
    }

}
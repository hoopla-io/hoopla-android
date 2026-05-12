package uz.alphazet.hoopla.ui.home

import android.view.View
import coil3.load
import uz.alphazet.data.models.StoryItemData
import uz.alphazet.domain.R as DomainR
import uz.alphazet.domain.rv.BaseAdapter
import uz.alphazet.domain.rv.BaseVH
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ItemStoryBinding

class StoryAdapter : BaseAdapter<StoryItemData>() {

    override fun onCreateViewHolder(view: View): BaseVH {
        return VH(ItemStoryBinding.bind(view))
    }

    override fun getItemViewType(position: Int): Int = R.layout.item_story

    inner class VH(private val binding: ItemStoryBinding) : BaseVH(binding.root) {
        override fun bind(position: Int) {
            val item = getItem(absoluteAdapterPosition) ?: return

            binding.cover.load(item.coverImageUrl)

            val seen = item.isSeen == true
            val ctx = itemView.context
            val density = ctx.resources.displayMetrics.density
            if (seen) {
                binding.storyCard.strokeColor = ctx.getColor(DomainR.color.grey_250)
                binding.storyCard.strokeWidth = (1f * density).toInt()
            } else {
                binding.storyCard.strokeColor = ctx.getColor(DomainR.color.primary)
                binding.storyCard.strokeWidth = (2f * density).toInt()
            }
        }
    }
}
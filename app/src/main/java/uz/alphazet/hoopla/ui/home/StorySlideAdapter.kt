package uz.alphazet.hoopla.ui.home

import android.view.View
import coil3.load
import uz.alphazet.data.models.StorySlideData
import uz.alphazet.domain.rv.BaseAdapter
import uz.alphazet.domain.rv.BaseVH
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ItemStorySlideBinding

class StorySlideAdapter : BaseAdapter<StorySlideData>() {

    var onSlideImageReady: ((position: Int) -> Unit)? = null
    var onSlideImageError: ((position: Int) -> Unit)? = null

    private val loadedPositions = mutableSetOf<Int>()

    fun isImageLoaded(position: Int): Boolean = position in loadedPositions

    override fun onCreateViewHolder(view: View): BaseVH {
        return VH(ItemStorySlideBinding.bind(view))
    }

    override fun getItemViewType(position: Int): Int = R.layout.item_story_slide

    inner class VH(private val binding: ItemStorySlideBinding) : BaseVH(binding.root) {
        override fun bind(position: Int) {
            val item = getItem(absoluteAdapterPosition) ?: return
            val pos = absoluteAdapterPosition
            loadedPositions.remove(pos)
            binding.slideImage.load(item.imageUrl) {
                listener(
                    onSuccess = { _, _ ->
                        if (bindingAdapterPosition == pos) {
                            loadedPositions.add(pos)
                            onSlideImageReady?.invoke(pos)
                        }
                    },
                    onError = { _, _ ->
                        if (bindingAdapterPosition == pos) {
                            onSlideImageError?.invoke(pos)
                        }
                    }
                )
            }
        }
    }
}
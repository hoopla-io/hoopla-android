package uz.alphazet.hoopla.ui.home

import android.view.View
import coil3.load
import coil3.request.crossfade
import uz.alphazet.data.models.StorySlideData
import uz.alphazet.domain.rv.BaseAdapter
import uz.alphazet.domain.rv.BaseVH
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ItemStorySlideBinding

/**
 * Full-screen slide images. Reports per-position load state back to [StoryGroupFragment],
 * which gates the slide timer on the image actually being on screen.
 */
class StorySlideAdapter : BaseAdapter<StorySlideData>() {

    var onSlideImageReady: ((position: Int) -> Unit)? = null
    var onSlideImageError: ((position: Int) -> Unit)? = null

    private val loadedPositions = mutableSetOf<Int>()
    private val failedPositions = mutableSetOf<Int>()

    fun isImageLoaded(position: Int): Boolean = position in loadedPositions
    fun isImageFailed(position: Int): Boolean = position in failedPositions

    /** Re-requests the image for [position] (after a failure). No-op if it isn't bound. */
    fun reload(position: Int) {
        failedPositions.remove(position)
        notifyItemChanged(position)
    }

    override fun onCreateViewHolder(view: View): BaseVH {
        return VH(ItemStorySlideBinding.bind(view))
    }

    override fun getItemViewType(position: Int): Int = R.layout.item_story_slide

    override fun onViewRecycled(holder: BaseVH) {
        // The holder is about to show another slide; its old position is no longer "loaded".
        val pos = (holder as? VH)?.boundPosition ?: -1
        if (pos != -1) {
            loadedPositions.remove(pos)
            failedPositions.remove(pos)
        }
        super.onViewRecycled(holder)
    }

    override fun submitList(list: List<StorySlideData>?) {
        loadedPositions.clear()
        failedPositions.clear()
        super.submitList(list)
    }

    inner class VH(private val binding: ItemStorySlideBinding) : BaseVH(binding.root) {
        var boundPosition = -1
            private set

        override fun bind(position: Int) {
            val item = getItem(absoluteAdapterPosition) ?: return
            val pos = absoluteAdapterPosition
            boundPosition = pos
            loadedPositions.remove(pos)
            failedPositions.remove(pos)
            binding.slideImage.load(item.imageUrl) {
                crossfade(CROSSFADE_MS)
                listener(
                    onSuccess = { _, _ ->
                        if (bindingAdapterPosition == pos) {
                            loadedPositions.add(pos)
                            onSlideImageReady?.invoke(pos)
                        }
                    },
                    onError = { _, _ ->
                        if (bindingAdapterPosition == pos) {
                            failedPositions.add(pos)
                            onSlideImageError?.invoke(pos)
                        }
                    }
                )
            }
        }
    }

    private companion object {
        const val CROSSFADE_MS = 160
    }
}

package uz.alphazet.hoopla.ui.shop_details

import android.view.View
import coil3.load
import uz.alphazet.data.models.PictureData
import uz.alphazet.domain.rv.BaseAdapter
import uz.alphazet.domain.rv.BaseVH
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ItemImageBinding

class ImagesAdapter : BaseAdapter<PictureData>() {

    override fun onCreateViewHolder(view: View): BaseVH {
        return VH(ItemImageBinding.bind(view))
    }

    override fun getItemViewType(position: Int): Int = R.layout.item_image

    private var onItemClickListenerWithPosition: ((PictureData, Int) -> Unit)? = null
    fun setOnItemClickListenerWithPosition(l: (PictureData, Int) -> Unit) {
        onItemClickListenerWithPosition = l
    }

    inner class VH(private val binding: ItemImageBinding) : BaseVH(binding.root) {

        override fun bind(position: Int) {
            val itemData = getItem(absoluteAdapterPosition) ?: return

            itemView.setOnClickListener {
                onItemClickListenerWithPosition?.invoke(itemData, absoluteAdapterPosition)
            }

            binding.image.load(itemData.pictureUrl)

        }
    }


}
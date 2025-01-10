package uz.i_tv.qahvazor.ui.shop_details

import android.view.View
import coil3.load
import uz.i_tv.data.models.PictureData
import uz.i_tv.domain.rv.BaseAdapter
import uz.i_tv.domain.rv.BaseVH
import uz.i_tv.qahvazor.R
import uz.i_tv.qahvazor.databinding.ItemImageBinding

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
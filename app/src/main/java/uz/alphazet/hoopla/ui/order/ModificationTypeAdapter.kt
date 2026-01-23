package uz.alphazet.hoopla.ui.order

import android.view.View
import androidx.annotation.DrawableRes
import coil3.load
import uz.alphazet.data.models.order.OrderDetails.ModificationItem
import uz.alphazet.data.rv.BaseItem
import uz.alphazet.domain.rv.BaseAdapter
import uz.alphazet.domain.rv.BaseVH
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ItemModificationBinding

class ModificationTypeAdapter : BaseAdapter<ModificationType>() {

    override fun onCreateViewHolder(view: View): BaseVH {
        return VH(ItemModificationBinding.bind(view))
    }

    override fun getItemViewType(position: Int): Int = R.layout.item_modification

    inner class VH(private val binding: ItemModificationBinding) : BaseVH(binding.root) {
        override fun bind(position: Int) {
            val itemData = getItem(absoluteAdapterPosition) ?: return

            binding.name.text = itemData.type
            binding.image.load(itemData.imageRes)

        }
    }

}

data class ModificationType(
    val type: String,
    @DrawableRes val imageRes: Int,
    val modifications: List<ModificationItem?>?
) : BaseItem {
    override val uniqueId: String
        get() = type
}
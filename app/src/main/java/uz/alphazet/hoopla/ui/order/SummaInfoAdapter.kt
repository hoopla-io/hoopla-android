package uz.alphazet.hoopla.ui.order

import android.view.View
import uz.alphazet.data.models.order.ModifierItemData
import uz.alphazet.domain.rv.BaseAdapter
import uz.alphazet.domain.rv.BaseVH
import uz.alphazet.domain.utils.formatToPrice
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ItemSummaInfoBinding

class SummaInfoAdapter : BaseAdapter<ModifierItemData>() {

    override fun onCreateViewHolder(view: View): BaseVH {
        return VH(ItemSummaInfoBinding.bind(view))
    }

    override fun getItemViewType(position: Int): Int = R.layout.item_summa_info

    inner class VH(private val binding: ItemSummaInfoBinding) : BaseVH(binding.root) {
        override fun bind(position: Int) {
            val itemData = getItem(absoluteAdapterPosition) ?: return

            binding.title.text = itemData.text
            binding.summa.text = "+".plus(itemData.modifierPrice.formatToPrice()).plus(" UZS")
        }
    }


}
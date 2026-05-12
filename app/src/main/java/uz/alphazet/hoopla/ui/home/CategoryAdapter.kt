package uz.alphazet.hoopla.ui.home

import android.view.View
import coil3.load
import uz.alphazet.data.models.CategoryData
import uz.alphazet.domain.R as DomainR
import uz.alphazet.domain.rv.BaseAdapter
import uz.alphazet.domain.rv.BaseVH
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ItemCategoryBinding

class CategoryAdapter : BaseAdapter<CategoryData>() {

    private var selectedId: Int? = null

    override fun onCreateViewHolder(view: View): BaseVH {
        return VH(ItemCategoryBinding.bind(view))
    }

    override fun getItemViewType(position: Int): Int = R.layout.item_category

    fun setSelectedId(id: Int?) {
        val oldId = selectedId
        selectedId = id
        currentList.forEachIndexed { index, item ->
            if (item.id == oldId || item.id == id) {
                notifyItemChanged(index)
            }
        }
    }

    inner class VH(private val binding: ItemCategoryBinding) : BaseVH(binding.root) {
        override fun bind(position: Int) {
            val itemData = getItem(absoluteAdapterPosition) ?: return

            binding.name.text = itemData.name
            binding.icon.load(itemData.imageUrl)

            val isSelected = itemData.id == selectedId
            if (isSelected) {
                binding.pill.setBackgroundResource(R.drawable.bg_category_pill_selected)
                binding.name.setTextColor(itemView.context.getColor(DomainR.color.primary))
            } else {
                binding.pill.setBackgroundResource(R.drawable.bg_category_pill)
                binding.name.setTextColor(itemView.context.getColor(DomainR.color.black_300))
            }
        }
    }

}
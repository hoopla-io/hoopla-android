package uz.alphazet.hoopla.ui.order

import android.annotation.SuppressLint
import android.view.View
import uz.alphazet.data.models.order.OrderDetails.ModificationItem
import uz.alphazet.domain.rv.BaseAdapter
import uz.alphazet.domain.rv.BaseVH
import uz.alphazet.domain.utils.formatToPrice
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ItemDrinkModificationBinding

class ModificationAdapter : BaseAdapter<ModificationItem>() {
    override fun onCreateViewHolder(view: View): BaseVH {
        return VH(ItemDrinkModificationBinding.bind(view))
    }

    override fun getItemViewType(position: Int): Int = R.layout.item_drink_modification

    private var selectedItemId = ""

    fun getSelectedItem() =
        currentList.find { item -> item.modificationId.equals(selectedItemId) }

    fun selectItem(id: String) {
        val lastItemIndex =
            currentList.indexOfFirst { item -> item.modificationId.equals(selectedItemId) }
        val newItemIndex = currentList.indexOfFirst { item -> item.modificationId.equals(id) }
        selectedItemId = id
        if (lastItemIndex == newItemIndex)
            selectedItemId = ""

        if (lastItemIndex != -1) notifyItemChanged(lastItemIndex)
        if (newItemIndex != -1) notifyItemChanged(newItemIndex)
    }

    inner class VH(private val binding: ItemDrinkModificationBinding) : BaseVH(binding.root) {
        @SuppressLint("SetTextI18n")
        override fun bind(position: Int) {
            val itemData = getItem(absoluteAdapterPosition) ?: return

            binding.checkbox.isChecked = itemData.modificationId.equals(selectedItemId)

            binding.sizeName.text = itemData.modificationName
            binding.summa.text = "+${itemData.modificationPrice?.formatToPrice()} UZS"
        }
    }


}
package uz.alphazet.hoopla.ui.order

import android.annotation.SuppressLint
import android.view.View
import uz.alphazet.data.models.order.OrderDetails.ModificationItem
import uz.alphazet.domain.rv.BaseAdapter
import uz.alphazet.domain.rv.BaseVH
import uz.alphazet.domain.utils.formatToPrice
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.visible
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
//        if (lastItemIndex == newItemIndex)
//            selectedItemId = ""

        if (lastItemIndex != -1) notifyItemChanged(lastItemIndex)
        if (newItemIndex != -1) notifyItemChanged(newItemIndex)
    }

    inner class VH(private val binding: ItemDrinkModificationBinding) : BaseVH(binding.root) {
        @SuppressLint("SetTextI18n")
        override fun bind(position: Int) {
            val itemData = getItem(absoluteAdapterPosition) ?: return

            // Same selectable pill row as the dynamic groups use — see item_drink_modification.xml.
            val isOn = itemData.modificationId.equals(selectedItemId)
            binding.root.isSelected = isOn
            binding.checkbox.isSelected = isOn
            binding.checkbox.setImageResource(
                if (isOn) uz.alphazet.domain.R.drawable.ic_check_small else 0
            )
            binding.summa.isSelected = isOn

            binding.sizeName.text = itemData.modificationName

            val delta = itemData.modificationPrice ?: 0.0
            if (delta > 0.0) {
                binding.summa.visible()
                binding.summa.text = "+${delta.formatToPrice()} UZS"
            } else {
                binding.summa.gone()
            }
        }
    }


}
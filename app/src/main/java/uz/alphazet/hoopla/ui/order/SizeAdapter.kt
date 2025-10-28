package uz.alphazet.hoopla.ui.order

import android.annotation.SuppressLint
import android.view.View
import uz.alphazet.data.models.order.OrderDetails.ModificationItem
import uz.alphazet.domain.rv.BaseAdapter
import uz.alphazet.domain.rv.BaseVH
import uz.alphazet.domain.utils.formatToPrice
import uz.alphazet.domain.utils.setBackgroundTintColor
import uz.alphazet.domain.utils.setTextColorRes
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ItemDrinkSizeBinding

class SizeAdapter : BaseAdapter<ModificationItem>() {
    override fun onCreateViewHolder(view: View): BaseVH {
        return VH(ItemDrinkSizeBinding.bind(view))
    }

    override fun getItemViewType(position: Int): Int = R.layout.item_drink_size

    private var selectedItemId = ""

    fun getSelectedItemItem() =
        currentList.find { item -> item.modificationId.equals(selectedItemId) }

    fun selectItem(id: String) {
        val lastItemIndex =
            currentList.indexOfFirst { item -> item.modificationId.equals(selectedItemId) }
        val newItemIndex = currentList.indexOfFirst { item -> item.modificationId.equals(id) }
        selectedItemId = id
        if (lastItemIndex != -1) notifyItemChanged(lastItemIndex)
        if (newItemIndex != -1) notifyItemChanged(newItemIndex)
    }

    inner class VH(private val binding: ItemDrinkSizeBinding) : BaseVH(binding.root) {
        @SuppressLint("SetTextI18n")
        override fun bind(position: Int) {
            val itemData = getItem(absoluteAdapterPosition) ?: return

            if (itemData.modificationId.equals(selectedItemId)) {
                binding.root.setBackgroundTintColor(uz.alphazet.domain.R.color.primary_300)
                binding.sizeName.setTextColorRes(uz.alphazet.domain.R.color.white)
                binding.summa.setTextColorRes(uz.alphazet.domain.R.color.white)
            } else {
                binding.root.setBackgroundTintColor(uz.alphazet.domain.R.color.white)
                binding.sizeName.setTextColorRes(uz.alphazet.domain.R.color.black_300)
                binding.summa.setTextColorRes(uz.alphazet.domain.R.color.black_300)
            }

            binding.sizeName.text = itemData.modificationName
            binding.summa.text = "+${itemData.modificationPrice?.formatToPrice()} UZS"
        }
    }


}
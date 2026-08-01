package uz.alphazet.hoopla.ui.order

import android.view.View
import uz.alphazet.domain.rv.BaseAdapter
import uz.alphazet.domain.rv.BaseVH
import uz.alphazet.domain.utils.PickupSlot
import uz.alphazet.domain.utils.setBackgroundTintColor
import uz.alphazet.domain.utils.setTextColorRes
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ItemPickupSlotBinding

/**
 * Grid of selectable pickup times. Only slots the shop can actually honour are ever
 * submitted to this adapter, so every cell here is pickable — see
 * [uz.alphazet.domain.utils.PickupTime.slots].
 *
 * Selection is tracked by instant rather than list position because the picker shows one
 * adapter per day and only a single slot across both may be selected at a time.
 */
class PickupSlotAdapter : BaseAdapter<PickupSlot>() {

    private var selectedEpochMillis: Long? = null

    override fun onCreateViewHolder(view: View): BaseVH = VH(ItemPickupSlotBinding.bind(view))

    override fun getItemViewType(position: Int): Int = R.layout.item_pickup_slot

    fun getSelectedSlot(): PickupSlot? =
        currentList.find { it.epochMillis == selectedEpochMillis }

    /** Selects [epochMillis], or clears the selection when it is null or not in this day. */
    fun selectSlot(epochMillis: Long?) {
        val previousIndex = currentList.indexOfFirst { it.epochMillis == selectedEpochMillis }
        val newIndex = currentList.indexOfFirst { it.epochMillis == epochMillis }
        selectedEpochMillis = if (newIndex == -1) null else epochMillis
        if (previousIndex != -1) notifyItemChanged(previousIndex)
        if (newIndex != -1) notifyItemChanged(newIndex)
    }

    inner class VH(private val binding: ItemPickupSlotBinding) : BaseVH(binding.root) {
        override fun bind(position: Int) {
            val itemData = getItem(absoluteAdapterPosition) ?: return

            binding.slotTime.text = itemData.time

            if (itemData.epochMillis == selectedEpochMillis) {
                binding.slotTime.setBackgroundTintColor(uz.alphazet.domain.R.color.primary_300)
                binding.slotTime.setTextColorRes(uz.alphazet.domain.R.color.white)
            } else {
                binding.slotTime.setBackgroundTintColor(uz.alphazet.domain.R.color.grey_300)
                binding.slotTime.setTextColorRes(uz.alphazet.domain.R.color.black_300)
            }
        }
    }
}

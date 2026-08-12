package uz.alphazet.hoopla.ui.order

import android.annotation.SuppressLint
import android.view.View
import uz.alphazet.data.models.order.ModifierGroupData
import uz.alphazet.data.models.order.OrderDetails.ModificationItem
import uz.alphazet.domain.rv.BaseAdapter
import uz.alphazet.domain.rv.BaseVH
import uz.alphazet.domain.utils.formatToPrice
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.visible
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ItemDrinkModificationBinding

/**
 * Renders the options of a single [ModifierGroupData] and owns its selection state.
 * Single-select groups (`maxSelect == 1`) behave like a radio group; otherwise selection
 * is multi-select, capped at [ModifierGroupData.maxSelect] (null = unlimited).
 *
 * @param onChanged invoked whenever the selection actually changes (used to re-price the order).
 */
class ModifierOptionAdapter(
    private val group: ModifierGroupData,
    private val onChanged: () -> Unit
) : BaseAdapter<ModificationItem>() {

    private val items: List<ModificationItem> = group.options.orEmpty()
    private val selectedIds = LinkedHashSet<String>()

    init {
        // Required groups (minSelect >= 1) start with their first option chosen, so the rule is
        // satisfied by default. Optional groups start empty (the customer opts in deliberately).
        if (group.isRequired) {
            items.firstOrNull()?.modificationId?.let { selectedIds.add(it) }
        }
        setOnItemClickListener { item -> toggle(item) }
        submitList(items)
    }

    val selectedCount: Int get() = selectedIds.size

    fun getSelectedOptions(): List<ModificationItem> =
        items.filter { it.modificationId in selectedIds }

    private fun toggle(item: ModificationItem) {
        val id = item.modificationId ?: return
        val changed = mutableListOf<String>()

        if (group.isSingleSelect) {
            when {
                !selectedIds.contains(id) -> {
                    changed.addAll(selectedIds)
                    selectedIds.clear()
                    selectedIds.add(id)
                    changed.add(id)
                }
                // Tapping the active option again clears it only when the group is optional.
                !group.isRequired -> {
                    selectedIds.remove(id)
                    changed.add(id)
                }
            }
        } else {
            if (selectedIds.contains(id)) {
                selectedIds.remove(id)
                changed.add(id)
            } else {
                val max = group.maxSelect
                if (max == null || selectedIds.size < max) {
                    selectedIds.add(id)
                    changed.add(id)
                } else {
                    return // already at the group's maximum — ignore the tap
                }
            }
        }

        changed.forEach { cid ->
            val idx = items.indexOfFirst { it.modificationId == cid }
            if (idx != -1) notifyItemChanged(idx)
        }
        onChanged()
    }

    override fun onCreateViewHolder(view: View): BaseVH {
        return VH(ItemDrinkModificationBinding.bind(view))
    }

    override fun getItemViewType(position: Int): Int = R.layout.item_drink_modification

    inner class VH(private val binding: ItemDrinkModificationBinding) : BaseVH(binding.root) {
        @SuppressLint("SetTextI18n")
        override fun bind(position: Int) {
            val item = getItem(absoluteAdapterPosition) ?: return

            // One circle for both selection modes — single vs multi is stated by the group's
            // Required/Optional badge now, not by a radio-vs-checkbox glyph.
            val isOn = item.modificationId in selectedIds
            binding.root.isSelected = isOn
            binding.checkbox.isSelected = isOn
            binding.checkbox.setImageResource(
                if (isOn) uz.alphazet.domain.R.drawable.ic_check_small else 0
            )
            binding.summa.isSelected = isOn

            binding.sizeName.text = item.modificationName

            // An option that costs nothing says nothing, rather than "+0 UZS".
            val delta = item.modificationPrice ?: 0.0
            if (delta > 0.0) {
                binding.summa.visible()
                binding.summa.text = "+${delta.formatToPrice()} UZS"
            } else {
                binding.summa.gone()
            }
        }
    }
}
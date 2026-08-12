package uz.alphazet.hoopla.ui.order

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import uz.alphazet.data.models.order.ModifierGroupData
import uz.alphazet.data.models.order.ModifierItemData
import uz.alphazet.domain.rv.BaseAdapter
import uz.alphazet.domain.rv.BaseVH
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.visible
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ItemModifierGroupBinding

/**
 * Renders the dynamic [ModifierGroupData] list returned by `validate-order`. Each group owns a
 * dedicated [ModifierOptionAdapter] (kept keyed by group key so selection survives view recycling)
 * shown in a nested list, with a header that displays the group name and its min/max rule hint.
 *
 * @param onSelectionChanged invoked whenever any group's selection changes (used to re-price).
 */
class ModifierGroupAdapter(
    private val onSelectionChanged: () -> Unit
) : BaseAdapter<ModifierGroupData>() {

    private var groups: List<ModifierGroupData> = emptyList()
    private val optionAdapters = mutableMapOf<String, ModifierOptionAdapter>()

    fun setGroups(newGroups: List<ModifierGroupData>) {
        groups = newGroups
        optionAdapters.clear()
        newGroups.forEach { group ->
            optionAdapters[group.key] = ModifierOptionAdapter(group) { onSelectionChanged() }
        }
        submitList(newGroups)
    }

    /** Sum of every currently selected option's price across all groups. */
    fun totalModifierPrice(): Double =
        optionAdapters.values.sumOf { adapter ->
            adapter.getSelectedOptions().sumOf { it.modificationPrice ?: 0.0 }
        }

    /** First group whose selection count is below its [ModifierGroupData.minSelect], or null. */
    fun firstUnsatisfiedGroup(): ModifierGroupData? =
        groups.firstOrNull { group ->
            (optionAdapters[group.key]?.selectedCount ?: 0) < group.minSelect
        }

    /** Flattens every selected option into the request shape, carrying each group's key. */
    fun buildModifiers(): ArrayList<ModifierItemData> {
        val result = ArrayList<ModifierItemData>()
        groups.forEach { group ->
            optionAdapters[group.key]?.getSelectedOptions()?.forEach { option ->
                result.add(
                    ModifierItemData(
                        modifierId = option.modificationId ?: "",
                        modifierGroupId = option.modificationGroupId,
                        modifierKey = option.modificationKey ?: group.key,
                        modifierPrice = option.modificationPrice ?: 0.0,
                        text = option.modificationName
                    )
                )
            }
        }
        return result
    }

    override fun onCreateViewHolder(view: View): BaseVH {
        return VH(ItemModifierGroupBinding.bind(view))
    }

    override fun getItemViewType(position: Int): Int = R.layout.item_modifier_group

    inner class VH(private val binding: ItemModifierGroupBinding) : BaseVH(binding.root) {
        override fun bind(position: Int) {
            val group = getItem(absoluteAdapterPosition) ?: return

            binding.title.text = group.name?.takeIf { it.isNotBlank() } ?: group.key

            binding.hint.text = badgeText(binding.root.context, group)

            val rule = ruleText(binding.root.context, group)
            if (rule.isEmpty()) binding.rule.gone() else {
                binding.rule.text = rule
                binding.rule.visible()
            }

            binding.optionsRv.layoutManager = LinearLayoutManager(binding.root.context)
            binding.optionsRv.isNestedScrollingEnabled = false
            binding.optionsRv.adapter = optionAdapters[group.key]
        }
    }

    /** Whether the group has to be answered at all — the badge beside its title. */
    internal fun badgeText(context: Context, group: ModifierGroupData): String =
        context.getString(
            if (group.isRequired) uz.alphazet.domain.R.string.modifier_badge_required
            else uz.alphazet.domain.R.string.modifier_badge_optional
        )

    /**
     * The part of the rule the Required/Optional badge cannot carry. "Pick one" and a plain
     * "at least 1" say nothing the badge has not already said, so both stay empty.
     */
    internal fun ruleText(context: Context, group: ModifierGroupData): String {
        val max = group.maxSelect
        return when {
            group.minSelect > 1 -> context.getString(
                uz.alphazet.domain.R.string.modifier_rule_at_least, group.minSelect
            )

            max != null && max > 1 -> context.getString(
                uz.alphazet.domain.R.string.modifier_rule_up_to, max
            )

            else -> ""
        }
    }
}
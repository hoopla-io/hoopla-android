package uz.alphazet.data.models.order

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import uz.alphazet.data.rv.BaseItem

/**
 * A first-class modifier group returned alongside the legacy [OrderDetails.Modification] map.
 * Each group has a display [name] and selection rules: a customer must pick at least
 * [minSelect] and at most [maxSelect] options. The group is identified by [key], which is
 * the same value each option's `modifierKey` carries on checkout.
 */
@Parcelize
data class ModifierGroupData(
    val key: String,
    val name: String?,
    val minSelect: Int = 0,
    val maxSelect: Int? = null, // null = unlimited
    val options: List<OrderDetails.ModificationItem>? = null
) : BaseItem, Parcelable {

    override val uniqueId: String
        get() = key

    /** Whether the customer must pick at least one option from this group. */
    val isRequired: Boolean get() = minSelect >= 1

    /** Whether exactly one option may be picked (radio behaviour). */
    val isSingleSelect: Boolean get() = maxSelect == 1
}
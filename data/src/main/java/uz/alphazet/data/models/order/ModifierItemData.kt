package uz.alphazet.data.models.order

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import uz.alphazet.data.rv.BaseItem

@Parcelize
data class ModifierItemData(
    val modifierId: String,
    val modifierGroupId: String?,
    val modifierKey: String,
    val modifierPrice: Double,
    val text: String? = null
) : BaseItem, Parcelable {
    override val uniqueId: String
        get() = modifierId
}
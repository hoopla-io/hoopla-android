package uz.alphazet.data.models.order

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import uz.alphazet.data.rv.BaseItem

@Parcelize
data class OrderDetails(
    val modifications: Modification,
    val drink: Drink?,
    val shop: Shop?,
    val validatedAt: String?,
    val validatedAtUnix: Int?,
    @SerializedName("cashback_percent")
    val cashbackPercent: Float?,
    // NEW — named groups with min/max selection rules; null/empty falls back to [modifications].
    val modifierGroups: List<ModifierGroupData>? = null
) : Parcelable {
    @Parcelize
    data class Modification(
        val size: List<ModificationItem?>?,
        val sugar: List<ModificationItem?>?,
        val milk: List<ModificationItem?>?,
        val syrup: List<ModificationItem?>?,
    ) : Parcelable

    @Parcelize
    data class ModificationItem(
        val modificationId: String?,
        val modificationGroupId: String?,
        val modificationName: String?,
        val modificationKey: String?,
        val modificationPrice: Double?
    ) : BaseItem, Parcelable {
        override val uniqueId: String
            get() = modificationId.toString()
    }

    @Parcelize
    data class Drink(
        val id: Int?,
        val name: String?,
        val amount: Double?,
        val imageUrl: String?,
        val description: String? = null
    ) : Parcelable

    @Parcelize
    data class Shop(
        val id: Int?,
        val name: String?
    ) : Parcelable
}
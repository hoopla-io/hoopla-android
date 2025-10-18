package uz.alphazet.data.models.order

import uz.alphazet.data.rv.BaseItem

data class OrderDetails(
    val modifications: Modification,
    val drink: Drink?,
    val partner: Partner?,
    val shop: Shop?,
    val validatedAt: String?,
    val validatedAtUnix: Int?
) {
    data class Modification(
        val size: List<ModificationItem?>?,
        val sugar: List<ModificationItem?>?,
    )

    data class ModificationItem(
        val modificationId: String?,
        val modificationName: String?,
        val modificationKey: String?,
        val modificationPrice: Double?
    ) : BaseItem {
        override val uniqueId: String
            get() = modificationId.toString()
    }

    data class Drink(
        val id: Int?,
        val name: String?,
        val amount: Double?,
        val imageUrl: String?
    )

    data class Partner(
        val id: Int?,
        val name: String?
    )

    data class Shop(
        val id: Int?,
        val name: String?
    )
}
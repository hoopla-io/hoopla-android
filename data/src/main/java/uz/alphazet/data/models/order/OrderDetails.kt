package uz.alphazet.data.models.order

data class OrderDetails(
    val addOns: List<AddOn?>?,
    val drink: Drink?,
    val partner: Partner?,
    val shop: Shop?,
    val validatedAt: String?,
    val validatedAtUnix: Int?
) {
    data class AddOn(
        val addOn: String?,
        val vendorAddOnId: String?
    )

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
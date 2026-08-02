package uz.alphazet.data.models

import uz.alphazet.data.rv.BaseItem

data class ShopItemData(
    val shopId: Int?,
    val name: String?,
    val pictureUrl: String?,
    val distance: Double?,
    val location: LocationData?,
    val logoUrl: String? = null,
    val partnerId: Int? = null,
    val acceptingOrders: Boolean? = null,
    val pausedUntil: String? = null,
    val shareUrl: String? = null,
    // Average of the last 100 completed-order reviews (1.0–5.0). New shops
    // default to 5.0 server-side; always present, but kept nullable defensively.
    val rating: Double? = null
) : BaseItem {
    override val uniqueId: String
        get() = shopId.toString()
}

data class PhoneNumber(
    val phoneNumber: String?
)

data class LocationData(
    val lat: Double,
    val lng: Double
)
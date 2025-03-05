package uz.alphazet.data.models

import uz.alphazet.data.rv.BaseItem

data class ShopItemData(
    val shopId: Int?,
    val name: String?,
    val pictureUrl: String?,
    val distance: Double?,
    val location: LocationData?
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
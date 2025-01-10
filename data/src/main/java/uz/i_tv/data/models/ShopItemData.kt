package uz.i_tv.data.models

import uz.i_tv.data.rv.BaseItem

data class ShopItemData(
    val shopId: Int?,
    val pictureUrl: String?,
    val location: LocationData?,
    val phoneNumbers: List<PhoneNumber>,
    val name: String?
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
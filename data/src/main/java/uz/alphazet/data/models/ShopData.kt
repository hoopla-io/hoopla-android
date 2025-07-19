package uz.alphazet.data.models

import uz.alphazet.data.models.PartnerData.UrlData
import uz.alphazet.data.rv.BaseItem

data class ShopData(
    val id: Int?,
    val partnerId: Int?,
    val name: String?,
    val canAcceptOrders: Boolean?,
    val location: LocationData?,
    val phoneNumbers: List<PhoneNumber?>?,
    val workingHours: List<WorkHour?>?,
    val pictures: List<PictureData?>?,
    val drinks: List<DrinkItemData>?,
    val urls: List<UrlData>?,
) {
    data class WorkHour(
        val weekDay: String?,
        val closeAt: String?,
        val openAt: String?
    ) : BaseItem {
        override val uniqueId: String
            get() = weekDay.toString()
    }
}

data class PictureData(
    val pictureUrl: String?
) : BaseItem {
    override val uniqueId: String
        get() = pictureUrl.toString()
}
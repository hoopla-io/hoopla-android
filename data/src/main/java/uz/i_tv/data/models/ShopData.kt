package uz.i_tv.data.models

import uz.i_tv.data.rv.BaseItem

data class ShopData(
    val id: Int?,
    val partnerId: Int?,
    val name: String?,
    val location: LocationData?,
    val phoneNumbers: List<PhoneNumber?>?,
    val workingHours: List<WorkHour?>?,
    val pictures: List<PictureData?>?,
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
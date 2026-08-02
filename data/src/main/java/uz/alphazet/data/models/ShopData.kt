package uz.alphazet.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import uz.alphazet.data.models.PartnerData.UrlData
import uz.alphazet.data.rv.BaseItem

data class ShopData(
    val id: Int?,
    val partnerId: Int?,
    val name: String?,
    val pictureUrl: String?,
    val canAcceptOrders: Boolean?,
    val location: LocationData?,
    val phoneNumbers: List<PhoneNumber?>?,
    val workingHours: List<WorkHour?>?,
    val pictures: List<PictureData?>?,
    val drinks: List<DrinkItemData>?,
    val urls: List<UrlData>?,
    val shareUrl: String? = null,
    // Average of the last 100 completed-order reviews (1.0–5.0). New shops
    // default to 5.0 server-side; always present, but kept nullable defensively.
    val rating: Double? = null,
) {
    // Parcelable so the schedule can travel to the checkout screen, where it constrains the
    // pickup-time picker without a second shop-detail request.
    @Parcelize
    data class WorkHour(
        val weekDay: String?,
        val closeAt: String?,
        val openAt: String?
    ) : BaseItem, Parcelable {
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
package uz.alphazet.data.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import uz.alphazet.data.rv.BaseItem

data class PartnerData(
    val description: String?,
    val id: Int?,
    val logoUrl: String?,
    val name: String?,
    val phoneNumbers: List<PhoneNumber>?,
    val urls: List<UrlData>?,
    val drinks: List<DrinkItemData>?,
) {
    data class PhoneNumber(
        val phoneNumber: String?
    )

    data class UrlData(
        val url: String?,
        val urlType: String?
    ) : BaseItem {
        override val uniqueId: String = url.toString()
    }
}

@Parcelize
data class DrinkItemData(
    val id: Int?,
    val name: String?,
    val pictureUrl: String?,
    val productPrice: Double?,
    val categoryName: String? = null,
    // Cached chain-wide rating aggregate for this drink (see hoopla-api
    // migration 000029). ratingAvg is null until the drink has at least one
    // rating -- distinguish "unrated" from "rated 0" in the UI.
    val ratingAvg: Double? = null,
    val ratingCount: Int? = null
) : BaseItem, Parcelable {
    override val uniqueId: String
        get() = id.toString()
}

object UrlTypes {
    const val URL_TYPE_WEB = "web"
    const val URL_TYPE_INSTAGRAM = "instagram"
    const val URL_TYPE_FACEBOOK = "facebook"
}
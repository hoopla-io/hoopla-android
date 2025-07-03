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
    )
}

@Parcelize
data class DrinkItemData(
    val id: Int?,
    val name: String?,
    val pictureUrl: String?
) : BaseItem, Parcelable {
    override val uniqueId: String
        get() = id.toString()
}

object UrlTypes {
    const val URL_TYPE_WEB = "web"
    const val URL_TYPE_INSTAGRAM = "instagram"
    const val URL_TYPE_FACEBOOK = "facebook"
}
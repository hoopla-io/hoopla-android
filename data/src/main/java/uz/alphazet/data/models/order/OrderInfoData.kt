package uz.alphazet.data.models.order

import com.google.gson.annotations.SerializedName

data class OrderInfoData(
    val id: Int?,
    val partnerName: String?,
    val shopName: String?,
    val purchasedAt: String?,
    val purchasedAtUnix: Long?,
    val drinkName: String?,
    val orderStatus: String?,
    /** Scheduled pickup instant echoed back by checkout; null for an ASAP order. */
    @SerializedName("pickup_at")
    val pickupAt: String? = null
)

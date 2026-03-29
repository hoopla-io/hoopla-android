package uz.alphazet.data.models

import com.google.gson.annotations.SerializedName

data class FeedbackDetail(
    @SerializedName("cashback_earned")
    val cashbackEarned: Double?,
    @SerializedName("cashback_used")
    val cashbackUsed: Double?,
    @SerializedName("drinkName")
    val drinkName: String?,
    @SerializedName("id")
    val id: Int?,
    @SerializedName("orderStatus")
    val orderStatus: String?,
    @SerializedName("partnerName")
    val partnerName: String?,
    @SerializedName("productPrice")
    val productPrice: Double?,
    @SerializedName("purchasedAt")
    val purchasedAt: String?,
    @SerializedName("purchasedAtUnix")
    val purchasedAtUnix: Long?,
    @SerializedName("shopName")
    val shopName: String?,
    @SerializedName("drinkImage")
    val drinkImage: String?
)
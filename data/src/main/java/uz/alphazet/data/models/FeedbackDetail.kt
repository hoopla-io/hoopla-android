package uz.alphazet.data.models

import com.google.gson.annotations.SerializedName

data class FeedbackDetail(
    @SerializedName("cashback_earned")
    val cashbackEarned: Int?,
    @SerializedName("cashback_used")
    val cashbackUsed: Int?,
    @SerializedName("drinkName")
    val drinkName: String?,
    @SerializedName("id")
    val id: Int?,
    @SerializedName("orderStatus")
    val orderStatus: String?,
    @SerializedName("partnerName")
    val partnerName: String?,
    @SerializedName("productPrice")
    val productPrice: Int?,
    @SerializedName("purchasedAt")
    val purchasedAt: String?,
    @SerializedName("purchasedAtUnix")
    val purchasedAtUnix: Int?,
    @SerializedName("shopName")
    val shopName: String?
)
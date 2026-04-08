package uz.alphazet.data.models

import com.google.gson.annotations.SerializedName

data class UserData(
    val name: String?,
    val phoneNumber: String?,
    val balance: Double?,
    @SerializedName("cashback_balance")
    val cashbackBalance: Double?,
    val currency: String?,
    val gender: String?,
    val dateOfBirth: String?,
    val userId: Int?,
    val subscription: SubscriptionData?,
    val unreadNotifications: Int?,
)

data class SubscriptionData(
    val id: Int?,
    val name: String?,
    val endDate: String?,
    val endDateUnix: Long?,
)
package uz.alphazet.data.models

data class UserData(
    val name: String?,
    val phoneNumber: String?,
    val balance: Double?,
    val currency: String?,
    val userId: Int?,
    val subscription: SubscriptionData?,
)

data class SubscriptionData(
    val id: Int?,
    val name: String?,
    val endDate: String?,
    val endDateUnix: Long?,
)
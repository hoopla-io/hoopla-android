package uz.alphazet.data.models.order

import com.google.gson.annotations.SerializedName

data class PaymentRequiredExceptionData(
    @SerializedName("amount")
    val amount: Int?,
    @SerializedName("checkout_url")
    val checkoutUrl: String?,
    @SerializedName("deeplink")
    val deeplink: String?,
    @SerializedName("expires_at")
    val expiresAt: String?,
    @SerializedName("order_id")
    val orderId: Int?,
    @SerializedName("short_link")
    val shortLink: String?
)
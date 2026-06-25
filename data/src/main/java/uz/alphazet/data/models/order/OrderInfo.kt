package uz.alphazet.data.models.order

import com.google.gson.annotations.SerializedName

data class OrderInfo(
    @SerializedName("cashback_earned")
    val cashbackEarned: Double?,
    @SerializedName("cashback_used")
    val cashbackUsed: Double?,
    @SerializedName("checkout_url")
    val checkoutUrl: String?,
    val drinkImageUrl: String?,
    val drinkName: String?,
    val fiscalLink: String?,
    val id: Int?,
    val items: List<Item?>?,
    val orderStatus: String?,
    val productPrice: Double?,
    val purchasedAt: String?,
    val purchasedAtUnix: Long?,
    val shopName: String?,
    val hasFeedback: Boolean?,
    val comment: String?,
    val promoCode: String?,
    val promoDiscount: Double?
) {
    data class Item(
        @SerializedName("item_type")
        val itemType: String?,
        val name: String?,
        val price: Double?
    )
}
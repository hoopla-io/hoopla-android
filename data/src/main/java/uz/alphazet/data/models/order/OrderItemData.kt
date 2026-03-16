package uz.alphazet.data.models.order

import com.google.gson.annotations.SerializedName
import uz.alphazet.data.rv.BaseItem

data class OrderItemData(
    val id: Int?,
    val shopName: String?,
    val shopIconUrl: String?,
    val drinkName: String?,
    val orderStatus: String?,
    val productPrice: Double?,
    val purchasedAt: String?,
    val purchasedAtUnix: Long?,
    @SerializedName("cashback_earned")
    val cashbackEarned: Double?,
    @SerializedName("cashback_used")
    val cashbackUsed: Double?,
    @SerializedName("checkout_url")
    val checkoutUrl: String?,
    val fiscalLink: String?
) : BaseItem {
    override val uniqueId: String
        get() = id.toString()
}

/**
 * "pending_payment"  - Waiting for user to pay (show "Waiting for payment")
 *   "paid"             - Payment received, order sent to vendor
 *   "pending"          - Order accepted by coffee shop, being prepared
 *   "completed"        - Order ready / completed
 *   "payment_failed"   - Payment failed (show error, allow retry)
 *   "payment_expired"  - Payment link expired (need to create new order)
 *   "cancelled"        - Order was cancelled
 *   "error"            - Error creating order at vendor
 */
object OrderStatus {
    const val PendingPayment = "pending_payment"
    const val Paid = "paid"
    const val Pending = "pending"
    const val Completed = "completed"
    const val PaymentFailed = "payment_failed"
    const val PaymentExpired = "payment_expired"
    const val Cancelled = "cancelled"
    const val Error = "error"
}
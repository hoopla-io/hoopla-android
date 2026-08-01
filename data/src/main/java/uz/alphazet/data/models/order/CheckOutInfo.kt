package uz.alphazet.data.models.order

data class CheckOutInfo(
    val amount: Int?,
    val checkout_url: String?,
    val deeplink: String?,
    val expires_at: String?,
    val order_id: Int?,
    val short_link: String?,
    /** Scheduled pickup instant echoed back by checkout; null for an ASAP order. */
    val pickup_at: String? = null
)
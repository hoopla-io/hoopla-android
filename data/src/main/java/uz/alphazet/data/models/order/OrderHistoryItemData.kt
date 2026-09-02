package uz.alphazet.data.models.order

import com.google.gson.annotations.SerializedName
import uz.alphazet.data.rv.BaseItem

/**
 * One past order as `v1/user/orders/history` returns it — the shop it was placed at and every
 * drink that went into it.
 *
 * Unlike [OrderItemData] this payload carries no order-level price and no order-level status:
 * both live on the individual drinks, so the two figures the card needs are derived here.
 */
data class OrderHistoryItemData(
    val id: Int?,
    val shopName: String?,
    val shopIconUrl: String?,
    val drinks: List<OrderHistoryDrinkData>?,
    val purchasedAt: String?,
    val purchasedAtUnix: Long?,
    @SerializedName("cashback_earned")
    val cashbackEarned: Double?,
    val hasFeedback: Boolean?
) : BaseItem {

    override val uniqueId: String
        get() = id.toString()

    /** What the whole order cost — the endpoint prices each drink, never the order. */
    val totalPrice: Double
        get() = drinks.orEmpty().sumOf { it.drinkPrice ?: 0.0 }

    /**
     * The one status worth putting on the card. Drinks in a single order normally move
     * together, so a status they all share is the order's; when they disagree there is no
     * honest one-word answer and the card shows none.
     */
    val commonStatus: String?
        get() = drinks.orEmpty().mapNotNull { it.status }.distinct().singleOrNull()
}

/** A single drink inside a [OrderHistoryItemData]. */
data class OrderHistoryDrinkData(
    val drinkId: Int?,
    val drinkName: String?,
    val drinkPrice: Double?,
    val status: String?,
    val drinkImageUrl: String?
)

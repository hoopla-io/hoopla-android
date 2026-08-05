package uz.alphazet.data.models.cart

import uz.alphazet.data.rv.BaseItem

/**
 * The server-side cart. Every mutating cart call answers with the whole recomputed cart, so
 * this is the single source of truth for the cart screen — nothing is totalled client-side.
 *
 * Fields are nullable for the same reason every other model here is: Gson happily writes null
 * into a non-null Kotlin property, and a missing total should render as "0", not crash.
 *
 * The response carries no shop name and no per-item picture — only [shopId] and
 * [CartItemData.drinkId]. Screens that need those resolve them from the shop data they already
 * hold (see `CartActivity`, which is handed the shop name by the screen that opened it).
 */
data class CartData(
    val id: Int?,
    val shopId: Int?,
    val partnerId: Int?,
    val status: String?,
    val promoCode: String?,
    val comment: String?,
    val items: List<CartItemData>?,
    val subtotal: Double?,
    val promoDiscount: Double?,
    val total: Double?
)

/** One line of the cart: a drink at a quantity, with the modifiers it was added with. */
data class CartItemData(
    val id: Int?,
    val drinkId: Int?,
    val name: String?,
    val quantity: Int?,
    val unitPrice: Double?,
    val lineTotal: Double?,
    val modifiers: List<CartItemModifierData>?
) : BaseItem {
    override val uniqueId: String
        get() = id.toString()
}

/** A modifier as the cart echoes it back — display only; it carries no id to re-submit. */
data class CartItemModifierData(
    val name: String?,
    val price: Double?
)

/** Payload of `GET /cart/count`, used to badge the cart entry point. */
data class CartCountData(
    val count: Int?
)

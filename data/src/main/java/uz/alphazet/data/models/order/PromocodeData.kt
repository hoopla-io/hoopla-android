package uz.alphazet.data.models.order

/**
 * Result of validating a promocode at checkout via `check-promocode`.
 * All money values ([discountAmount], [subtotal], [total]) are in **sum**,
 * matching the prices the app already displays.
 */
data class PromocodeData(
    val valid: Boolean?,
    val code: String?,
    val discountType: String?,
    val discountValue: Double?,
    val discountAmount: Double?,
    val subtotal: Double?,
    val total: Double?
)
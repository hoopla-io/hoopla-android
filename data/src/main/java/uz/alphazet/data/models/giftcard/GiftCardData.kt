package uz.alphazet.data.models.giftcard

/**
 * Result of redeeming a gift card via `gift-cards/redeem`. Redemption is a
 * one-time top-up: the card's full value is moved into the wallet balance.
 *
 * Both money values ([credited], [balance]) are in **sum**, matching the
 * balance the app already displays elsewhere.
 */
data class GiftCardData(
    val credited: Double?,
    val balance: Double?,
    val currency: String?
)
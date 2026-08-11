package uz.alphazet.hoopla.ui.orders

import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import uz.alphazet.data.models.order.OrderStatus
import uz.alphazet.domain.R

/**
 * Where an order sits in the pay → prepare → pick up flow the customer actually experiences.
 *
 * The API status vocabulary is wider than that flow (it also distinguishes how a payment failed,
 * and spells "accepted" two ways), so the card collapses it into the four stages worth drawing
 * plus a catch-all for orders that need the customer to open the details. Both the card and the
 * screen that routes its button read the mapping from here, so the two can't drift apart.
 */
enum class ActiveOrderStage(
    /** Filled segments of the progress bar, 0..[TOTAL_STEPS]. */
    val step: Int,
    @get:StringRes val actionLabelRes: Int,
    @get:DrawableRes val actionIconRes: Int,
    @get:ColorRes val dotColorRes: Int
) {

    /** Nothing happens at the coffee shop until this one is resolved. */
    AwaitingPayment(
        step = 0,
        actionLabelRes = R.string.continue_payment,
        actionIconRes = R.drawable.ic_payments,
        dotColorRes = R.color.orange_50
    ),

    /** Paid and handed to the shop, not yet on the counter. */
    Accepted(
        step = 1,
        actionLabelRes = R.string.pickup_qr,
        actionIconRes = R.drawable.ic_qr_code,
        dotColorRes = R.color.white
    ),

    Preparing(
        step = 2,
        actionLabelRes = R.string.pickup_qr,
        actionIconRes = R.drawable.ic_qr_code,
        dotColorRes = R.color.white
    ),

    /** Waiting on the counter — the QR is the only thing left to do. */
    Ready(
        step = 3,
        actionLabelRes = R.string.pickup_qr,
        actionIconRes = R.drawable.ic_qr_code,
        dotColorRes = R.color.green_60
    ),

    /** Cancelled or failed: no progress to show, only an explanation on the details screen. */
    NeedsAttention(
        step = 0,
        actionLabelRes = R.string.order_details,
        actionIconRes = R.drawable.ic_info,
        dotColorRes = R.color.error_100
    );

    companion object {

        const val TOTAL_STEPS = 3

        fun of(orderStatus: String?): ActiveOrderStage = when (orderStatus) {
            OrderStatus.PendingPayment -> AwaitingPayment
            OrderStatus.Paid, CREATED -> Accepted
            OrderStatus.Pending, PREPARING -> Preparing
            OrderStatus.Completed -> Ready

            OrderStatus.PaymentFailed,
            OrderStatus.PaymentExpired,
            OrderStatus.Cancelled,
            OrderStatus.Error -> NeedsAttention

            // An unknown status still came back from the active-orders endpoint, so the order is
            // live: show it as accepted rather than hiding the QR the customer may need.
            else -> Accepted
        }

        /** Statuses the API sends that [OrderStatus] doesn't declare. */
        private const val CREATED = "created"
        private const val PREPARING = "preparing"

        /** Customer-facing wording for the status pill — friendlier than the raw status name. */
        @StringRes
        fun labelRes(orderStatus: String?): Int = when (orderStatus) {
            OrderStatus.PendingPayment -> R.string.pending_payment
            OrderStatus.PaymentFailed -> R.string.payment_failed
            OrderStatus.PaymentExpired -> R.string.payment_expired
            OrderStatus.Cancelled -> R.string.cancelled
            OrderStatus.Error -> R.string.error
            else -> when (of(orderStatus)) {
                Ready -> R.string.order_status_ready
                Preparing -> R.string.order_status_preparing
                else -> R.string.order_status_accepted
            }
        }
    }
}

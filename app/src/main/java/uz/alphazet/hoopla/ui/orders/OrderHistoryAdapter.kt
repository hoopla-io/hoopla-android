package uz.alphazet.hoopla.ui.orders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import coil3.load
import uz.alphazet.data.models.order.OrderHistoryDrinkData
import uz.alphazet.data.models.order.OrderHistoryItemData
import uz.alphazet.data.models.order.OrderStatus
import uz.alphazet.domain.rv.BasePagingAdapter
import uz.alphazet.domain.rv.BaseVH
import uz.alphazet.domain.utils.formatToPrice
import uz.alphazet.domain.utils.getDateDDMMMMYYYYHHmm
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.setTextColorRes
import uz.alphazet.domain.utils.visible
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ItemOrderHistoryBinding
import uz.alphazet.hoopla.databinding.ItemOrderHistoryDrinkBinding

/**
 * The paged order history. `v1/user/orders/history` returns an order with its drinks nested
 * inside, so a row is a card — the shop and the total on top, the drinks it was made of below.
 */
class OrderHistoryAdapter : BasePagingAdapter<OrderHistoryItemData>() {

    private var onRateClickListener: ((OrderHistoryItemData) -> Unit)? = null

    /** The card's only action: rate the order. Shown only while there is a rating to give. */
    fun setOnRateClickListener(l: (OrderHistoryItemData) -> Unit) {
        onRateClickListener = l
    }

    override fun onCreateViewHolder(view: View, viewType: Int): BaseVH {
        return VH(ItemOrderHistoryBinding.bind(view))
    }

    override fun getItemViewType(position: Int): Int = R.layout.item_order_history

    inner class VH(private val binding: ItemOrderHistoryBinding) : BaseVH(binding.root) {

        override fun bind(position: Int) {
            val item = getItem(absoluteAdapterPosition) ?: return

            binding.shopIcon.load(item.shopIconUrl)
            binding.shopName.text = item.shopName
            binding.time.text = item.purchasedAtUnix?.getDateDDMMMMYYYYHHmm()
            binding.total.text = item.totalPrice.formatToPrice().plus(" UZS")

            bindStatus(item.commonStatus)
            bindDrinks(item.drinks.orEmpty())
            bindFooter(item)
        }

        /** A status we have no wording for is left off the card rather than shown raw. */
        private fun bindStatus(status: String?) {
            val label = statusLabel(status)
            if (label == null) {
                binding.status.gone()
                return
            }
            binding.status.visible()
            binding.status.setText(label.first)
            binding.status.setTextColorRes(label.second)
        }

        /**
         * The drinks the order was made of. Built here rather than by a nested adapter because
         * the count varies per order and stays small; the same order can also hold the same
         * drink twice, which leaves nothing stable to diff on.
         */
        private fun bindDrinks(drinks: List<OrderHistoryDrinkData>) {
            val container = binding.drinks
            container.removeAllViews()

            if (drinks.isEmpty()) {
                container.gone()
                binding.drinksDivider.gone()
                return
            }
            container.visible()
            binding.drinksDivider.visible()

            drinks.forEach { drink ->
                val row = ItemOrderHistoryDrinkBinding.inflate(
                    LayoutInflater.from(container.context), container, true
                )

                row.drinkImage.load(drink.drinkImageUrl)
                row.drinkName.text = drink.drinkName
                row.drinkPrice.text = (drink.drinkPrice ?: 0.0).formatToPrice().plus(" UZS")

                if (container.childCount > 1) {
                    (row.root.layoutParams as ViewGroup.MarginLayoutParams).topMargin =
                        container.resources.getDimensionPixelSize(R.dimen.order_history_drink_gap)
                }
            }
        }

        /**
         * What the order gave back and what it still asks for. The footer disappears entirely
         * when it would be empty, so a plain settled order stays a two-line card.
         */
        private fun bindFooter(item: OrderHistoryItemData) {
            val cashback = item.cashbackEarned ?: 0.0
            val earned = cashback > 0.0
            // Emptied rather than hidden: this is the row's flexible half, and it is what
            // holds the button against the end edge on an order that earned nothing.
            binding.cashback.text =
                if (earned) "+".plus(cashback.formatToPrice()).plus(" UZS") else ""

            // `hasFeedback` is the server's own answer to "is a rating still owed", so it
            // decides this — except on an order that never happened, which there is nothing
            // to rate.
            val rateable = item.hasFeedback != true && item.commonStatus !in UNRATEABLE_STATUSES
            binding.rate.isVisible = rateable
            binding.rate.setOnClickListener { onRateClickListener?.invoke(item) }

            binding.footer.isVisible = earned || rateable
        }
    }

    private companion object {

        /** Orders that were cancelled or never paid for: no drink was ever served. */
        val UNRATEABLE_STATUSES = setOf(
            OrderStatus.PendingPayment,
            OrderStatus.PaymentFailed,
            OrderStatus.PaymentExpired,
            OrderStatus.Cancelled,
            OrderStatus.Error
        )

        /** Label and colour for an order status, or null when we have no wording for it. */
        fun statusLabel(status: String?): Pair<Int, Int>? = when (status) {
            OrderStatus.PendingPayment ->
                uz.alphazet.domain.R.string.pending_payment to uz.alphazet.domain.R.color.purple_80

            OrderStatus.Paid, OrderStatus.Completed ->
                uz.alphazet.domain.R.string.completed to uz.alphazet.domain.R.color.green_80

            OrderStatus.Pending ->
                uz.alphazet.domain.R.string.pending to uz.alphazet.domain.R.color.purple_80

            OrderStatus.PaymentFailed ->
                uz.alphazet.domain.R.string.payment_failed to uz.alphazet.domain.R.color.error_300

            OrderStatus.PaymentExpired ->
                uz.alphazet.domain.R.string.payment_expired to uz.alphazet.domain.R.color.error_300

            OrderStatus.Cancelled ->
                uz.alphazet.domain.R.string.cancelled to uz.alphazet.domain.R.color.error_300

            OrderStatus.Error ->
                uz.alphazet.domain.R.string.error to uz.alphazet.domain.R.color.error_300

            "created" ->
                uz.alphazet.domain.R.string.created to uz.alphazet.domain.R.color.purple_80

            "preparing" ->
                uz.alphazet.domain.R.string.preparing to uz.alphazet.domain.R.color.blue_80

            // History-only wording: the drink was ordered and the order has moved on.
            "ordered" ->
                uz.alphazet.domain.R.string.ordered to uz.alphazet.domain.R.color.purple_80

            else -> null
        }
    }
}

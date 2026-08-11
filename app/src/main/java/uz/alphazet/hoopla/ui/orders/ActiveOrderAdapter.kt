package uz.alphazet.hoopla.ui.orders

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import coil3.load
import uz.alphazet.data.models.order.OrderItemData
import uz.alphazet.domain.rv.BaseAdapter
import uz.alphazet.domain.rv.BaseVH
import uz.alphazet.domain.utils.PickupTime
import uz.alphazet.domain.utils.getDateHHmm
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.visible
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ItemActiveOrderBinding

class ActiveOrderAdapter : BaseAdapter<OrderItemData>() {

    private var onActionClickListener: ((OrderItemData) -> Unit)? = null

    /**
     * The card's primary button. What it does depends on the order's
     * [stage][ActiveOrderStage] — pay, show the pickup QR, or open the details.
     */
    fun setOnActionClickListener(l: (OrderItemData) -> Unit) {
        onActionClickListener = l
    }

    override fun onCreateViewHolder(view: View): BaseVH = VH(ItemActiveOrderBinding.bind(view))

    override fun getItemViewType(position: Int): Int = R.layout.item_active_order

    inner class VH(private val binding: ItemActiveOrderBinding) : BaseVH(binding.root) {
        override fun bind(position: Int) {
            val item = getItem(absoluteAdapterPosition) ?: return
            val context = binding.root.context
            val stage = ActiveOrderStage.of(item.orderStatus)

            // The number is what the barista asks for at the counter; without one the card still
            // needs a label, and "current order" is what the customer would call it anyway.
            binding.orderNumber.text = item.id?.let {
                context.getString(uz.alphazet.domain.R.string.order_number_value, it)
            } ?: context.getString(uz.alphazet.domain.R.string.current_order)
            binding.status.setText(ActiveOrderStage.labelRes(item.orderStatus))
            ViewCompat.setBackgroundTintList(
                binding.statusDot,
                ContextCompat.getColorStateList(context, stage.dotColorRes)
            )

            binding.image.load(item.shopIconUrl)
            binding.drinkName.text = item.drinkName
            val subtitle = item.subtitle(context)
            binding.subtitle.text = subtitle
            if (subtitle.isBlank()) binding.subtitle.gone() else binding.subtitle.visible()

            binding.step1.setStepFilled(stage.step >= 1)
            binding.step2.setStepFilled(stage.step >= 2)
            binding.step3.setStepFilled(stage.step >= 3)

            binding.action.setText(stage.actionLabelRes)
            binding.action.setIconResource(stage.actionIconRes)
            binding.action.setOnClickListener { onActionClickListener?.invoke(item) }
        }
    }
}

private fun View.setStepFilled(filled: Boolean) {
    setBackgroundResource(
        if (filled) R.drawable.bg_active_order_step_on else R.drawable.bg_active_order_step_off
    )
}

/**
 * "Union Cafe · Получение: 17:30" — where the order is waiting, and when it is due.
 *
 * A scheduled pickup is the time the customer committed to, so it wins over the "ordered at"
 * stamp; an ASAP order has no promised time, and the stamp is what makes the wait legible.
 * Either part can be missing, so the separator is only drawn between the parts that exist.
 */
private fun OrderItemData.subtitle(context: Context): String {
    val pickupAt = PickupTime.parse(pickupAt)
    val time = when {
        // Within today the bare time reads best; a slot that fell to tomorrow needs its date,
        // otherwise "09:00" looks like a pickup that is already overdue.
        pickupAt != null -> context.getString(
            uz.alphazet.domain.R.string.pickup_at_value,
            if (PickupTime.dayOffset(pickupAt) == 0) PickupTime.formatForDisplay(pickupAt)
            else PickupTime.formatDayTimeForDisplay(pickupAt)
        )

        purchasedAtUnix != null -> context.getString(
            uz.alphazet.domain.R.string.ordered_at_value,
            purchasedAtUnix?.getDateHHmm()
        )

        else -> null
    }
    return listOfNotNull(shopName?.takeIf { it.isNotBlank() }, time).joinToString(" · ")
}

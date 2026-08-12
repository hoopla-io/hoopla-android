package uz.alphazet.hoopla.ui.shop_details

import android.view.View
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import uz.alphazet.data.models.DrinkItemData
import uz.alphazet.domain.rv.BaseAdapter
import uz.alphazet.domain.rv.BaseVH
import uz.alphazet.domain.utils.formatToPrice
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ItemDrinkVerticalBinding

/**
 * A cart line the menu card may step in place: the drink is in the cart exactly once and was
 * added without modifiers, so there is no question which line a "+" or "−" means.
 */
data class CartLineRef(val itemId: Int, val quantity: Int)

class DrinksAdapter : BaseAdapter<DrinkItemData>() {

    override fun onCreateViewHolder(view: View): BaseVH {
        return VH(ItemDrinkVerticalBinding.bind(view))
    }

    override fun getItemViewType(position: Int): Int = R.layout.item_drink_vertical

    /** Whether this shop is currently taking orders; false leaves the cards read-only. */
    var isClickable = true
        set(value) {
            if (field == value) return
            field = value
            rebindRows()
        }

    /**
     * drinkId -> the one line holding it. DiffUtil keys on [DrinkItemData.uniqueId], which knows
     * nothing about the cart, so a change here has to re-bind the rows itself.
     */
    var cartLines: Map<Int, CartLineRef> = emptyMap()
        set(value) {
            if (field == value) return
            field = value
            rebindRows()
        }

    /** Drinks whose "does this need options?" round-trip is still in the air. */
    var pendingAdds: Set<Int> = emptySet()
        set(value) {
            if (field == value) return
            field = value
            rebindRows()
        }

    private var recyclerView: RecyclerView? = null

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
    }

    /** Cart updates can land mid-layout, and notifying then throws — so it waits for the frame. */
    private fun rebindRows() {
        val view = recyclerView
        if (view != null && (view.isComputingLayout || view.isLayoutRequested)) {
            view.post { notifyItemRangeChanged(0, itemCount) }
        } else {
            notifyItemRangeChanged(0, itemCount)
        }
    }

    private var onAddClickListener: ((DrinkItemData) -> Unit)? = null

    /** Reports the quantity the line should have; 0 means "drop this line". */
    private var onQuantityChangeListener: ((CartLineRef, Int) -> Unit)? = null

    fun setOnAddClickListener(l: (DrinkItemData) -> Unit) {
        onAddClickListener = l
    }

    fun setOnQuantityChangeListener(l: (CartLineRef, Int) -> Unit) {
        onQuantityChangeListener = l
    }

    inner class VH(private val binding: ItemDrinkVerticalBinding) : BaseVH(binding.root) {

        /** The row's drink as the list holds it now, or null once the row has been detached. */
        private fun currentItem(): DrinkItemData? =
            absoluteAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }
                ?.let { currentList.getOrNull(it) }

        override fun bind(position: Int) {
            val itemData = currentItem() ?: return

            binding.name.text = itemData.name
            binding.image.load(itemData.pictureUrl)
            binding.price.text = itemData.productPrice?.formatToPrice().plus(" UZS")

            val drinkId = itemData.id
            val line = drinkId?.let { cartLines[it] }
            val adding = drinkId != null && drinkId in pendingAdds

            // A shop that is closed or paused refuses every drink tap, so it offers no controls.
            val stepping = isClickable && line != null && !adding
            binding.stepper.isVisible = stepping
            binding.addProgress.isVisible = adding

            // "add" is only ever GONE in favour of the stepper. Hiding it any other way keeps it
            // INVISIBLE so it still holds its space: action_barrier ignores gone widgets, and with
            // every one of them gone it would collapse and squeeze the price to nothing.
            binding.add.visibility = when {
                stepping -> View.GONE
                adding || !isClickable -> View.INVISIBLE
                else -> View.VISIBLE
            }

            if (line != null) binding.quantity.text = line.quantity.toString()

            binding.add.setOnClickListener {
                val current = currentItem() ?: return@setOnClickListener
                onAddClickListener?.invoke(current)
            }

            // Both step from the line as it stands now, not from the value captured at bind:
            // a tap arriving after a re-bind would otherwise work off a stale count.
            binding.minus.setOnClickListener {
                val current = currentLine() ?: return@setOnClickListener
                onQuantityChangeListener?.invoke(current, (current.quantity - 1).coerceAtLeast(0))
            }
            binding.plus.setOnClickListener {
                val current = currentLine() ?: return@setOnClickListener
                onQuantityChangeListener?.invoke(current, current.quantity + 1)
            }
        }

        private fun currentLine(): CartLineRef? = currentItem()?.id?.let { cartLines[it] }
    }
}

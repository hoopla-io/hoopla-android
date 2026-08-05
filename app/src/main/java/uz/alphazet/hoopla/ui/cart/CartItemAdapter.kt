package uz.alphazet.hoopla.ui.cart

import android.view.View
import android.widget.ImageView
import androidx.core.view.setPadding
import androidx.recyclerview.widget.RecyclerView
import coil3.load
import coil3.request.error
import coil3.request.placeholder
import uz.alphazet.data.models.cart.CartItemData
import uz.alphazet.domain.rv.BaseAdapter
import uz.alphazet.domain.rv.BaseVH
import uz.alphazet.domain.utils.formatToPrice
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.visible
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ItemCartBinding

/**
 * One row per cart line. BaseAdapter only wires a click on the whole row, so the quantity and
 * delete controls get their own listeners — the same shape DeviceAdapter uses for its revoke
 * action.
 */
class CartItemAdapter : BaseAdapter<CartItemData>() {

    /**
     * drinkId -> picture, resolved from the shop's menu because the cart response carries no
     * images. Setting it re-binds the visible rows; unknown drinks keep the placeholder.
     */
    var drinkImages: Map<Int, String> = emptyMap()
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

    /**
     * The images arrive from their own request, which can land mid-layout — notifying then
     * throws, so it waits for the frame to finish.
     */
    private fun rebindRows() {
        val view = recyclerView
        if (view != null && (view.isComputingLayout || view.isLayoutRequested)) {
            view.post { notifyItemRangeChanged(0, itemCount) }
        } else {
            notifyItemRangeChanged(0, itemCount)
        }
    }

    /** Reports the quantity the line should have; 0 means "remove this line". */
    private var onQuantityChangeListener: ((CartItemData, Int) -> Unit)? = null
    private var onRemoveClickListener: ((CartItemData) -> Unit)? = null

    fun setOnQuantityChangeListener(l: (CartItemData, Int) -> Unit) {
        onQuantityChangeListener = l
    }

    fun setOnRemoveClickListener(l: (CartItemData) -> Unit) {
        onRemoveClickListener = l
    }

    override fun onCreateViewHolder(view: View): BaseVH = VH(ItemCartBinding.bind(view))

    override fun getItemViewType(position: Int): Int = R.layout.item_cart

    inner class VH(private val binding: ItemCartBinding) : BaseVH(binding.root) {

        /** The row's item as the list holds it now, or null once the row has been detached. */
        private fun currentItem(): CartItemData? =
            absoluteAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }
                ?.let { currentList.getOrNull(it) }

        override fun bind(position: Int) {
            val item = currentItem() ?: return

            binding.name.text = item.name

            // The placeholder stays for drinks the menu did not resolve — a cart row without a
            // picture still reads fine, so a miss is not worth surfacing.
            val imageUrl = item.drinkId?.let { drinkImages[it] }
            if (imageUrl == null) {
                binding.image.setImageResource(uz.alphazet.domain.R.drawable.ic_coffee_cup)
                binding.image.setPadding(PLACEHOLDER_PADDING_PX)
                binding.image.scaleType = ImageView.ScaleType.CENTER_INSIDE
            } else {
                binding.image.setPadding(0)
                binding.image.scaleType = ImageView.ScaleType.CENTER_CROP
                binding.image.load(imageUrl) {
                    placeholder(uz.alphazet.domain.R.drawable.ic_coffee_cup)
                    error(uz.alphazet.domain.R.drawable.ic_coffee_cup)
                }
            }

            // Every modifier the line was added with, on one line under the name. The cart
            // response has no ids for these, so they are display-only.
            val modifiers = item.modifiers.orEmpty().mapNotNull { it.name?.takeIf(String::isNotBlank) }
            if (modifiers.isEmpty()) {
                binding.modifiers.gone()
            } else {
                binding.modifiers.visible()
                binding.modifiers.text = modifiers.joinToString(", ")
            }

            val quantity = item.quantity ?: 0
            binding.quantity.text = quantity.toString()
            binding.lineTotal.text = (item.lineTotal ?: 0.0).formatToPrice().plus(" UZS")

            // The listeners re-read the row rather than closing over `item`/`quantity`: a tap
            // arriving after the list was rebound would otherwise step from a stale count.
            binding.minus.setOnClickListener {
                val current = currentItem() ?: return@setOnClickListener
                // Stepping below one is how a line is dropped, so the server is told 0 rather
                // than being sent an invalid quantity.
                onQuantityChangeListener
                    ?.invoke(current, ((current.quantity ?: 0) - 1).coerceAtLeast(0))
            }
            binding.plus.setOnClickListener {
                val current = currentItem() ?: return@setOnClickListener
                onQuantityChangeListener?.invoke(current, (current.quantity ?: 0) + 1)
            }
            binding.remove.setOnClickListener {
                val current = currentItem() ?: return@setOnClickListener
                onRemoveClickListener?.invoke(current)
            }
        }
    }

    private companion object {
        /** Inset for the placeholder glyph, which is an icon rather than a photo. */
        const val PLACEHOLDER_PADDING_PX = 16
    }

}

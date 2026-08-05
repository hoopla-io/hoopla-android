package uz.alphazet.hoopla.ui.cart

import android.view.View
import androidx.recyclerview.widget.RecyclerView
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

}

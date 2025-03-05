package uz.alphazet.hoopla.ui.qr_code

import android.view.View
import uz.alphazet.data.models.OrderItemData
import uz.alphazet.domain.rv.BaseAdapter
import uz.alphazet.domain.rv.BaseVH
import uz.alphazet.domain.utils.getDateDDMMMMYYYYHHmm
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ItemOrderBinding

class OrderAdapter : BaseAdapter<OrderItemData>() {

    override fun onCreateViewHolder(view: View): BaseVH {
        return VH(ItemOrderBinding.bind(view))
    }

    override fun getItemViewType(position: Int): Int = R.layout.item_order

    inner class VH(private val binding: ItemOrderBinding) : BaseVH(binding.root) {
        override fun bind(position: Int) {
            val itemData = getItem(absoluteAdapterPosition) ?: return

            binding.partnerName.text = itemData.partnerName
            binding.shopName.text = itemData.shopName

            binding.time.text = itemData.purchasedAtUnix?.getDateDDMMMMYYYYHHmm()

        }
    }

}
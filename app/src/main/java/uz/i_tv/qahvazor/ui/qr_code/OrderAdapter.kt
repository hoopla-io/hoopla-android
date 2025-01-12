package uz.i_tv.qahvazor.ui.qr_code

import android.view.View
import uz.i_tv.data.models.OrderItemData
import uz.i_tv.domain.rv.BaseAdapter
import uz.i_tv.domain.rv.BaseVH
import uz.i_tv.domain.utils.getDateDDMMMMYYYYHHmm
import uz.i_tv.qahvazor.R
import uz.i_tv.qahvazor.databinding.ItemOrderBinding

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
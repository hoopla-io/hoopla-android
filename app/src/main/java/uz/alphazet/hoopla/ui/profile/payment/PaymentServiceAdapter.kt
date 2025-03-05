package uz.alphazet.hoopla.ui.profile.payment

import android.view.View
import coil3.load
import uz.alphazet.data.models.PaymentServiceItemData
import uz.alphazet.domain.rv.BaseAdapter
import uz.alphazet.domain.rv.BaseVH
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ItemPaymentServiceBinding

class PaymentServiceAdapter : BaseAdapter<PaymentServiceItemData>() {

    override fun onCreateViewHolder(view: View): BaseVH {
        return VH(ItemPaymentServiceBinding.bind(view))
    }

    override fun getItemViewType(position: Int): Int = R.layout.item_payment_service

    inner class VH(private val binding: ItemPaymentServiceBinding) : BaseVH(binding.root) {
        override fun bind(position: Int) {
            val itemData = getItem(absoluteAdapterPosition) ?: return

            binding.image.load(itemData.logoUrl)
        }
    }


}
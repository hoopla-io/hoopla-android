package uz.i_tv.qahvazor.ui.profile.payment

import android.view.View
import coil3.load
import uz.i_tv.data.models.PaymentServiceItemData
import uz.i_tv.domain.rv.BaseAdapter
import uz.i_tv.domain.rv.BaseVH
import uz.i_tv.qahvazor.R
import uz.i_tv.qahvazor.databinding.ItemPaymentServiceBinding

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
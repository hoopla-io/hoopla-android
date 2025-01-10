package uz.i_tv.qahvazor.ui.shop_details

import android.view.View
import uz.i_tv.data.models.ShopData
import uz.i_tv.domain.rv.BaseAdapter
import uz.i_tv.domain.rv.BaseVH
import uz.i_tv.qahvazor.R
import uz.i_tv.qahvazor.databinding.ItemWorkTimeBinding

class WorkTimeAdapter : BaseAdapter<ShopData.WorkHour>() {

    override fun onCreateViewHolder(view: View): BaseVH {
        return VH(ItemWorkTimeBinding.bind(view))
    }

    override fun getItemViewType(position: Int): Int = R.layout.item_work_time

    inner class VH(private val binding: ItemWorkTimeBinding) : BaseVH(binding.root) {
        override fun bind(position: Int) {
            val itemData = getItem(absoluteAdapterPosition) ?: return

            binding.days.text = itemData.weekDay
            binding.hours.text = itemData.openAt?.plus(" - ")?.plus(itemData.closeAt)

        }
    }


}
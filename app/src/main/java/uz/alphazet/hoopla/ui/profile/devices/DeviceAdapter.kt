package uz.alphazet.hoopla.ui.profile.devices

import android.view.View
import uz.alphazet.data.models.DeviceSessionData
import uz.alphazet.domain.rv.BaseAdapter
import uz.alphazet.domain.rv.BaseVH
import uz.alphazet.domain.utils.getDateDDMMMMYYYYHHmm
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.visible
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ItemDeviceBinding

class DeviceAdapter : BaseAdapter<DeviceSessionData>() {

    /** Session id of the device the app is currently running on (best-effort match). */
    var currentDeviceId: Int? = null

    private var onRevokeClickListener: ((DeviceSessionData) -> Unit)? = null

    fun setOnRevokeClickListener(l: (DeviceSessionData) -> Unit) {
        onRevokeClickListener = l
    }

    override fun onCreateViewHolder(view: View): BaseVH = VH(ItemDeviceBinding.bind(view))

    override fun getItemViewType(position: Int): Int = R.layout.item_device

    inner class VH(private val binding: ItemDeviceBinding) : BaseVH(binding.root) {
        override fun bind(position: Int) {
            val item = getItem(absoluteAdapterPosition) ?: return
            val context = binding.root.context

            binding.name.text =
                item.deviceName ?: context.getString(uz.alphazet.domain.R.string.unknown_device)

            binding.subtitle.text = listOfNotNull(
                item.platform,
                item.appVersion?.let { "v$it" },
                item.ip
            ).joinToString(" · ")

            binding.lastActive.text = item.lastActiveAt?.let {
                context.getString(
                    uz.alphazet.domain.R.string.last_active,
                    it.getDateDDMMMMYYYYHHmm()
                )
            }

            val isCurrent = item.id != null && item.id == currentDeviceId
            if (isCurrent) {
                binding.thisDevice.visible()
                binding.revoke.gone()
            } else {
                binding.thisDevice.gone()
                binding.revoke.visible()
                binding.revoke.setOnClickListener { onRevokeClickListener?.invoke(item) }
            }
        }
    }

}

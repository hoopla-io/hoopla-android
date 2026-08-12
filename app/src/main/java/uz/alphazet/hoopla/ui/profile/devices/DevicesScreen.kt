package uz.alphazet.hoopla.ui.profile.devices

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.DeviceSessionData
import uz.alphazet.domain.ui.BaseFragment
import uz.alphazet.domain.ui.showRequestDF
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.visible
import uz.alphazet.domain.viewbinding.viewBinding
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ScreenDevicesBinding
import uz.alphazet.hoopla.ui.popScreen

class DevicesScreen : BaseFragment(R.layout.screen_devices), SwipeRefreshLayout.OnRefreshListener {

    private val binding by viewBinding(ScreenDevicesBinding::bind)
    private val viewModel: DevicesVM by viewModel()

    private val adapter = DeviceAdapter()

    override fun initialize() {
        binding.deviceRv.adapter = adapter
        binding.swipeRefreshLayout.setOnRefreshListener(this)

        binding.toolbar.setNavigationOnClickListener { popScreen() }

        adapter.setOnRevokeClickListener { device ->
            val id = device.id ?: return@setOnRevokeClickListener
            showRequestDF(
                getString(uz.alphazet.domain.R.string.log_out),
                getString(uz.alphazet.domain.R.string.question_revoke_device),
                getString(uz.alphazet.domain.R.string.yes),
                getString(uz.alphazet.domain.R.string.no)
            ) {
                launch {
                    viewModel.revokeDevice(id).collectLatest(::collectRevoke)
                }
            }
        }

        viewModel.getDevices()

        launch {
            viewModel.devicesFlow.collectLatest(::collectDevices)
        }
    }

    private fun collectDevices(t: UIResource<List<DeviceSessionData>>) = t.collect {
        val list = it.orEmpty()
        adapter.currentDeviceId = viewModel.findCurrentDeviceId(list)
        adapter.submitList(list)
        if (list.isEmpty()) binding.emptyState.visible() else binding.emptyState.gone()
    }

    private fun collectRevoke(t: UIResource<Any>) = t.collect {
        viewModel.getDevices()
    }

    override fun onRefresh() {
        viewModel.getDevices()
    }

    override fun showLoading() {
        binding.swipeRefreshLayout.isRefreshing = true
    }

    override fun hideLoading() {
        binding.swipeRefreshLayout.isRefreshing = false
    }

}

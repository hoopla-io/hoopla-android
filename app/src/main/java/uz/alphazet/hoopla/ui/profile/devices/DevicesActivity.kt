package uz.alphazet.hoopla.ui.profile.devices

import android.os.Bundle
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.DeviceSessionData
import uz.alphazet.domain.ui.BaseActivity
import uz.alphazet.domain.ui.showRequestDF
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.visible
import uz.alphazet.hoopla.databinding.ScreenDevicesBinding

class DevicesActivity : BaseActivity(), SwipeRefreshLayout.OnRefreshListener {

    private lateinit var binding: ScreenDevicesBinding
    private val viewModel: DevicesVM by viewModel()

    private val adapter = DeviceAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ScreenDevicesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.deviceRv.adapter = adapter
        binding.swipeRefreshLayout.setOnRefreshListener(this)

        binding.toolbar.setNavigationOnClickListener { finish() }

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

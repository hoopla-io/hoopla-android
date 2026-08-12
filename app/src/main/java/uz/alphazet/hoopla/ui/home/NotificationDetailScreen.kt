package uz.alphazet.hoopla.ui.home

import androidx.core.os.bundleOf
import coil3.load
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.NotificationDetail
import uz.alphazet.domain.ui.BaseFragment
import uz.alphazet.domain.utils.Constants
import uz.alphazet.domain.viewbinding.viewBinding
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ScreenNotificationDetailBinding
import uz.alphazet.hoopla.ui.popScreen

class NotificationDetailScreen : BaseFragment(R.layout.screen_notification_detail) {

    private val binding by viewBinding(ScreenNotificationDetailBinding::bind)
    private val viewModel: NotificationVM by viewModel()

    private val notificationId by lazy { arguments?.getInt(Constants.ID, -1) ?: -1 }

    override fun initialize() {
        binding.toolbar.setNavigationOnClickListener { popScreen() }

        viewModel.getNotificationDetail(notificationId)

        launch {
            viewModel.notificationDetailFlow.collectLatest(::collectData)
        }
    }

    private fun collectData(t: UIResource<NotificationDetail>) = t.collect { data ->
        binding.image.load(data?.files?.imageUrl)
        binding.name.text = data?.notificationTitle
        binding.desc.text = data?.notificationDescription
    }

    companion object {
        fun newInstance(id: Int) = NotificationDetailScreen().apply {
            arguments = bundleOf(Constants.ID to id)
        }
    }

}

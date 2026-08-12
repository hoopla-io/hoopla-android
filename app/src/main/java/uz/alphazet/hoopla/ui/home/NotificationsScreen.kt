package uz.alphazet.hoopla.ui.home

import androidx.paging.PagingData
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.models.NotificationItemData
import uz.alphazet.domain.ui.BaseFragment
import uz.alphazet.domain.viewbinding.viewBinding
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ScreenNotificationsBinding
import uz.alphazet.hoopla.ui.navigateTo
import uz.alphazet.hoopla.ui.popScreen

class NotificationsScreen : BaseFragment(R.layout.screen_notifications) {

    private val binding by viewBinding(ScreenNotificationsBinding::bind)
    private val viewModel: NotificationVM by viewModel()

    private val adapter = NotificationAdapter()

    override fun initialize() {
        binding.notificationRv.adapter = adapter

        binding.toolbar.setNavigationOnClickListener { popScreen() }

        viewModel.markRead()

        adapter.setOnItemClickListener {
            navigateTo(NotificationDetailScreen.newInstance(it?.notificationId ?: -1))
        }

        launch {
            viewModel.getNotificationsPager().collectLatest(::collectData)
        }
    }

    private suspend fun collectData(t: PagingData<NotificationItemData>) {
        adapter.submitData(t)
    }

}

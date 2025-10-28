package uz.alphazet.hoopla.ui.home

import android.os.Bundle
import androidx.paging.PagingData
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.models.NotificationItemData
import uz.alphazet.domain.ui.BaseActivity
import uz.alphazet.hoopla.databinding.ScreenNotificationsBinding

class NotificationsScreen : BaseActivity() {

    private lateinit var binding: ScreenNotificationsBinding
    private val viewModel: NotificationVM by viewModel()

    private val adapter = NotificationAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ScreenNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.notificationRv.adapter = adapter

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        launch {
            viewModel.getNotificationsPager().collectLatest(::collectData)
        }

    }

    private suspend fun collectData(t: PagingData<NotificationItemData>) {
        adapter.submitData(t)
    }

    override fun updateStatusBarViewHeight() {
        launch {
            val statusBarHeight = getStatusBarHeight()
            binding.statusBarView.layoutParams.height = statusBarHeight
            binding.statusBarView.requestLayout()
        }
    }

}
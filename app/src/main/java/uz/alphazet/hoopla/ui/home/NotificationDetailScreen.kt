package uz.alphazet.hoopla.ui.home

import android.os.Bundle
import coil3.load
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.NotificationDetail
import uz.alphazet.domain.ui.BaseActivity
import uz.alphazet.hoopla.databinding.ScreenNotificationDetailBinding

class NotificationDetailScreen : BaseActivity() {

    private lateinit var binding: ScreenNotificationDetailBinding
    private val viewModel: NotificationVM by viewModel()

    private val id by lazy { intent.getIntExtra("id", -1) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ScreenNotificationDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setNavigationOnClickListener { finish() }

        viewModel.getNotificationDetail(id)

        launch {
            viewModel.notificationDetailFlow.collectLatest(::collectData)
        }

    }

    private fun collectData(t: UIResource<NotificationDetail>) = t.collect { data ->
        binding.image.load(data?.files?.imageUrl)
        binding.name.text = data?.notificationTitle
        binding.desc.text = data?.notificationDescription

    }

}
package uz.alphazet.hoopla.ui.profile.payment

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.PaymentServiceItemData
import uz.alphazet.domain.ui.BaseActivity
import uz.alphazet.hoopla.databinding.ScreenPaymentServicesBinding
import uz.alphazet.hoopla.ui.profile.payment.TopUpViaPaymentServiceBD.Companion.showTopUpViaPaymentServiceBD

class PaymentServicesActivity : BaseActivity() {

    private lateinit var binding: ScreenPaymentServicesBinding
    private val viewModel: PaymentVM by viewModel()
    private val adapter = PaymentServiceAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ScreenPaymentServicesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.subscriptionRv.adapter = adapter

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        adapter.setOnItemClickListener {
            showTopUpViaPaymentServiceBD(it)
        }

        lifecycleScope.launch {
            viewModel.getPaymentServices().collectLatest(::collectPaymentServices)
        }

    }

    override fun updateStatusBarViewHeight() {
        launch {
            val statusBarHeight = getStatusBarHeight()
            binding.statusBarView.layoutParams.height = statusBarHeight
            binding.statusBarView.requestLayout()
        }
    }

    private fun collectPaymentServices(t: UIResource<List<PaymentServiceItemData>>) =
        t.collect {
            adapter.submitList(it)
        }

}
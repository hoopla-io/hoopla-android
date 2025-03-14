package uz.alphazet.hoopla.ui.profile.payment

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.PaymentServiceItemData
import uz.alphazet.domain.ui.BaseFragment
import uz.alphazet.domain.viewbinding.viewBinding
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ScreenPaymentServicesBinding
import uz.alphazet.hoopla.ui.profile.payment.TopUpViaPaymentServiceBD.Companion.showTopUpViaPaymentServiceBD

class PaymentServicesScreen : BaseFragment(R.layout.screen_payment_services) {

    private val binding by viewBinding(ScreenPaymentServicesBinding::bind)
    private val viewModel: PaymentVM by viewModel()

    private val adapter = PaymentServiceAdapter()

    override fun initialize() {

        binding.subscriptionRv.adapter = adapter

        binding.toolbar.setNavigationOnClickListener {
            exit()
        }

        adapter.setOnItemClickListener {
            showTopUpViaPaymentServiceBD(it)
        }

        lifecycleScope.launch {
            viewModel.getPaymentServices().collectLatest(::collectPaymentServices)
        }

    }

    private fun collectPaymentServices(t: UIResource<List<PaymentServiceItemData>>) =
        t.collect {
            adapter.submitList(it)
        }

}
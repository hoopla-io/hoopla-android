package uz.i_tv.qahvazor.ui.profile.payment

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.i_tv.data.UIResource
import uz.i_tv.data.models.PaymentServiceItemData
import uz.i_tv.domain.ui.BaseFragment
import uz.i_tv.domain.viewbinding.viewBinding
import uz.i_tv.qahvazor.R
import uz.i_tv.qahvazor.databinding.ScreenPaymentServicesBinding
import uz.i_tv.qahvazor.ui.profile.payment.TopUpViaPaymentServiceBD.Companion.showTopUpViaPaymentServiceBD

class PaymentServicesScreen : BaseFragment(R.layout.screen_payment_services) {

    private val binding by viewBinding(ScreenPaymentServicesBinding::bind)
    private val viewModel: PaymentVM by viewModel()

    private val adapter = PaymentServiceAdapter()

    override fun initialize() {

        binding.subscriptionRv.adapter = adapter

        adapter.setOnItemClickListener {
            showTopUpViaPaymentServiceBD(it)
        }

        lifecycleScope.launch {
            viewModel.getPaymentServices().collectLatest(::collectPaymentServices)
        }

    }

    private suspend fun collectPaymentServices(t: UIResource<List<PaymentServiceItemData>>) =
        t.collect {
            adapter.submitList(it)
        }

}
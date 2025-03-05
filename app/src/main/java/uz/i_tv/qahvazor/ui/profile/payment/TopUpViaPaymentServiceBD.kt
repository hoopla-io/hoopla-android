package uz.i_tv.qahvazor.ui.profile.payment

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.i_tv.data.UIResource
import uz.i_tv.data.models.PaymentServiceCheckOutData
import uz.i_tv.data.models.PaymentServiceItemData
import uz.i_tv.domain.ui.BaseActivity
import uz.i_tv.domain.ui.BaseBottomSheetDF
import uz.i_tv.domain.ui.BaseFragment
import uz.i_tv.domain.utils.intentToBrowser
import uz.i_tv.domain.viewbinding.viewBinding
import uz.i_tv.qahvazor.R
import uz.i_tv.qahvazor.databinding.DialogTopUpViaPaymentServiceBinding

class TopUpViaPaymentServiceBD(private val serviceData: PaymentServiceItemData) :
    BaseBottomSheetDF(R.layout.dialog_top_up_via_payment_service) {

    private val binding by viewBinding(DialogTopUpViaPaymentServiceBinding::bind)
    private val viewModel: PaymentVM by viewModel()

    override fun initialize() {

        binding.title.text = getString(uz.i_tv.domain.R.string.label_top_up_via_, serviceData.name)

        binding.inputAmount.requestFocus()

        binding.btSend.setOnClickListener {
            val text = binding.inputAmount.text
            if (text.isNullOrEmpty()) return@setOnClickListener
            val amount = text.toString().toDoubleOrNull()
            if (amount != null) {
                lifecycleScope.launch {
                    viewModel.topUpViaPayService(serviceData.id ?: -1, amount)
                }
            }
        }

        lifecycleScope.launch {
            viewModel.topUpDataFlow.collectLatest(::collectData)
        }

    }

    private fun collectData(t: UIResource<PaymentServiceCheckOutData>) = t.collect {
        dismiss()
        requireContext().intentToBrowser(it?.checkoutUrl ?: "")
    }

    override fun showLoading() {
        binding.btSend.isClickable = false
        binding.btSend.startAnimation()
    }

    override fun hideLoading() {
        binding.btSend.isClickable = true
        binding.btSend.revertAnimation()
    }

    companion object {
        private const val TAG = "TopUpViaPaymentServiceBD"

        fun BaseActivity.showTopUpViaPaymentServiceBD(serviceData: PaymentServiceItemData) {
            val current = supportFragmentManager.findFragmentByTag(TAG)
            if (current == null) {
                TopUpViaPaymentServiceBD(serviceData).show(supportFragmentManager, TAG)
            }
        }

        fun BaseFragment.showTopUpViaPaymentServiceBD(serviceData: PaymentServiceItemData) {
            val current = childFragmentManager.findFragmentByTag(TAG)
            if (current == null) {
                TopUpViaPaymentServiceBD(serviceData).show(childFragmentManager, TAG)
            }
        }

    }

}
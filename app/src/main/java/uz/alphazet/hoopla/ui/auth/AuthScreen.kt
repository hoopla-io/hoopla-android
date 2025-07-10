package uz.alphazet.hoopla.ui.auth

import android.os.Bundle
import android.view.View
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.LoginSessionData
import uz.alphazet.domain.ui.BaseFragment
import uz.alphazet.domain.utils.disable
import uz.alphazet.domain.utils.enable
import uz.alphazet.domain.utils.hideKeyboard
import uz.alphazet.domain.viewbinding.viewBinding
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ScreenAuthInputPhoneBinding
import uz.alphazet.hoopla.ui.auth.ConfirmPhoneNumberScreen.Companion.PHONE_FORMATTED
import uz.alphazet.hoopla.ui.auth.ConfirmPhoneNumberScreen.Companion.SESSION_ID

class AuthScreen : BaseFragment(R.layout.screen_auth_input_phone) {

    private val binding by viewBinding(ScreenAuthInputPhoneBinding::bind)
    private val viewModel: AuthVM by viewModel()

    private var phone: String
        get() = "998" + binding.inputPhone.rawText.toString()
        set(value) {
            binding.inputPhone.setText(value)
        }

    override var forceKeyboard: Boolean = true

    override fun initialize() {
        binding.inputPhone.requestFocus()
        binding.btSend.setOnClickListener(this)
        binding.btSend.disable()

        binding.inputPhone.doOnTextChanged { text, start, before, count ->
            if (phone.length == 12) {
                binding.btSend.enable()
            } else {
                binding.btSend.disable()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.withStarted {
                launch {
                    viewModel.sendSmsFlow.collectLatest(::collectSendSmsResource)
                }
            }
        }

    }

    private fun collectSendSmsResource(t: UIResource<LoginSessionData>?) = t?.collect {
        val bundle = Bundle()
        bundle.putString(SESSION_ID, it?.sessionId)
        bundle.putString(PHONE_FORMATTED, (binding.inputPhone.text ?: "").toString())

        val screen = ConfirmPhoneNumberScreen()
        screen.arguments = bundle
        (requireActivity() as? AuthActivity)?.navigateTo(screen)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btSend -> {
                if ((phone.length < 12)) {
                    binding.inputPhoneLayout.error =
                        getString(uz.alphazet.domain.R.string.phone_number_is_not_valid)
                } else {
                    hideKeyboard()
                    viewModel.sendSms(phone)
                }
            }
        }
    }

    override fun showLoading() {
        binding.inputPhone.isClickable = false
        binding.btSend.startAnimation()
    }

    override fun hideLoading() {
        binding.inputPhone.isClickable = true
        binding.btSend.revertAnimation()
    }

}
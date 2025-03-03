package uz.i_tv.qahvazor.ui.auth

import android.view.View
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.i_tv.data.UIResource
import uz.i_tv.data.models.LoginSessionData
import uz.i_tv.domain.ui.BaseFragment
import uz.i_tv.domain.utils.disable
import uz.i_tv.domain.utils.enable
import uz.i_tv.domain.utils.hideKeyboard
import uz.i_tv.domain.viewbinding.viewBinding
import uz.i_tv.qahvazor.R
import uz.i_tv.qahvazor.databinding.ScreenAuthInputPhoneBinding
import uz.i_tv.qahvazor.ui.Screens

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
        navigateTo(
            Screens.confirmationScreen(
                it?.sessionId ?: "",
                (binding.inputPhone.text ?: "").toString()
            )
        )
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btSend -> {
                if ((phone.length < 12)) {
                    binding.inputPhoneLayout.error = "getString(R.string.error_phone_number)"
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
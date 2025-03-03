package uz.i_tv.qahvazor.ui.auth

import android.os.CountDownTimer
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.i_tv.data.UIResource
import uz.i_tv.data.models.ConfirmSMSData
import uz.i_tv.data.models.LoginSessionData
import uz.i_tv.domain.ui.BaseFragment
import uz.i_tv.domain.utils.disable
import uz.i_tv.domain.utils.intentToBrowser
import uz.i_tv.domain.utils.setTextColorRes
import uz.i_tv.domain.utils.visible
import uz.i_tv.domain.viewbinding.viewBinding
import uz.i_tv.qahvazor.R
import uz.i_tv.qahvazor.databinding.ScreenConfirmPhoneNumberBinding
import uz.i_tv.qahvazor.ui.Screens

class ConfirmPhoneNumberScreen : BaseFragment(R.layout.screen_confirm_phone_number) {

    private val binding by viewBinding(ScreenConfirmPhoneNumberBinding::bind)
    private val viewModel: AuthVM by viewModel()

    private var sessionId: String? = null
    private val phoneNumberFormatted by lazy { arguments?.getString(PHONE_FORMATTED) }

    private var time: Long = 180

    override var forceKeyboard: Boolean = true

    override fun initialize() {
        sessionId = arguments?.getString(SESSION_ID)

        countDownTimer.start()
        binding.phoneNumber.text = phoneNumberFormatted

        binding.inputCode.requestFocus()

        binding.backImg.setOnClickListener(this)
        binding.sendAgain.setOnClickListener(this)
        binding.btConfirm.setOnClickListener(this)
        binding.privacyPolicy.setOnClickListener(this)

        binding.inputCode.doAfterTextChanged {
            binding.btConfirm.performClick()
        }

        launch {
            viewModel.confirmSmsFlow.collectLatest(::collectConfirmSms)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.withStarted {
                launch {
                    viewModel.resendSmsFlow.collectLatest(::collectResendSmsResource)
                }
            }
        }

    }

    private fun collectConfirmSms(t: UIResource<ConfirmSMSData>) = t.collect(onError = {
        binding.inputCode.setItemBackground(resources.getDrawable(uz.i_tv.domain.R.drawable.bg_item_error_input_code))
        binding.inputCode.setTextColorRes(uz.i_tv.domain.R.color.error_300)
        binding.errorTextCode.visible()
    }) { confirmData ->
        cache.accessToken = confirmData?.jwt?.accessToken
        cache.refreshToken = confirmData?.jwt?.refreshToken
        cache.tokenExpireAt = confirmData?.jwt?.expireAt ?: 0L

        replaceScreen(Screens.bottomNav())
    }

    private fun collectResendSmsResource(t: UIResource<LoginSessionData>) = t?.collect {
        sessionId = it?.sessionId
        time = 180
        countDownTimer.start()
        binding.sendAgain.setText(uz.i_tv.domain.R.string.send_again_)
        binding.sendAgain.disable()
        binding.inputCode.text?.clear()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.backImg -> exit()
            R.id.send_again -> {
                viewModel.resendSms(sessionId ?: "")
            }

            R.id.btConfirm -> {
                val codeInt = code.toIntOrNull()
                if (!sessionId.isNullOrEmpty() && code.length == 5 && codeInt != null) {
                    viewModel.confirmSMS(codeInt, sessionId ?: "")
                }
            }

            R.id.privacy_policy -> {
                requireContext().intentToBrowser("https://hoopla.uz/ru/privacy-policy")
            }
        }
    }

    private val countDownTimer = object : CountDownTimer(time * 1000, 1000) {

        override fun onTick(p0: Long) {
            val minute = time / 60
            val second = time % 60
            if (p0 < 0)
                this.onFinish()
            else {
                binding.timer.text = "${checkDigit(minute)}:" + checkDigit(second)
                time--
            }
        }

        override fun onFinish() {
            binding.timer.text = ""
            binding.sendAgain.isEnabled = true
            binding.sendAgain.isClickable = true
            binding.sendAgain.text = getString(uz.i_tv.domain.R.string.send_again_)
        }

    }

    override fun onDestroyView() {
        countDownTimer.cancel()
        super.onDestroyView()
    }

    private var code: String
        get() = binding.inputCode.text?.toString() ?: ""
        set(value) {
            binding.inputCode.setText(value)
        }

    fun checkDigit(number: Long): String = if (number <= 9) {
        "0$number"
    } else {
        number.toString()
    }

    companion object {

        private const val SESSION_ID = "session_id"
        private const val PHONE_FORMATTED = "PHONE_FORMATTED"

        fun withArgs(sessionId: String, phoneNumberFormatted: String): ConfirmPhoneNumberScreen =
            ConfirmPhoneNumberScreen().apply {
                arguments =
                    bundleOf(SESSION_ID to sessionId, PHONE_FORMATTED to phoneNumberFormatted)
            }

    }

}
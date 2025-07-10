package uz.alphazet.hoopla.ui.auth

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.ConfirmSMSData
import uz.alphazet.data.models.LoginSessionData
import uz.alphazet.domain.ui.BaseFragment
import uz.alphazet.domain.utils.disable
import uz.alphazet.domain.utils.intentToBrowser
import uz.alphazet.domain.utils.setTextColorRes
import uz.alphazet.domain.utils.visible
import uz.alphazet.domain.viewbinding.viewBinding
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ScreenConfirmPhoneNumberBinding
import java.util.regex.Matcher
import java.util.regex.Pattern

class ConfirmPhoneNumberScreen : BaseFragment(R.layout.screen_confirm_phone_number) {

    private val binding by viewBinding(ScreenConfirmPhoneNumberBinding::bind)
    private val viewModel: AuthVM by viewModel()

    private val smsBroadcastReceiver by lazy { MySMSBroadcastReceiver() }

    private var sessionId: String? = null
    private val phoneNumberFormatted by lazy { arguments?.getString(PHONE_FORMATTED) }

    private var time: Long = 180

    override var forceKeyboard: Boolean = true

    override fun onStart() {
        super.onStart()

        startSMSRetrieverClient()

        smsBroadcastReceiver.init(object : MySMSBroadcastReceiver.OTPReceiveListener {
            override fun onOTPReceived(extras: Bundle?) {
                val consentIntent =
                    extras?.getParcelable<Intent>(SmsRetriever.EXTRA_CONSENT_INTENT)

                val message = extras?.get(SmsRetriever.EXTRA_SMS_MESSAGE) as String?

                if (message.isNullOrEmpty()) {
                    try {
                        // Start activity to show consent dialog to user, activity must be started in
                        // 5 minutes, otherwise you'll receive another TIMEOUT intent
                        smsReceiverLauncher.launch(consentIntent)
                    } catch (e: ActivityNotFoundException) {
                        // Handle the exception ...
                    }
                } else {
                    val pattern: Pattern = Pattern.compile("(\\d{5})")
                    //   \d is for a digit
                    //   {} is the number of digits here 5.
                    val matcher: Matcher = pattern.matcher(message)
                    var code: String? = ""
                    if (matcher.find()) {
                        code = matcher.group(0) // 5 digit number
                        binding.inputCode.setText(code)
                    }
                }
            }

            override fun onOTPTimeOut() {

            }

        })

        requireActivity().registerReceiver(
            smsBroadcastReceiver,
            IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION),
            Context.RECEIVER_EXPORTED
        )

    }

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
        binding.inputCode.setItemBackground(resources.getDrawable(uz.alphazet.domain.R.drawable.bg_item_error_input_code))
        binding.inputCode.setTextColorRes(uz.alphazet.domain.R.color.error_300)
        binding.errorTextCode.visible()
    }) { confirmData ->
        cache.accessToken = confirmData?.jwt?.accessToken
        cache.refreshToken = confirmData?.jwt?.refreshToken
        cache.tokenExpireAt = confirmData?.jwt?.expireAt ?: 0L

        requireActivity().finish()
//        replaceScreen(Screens.bottomNav())
    }

    private fun collectResendSmsResource(t: UIResource<LoginSessionData>) = t?.collect {
        sessionId = it?.sessionId
        time = 180
        countDownTimer.start()
        binding.sendAgain.setText(uz.alphazet.domain.R.string.send_again_)
        binding.sendAgain.disable()
        binding.inputCode.text?.clear()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.backImg -> requireActivity().onBackPressed()
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

    private fun startSMSRetrieverClient() {
        val client = SmsRetriever.getClient(requireActivity())
        val task: Task<Void> = client.startSmsRetriever()
        task.addOnSuccessListener { aVoid -> }
        task.addOnFailureListener { e -> }
    }

    private val smsReceiverLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                // Get SMS message content
                val message = result.data?.getStringExtra(SmsRetriever.EXTRA_SMS_MESSAGE)

                val pattern: Pattern = Pattern.compile("(\\d{5})")
                //   \d is for a digit
                //   {} is the number of digits here 5.
                val matcher: Matcher = pattern.matcher(message)
                var code: String? = ""
                if (matcher.find()) {
                    code = matcher.group(0) // 5 digit number
                    binding.inputCode.setText(code)
                }

            } else {
                Toast.makeText(context, "Otp retrieval failed", Toast.LENGTH_SHORT).show()
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
            binding.sendAgain.text = getString(uz.alphazet.domain.R.string.send_again_)
        }

    }

    override fun onDestroyView() {
        requireActivity().unregisterReceiver(smsBroadcastReceiver)
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

        const val TAG = "ConfirmPhoneNumberScreen"
        const val SESSION_ID = "session_id"
        const val PHONE_FORMATTED = "PHONE_FORMATTED"

        fun withArgs(sessionId: String, phoneNumberFormatted: String): ConfirmPhoneNumberScreen =
            ConfirmPhoneNumberScreen().apply {
                arguments =
                    bundleOf(SESSION_ID to sessionId, PHONE_FORMATTED to phoneNumberFormatted)
            }

    }

}
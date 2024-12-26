package uz.i_tv.qahvazor.ui.auth

import android.view.View
import androidx.core.widget.doAfterTextChanged
import uz.i_tv.domain.ui.BaseFragment
import uz.i_tv.domain.utils.disable
import uz.i_tv.domain.utils.enable
import uz.i_tv.domain.viewbinding.viewBinding
import uz.i_tv.qahvazor.R
import uz.i_tv.qahvazor.databinding.ScreenAuthInputPhoneBinding

class AuthScreen : BaseFragment(R.layout.screen_auth_input_phone) {

    private val binding by viewBinding(ScreenAuthInputPhoneBinding::bind)

    override fun initialize() {
        binding.inputPhone.requestFocus()
        binding.privacyPolicy.setOnClickListener(this)
        binding.btSend.setOnClickListener(this)
        binding.btSend.disable()

        binding.inputPhone.doAfterTextChanged {
            binding.inputPhone.setSelection(it?.length ?: binding.inputPhone.text?.length ?: 0)
        }

        binding.publicCheckbox.setOnCheckedChangeListener { compoundButton, isChecked ->
            if (isChecked)
                binding.btSend.enable()
            else
                binding.btSend.disable()
        }

    }

    override fun onClick(view: View) {
        when (view.id) {

        }
    }

}
package uz.alphazet.hoopla.ui.profile

import android.content.Intent
import androidx.core.widget.doOnTextChanged
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.UserData
import uz.alphazet.domain.ui.BaseFragment
import uz.alphazet.domain.ui.showRequestDF
import uz.alphazet.domain.utils.disable
import uz.alphazet.domain.utils.enable
import uz.alphazet.domain.viewbinding.viewBinding
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ScreenEditProfileBinding
import uz.alphazet.hoopla.ui.auth.AuthActivity
import uz.alphazet.hoopla.ui.popScreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EditProfileScreen : BaseFragment(R.layout.screen_edit_profile) {

    private val binding by viewBinding(ScreenEditProfileBinding::bind)
    private val viewModel: ProfileVM by viewModel()

    private var oldUserData: UserData? = null

    override fun initialize() {
        viewModel.editMe()
        launch {
            viewModel.userDataFlow.collectLatest(::collectUserData)
        }

        binding.btSend.disable()

        binding.toolbar.setNavigationOnClickListener {
            popScreen()
        }
    }

    private fun collectUserData(t: UIResource<UserData>) = t.collect { data ->
        oldUserData = data
        binding.inputName.setText(data?.name)
        if (data?.dateOfBirth != null) {
            binding.inputBirth.text = data.dateOfBirth
        }
        when (data?.gender) {
            "male" -> binding.genderSpinner.selectItemByIndex(0)
            "female" -> binding.genderSpinner.selectItemByIndex(1)
            null -> binding.genderSpinner.text = getString(uz.alphazet.domain.R.string.unknown)
        }

        binding.genderSpinner.dismissWhenNotifiedItemSelected = true

        binding.inputBirth.setOnClickListener {
            showDatePickerDialog(data?.dateOfBirth)
        }

        binding.genderSpinner.setOnSpinnerItemSelectedListener<String> { oldIndex, oldItem, newIndex, newText ->
            onChangedUserData()
        }

        binding.inputName.doOnTextChanged { text: CharSequence?, start: Int, before: Int, count: Int ->
            onChangedUserData()
        }

        binding.btSend.setOnClickListener {
            val name: String? = binding.inputName.text.toString()
            val gender = when (binding.genderSpinner.selectedIndex) {
                0 -> "male"
                1 -> "female"
                else -> null
            }
            val birth: String? = binding.inputBirth.text.toString()
            launch {
                viewModel.updateMe(name, gender, birth).collectLatest(::collectUpdateData)
            }
        }

        binding.btDeleteAccount.setOnClickListener {
            showRequestDF(
                title = getString(uz.alphazet.domain.R.string.delete_account_confirm_title),
                message = getString(uz.alphazet.domain.R.string.delete_account_confirm_desc),
                yes = getString(uz.alphazet.domain.R.string.yes),
                no = getString(uz.alphazet.domain.R.string.cancel)
            ) {
                launch {
                    viewModel.deactivate().collectLatest(::collectDeactivateData)
                }
            }
        }
    }

    private fun collectUpdateData(t: UIResource<Any>) = t.collect {
        popScreen()
    }

    private fun collectDeactivateData(t: UIResource<Any>) = t.collect {
        cache.clearTokens()
        val intent = Intent(requireContext(), AuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }

    private fun onChangedUserData() {
        val name = binding.inputName.text.toString()
        val gender = when (binding.genderSpinner.selectedIndex) {
            0 -> "male"
            1 -> "female"
            else -> null
        }
        val birth = binding.inputBirth.text.toString()

        if (oldUserData?.name.equals(name)
            && oldUserData?.gender.equals(gender)
            && oldUserData?.dateOfBirth.equals(birth)
        ) {
            binding.btSend.disable()
        } else {
            binding.btSend.enable()
        }
    }

    private fun showDatePickerDialog(birthday: String?) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        val selectedDate = birthday?.let {
            try {
                sdf.parse(it)?.time ?: MaterialDatePicker.todayInUtcMilliseconds()
            } catch (e: Exception) {
                MaterialDatePicker.todayInUtcMilliseconds()
            }
        } ?: MaterialDatePicker.todayInUtcMilliseconds()

        val today = MaterialDatePicker.todayInUtcMilliseconds()
        val constraints = CalendarConstraints.Builder()
            .setEnd(today)
            .setValidator(DateValidatorPointBackward.now())
            .build()

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select your birthday")
            .setSelection(selectedDate)
            .setCalendarConstraints(constraints)
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val formatted = sdf.format(Date(selection))
            binding.inputBirth.text = formatted
            onChangedUserData()
        }

        datePicker.show(childFragmentManager, "BIRTHDAY_PICKER")
    }

}

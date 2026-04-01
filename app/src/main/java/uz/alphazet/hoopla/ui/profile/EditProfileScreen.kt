package uz.alphazet.hoopla.ui.profile

import android.content.Intent
import android.os.Bundle
import androidx.core.widget.doOnTextChanged
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.flow.collectLatest
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.UserData
import uz.alphazet.domain.cache.AppCache
import uz.alphazet.domain.ui.BaseActivity
import uz.alphazet.domain.ui.showRequestDF
import uz.alphazet.domain.utils.disable
import uz.alphazet.domain.utils.enable
import uz.alphazet.hoopla.databinding.ScreenEditProfileBinding
import uz.alphazet.hoopla.ui.auth.AuthActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EditProfileScreen : BaseActivity() {

    private lateinit var binding: ScreenEditProfileBinding
    private val viewModel: ProfileVM by viewModel()
    private val cache: AppCache by inject()

    private var oldUserData: UserData? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ScreenEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel.editMe()
        launch {
            viewModel.userDataFlow.collectLatest(::collectUserData)
        }

        binding.btSend.disable()

        binding.toolbar.setNavigationOnClickListener {
            finish()
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
        setResult(PROFILE_EDIT_RESULT)
        finish()
    }

    private fun collectDeactivateData(t: UIResource<Any>) = t.collect {
        cache.clearTokens()
        val intent = Intent(this, AuthActivity::class.java).apply {
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

        datePicker.show(supportFragmentManager, "BIRTHDAY_PICKER")
    }

    companion object {
        const val PROFILE_EDIT_RESULT = 234
    }

}
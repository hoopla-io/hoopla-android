package uz.alphazet.hoopla.ui.profile

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.core.widget.doOnTextChanged
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.UserData
import uz.alphazet.domain.ui.BaseActivity
import uz.alphazet.domain.utils.disable
import uz.alphazet.domain.utils.enable
import uz.alphazet.hoopla.databinding.ScreenEditProfileBinding
import java.util.Calendar

class EditProfileScreen : BaseActivity() {

    private lateinit var binding: ScreenEditProfileBinding
    private val viewModel: ProfileVM by viewModel()

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

        binding.inputBirth.setOnClickListener {
            showDatePickerDialog()
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
    }

    private fun collectUpdateData(t: UIResource<Any>) = t.collect {
        setResult(PROFILE_EDIT_RESULT)
        finish()
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

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val date = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                binding.inputBirth.text = date
                onChangedUserData()
            },
            year, month, day
        )

        datePickerDialog.show()
    }

    companion object {
        const val PROFILE_EDIT_RESULT = 234
    }

}
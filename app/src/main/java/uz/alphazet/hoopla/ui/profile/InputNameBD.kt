package uz.alphazet.hoopla.ui.profile

import android.content.DialogInterface
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.domain.ui.BaseBottomSheetDF
import uz.alphazet.domain.ui.BaseFragment
import uz.alphazet.domain.utils.disable
import uz.alphazet.domain.utils.enable
import uz.alphazet.domain.viewbinding.viewBinding
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.DialogInputNameBinding

class InputNameBD(
    private val onCompleted: () -> Unit = {}
) : BaseBottomSheetDF(R.layout.dialog_input_name) {

    private val binding by viewBinding(DialogInputNameBinding::bind)
    private val viewModel: ProfileVM by viewModel()

    override var forceKeyboard: Boolean = true

    override fun initialize() {
        binding.btSave.disable()

        binding.inputName.requestFocus()

        binding.inputName.doAfterTextChanged { text ->
            if (text.isNullOrBlank()) binding.btSave.disable() else binding.btSave.enable()
        }

        binding.btSave.setOnClickListener {
            val name = binding.inputName.text?.toString()?.trim().orEmpty()
            if (name.isEmpty()) return@setOnClickListener
            launch {
                viewModel.updateMe(name, null, null).collectLatest(::collectUpdate)
            }
        }
    }

    private fun collectUpdate(t: UIResource<Any>) = t.collect {
        dismiss()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onCompleted()
    }

    override fun showLoading() {
        binding.btSave.isClickable = false
        binding.btSave.startAnimation()
    }

    override fun hideLoading() {
        binding.btSave.isClickable = true
        binding.btSave.revertAnimation()
    }

    companion object {
        private const val TAG = "InputNameBD"

        fun BaseFragment.showInputNameBD(onCompleted: () -> Unit = {}) {
            val current = childFragmentManager.findFragmentByTag(TAG)
            if (current == null) {
                InputNameBD(onCompleted).show(childFragmentManager, TAG)
            }
        }

        fun FragmentActivity.showInputNameBD(onCompleted: () -> Unit = {}) {
            val current = supportFragmentManager.findFragmentByTag(TAG)
            if (current == null) {
                InputNameBD(onCompleted).show(supportFragmentManager, TAG)
            }
        }
    }
}
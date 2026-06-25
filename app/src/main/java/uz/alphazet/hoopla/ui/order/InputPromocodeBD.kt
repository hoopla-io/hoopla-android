package uz.alphazet.hoopla.ui.order

import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.order.ModifierItemData
import uz.alphazet.data.models.order.PromocodeData
import uz.alphazet.domain.ui.BaseBottomSheetDF
import uz.alphazet.domain.utils.disable
import uz.alphazet.domain.utils.enable
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.visible
import uz.alphazet.domain.viewbinding.viewBinding
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.DialogInputPromocodeBinding

/**
 * Bottom-sheet for entering a promocode. It validates the code against
 * `check-promocode` and only reports back a [PromocodeData] once the discount is
 * confirmed valid; invalid codes are shown inline so the user can retry without
 * leaving the sheet.
 */
class InputPromocodeBD(
    private val shopId: Int,
    private val drinkId: Int,
    private val modifiers: ArrayList<ModifierItemData>,
    private val currentCode: String? = null,
    private val onPromoApplied: (PromocodeData) -> Unit = {}
) : BaseBottomSheetDF(R.layout.dialog_input_promocode) {

    private val binding by viewBinding(DialogInputPromocodeBinding::bind)
    private val viewModel: OrderVM by viewModel()

    override var forceKeyboard: Boolean = true

    override fun initialize() {
        if (!currentCode.isNullOrBlank()) {
            binding.inputPromo.setText(currentCode)
            binding.inputPromo.setSelection(currentCode.length)
            binding.btApply.enable()
        } else {
            binding.btApply.disable()
        }

        binding.inputPromo.requestFocus()

        binding.inputPromo.doAfterTextChanged { text ->
            binding.error.gone()
            if (text.isNullOrBlank()) binding.btApply.disable() else binding.btApply.enable()
        }

        binding.btApply.setOnClickListener {
            val code = binding.inputPromo.text?.toString()?.trim().orEmpty()
            if (code.isEmpty()) return@setOnClickListener
            launch {
                viewModel.checkPromocode(code, shopId, drinkId, modifiers)
                    .collectLatest(::collectPromo)
            }
        }
    }

    private fun collectPromo(t: UIResource<PromocodeData>) = t.collect(
        onError = { throwable -> showError(throwable.message) }
    ) { data ->
        if (data == null || data.valid == false || (data.discountAmount ?: 0.0) <= 0.0) {
            showError(getString(uz.alphazet.domain.R.string.promo_invalid))
            return@collect
        }
        onPromoApplied(data)
        dismiss()
    }

    private fun showError(message: String?) {
        binding.error.text = message ?: getString(uz.alphazet.domain.R.string.promo_invalid)
        binding.error.visible()
    }

    override fun showLoading() {
        binding.btApply.isClickable = false
        binding.btApply.startAnimation()
    }

    override fun hideLoading() {
        binding.btApply.isClickable = true
        binding.btApply.revertAnimation()
    }

    companion object {
        private const val TAG = "InputPromocodeBD"

        fun FragmentActivity.showInputPromocodeBD(
            shopId: Int,
            drinkId: Int,
            modifiers: ArrayList<ModifierItemData>,
            currentCode: String? = null,
            onPromoApplied: (PromocodeData) -> Unit = {}
        ) {
            val current = supportFragmentManager.findFragmentByTag(TAG)
            if (current == null) {
                InputPromocodeBD(shopId, drinkId, modifiers, currentCode, onPromoApplied)
                    .show(supportFragmentManager, TAG)
            }
        }
    }
}
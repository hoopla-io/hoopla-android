package uz.alphazet.hoopla.ui.cart

import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.cart.CartData
import uz.alphazet.domain.ui.BaseBottomSheetDF
import uz.alphazet.domain.utils.disable
import uz.alphazet.domain.utils.enable
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.visible
import uz.alphazet.domain.viewbinding.viewBinding
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.DialogInputPromocodeBinding

/**
 * Bottom-sheet for putting a promocode on the cart. Unlike the single-item
 * [uz.alphazet.hoopla.ui.order.InputPromocodeBD], there is no separate validation call — the
 * cart endpoint applies the code and answers with the recomputed cart, so the sheet hands that
 * cart straight back to the screen. Rejected codes are shown inline so the customer can retry
 * without losing the sheet.
 */
class InputCartPromoBD(
    private val currentCode: String? = null,
    private val onPromoApplied: (CartData?) -> Unit = {}
) : BaseBottomSheetDF(R.layout.dialog_input_promocode) {

    private val binding by viewBinding(DialogInputPromocodeBinding::bind)
    private val viewModel: CartVM by viewModel()

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
                viewModel.applyPromo(code).collectLatest(::collectPromo)
            }
        }
    }

    private fun collectPromo(t: UIResource<CartData>) = t.collect(
        onError = { throwable -> showError(throwable.message) }
    ) { data ->
        // The server only echoes a promoCode back when it actually took effect.
        if (data?.promoCode.isNullOrBlank()) {
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
        private const val TAG = "InputCartPromoBD"

        fun FragmentActivity.showInputCartPromoBD(
            currentCode: String? = null,
            onPromoApplied: (CartData?) -> Unit = {}
        ) {
            val current = supportFragmentManager.findFragmentByTag(TAG)
            if (current == null) {
                InputCartPromoBD(currentCode, onPromoApplied)
                    .show(supportFragmentManager, TAG)
            }
        }
    }
}

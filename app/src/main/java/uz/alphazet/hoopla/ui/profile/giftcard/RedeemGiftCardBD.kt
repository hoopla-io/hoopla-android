package uz.alphazet.hoopla.ui.profile.giftcard

import androidx.core.widget.doAfterTextChanged
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.giftcard.GiftCardData
import uz.alphazet.domain.ui.BaseBottomSheetDF
import uz.alphazet.domain.ui.BaseFragment
import uz.alphazet.domain.utils.disable
import uz.alphazet.domain.utils.enable
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.visible
import uz.alphazet.domain.viewbinding.viewBinding
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.DialogRedeemGiftCardBinding

/**
 * Bottom-sheet for redeeming a gift card. The customer enters the card code and
 * on success the card's full value is credited to their wallet balance in a
 * single step. Invalid codes are shown inline (the server message) so the user
 * can retry without leaving the sheet. The redeemed [GiftCardData] (including the
 * new balance) is reported back via [onRedeemed].
 */
class RedeemGiftCardBD(
    private val onRedeemed: (GiftCardData) -> Unit = {}
) : BaseBottomSheetDF(R.layout.dialog_redeem_gift_card) {

    private val binding by viewBinding(DialogRedeemGiftCardBinding::bind)
    private val viewModel: GiftCardVM by viewModel()

    override var forceKeyboard: Boolean = true

    override fun initialize() {
        binding.btRedeem.disable()

        binding.inputCode.requestFocus()

        binding.inputCode.doAfterTextChanged { text ->
            binding.error.gone()
            if (text.isNullOrBlank()) binding.btRedeem.disable() else binding.btRedeem.enable()
        }

        binding.btRedeem.setOnClickListener {
            val code = binding.inputCode.text?.toString()?.trim().orEmpty()
            if (code.isEmpty()) return@setOnClickListener
            launch {
                viewModel.redeem(code).collectLatest(::collectRedeem)
            }
        }
    }

    private fun collectRedeem(t: UIResource<GiftCardData>) = t.collect(
        onError = { throwable -> showError(throwable.message) }
    ) { data ->
        if (data == null) {
            showError(null)
            return@collect
        }
        onRedeemed(data)
        dismiss()
    }

    private fun showError(message: String?) {
        binding.error.text = message ?: getString(uz.alphazet.domain.R.string.gift_card_invalid)
        binding.error.visible()
    }

    override fun showLoading() {
        binding.btRedeem.isClickable = false
        binding.btRedeem.startAnimation()
    }

    override fun hideLoading() {
        binding.btRedeem.isClickable = true
        binding.btRedeem.revertAnimation()
    }

    companion object {
        private const val TAG = "RedeemGiftCardBD"

        fun BaseFragment.showRedeemGiftCardBD(onRedeemed: (GiftCardData) -> Unit = {}) {
            val current = childFragmentManager.findFragmentByTag(TAG)
            if (current == null) {
                RedeemGiftCardBD(onRedeemed).show(childFragmentManager, TAG)
            }
        }
    }
}
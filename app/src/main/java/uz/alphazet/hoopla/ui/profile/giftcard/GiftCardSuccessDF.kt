package uz.alphazet.hoopla.ui.profile.giftcard

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import androidx.core.view.WindowCompat
import androidx.fragment.app.DialogFragment
import uz.alphazet.data.models.giftcard.GiftCardData
import uz.alphazet.domain.ui.BaseDialog
import uz.alphazet.domain.ui.BaseFragment
import uz.alphazet.domain.utils.formatToPrice
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.viewbinding.viewBinding
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.DialogGiftCardSuccessBinding

/**
 * Celebratory confirmation shown after a gift card is successfully redeemed.
 * Rendered full-screen (non-floating window) so it reads as a dedicated success
 * screen rather than a small alert. Displays the credited amount and the
 * customer's updated wallet balance from the returned [GiftCardData]. Purely
 * informational — the close button or the "Great" button dismisses it.
 */
class GiftCardSuccessDF(
    private val data: GiftCardData,
    private val onApprove: () -> Unit = {}
) : BaseDialog(R.layout.dialog_gift_card_success) {

    private val binding by viewBinding(DialogGiftCardSuccessBinding::bind)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialogTheme)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            // BaseDialog forces a transparent window for its centered card dialogs;
            // restore an opaque background so the full-screen layout paints solidly.
            setBackgroundDrawable(ColorDrawable(Color.WHITE))
            WindowCompat.getInsetsController(this, decorView).isAppearanceLightStatusBars = true
        }
    }

    override fun initialize() {
        val currency = data.currency.orEmpty()

        val credited = data.credited?.formatToPrice().orEmpty()
        binding.tvCredited.text = getString(
            uz.alphazet.domain.R.string.gift_card_success_credited,
            "$credited $currency".trim()
        )

        val balance = data.balance?.formatToPrice()
        if (balance == null) {
            binding.tvBalance.gone()
        } else {
            binding.tvBalance.text = getString(
                uz.alphazet.domain.R.string.gift_card_success_balance,
                "$balance $currency".trim()
            )
        }

        binding.btClose.setOnClickListener { dismiss() }

        binding.btApprove.setOnClickListener {
            dismiss()
            onApprove()
        }
    }

    companion object {
        private const val TAG = "GiftCardSuccessDF"

        fun BaseFragment.showGiftCardSuccessDF(
            data: GiftCardData,
            onApprove: () -> Unit = {}
        ) {
            if (childFragmentManager.findFragmentByTag(TAG) == null) {
                GiftCardSuccessDF(data, onApprove).show(childFragmentManager, TAG)
            }
        }
    }
}
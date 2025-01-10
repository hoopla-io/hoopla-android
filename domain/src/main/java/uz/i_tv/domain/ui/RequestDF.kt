package uz.i_tv.domain.ui

import uz.i_tv.domain.R
import uz.i_tv.domain.databinding.DialogMessageBinding
import uz.i_tv.domain.databinding.DialogRequestBinding
import uz.i_tv.domain.viewbinding.viewBinding

fun BaseFragment.showRequestDF(
    title: String,
    message: String,
    yes: String,
    no: String,
    onCancel: () -> Unit = {},
    onApprove: () -> Unit
) {
    if (childFragmentManager.findFragmentByTag("REQUEST_DF") == null)
        RequestDF(title, message, yes, no, onCancel, onApprove).show(
            childFragmentManager,
            "REQUEST_DF"
        )
}

fun BaseBottomSheetDF.showRequestDF(
    title: String,
    message: String,
    yes: String,
    no: String,
    onCancel: () -> Unit = {},
    onApprove: () -> Unit
) {
    if (childFragmentManager.findFragmentByTag("REQUEST_DF") == null)
        RequestDF(title, message, yes, no, onCancel, onApprove).show(
            childFragmentManager,
            "REQUEST_DF"
        )
}

class RequestDF(
    private val title: String,
    private val message: String,
    private val yes: String,
    private val no: String,
    private val onCancel: () -> Unit = {},
    private val onApprove: () -> Unit
) : BaseDialog(R.layout.dialog_request) {

    private val binding by viewBinding(DialogRequestBinding::bind)

    override fun initialize() {

        binding.tvTitle.text = title
        binding.tvMessage.text = message
        binding.btApprove.text = yes
        binding.btCancel.text = no

        binding.btApprove.setOnClickListener {
            onApprove()
            dismiss()
        }

        binding.btCancel.setOnClickListener {
            dismiss()
        }
    }


    override fun dismiss() {
        onCancel()
        super.dismiss()
    }

}


fun BaseFragment.showMessageDF(
    title: String,
    message: String,
    ok: String,
    onApprove: () -> Unit
) {
    if (childFragmentManager.findFragmentByTag("MessageDF") == null)
        MessageDF(title, message, ok, onApprove).show(
            childFragmentManager,
            "MessageDF"
        )
}

class MessageDF(
    private val title: String,
    private val message: String,
    private val ok: String,
    private val onApprove: () -> Unit
) : BaseDialog(R.layout.dialog_message) {

    private val binding by viewBinding(DialogMessageBinding::bind)

    override fun initialize() {

        binding.tvTitle.text = title
        binding.tvMessage.text = message
        binding.btApprove.text = ok

        binding.btApprove.setOnClickListener {
            dismiss()
            onApprove()
        }
    }

}
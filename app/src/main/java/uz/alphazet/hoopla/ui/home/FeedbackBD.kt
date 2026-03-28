package uz.alphazet.hoopla.ui.home

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.FeedbackDetail
import uz.alphazet.domain.ui.BaseFragment
import uz.alphazet.domain.ui.BaseBottomSheetDF
import uz.alphazet.domain.utils.formatToPrice
import uz.alphazet.domain.utils.getDateDDMMMMYYYYHHmm
import uz.alphazet.domain.viewbinding.viewBinding
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.DialogFeedbackBinding

class FeedbackBD(
    private val feedbackDetail: FeedbackDetail,
    private val onSubmitted: () -> Unit = {}
) : BaseBottomSheetDF(R.layout.dialog_feedback) {

    private val binding by viewBinding(DialogFeedbackBinding::bind)
    private val viewModel: HomeVM by viewModel()

    override fun initialize() {
        binding.drinkName.text = feedbackDetail.drinkName ?: getString(uz.alphazet.domain.R.string.unknown)
        binding.partnerName.text = buildString {
            if (!feedbackDetail.partnerName.isNullOrBlank()) append(feedbackDetail.partnerName)
            if (!feedbackDetail.shopName.isNullOrBlank()) {
                if (isNotEmpty()) append(" · ")
                append(feedbackDetail.shopName)
            }
        }
        val price = feedbackDetail.productPrice
        binding.price.text = if (price != null) price.formatToPrice().plus(" UZS") else ""
        binding.purchasedAt.text = feedbackDetail.purchasedAtUnix?.getDateDDMMMMYYYYHHmm()

        binding.btSubmit.setOnClickListener {
            val rating = binding.ratingBar.rating.toInt()
            val comment = binding.commentInput.text?.toString()
            viewModel.submitFeedback(feedbackDetail.id ?: return@setOnClickListener, rating, comment)
        }

        lifecycleScope.launch {
            viewModel.submitFeedbackFlow.collectLatest(::collectSubmit)
        }
    }

    private fun collectSubmit(t: UIResource<Any>) = t.collect {
        onSubmitted()
        dismiss()
    }

    override fun showLoading() {
//        binding.btSubmit.isClickable = false
//        binding.btSubmit.startAnimation()
    }

    override fun hideLoading() {
//        binding.btSubmit.isClickable = true
//        binding.btSubmit.revertAnimation()
    }

    companion object {
        private const val TAG = "FeedbackBD"

        fun BaseFragment.showFeedbackBD(
            feedbackDetail: FeedbackDetail,
            onSubmitted: () -> Unit = {}
        ) {
            val current = childFragmentManager.findFragmentByTag(TAG)
            if (current == null) {
                FeedbackBD(feedbackDetail, onSubmitted).show(childFragmentManager, TAG)
            }
        }
    }
}
package uz.alphazet.hoopla.ui.profile.subscriptions

import android.content.Intent
import android.os.Bundle
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.SubscriptionItemData
import uz.alphazet.domain.ui.BaseActivity
import uz.alphazet.domain.ui.showMessageDF
import uz.alphazet.domain.ui.showRequestDF
import uz.alphazet.domain.utils.formatToPrice
import uz.alphazet.hoopla.databinding.ScreenSubscriptionsBinding
import uz.alphazet.hoopla.ui.profile.payment.PaymentServicesActivity

class SubscriptionActivity : BaseActivity(), SwipeRefreshLayout.OnRefreshListener {

    private lateinit var binding: ScreenSubscriptionsBinding
    private val viewModel: SubscriptionVM by viewModel()
    private val adapter = SubscriptionAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ScreenSubscriptionsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.swipeRefreshLayout.setOnRefreshListener(this)

        binding.subscriptionRv.adapter = adapter

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        launch {
            viewModel.getSubscriptions().collectLatest(::collectData)
        }

        adapter.setOnItemClickListener { item ->
            showRequestDF(
                item.name ?: "",
                getString(
                    uz.alphazet.domain.R.string.label_question_purchase_subscription,
                    item.price?.formatToPrice(),
                    item.currency ?: "",
                    item.days.toString()
                ),
                getString(uz.alphazet.domain.R.string.yes),
                getString(uz.alphazet.domain.R.string.no),
                onCancel = {},
                onApprove = {
                    launch {
                        viewModel.buySubscription(item.id ?: -1).collectLatest {
                            collectBuySubscriptionData(it, item)
                        }
                    }
                })
        }

    }

    private fun collectData(t: UIResource<List<SubscriptionItemData>>) = t.collect {
        binding.swipeRefreshLayout.isRefreshing = false
        adapter.submitList(it)
    }

    private fun collectBuySubscriptionData(t: UIResource<Any>, item: SubscriptionItemData) =
        t.collect {
            onRefresh()
            showMessageDF(
                getString(uz.alphazet.domain.R.string.successfully_bought),
                getString(
                    uz.alphazet.domain.R.string.label_successfully_bought,
                    item.name,
                    item.days.toString(),
                    item.price?.formatToPrice(),
                    item.currency
                ),
                "OK"
            ) {}
        }

    override fun onPreconditionRequiredException(message: String?, code: Int) {
        super.onPreconditionRequiredException(message, code)
        val intent1 = Intent(this, PaymentServicesActivity::class.java)
        startActivity(intent1)
    }

    override fun showLoading() {
        binding.swipeRefreshLayout.isRefreshing = true
    }

    override fun onRefresh() {
        launch {
            viewModel.getSubscriptions().collectLatest(::collectData)
        }
    }

    override fun updateStatusBarViewHeight() {
        launch {
            val statusBarHeight = getStatusBarHeight()
            binding.statusBarView.layoutParams.height = statusBarHeight
            binding.statusBarView.requestLayout()
        }
    }

}
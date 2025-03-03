package uz.i_tv.qahvazor.ui.profile.subscriptions

import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.i_tv.data.UIResource
import uz.i_tv.data.models.SubscriptionItemData
import uz.i_tv.domain.ui.BaseFragment
import uz.i_tv.domain.ui.showMessageDF
import uz.i_tv.domain.ui.showRequestDF
import uz.i_tv.domain.utils.formatToPrice
import uz.i_tv.domain.viewbinding.viewBinding
import uz.i_tv.qahvazor.R
import uz.i_tv.qahvazor.databinding.ScreenSubscriptionsBinding

class SubscriptionsScreen : BaseFragment(R.layout.screen_subscriptions),
    SwipeRefreshLayout.OnRefreshListener {

    private val binding by viewBinding(ScreenSubscriptionsBinding::bind)
    private val viewModel: SubscriptionVM by viewModel()

    private val adapter = SubscriptionAdapter()

    override fun initialize() {

        binding.swipeRefreshLayout.setOnRefreshListener(this)

        binding.subscriptionRv.adapter = adapter

        binding.toolbar.setNavigationOnClickListener {
            exit()
        }

        launch {
            viewModel.getSubscriptions().collectLatest(::collectData)
        }

        adapter.setOnItemClickListener { item ->
            showRequestDF(item.name ?: "",
                "Are you sure you want to purchase this subscription for ${item.price?.formatToPrice()} ${item.currency}?",
                "Yes", "No",
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
                "Successfully bought!",
                "You have successfully purchased ${item.name} subscription for ${item.days} days for ${item.price?.formatToPrice()} ${item.currency}!",
                "OK"
            ) {}
        }

    override fun showLoading() {
        binding.swipeRefreshLayout.isRefreshing = true
    }

    override fun onRefresh() {
        launch {
            viewModel.getSubscriptions().collectLatest(::collectData)
        }
    }


}
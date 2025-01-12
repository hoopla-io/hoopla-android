package uz.i_tv.qahvazor.ui.home

import android.content.Intent
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.i_tv.data.UIResource
import uz.i_tv.data.models.PartnerItemData
import uz.i_tv.domain.ui.BaseFragment
import uz.i_tv.domain.viewbinding.viewBinding
import uz.i_tv.qahvazor.R
import uz.i_tv.qahvazor.databinding.ScreenHomeBinding
import uz.i_tv.qahvazor.ui.partner_details.PartnerDetailScreen
import uz.i_tv.qahvazor.ui.partner_details.PartnerDetailScreen.Companion.PARTNER_ID

class HomeScreen : BaseFragment(R.layout.screen_home), SwipeRefreshLayout.OnRefreshListener {

    private val binding by viewBinding(ScreenHomeBinding::bind)
    private val viewModel: HomeVM by viewModel()

    private val adapter = PartnerAdapter()

    override fun initialize() {

        binding.swipeRefreshLayout.setOnRefreshListener(this)

        binding.partnersRv.adapter = adapter

        launch {
            viewModel.getPartners().collectLatest(::collectPartners)
        }

        adapter.setOnItemClickListener {
            val intent = Intent(requireActivity(), PartnerDetailScreen::class.java)
            intent.putExtra(PARTNER_ID, it.id ?: -1)
            startActivity(intent)
        }

    }

    private fun collectPartners(t: UIResource<List<PartnerItemData>>) = t.collect {
        binding.swipeRefreshLayout.isRefreshing = false

        adapter.submitList(it)
    }

    override fun showLoading() {
        binding.swipeRefreshLayout.isRefreshing = true
    }

    override fun onRefresh() {
        launch {
            viewModel.getPartners().collectLatest(::collectPartners)
        }
    }

}
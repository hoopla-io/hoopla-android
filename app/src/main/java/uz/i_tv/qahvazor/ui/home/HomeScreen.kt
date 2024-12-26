package uz.i_tv.qahvazor.ui.home

import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.i_tv.data.UIResource
import uz.i_tv.data.models.CompanyItemData
import uz.i_tv.domain.ui.BaseFragment
import uz.i_tv.domain.viewbinding.viewBinding
import uz.i_tv.qahvazor.R
import uz.i_tv.qahvazor.databinding.ScreenHomeBinding

class HomeScreen : BaseFragment(R.layout.screen_home) {

    private val binding by viewBinding(ScreenHomeBinding::bind)
    private val viewModel: HomeVM by viewModel()

    private val adapter = CompanyAdapter()

    override fun initialize() {

        binding.companyRv.adapter = adapter

        launch {
            viewModel.getCompanies().collectLatest(::collectCompanies)
        }

    }

    private fun collectCompanies(t: UIResource<List<CompanyItemData>>) = t.collect {
        adapter.submitList(it)
    }

}
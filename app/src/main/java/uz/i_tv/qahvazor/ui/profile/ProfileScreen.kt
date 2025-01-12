package uz.i_tv.qahvazor.ui.profile

import android.view.View
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.i_tv.data.UIResource
import uz.i_tv.data.models.UserData
import uz.i_tv.domain.ui.BaseFragment
import uz.i_tv.domain.ui.BaseRootActivity
import uz.i_tv.domain.ui.showRequestDF
import uz.i_tv.domain.utils.gone
import uz.i_tv.domain.utils.visible
import uz.i_tv.domain.viewbinding.viewBinding
import uz.i_tv.qahvazor.R
import uz.i_tv.qahvazor.databinding.ScreenProfileBinding
import uz.i_tv.qahvazor.ui.Screens

class ProfileScreen : BaseFragment(R.layout.screen_profile), SwipeRefreshLayout.OnRefreshListener {

    private val binding by viewBinding(ScreenProfileBinding::bind)
    private val viewModel: ProfileVM by viewModel()

    override fun initialize() {

        binding.subscription.setOnClickListener(this)
        binding.logout.setOnClickListener(this)
        binding.login.setOnClickListener(this)

        binding.swipeRefreshLayout.setOnRefreshListener(this)

        viewModel.getUser()

        launch {
            viewModel.userDataFlow.collectLatest(::collectUserData)
        }
    }

    private fun collectUserData(t: UIResource<UserData>) = t.collect {
        binding.unAuthGroup.gone()
        binding.authGroup.visible()
        binding.logout.visible()

        binding.name.text = it?.name
        binding.phoneNumber.text = it?.phoneNumber
    }

    private fun collectLogoutData(t: UIResource<Any>) = t.collect {
        cache.clearTokens()
        newRootScreen(Screens.bottomNav())
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.subscription -> {
                navigateTo(Screens.subscriptionsScreen())
            }

            R.id.logout -> {
                showRequestDF(
                    "Log out",
                    "Are you sure log out from the app",
                    "Yes",
                    "No"
                ) {
                    launch {
                        viewModel.logout().collectLatest(::collectLogoutData)
                    }
                }
            }

            R.id.login -> {
                (requireActivity() as BaseRootActivity).navigateToAuthScreen()
            }
        }
    }

    override fun onUnauthorizedException(message: String?, code: Int) {
        binding.unAuthGroup.visible()
        binding.authGroup.gone()
        binding.logout.gone()
    }

    override fun showLoading() {
        binding.swipeRefreshLayout.isRefreshing = true
    }

    override fun hideLoading() {
        binding.swipeRefreshLayout.isRefreshing = false
    }

    override fun onRefresh() {
        viewModel.getUser()
    }

}
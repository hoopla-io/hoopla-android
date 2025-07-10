package uz.alphazet.hoopla.ui.profile

import android.content.Intent
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import kotlinx.coroutines.flow.collectLatest
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.UserData
import uz.alphazet.domain.ui.BaseActivity
import uz.alphazet.domain.ui.BaseFragment
import uz.alphazet.domain.ui.showRequestDF
import uz.alphazet.domain.utils.formatPhoneNumber
import uz.alphazet.domain.utils.formatToPrice
import uz.alphazet.domain.utils.getDateDMMMMYYYY
import uz.alphazet.domain.utils.gone
import uz.alphazet.domain.utils.intentToBrowser
import uz.alphazet.domain.utils.log
import uz.alphazet.domain.utils.visible
import uz.alphazet.domain.viewbinding.viewBinding
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ScreenProfileBinding
import uz.alphazet.hoopla.ui.MainActivity
import uz.alphazet.hoopla.ui.auth.AuthActivity
import uz.alphazet.hoopla.ui.home.HomeScreen
import uz.alphazet.hoopla.ui.profile.payment.PaymentServicesActivity
import uz.alphazet.hoopla.ui.profile.settings.SelectLanguageBD.Companion.showSelectLanguageBD
import uz.alphazet.hoopla.ui.profile.subscriptions.SubscriptionActivity

class ProfileScreen : BaseFragment(R.layout.screen_profile), SwipeRefreshLayout.OnRefreshListener {

    private val binding by viewBinding(ScreenProfileBinding::bind)
    private val viewModel: ProfileVM by viewModel()

    private val authListener =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            viewModel.getUser()
        }

    private val subscriptionListener =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

        }

    private val paymentListener =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

        }

    override fun initialize() {

        binding.selectTariff.setOnClickListener(this)
        binding.subscription.setOnClickListener(this)
        binding.topUp.setOnClickListener(this)
        binding.languages.setOnClickListener(this)
        binding.logout.setOnClickListener(this)
        binding.login.setOnClickListener(this)
        binding.support.setOnClickListener(this)
        binding.privacyPolicy.setOnClickListener(this)
        binding.termOfUse.setOnClickListener(this)
        binding.settings.setOnClickListener(this)

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
        binding.phoneNumber.text = it?.phoneNumber?.formatPhoneNumber()
        binding.balance.text = it?.balance?.formatToPrice().plus(" ${it?.currency}")

        if (it?.subscription != null) {
            binding.activeTariffInfoCard.visible()
            binding.selectTariff.gone()
            binding.subscriptionName.text = it.subscription?.name
            binding.subscriptionEndDate.text = it.subscription?.endDateUnix?.getDateDMMMMYYYY()
        } else {
            binding.activeTariffInfoCard.gone()
            binding.selectTariff.visible()
        }
    }

    private fun collectLogoutData(t: UIResource<Any>) = t.collect {
        cache.clearTokens()
        (requireActivity() as? MainActivity)?.callOnLogOut()
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.select_tariff, R.id.subscription -> {
                val intent1 = Intent(requireActivity(), SubscriptionActivity::class.java)
                subscriptionListener.launch(intent1)
            }

            R.id.top_up -> {
                val intent1 = Intent(requireActivity(), PaymentServicesActivity::class.java)
                paymentListener.launch(intent1)
            }

            R.id.logout -> {
                showRequestDF(
                    getString(uz.alphazet.domain.R.string.log_out),
                    getString(uz.alphazet.domain.R.string.question_log_out),
                    getString(uz.alphazet.domain.R.string.yes),
                    getString(uz.alphazet.domain.R.string.no)
                ) {
                    launch {
                        viewModel.logout().collectLatest(::collectLogoutData)
                    }
                }
            }

            R.id.login -> {
                val intent = Intent(requireActivity(), AuthActivity::class.java)
                authListener.launch(intent)
            }

            R.id.languages -> {
                showSelectLanguageBD {
                    (requireActivity() as BaseActivity).updateLocale(it)
                }
            }

            R.id.privacyPolicy -> {
                requireContext().intentToBrowser("https://hoopla.uz/ru/privacy-policy")
                AppSignatureHelper(requireContext()).getAppSignatures().log("PROFILE_SCREEN")
            }

            R.id.support -> {
                requireContext().intentToBrowser("https://t.me/alphazzet")
            }

            R.id.termOfUse -> {
                requireContext().intentToBrowser("https://hoopla.uz/ru/terms-of-use")
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

    override fun toString(): String {
        return HomeScreen.Companion.TAG
    }

    companion object {
        const val TAG = "ProfileScreen"
    }

}
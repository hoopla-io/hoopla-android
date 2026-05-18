package uz.alphazet.hoopla.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.fragment.app.commit
import org.koin.android.ext.android.inject
import uz.alphazet.domain.cache.AppCache
import uz.alphazet.domain.ui.BaseActivity
import uz.alphazet.domain.ui.BaseFragment
import uz.alphazet.domain.ui.showRequestDF
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ActivityMainBinding
import uz.alphazet.hoopla.ui.auth.AuthActivity
import uz.alphazet.hoopla.ui.home.HomeScreen
import uz.alphazet.hoopla.ui.map.MapScreen
import uz.alphazet.hoopla.ui.profile.ProfileScreen
import uz.alphazet.hoopla.ui.orders.OrdersScreen
import uz.alphazet.hoopla.util.InAppUpdateManager

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var inAppUpdateManager: InAppUpdateManager

    private val cache: AppCache by inject()

    private val currentFragment: BaseFragment?
        get() = supportFragmentManager.fragments.firstOrNull { it.isVisible } as? BaseFragment

    private val authListener =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        inAppUpdateManager = InAppUpdateManager(
            activity = this,
            rootView = binding.root,
            anchorView = binding.bottomNav,
        )
        inAppUpdateManager.checkForUpdate()

        if (supportFragmentManager.fragments.isEmpty()) {
            binding.bottomNav.selectedItemId = R.id.home
            selectTab(R.id.home)
        }

        binding.bottomNav.setOnItemReselectedListener { }
        binding.bottomNav.setOnItemSelectedListener { menuItem ->
            if (menuItem.itemId == R.id.orders && cache.accessToken.isNullOrEmpty()) {
                showRequestDF(
                    getString(uz.alphazet.domain.R.string.sign_in),
                    getString(uz.alphazet.domain.R.string.you_r_not_logged_in),
                    getString(uz.alphazet.domain.R.string.sign_in),
                    getString(uz.alphazet.domain.R.string.cancel)
                ) {
                    val intent = Intent(this, AuthActivity::class.java)
                    authListener.launch(intent)
                }
                false
            } else {
                selectTab(menuItem.itemId)
                true
            }
        }
    }

    override fun onApplySystemBarInsets(systemBars: Insets) {
        binding.bottomNav.updatePadding(bottom = systemBars.bottom)
    }

    fun callOnLogOut() {
        supportFragmentManager.commit { replace(R.id.fragment_container, HomeScreen()) }
        supportFragmentManager.executePendingTransactions()
    }

    fun replaceScreen(screen: BaseFragment) {
        supportFragmentManager.commit { replace(R.id.fragment_container, screen) }
        supportFragmentManager.executePendingTransactions()
    }

    fun navigateToOrdersScreen() {
        binding.bottomNav.selectedItemId = R.id.orders
//        selectTab(R.id.orders)
    }

    private fun selectTab(itemId: Int) {
        val newFragment = supportFragmentManager.findFragmentByTag(itemId.toString())
        if (currentFragment != null && newFragment != null && newFragment === currentFragment) return

        supportFragmentManager.beginTransaction().apply {
            if (newFragment == null) {
                val fragment = getFragment(itemId)
                add(
                    R.id.fragment_container,
                    fragment,
                    itemId.toString()
                )
            }
            currentFragment?.let { hide(it) }
            if (newFragment != null) show(newFragment)
        }.commit()
    }

    private fun getFragment(itemId: Int): BaseFragment {
        return when (itemId) {
            R.id.home -> HomeScreen()
            R.id.map -> MapScreen()
            R.id.orders -> OrdersScreen()
            R.id.profile -> ProfileScreen()
            else -> HomeScreen()
        }
    }

}
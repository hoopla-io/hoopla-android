package uz.alphazet.hoopla.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.fragment.app.commit
import kotlinx.coroutines.flow.collectLatest
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.cart.CartData
import uz.alphazet.domain.cache.AppCache
import uz.alphazet.domain.ui.BaseActivity
import uz.alphazet.domain.ui.BaseFragment
import uz.alphazet.domain.ui.showRequestDF
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.databinding.ActivityMainBinding
import uz.alphazet.hoopla.ui.auth.AuthActivity
import uz.alphazet.hoopla.ui.cart.CartScreen
import uz.alphazet.hoopla.ui.cart.CartVM
import uz.alphazet.hoopla.ui.home.HomeScreen
import uz.alphazet.hoopla.ui.map.MapScreen
import uz.alphazet.hoopla.ui.profile.ProfileScreen
import uz.alphazet.hoopla.ui.orders.OrdersScreen
import uz.alphazet.hoopla.util.InAppUpdateManager

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var inAppUpdateManager: InAppUpdateManager

    private val cache: AppCache by inject()
    private val cartViewModel: CartVM by viewModel()

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

        launch { cartViewModel.cartFlow.collectLatest(::collectCartCount) }

        binding.bottomNav.setOnItemReselectedListener { }
        binding.bottomNav.setOnItemSelectedListener { menuItem ->
            if (menuItem.itemId in AUTHED_TABS && cache.accessToken.isNullOrEmpty()) {
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

    override fun onResume() {
        super.onResume()
        refreshCartCount()
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

    fun navigateToHomeScreen() {
        binding.bottomNav.selectedItemId = R.id.home
    }

    /**
     * Keeps the cart tab badged with the number of drinks waiting. Refreshed on resume because
     * items are added from the shop and order screens, which live outside this activity.
     */
    private fun refreshCartCount() {
        if (cache.accessToken.isNullOrEmpty()) {
            binding.bottomNav.removeBadge(R.id.cart)
            return
        }
        // The cart tab reads the cart itself and hands it straight over, so asking again here
        // would just be a second request for the same answer.
        if (binding.bottomNav.selectedItemId == R.id.cart) return
        cartViewModel.getCart()
    }

    private fun collectCartCount(t: UIResource<CartData>) = t.collect(
        // A background refresh: it must not spin the screen, and a failure must not toast over
        // whichever tab the customer is actually looking at.
        onLoading = null,
        onError = { binding.bottomNav.removeBadge(R.id.cart) }
    ) { data -> onCartUpdated(data) }

    /**
     * Badges the tab from a cart someone else already fetched. [CartScreen] calls this on every
     * render: it holds its own [CartVM] instance, so without being told the badge would sit on
     * a count the customer has just changed in front of it.
     */
    fun onCartUpdated(cart: CartData?) {
        val count = cart?.items.orEmpty().sumOf { it.quantity ?: 0 }
        if (count <= 0) {
            binding.bottomNav.removeBadge(R.id.cart)
        } else {
            binding.bottomNav.getOrCreateBadge(R.id.cart).apply {
                isVisible = true
                number = count
            }
        }
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

    private companion object {
        /** Tabs that are meaningless without an account, and so prompt to sign in first. */
        val AUTHED_TABS = setOf(R.id.orders, R.id.cart)
    }

    private fun getFragment(itemId: Int): BaseFragment {
        return when (itemId) {
            R.id.home -> HomeScreen()
            R.id.map -> MapScreen()
            R.id.cart -> CartScreen()
            R.id.orders -> OrdersScreen()
            R.id.profile -> ProfileScreen()
            else -> HomeScreen()
        }
    }

}
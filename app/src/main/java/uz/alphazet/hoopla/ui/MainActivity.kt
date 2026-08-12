package uz.alphazet.hoopla.ui

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentManager
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
import uz.alphazet.hoopla.ui.shop_details.ShopDetailScreen
import uz.alphazet.hoopla.util.InAppUpdateManager

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var inAppUpdateManager: InAppUpdateManager

    private val cache: AppCache by inject()
    private val cartViewModel: CartVM by viewModel()

    /**
     * The screen the customer is actually looking at: the topmost visible fragment in the
     * container. Hidden tabs and any open dialog fragment — which lives outside the container —
     * are both excluded, so this is the one that back is dispatched to and the one a push hides.
     */
    private val currentFragment: BaseFragment?
        get() = supportFragmentManager.fragments
            .lastOrNull { it.isVisible && it.id == R.id.fragment_container } as? BaseFragment

    private val authListener =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

        }

    /** Shop id from a deep link, waiting for the fragment manager to be safe to push on. */
    private var pendingShopId: Int? = null

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

        binding.bottomNav.setOnItemReselectedListener { clearDetailStack() }
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
                clearDetailStack()
                selectTab(menuItem.itemId)
                true
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (currentFragment?.onBackPressed() == true) return
                if (supportFragmentManager.backStackEntryCount > 0) {
                    dismissKeyboard()
                    supportFragmentManager.popBackStack()
                } else {
                    finish()
                }
            }
        })

        if (savedInstanceState == null) captureDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        captureDeepLink(intent)
    }

    /**
     * hoopla.uz/shop/{id} links land here now that the shop screen is a fragment: the shop id
     * follows the "shop" path segment in any of the /shop, /uz/shop, /ru/shop, /en/shop forms.
     *
     * Only parsed here — neither entry point can push yet. onNewIntent runs while the host is
     * still stopped, where a commit would be dropped as state-saved, and in onCreate the tab
     * beneath has no view yet, so [navigateTo] would have nothing to hide it by.
     */
    private fun captureDeepLink(intent: Intent?) {
        val segments = intent?.data?.pathSegments ?: return
        val shopIndex = segments.indexOfFirst { it == "shop" }
        if (shopIndex < 0) return
        pendingShopId = segments.getOrNull(shopIndex + 1)?.toIntOrNull() ?: return
        // Consumed, so a later recreation does not reopen the shop over the customer's work.
        intent.data = null
    }

    override fun onResumeFragments() {
        super.onResumeFragments()
        pendingShopId?.let { shopId ->
            pendingShopId = null
            navigateTo(ShopDetailScreen.newInstance(shopId))
        }
    }

    override fun onResume() {
        super.onResume()
        refreshCartCount()
    }

    override fun onApplySystemBarInsets(systemBars: Insets) {
        binding.bottomNav.updatePadding(bottom = systemBars.bottom)
    }

    /**
     * Pushes a detail screen on top of the current tab. The tab underneath is hidden, not
     * destroyed, so popping back restores it with its state and scroll position intact.
     *
     * Dropped once the state is saved: these pushes are reached from error handlers and network
     * callbacks that can land after the app is backgrounded, where committing would throw.
     */
    fun navigateTo(screen: BaseFragment) {
        if (supportFragmentManager.isStateSaved) return
        // A tab added moments ago may still be a pending transaction, and an unsettled container
        // would leave currentFragment null — pushing on top of a tab that never got hidden.
        supportFragmentManager.executePendingTransactions()
        supportFragmentManager.commit {
            setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right,
            )
            currentFragment?.let { hide(it) }
            add(R.id.fragment_container, screen)
            addToBackStack(null)
        }
    }

    /** Pops every pushed detail screen, landing back on the current tab's root. */
    private fun clearDetailStack() {
        if (supportFragmentManager.isStateSaved) return
        if (supportFragmentManager.backStackEntryCount > 0) {
            dismissKeyboard()
            supportFragmentManager.popBackStackImmediate(
                null,
                FragmentManager.POP_BACK_STACK_INCLUSIVE
            )
        }
    }

    /**
     * Leaving a screen used to finish its activity, which tore the keyboard down with the
     * window. Popping a fragment inside this one shared window does not, so a search or comment
     * field would otherwise leave the keyboard stranded over whatever is revealed.
     */
    private fun dismissKeyboard() {
        val target = currentFocus ?: binding.root
        getSystemService(InputMethodManager::class.java)
            ?.hideSoftInputFromWindow(target.windowToken, 0)
    }

    fun callOnLogOut() {
        if (supportFragmentManager.isStateSaved) return
        clearDetailStack()
        // replace() clears the container outright — every tab the signed-in customer opened,
        // not just the visible one — and seeds it with a home tab selectTab can find again.
        supportFragmentManager.commit {
            replace(R.id.fragment_container, HomeScreen(), R.id.home.toString())
        }
        supportFragmentManager.executePendingTransactions()
        binding.bottomNav.selectedItemId = R.id.home
    }

    /**
     * A successful checkout used to finish the whole Checkout → Order → ShopDetail activity
     * stack via resultCode 203 relays; with fragments the same landing — Orders tab, detail
     * stack gone — is this single host call.
     */
    fun navigateToOrdersScreen() {
        clearDetailStack()
        binding.bottomNav.selectedItemId = R.id.orders
    }

    fun navigateToHomeScreen() {
        clearDetailStack()
        binding.bottomNav.selectedItemId = R.id.home
    }

    /**
     * Keeps the cart tab badged with the number of drinks waiting. Refreshed on resume because
     * items can still be added from screens outside this activity (auth, story viewer), and on
     * demand from the order flow, which mutates the cart without ever leaving this activity.
     */
    fun refreshCartCount() {
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
        if (supportFragmentManager.isStateSaved) return
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

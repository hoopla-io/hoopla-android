package uz.i_tv.qahvazor.ui

import android.os.Handler
import android.os.Looper
import androidx.core.os.bundleOf
import com.github.terrakok.cicerone.Screen
import uz.i_tv.domain.ui.BaseFragment
import uz.i_tv.domain.ui.BaseRootActivity
import uz.i_tv.domain.ui.NavigationFragment
import uz.i_tv.domain.ui.showRequestDF
import uz.i_tv.domain.utils.log
import uz.i_tv.domain.viewbinding.viewBinding
import uz.i_tv.qahvazor.R
import uz.i_tv.qahvazor.databinding.ScreenBottomNavBinding
import uz.i_tv.qahvazor.ui.Screens.BottomNavPages.home
import uz.i_tv.qahvazor.ui.Screens.BottomNavPages.profile
import uz.i_tv.qahvazor.ui.Screens.BottomNavPages.qrCode
import uz.i_tv.qahvazor.ui.TabContainerFragment.Companion.INITIAL_PAGE_ID
import uz.i_tv.qahvazor.ui.TabContainerFragment.Companion.TAB_HOME
import uz.i_tv.qahvazor.ui.TabContainerFragment.Companion.TAB_PROFILE
import uz.i_tv.qahvazor.ui.TabContainerFragment.Companion.TAB_QR_CODE

class BottomNavScreen : BaseFragment(R.layout.screen_bottom_nav) {

    private val binding by viewBinding(ScreenBottomNavBinding::bind)
    private val initialPageId: Int by lazy {
        arguments?.getInt(INITIAL_PAGE_ID, TAB_HOME) ?: TAB_HOME
    }

    override fun initialize() {

        binding.fab.setOnClickListener {
            if (cache.accessToken.isNullOrEmpty()) {
                showRequestDF(
                    "Sign in",
                    "You are not logged in!\nPlease sign in",
                    "Sign in",
                    "Cancel"
                ) {
                    (requireActivity() as BaseRootActivity).navigateToAuthScreen()
                }
            } else
                if (binding.bottomNav.selectedItemId != TAB_QR_CODE)
                    binding.bottomNav.selectedItemId = TAB_QR_CODE
        }

        binding.bottomNav.setOnItemReselectedListener { }
        binding.bottomNav.setOnItemSelectedListener { menuItem ->
            selectTab(menuItem.itemId)
            true
        }

        if (isFirstTime) {
            initialPageId.log("ON_LANG_CHANGED")
//            binding.bottomNav.selectedItemId = TAB_TODAY_FREE
            selectTab(TAB_HOME)
        }
    }

    override fun onBackPressed(): Boolean {

        return if (binding.bottomNav.selectedItemId == TAB_HOME) {
            false
        } else {
            binding.bottomNav.selectedItemId = TAB_HOME
            true
        }
    }

    private fun selectTab(itemId: Int) {
        val fm = childFragmentManager
        val current = currentFragment
        val newFragment = fm.findFragmentByTag(itemId.toString())
        if (current != null && newFragment != null && newFragment === currentFragment) return

        fm.beginTransaction().apply {
            if (newFragment == null) {
                val fragment = TabContainerFragment.getNewInstance(itemId)
                add(
                    R.id.containerBottomNav,
                    fragment,
                    itemId.toString()
                )
            }
            if (current != null) hide(current)
            if (newFragment != null) show(newFragment)
        }.commit()

    }

    private val isFirstTime
        get() = childFragmentManager.findFragmentByTag(TAB_HOME.toString()) == null &&
                childFragmentManager.findFragmentByTag(TAB_QR_CODE.toString()) == null &&
                childFragmentManager.findFragmentByTag(TAB_PROFILE.toString()) == null

    private val currentFragment: TabContainerFragment?
        get() = childFragmentManager.fragments.firstOrNull { it.isVisible } as? TabContainerFragment

    private val handler = Handler(Looper.getMainLooper())

    companion object {

        fun getNewInstance(initialPageId: Int = TAB_HOME) = BottomNavScreen()
            .apply {
                arguments = bundleOf(INITIAL_PAGE_ID to initialPageId)
            }
    }


}


class TabContainerFragment : NavigationFragment() {

    override val rootScreen: Screen by lazy {
        when (arguments?.getInt(ITEM_ID)) {
            TAB_HOME -> home()
            TAB_QR_CODE -> qrCode()
            TAB_PROFILE -> profile()
            else -> profile()
        }
    }

    companion object {
        private const val ITEM_ID = "bn_item_id"

        const val INITIAL_PAGE_ID = "INITIAL_PAGE_ID"
        val TAB_HOME = R.id.home
        val TAB_QR_CODE = R.id.qr_code
        val TAB_PROFILE = R.id.profile

        fun getNewInstance(itemId: Int) = TabContainerFragment()
            .apply {
                arguments = bundleOf(ITEM_ID to itemId)
            }
    }
}
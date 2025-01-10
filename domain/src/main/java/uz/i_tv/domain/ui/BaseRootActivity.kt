package uz.i_tv.domain.ui

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.terrakok.cicerone.Cicerone
import com.github.terrakok.cicerone.Navigator
import com.github.terrakok.cicerone.Router
import com.github.terrakok.cicerone.Screen
import com.github.terrakok.cicerone.androidx.AppNavigator
import com.zeugmasolutions.localehelper.LocaleHelper
import com.zeugmasolutions.localehelper.LocaleHelperActivityDelegate
import com.zeugmasolutions.localehelper.LocaleHelperActivityDelegateImpl
import uz.i_tv.domain.R
import java.util.*

abstract class BaseRootActivity : AppCompatActivity(R.layout.activity_root) {

    private val localeDelegate: LocaleHelperActivityDelegate = LocaleHelperActivityDelegateImpl()

    override fun getDelegate() = localeDelegate.getAppCompatDelegate(super.getDelegate())

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(localeDelegate.attachBaseContext(newBase))
    }

    private val cicerone: Cicerone<Router> by lazy {
        ciceroneMap.getOrPut(ciceroneKey) { Cicerone.create() }
    }

    val router: Router
        get() = cicerone.router

    protected val currentFragment: BaseFragment?
        get() = supportFragmentManager.findFragmentById(navigationContainerId) as? BaseFragment

    private val navigator by lazy { createNavigator() }
    protected open fun createNavigator(): Navigator = AppNavigator(this, navigationContainerId)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        localeDelegate.onCreate(this)

        ciceroneKey = savedInstanceState?.getString(STATE_CICERONE_KEY)
            ?: "AppActivity_${hashCode()}"

        if (supportFragmentManager.fragments.isEmpty()) {
            router.newRootScreen(rootScreen)
        }

        onActivityCreated(savedInstanceState)
    }


    abstract fun logOut()
    abstract fun navigateToAuthScreen()

    open fun onActivityCreated(savedInstanceState: Bundle?) {}

    override fun onResumeFragments() {
        super.onResumeFragments()
        cicerone.getNavigatorHolder().setNavigator(navigator)
    }

    override fun onBackPressed() {
        if (currentFragment?.onBackPressed() != true) {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        localeDelegate.onResumed(this)
    }

    override fun onPause() {
        cicerone.getNavigatorHolder().removeNavigator()
        super.onPause()
        localeDelegate.onPaused()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) {
            ciceroneMap.remove(ciceroneKey)
        }
    }

    override fun createConfigurationContext(overrideConfiguration: Configuration): Context {
        val context = super.createConfigurationContext(overrideConfiguration)
        return LocaleHelper.onAttach(context)
    }

    override fun getApplicationContext(): Context =
        localeDelegate.getApplicationContext(super.getApplicationContext())

    open fun updateLocale(locale: Locale) {
        localeDelegate.setLocale(this, locale)
    }

    companion object {
        private const val STATE_CICERONE_KEY = "STATE_CICERONE_KEY"
        private val ciceroneMap = mutableMapOf<String, Cicerone<Router>>()
    }

    private lateinit var ciceroneKey: String

    private val navigationContainerId: Int = R.id.mainContainer
    protected abstract val rootScreen: Screen

}
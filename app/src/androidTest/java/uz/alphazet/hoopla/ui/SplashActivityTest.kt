package uz.alphazet.hoopla.ui

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI Automator tests for [SplashActivity]. Splash holds for ~1s then either:
 *  - Launches [MainActivity] when `isFirstTime == false` (bottom nav visible).
 *  - Replaces the `intro_container` with the onboarding fragment when the app
 *    is opened for the very first time.
 *
 * Tested flows:
 *  1. The splash layout (`logo_img`) renders briefly after launch.
 *  2. When `isFirstTime` is false (the state [BaseUiTest.setUp] seeds), the
 *     splash routes to MainActivity — asserted by the bottom nav `home` item.
 *  3. Even with auth tokens cleared the splash still routes to MainActivity —
 *     login status does not affect routing, only `isFirstTime` does.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SplashActivityTest : BaseUiTest() {

    @Before
    override fun setUp() {
        // Force non-first-launch so Splash routes to MainActivity.
        val ctx: Context = ApplicationProvider.getApplicationContext()
        ctx.getSharedPreferences("app_cache", Context.MODE_PRIVATE).edit()
            .putBoolean("isFirstTime", false)
            .commit()
        super.setUp()
    }

    // -----------------------------------------------------------------------
    // 1. Splash logo renders on launch
    // -----------------------------------------------------------------------

    @Test
    fun splash_logo_is_visible_on_launch() {
        launchSplashOnly()

        // logo_img is the sole child until handler fires at ~1s — give it a
        // short window but longer than the post-delay so it is caught before
        // MainActivity replaces it.
        val logoImg = device.wait(Until.findObject(By.res(APP_PACKAGE, "logo_img")), TIMEOUT_MS)
        assertNotNull(
            "logo_img not visible immediately after splash launch — screen_splash did not inflate",
            logoImg
        )
    }

    // -----------------------------------------------------------------------
    // 2. Non-first-launch routes to MainActivity
    // -----------------------------------------------------------------------

    @Test
    fun splash_routes_to_main_activity_when_not_first_launch() {
        launchSplashOnly()

        // MainActivity finishes splash after ~1s; the home bottom-nav item
        // proves MainActivity is in front.
        val homeNav = device.wait(Until.findObject(By.res(APP_PACKAGE, "home")), LAUNCH_READY_MS)
        assertNotNull(
            "home bottom-nav item not visible — splash did not transition to MainActivity within ${LAUNCH_READY_MS}ms",
            homeNav
        )
    }

    // -----------------------------------------------------------------------
    // 3. Logged-out state still routes through splash unchanged
    // -----------------------------------------------------------------------

    @Test
    fun splash_routes_to_main_regardless_of_auth_state() {
        clearAuthTokens()
        launchSplashOnly()

        val logo = device.wait(Until.findObject(By.res(APP_PACKAGE, "logo")), LAUNCH_READY_MS)
        assertNotNull(
            "Home screen logo not visible — splash did not route to MainActivity while logged out",
            logo
        )
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Launches SplashActivity directly (identical to the OS launcher flow).
     * Avoids [launchApp] here because that helper waits for MainActivity —
     * we want to observe the splash before it finishes.
     */
    private fun launchSplashOnly() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val intent = ctx.packageManager
            .getLaunchIntentForPackage(APP_PACKAGE)!!
            .apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK) }
        ctx.startActivity(intent)

        assertNotNull(
            "App did not launch within ${TIMEOUT_MS}ms",
            device.wait(Until.hasObject(By.pkg(APP_PACKAGE).depth(0)), TIMEOUT_MS)
        )
    }
}
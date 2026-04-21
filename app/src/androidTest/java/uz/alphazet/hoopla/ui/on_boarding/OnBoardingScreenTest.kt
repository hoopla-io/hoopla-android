package uz.alphazet.hoopla.ui.on_boarding

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uz.alphazet.hoopla.ui.BaseUiTest

/**
 * UI Automator tests for [OnBoardingScreen] — the first-launch intro fragment
 * that [uz.alphazet.hoopla.ui.SplashActivity] swaps into `intro_container`
 * when `AppCache.isFirstTime == true`.
 *
 * Tested flows:
 *  1. Onboarding page 1 renders — `image`, `title`, `desc`, `skip`, `next`
 *     and the two progress indicators are all visible.
 *  2. Tapping `next` advances to page 2 without leaving onboarding (bottom
 *     nav still absent).
 *  3. Tapping `next` on page 2 lands on MainActivity (home logo visible).
 *  4. Tapping `skip` on page 1 immediately lands on MainActivity.
 *
 * Prerequisites:
 *  - Device / emulator has the debug build installed.
 *  - `isFirstTime` is seeded to `true` *after* [BaseUiTest.setUp] runs
 *    (since BaseUiTest intentionally writes `false` to make other tests
 *    bypass onboarding). We then launch the app ourselves instead of using
 *    the inherited [launchApp] — that helper waits for bottom-nav items
 *    that only exist on MainActivity.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class OnBoardingScreenTest : BaseUiTest() {

    @Before
    override fun setUp() {
        super.setUp()

        // Overwrite the `isFirstTime=false` that BaseUiTest seeds so Splash
        // hosts the onboarding fragment.
        val ctx: Context = ApplicationProvider.getApplicationContext()
        ctx.getSharedPreferences("app_cache", Context.MODE_PRIVATE).edit()
            .putBoolean("isFirstTime", true)
            .commit()

        val intent = ctx.packageManager
            .getLaunchIntentForPackage(APP_PACKAGE)!!
            .apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK) }
        ctx.startActivity(intent)

        assertNotNull(
            "App did not launch within ${TIMEOUT_MS}ms",
            device.wait(Until.hasObject(By.pkg(APP_PACKAGE).depth(0)), TIMEOUT_MS)
        )
    }

    // -----------------------------------------------------------------------
    // 1. Onboarding page 1 renders
    // -----------------------------------------------------------------------

    @Test
    fun onboarding_page_one_renders_all_chrome() {
        waitForOnboardingReady()

        assertNotNull(
            "onboarding image not visible — fragment did not inflate",
            device.findObject(By.res(APP_PACKAGE, "image"))
        )
        assertNotNull("title missing on onboarding", device.findObject(By.res(APP_PACKAGE, "title")))
        assertNotNull("desc missing on onboarding", device.findObject(By.res(APP_PACKAGE, "desc")))
        assertNotNull("skip button missing", device.findObject(By.res(APP_PACKAGE, "skip")))
        assertNotNull("next button missing", device.findObject(By.res(APP_PACKAGE, "next")))
        assertNotNull("indicator1 missing", device.findObject(By.res(APP_PACKAGE, "indicator1")))
        assertNotNull("indicator2 missing", device.findObject(By.res(APP_PACKAGE, "indicator2")))
    }

    // -----------------------------------------------------------------------
    // 2. `next` on page 1 → still on onboarding
    // -----------------------------------------------------------------------

    @Test
    fun tapping_next_on_page_one_advances_but_stays_on_onboarding() {
        waitForOnboardingReady()

        waitForId("next").click()
        device.waitForIdle(IDLE_TIMEOUT_MS)

        assertNotNull(
            "next button disappeared after first tap — must stay visible on page 2",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "next")), TIMEOUT_MS)
        )
        assertNotNull(
            "indicator2 missing after advancing to page 2",
            device.findObject(By.res(APP_PACKAGE, "indicator2"))
        )
        assertFalse(
            "bottom_nav visible while still on onboarding page 2",
            device.hasObject(By.res(APP_PACKAGE, "bottom_nav"))
        )
    }

    // -----------------------------------------------------------------------
    // 3. `next` on page 2 → MainActivity
    // -----------------------------------------------------------------------

    @Test
    fun tapping_next_on_page_two_navigates_to_main_activity() {
        waitForOnboardingReady()

        waitForId("next").click()
        device.waitForIdle(IDLE_TIMEOUT_MS)

        waitForId("next").click()
        device.waitForIdle(IDLE_TIMEOUT_MS)

        assertNotNull(
            "Home logo not visible after completing onboarding — did not navigate to MainActivity",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "logo")), LAUNCH_READY_MS)
        )
    }

    // -----------------------------------------------------------------------
    // 4. `skip` → MainActivity
    // -----------------------------------------------------------------------

    @Test
    fun tapping_skip_navigates_to_main_activity() {
        waitForOnboardingReady()

        waitForId("skip").click()
        device.waitForIdle(IDLE_TIMEOUT_MS)

        assertNotNull(
            "Home logo not visible after tapping skip — did not bypass onboarding",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "logo")), LAUNCH_READY_MS)
        )
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Waits until the onboarding fragment is visible — `next` is the tell. */
    private fun waitForOnboardingReady() {
        assertNotNull(
            "onboarding fragment did not appear within ${LAUNCH_READY_MS}ms",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "next")), LAUNCH_READY_MS)
        )
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }
}
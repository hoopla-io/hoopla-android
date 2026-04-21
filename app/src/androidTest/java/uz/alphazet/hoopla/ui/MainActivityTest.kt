package uz.alphazet.hoopla.ui

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI Automator tests for [MainActivity] — bottom navigation and the
 * unauthenticated orders-tab guard.
 *
 * Tested flows:
 *  1. App launches with the Home tab selected — both the bottom-nav item AND
 *     actual Home content (logo + search card) are visible.
 *  2. Tapping the Profile tab shows the profile header.
 *  3. Tapping the Orders tab while logged out shows the locale-specific
 *     sign-in dialog (message resolved from resources, not a hard-coded
 *     Russian literal).
 *  4. Tapping the Map tab reveals the Yandex MapView (not merely the bottom
 *     nav).
 *  5. After navigating away from and back to the Home tab, the bottom nav
 *     remains visible — other tabs are already asserted by tests 2/4.
 *
 * Prerequisites:
 *  - Device / emulator has the debug build installed.
 *  - User is NOT logged in — [setUp] calls [clearAuthTokens] to guarantee this
 *    so test 3's assertion is unconditional.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityTest : BaseUiTest() {

    @Before
    override fun setUp() {
        // Guarantee logged-out state so the orders-tab guard fires unconditionally.
        clearAuthTokens()
        super.setUp()
        launchApp()
    }

    // -----------------------------------------------------------------------
    // 1. Home tab is selected on launch — AND its content is rendered
    // -----------------------------------------------------------------------

    @Test
    fun app_launches_and_home_screen_is_visible() {
        waitForId("bottom_nav")

        // Content check — proves HomeScreen was actually chosen as the startDestination
        // rather than just "home" being in the bottom-nav a11y tree.
        val logo = device.wait(Until.findObject(By.res(APP_PACKAGE, "logo")), TIMEOUT_MS)
        assertNotNull("Home screen 'logo' not visible — Home is not the default tab", logo)
        val search = device.wait(Until.findObject(By.res(APP_PACKAGE, "search")), TIMEOUT_MS)
        assertNotNull("Home screen 'search' card not visible", search)
    }

    // -----------------------------------------------------------------------
    // 2. Navigating to the Profile tab
    // -----------------------------------------------------------------------

    @Test
    fun tapping_profile_tab_opens_profile_screen() {
        waitForId("profile").click()

        // Resource ID is language-independent, so this assertion is robust.
        val header = device.wait(Until.findObject(By.res(APP_PACKAGE, "header_title")), TIMEOUT_MS)
        assertNotNull("Profile screen header_title not found after tapping Profile tab", header)
    }

    // -----------------------------------------------------------------------
    // 3. Orders tab guard — locale-aware dialog assertion
    // -----------------------------------------------------------------------

    @Test
    fun tapping_orders_tab_when_logged_out_shows_sign_in_dialog() {
        waitForId("orders").click()

        // Resolve the expected dialog message in the active pref language so
        // the test passes on RU / EN / UZ devices alike. We match the first
        // line only — dialog renderers sometimes split on "\n".
        val expected = localizedString(uz.alphazet.domain.R.string.you_r_not_logged_in)
            .substringBefore('\n')
            .trim()

        val dialog = device.wait(Until.findObject(By.textContains(expected)), TIMEOUT_MS)
        assertNotNull(
            "Sign-in dialog message '$expected' not found when tapping Orders while logged out",
            dialog
        )

        // Dismiss so subsequent tests start on a clean state.
        device.pressBack()
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }

    // -----------------------------------------------------------------------
    // 4. Map tab — verify the MapView itself, not just bottom_nav persistence
    // -----------------------------------------------------------------------

    @Test
    fun tapping_map_tab_opens_map_view() {
        waitForId("map").click()

        val mapView = device.wait(Until.findObject(By.res(APP_PACKAGE, "map_view")), TIMEOUT_MS)
        assertNotNull("Yandex MapView (R.id.map_view) not found after tapping Map tab", mapView)

        val bottomNav = device.wait(Until.findObject(By.res(APP_PACKAGE, "bottom_nav")), TIMEOUT_MS)
        assertNotNull("Bottom nav disappeared after navigating to Map tab", bottomNav)
    }

    // -----------------------------------------------------------------------
    // 5. Bottom nav survives navigation back to the Home tab
    //    (Profile + Map tabs are already asserted by tests 2 and 4.)
    // -----------------------------------------------------------------------

    @Test
    fun bottom_nav_persists_after_returning_to_home_tab() {
        waitForId("profile").click()
        device.waitForIdle(IDLE_TIMEOUT_MS)

        waitForId("home").click()
        device.waitForIdle(IDLE_TIMEOUT_MS)

        val nav = device.wait(Until.findObject(By.res(APP_PACKAGE, "bottom_nav")), TIMEOUT_MS)
        assertNotNull("Bottom nav must remain visible after returning to Home", nav)
    }
}
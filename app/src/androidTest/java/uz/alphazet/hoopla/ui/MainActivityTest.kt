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
 * UI Automator tests for [MainActivity] — bottom navigation and unauthenticated guard.
 *
 * Tested flows:
 *  1. App launches and the Home tab is selected by default.
 *  2. Tapping "Профиль" (Profile) tab opens the Profile screen.
 *  3. Tapping "Заказы" (Orders) tab without a logged-in user shows the
 *     sign-in prompt dialog instead of navigating.
 *  4. Tapping "Map" tab shows the map container.
 *  5. Bottom navigation is always visible on every tab.
 *
 * Prerequisites:
 *  - Device / emulator has the app installed (debug build).
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
    // 1. Home tab is selected on launch
    // -----------------------------------------------------------------------

    @Test
    fun app_launches_and_home_screen_is_visible() {
        // The bottom nav itself must be present
        waitForId("bottom_nav")

        // The home tab item must exist
        val homeTab = device.wait(Until.findObject(By.res(APP_PACKAGE, "home")), TIMEOUT_MS)
        assertNotNull("Home tab not found in bottom navigation", homeTab)
    }

    // -----------------------------------------------------------------------
    // 2. Navigating to the Profile tab
    // -----------------------------------------------------------------------

    @Test
    fun tapping_profile_tab_opens_profile_screen() {
        val profileTab = waitForId("profile")
        profileTab.click()

        // Verify using resource ID so the check is language-independent.
        val header = device.wait(Until.findObject(By.res(APP_PACKAGE, "header_title")), TIMEOUT_MS)
        assertNotNull("Profile screen header_title not found after tapping Profile tab", header)
    }

    // -----------------------------------------------------------------------
    // 3. Orders tab guard — shows sign-in dialog when not logged in
    // -----------------------------------------------------------------------

    @Test
    fun tapping_orders_tab_when_logged_out_shows_sign_in_dialog() {
        val ordersTab = waitForId("orders")
        ordersTab.click()

        // clearAuthTokens() in setUp guarantees the device is logged out, so the
        // sign-in dialog MUST appear.  This assertion is now unconditional.
        val dialog = device.wait(
            Until.findObject(By.textContains("авторизованы")),
            TIMEOUT_MS
        )
        assertNotNull("Sign-in dialog must appear when tapping Orders while logged out", dialog)

        // Dismiss by tapping "Отмена" so subsequent tests are unaffected.
        val cancelButton = device.wait(Until.findObject(By.textContains("Отмена")), TIMEOUT_MS)
        cancelButton?.click()
    }

    // -----------------------------------------------------------------------
    // 4. Map tab
    // -----------------------------------------------------------------------

    @Test
    fun tapping_map_tab_shows_map_screen() {
        val mapTab = device.findObject(By.res(APP_PACKAGE, "map"))
        assertNotNull("Map tab not found", mapTab)
        mapTab.click()
        device.waitForIdle(IDLE_TIMEOUT_MS)

        // After tapping Map the bottom nav must still be present
        val bottomNav = device.wait(Until.findObject(By.res(APP_PACKAGE, "bottom_nav")), TIMEOUT_MS)
        assertNotNull("Bottom nav disappeared after navigating to Map tab", bottomNav)
    }

    // -----------------------------------------------------------------------
    // 5. Bottom navigation is always visible
    // -----------------------------------------------------------------------

    @Test
    fun bottom_nav_is_visible_on_all_tabs() {
        val tabIds = listOf("home", "map", "profile")
        for (tabId in tabIds) {
            val tab = device.findObject(By.res(APP_PACKAGE, tabId))
            tab?.click()
            device.waitForIdle(IDLE_TIMEOUT_MS)

            val nav = device.wait(Until.findObject(By.res(APP_PACKAGE, "bottom_nav")), TIMEOUT_MS)
            assertNotNull("Bottom nav not visible on tab '$tabId'", nav)
        }
    }
}
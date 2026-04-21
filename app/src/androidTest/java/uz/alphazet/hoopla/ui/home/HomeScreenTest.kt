package uz.alphazet.hoopla.ui.home

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uz.alphazet.hoopla.ui.BaseUiTest

/**
 * UI Automator tests for [uz.alphazet.hoopla.ui.home.HomeScreen].
 *
 * Tested flows (Home is the start destination, no auth required):
 *  1. Home renders with its core elements — logo, search card, scan FAB
 *     and the [SwipeRefreshLayout] container.
 *  2. Tapping the search card opens [uz.alphazet.hoopla.ui.search.SearchScreen]
 *     (asserted by `inputSearch`).
 *  3. Tapping the notification button opens
 *     [uz.alphazet.hoopla.ui.home.NotificationsScreen] (asserted by
 *     `notification_rv`).
 *  4. The categories RecyclerView becomes visible once the categories API
 *     returns a non-empty list.
 *  5. Pull-to-refresh leaves the user on the Home screen — swipe-refresh
 *     completes without navigation or crash.
 *
 * Prerequisites:
 *  - Device / emulator has the debug build installed.
 *  - Network is reachable — flow 4 depends on the categories endpoint.
 *  - Logged-out state is enforced via [clearAuthTokens] so the test order
 *    is independent of any prior sign-in.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class HomeScreenTest : BaseUiTest() {

    @Before
    override fun setUp() {
        clearAuthTokens()
        super.setUp()
        launchApp()

        // Make sure we are on the Home tab (it is the default start destination
        // but tap explicitly so we are robust against any state the launcher
        // may have restored).
        waitForId("home").click()
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }

    // -----------------------------------------------------------------------
    // 1. Core elements render on launch
    // -----------------------------------------------------------------------

    @Test
    fun home_screen_renders_with_core_elements_on_launch() {
        assertNotNull(
            "logo not visible — Home is not the active screen",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "logo")), TIMEOUT_MS)
        )
        assertNotNull(
            "search card not visible on Home",
            device.findObject(By.res(APP_PACKAGE, "search"))
        )
        assertNotNull(
            "scan FAB not visible on Home",
            device.findObject(By.res(APP_PACKAGE, "scan"))
        )
        assertNotNull(
            "swipe_refresh_layout not part of the inflated HomeScreen",
            device.findObject(By.res(APP_PACKAGE, "swipe_refresh_layout"))
        )
    }

    // -----------------------------------------------------------------------
    // 2. Tapping the search card opens SearchScreen
    // -----------------------------------------------------------------------

    @Test
    fun tapping_search_card_opens_search_screen() {
        waitForId("search").click()

        assertNotNull(
            "SearchScreen inputSearch not found after tapping the search card",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "inputSearch")), TIMEOUT_MS)
        )

        // Pop back to Home so subsequent tests start cleanly.
        device.pressBack()
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }

    // -----------------------------------------------------------------------
    // 3. Tapping the notification button opens NotificationsScreen
    // -----------------------------------------------------------------------

    @Test
    fun tapping_notification_card_opens_notifications_screen() {
        waitForId("notification").click()

        assertNotNull(
            "NotificationsScreen notification_rv not found after tapping the notification button",
            device.wait(
                Until.findObject(By.res(APP_PACKAGE, "notification_rv")),
                LAUNCH_READY_MS
            )
        )

        device.pressBack()
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }

    // -----------------------------------------------------------------------
    // 4. Categories RecyclerView becomes visible after the API responds
    // -----------------------------------------------------------------------

    @Test
    fun categories_recycler_becomes_visible_after_load() {
        // categories_rv is GONE in XML by default; the success branch in
        // collectCategories() makes it visible. Use the larger budget — this
        // depends on a network round-trip during HomeScreen.initialize().
        assertNotNull(
            "categories_rv did not become visible — categories endpoint may be unreachable",
            device.wait(
                Until.findObject(By.res(APP_PACKAGE, "categories_rv")),
                LAUNCH_READY_MS
            )
        )
    }

    // -----------------------------------------------------------------------
    // 5. Pull-to-refresh keeps the user on the Home screen
    // -----------------------------------------------------------------------

    @Test
    fun pull_to_refresh_keeps_home_screen_rendered() {
        val swipe = waitForId("swipe_refresh_layout")

        // Swipe down inside the SwipeRefreshLayout to trigger onRefresh().
        // 50 steps ≈ ~500ms — slow enough that the gesture is recognised as
        // a pull-to-refresh and not a fling.
        val bounds = swipe.visibleBounds
        device.swipe(
            bounds.centerX(),
            bounds.top + 50,
            bounds.centerX(),
            bounds.bottom - 50,
            50
        )
        device.waitForIdle(IDLE_TIMEOUT_MS)

        // Home logo must still be visible — i.e. we did not navigate away
        // and the screen did not crash.
        assertNotNull(
            "logo disappeared after pull-to-refresh — Home may have crashed or navigated away",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "logo")), TIMEOUT_MS)
        )
    }
}
package uz.alphazet.hoopla.ui.map

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
 * UI Automator tests for [uz.alphazet.hoopla.ui.map.MapScreen] — the Yandex
 * MapView-based shops map reached via the bottom nav.
 *
 * Tested flows:
 *  1. Tapping the Map tab renders the MapView (`map_view`), the header app
 *     bar and the lowercase logo TextView.
 *  2. The bottom navigation remains visible on the Map tab so the user can
 *     return to other tabs.
 *  3. Tapping Home after Map returns to the Home screen (bottom nav
 *     survives cross-tab navigation).
 *
 * Prerequisites:
 *  - Device / emulator has the debug build installed.
 *  - Yandex MapKit is configured with a valid API key so the MapView inflates
 *    rather than throwing during `onCreateView`.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class MapScreenTest : BaseUiTest() {

    @Before
    override fun setUp() {
        clearAuthTokens()
        super.setUp()
        launchApp()

        waitForId("map").click()
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }

    // -----------------------------------------------------------------------
    // 1. Map screen renders core elements
    // -----------------------------------------------------------------------

    @Test
    fun map_screen_renders_header_and_mapview() {
        val mapView = device.wait(Until.findObject(By.res(APP_PACKAGE, "map_view")), TIMEOUT_MS)
        assertNotNull("Yandex MapView (map_view) not found after tapping Map tab", mapView)

        assertNotNull(
            "header_layout missing — MapScreen did not inflate",
            device.findObject(By.res(APP_PACKAGE, "header_layout"))
        )
        assertNotNull(
            "lowercase app-name logo TextView not found in map header",
            device.findObject(By.res(APP_PACKAGE, "logo"))
        )
    }

    // -----------------------------------------------------------------------
    // 2. Bottom nav stays visible on Map tab
    // -----------------------------------------------------------------------

    @Test
    fun bottom_nav_remains_visible_on_map_tab() {
        assertNotNull(
            "map_view not ready",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "map_view")), TIMEOUT_MS)
        )
        assertNotNull(
            "Bottom nav disappeared on Map tab — user cannot return to Home",
            device.findObject(By.res(APP_PACKAGE, "bottom_nav"))
        )
    }

    // -----------------------------------------------------------------------
    // 3. Map → Home round-trip via bottom nav
    // -----------------------------------------------------------------------

    @Test
    fun tapping_home_after_map_returns_to_home_screen() {
        assertNotNull(
            "map_view not ready",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "map_view")), TIMEOUT_MS)
        )

        waitForId("home").click()
        device.waitForIdle(IDLE_TIMEOUT_MS)

        // Home's `search` card is distinctive to HomeScreen (Map's `logo` has
        // the same id as Home's, so search is a stronger tell).
        assertNotNull(
            "search card not visible after tapping Home from Map — did not navigate back to Home",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "search")), TIMEOUT_MS)
        )
    }
}
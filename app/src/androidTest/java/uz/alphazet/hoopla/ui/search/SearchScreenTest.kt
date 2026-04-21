package uz.alphazet.hoopla.ui.search

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
 * UI Automator tests for [uz.alphazet.hoopla.ui.search.SearchScreen], reached
 * from Home by tapping the `search` card. SearchScreen is a standalone
 * Activity, not a bottom-nav destination.
 *
 * Tested flows:
 *  1. Tapping the Home search card opens [SearchScreen] — toolbar and
 *     input field (`inputSearch`) render.
 *  2. Typing into `inputSearch` does not crash — the screen keeps the input
 *     and the results RecyclerView (`items_rv`) stays in the hierarchy.
 *  3. Tapping the toolbar back button finishes the activity and returns to
 *     the Home screen (Home `logo` becomes visible again).
 *
 * Prerequisites:
 *  - Device / emulator has the debug build installed.
 *  - Network is reachable — the `doAfterTextChanged` handler hits the
 *    near-shops endpoint; we don't assert on results, only that the screen
 *    survives the query.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SearchScreenTest : BaseUiTest() {

    @Before
    override fun setUp() {
        clearAuthTokens()
        super.setUp()
        launchApp()

        // Enter SearchScreen from Home. Tapping the `search` card on Home
        // launches SearchScreen via the home flow, matching production usage.
        waitForId("home").click()
        device.waitForIdle(IDLE_TIMEOUT_MS)
        waitForId("search").click()
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }

    // -----------------------------------------------------------------------
    // 1. SearchScreen renders core elements
    // -----------------------------------------------------------------------

    @Test
    fun search_screen_renders_toolbar_and_input() {
        assertNotNull(
            "inputSearch not visible — SearchScreen did not mount",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "inputSearch")), TIMEOUT_MS)
        )
        assertNotNull(
            "toolbar missing on SearchScreen",
            device.findObject(By.res(APP_PACKAGE, "toolbar"))
        )
        assertNotNull(
            "items_rv missing — search results list has no host",
            device.findObject(By.res(APP_PACKAGE, "items_rv"))
        )
    }

    // -----------------------------------------------------------------------
    // 2. Typing a query does not crash the screen
    // -----------------------------------------------------------------------

    @Test
    fun typing_a_query_keeps_screen_rendered() {
        val input = waitForId("inputSearch")
        input.click()
        device.waitForIdle(IDLE_TIMEOUT_MS)

        // `input text` preserves per-character TextWatcher firings so the
        // doAfterTextChanged handler runs just like production.
        device.executeShellCommand("input text hoopla")
        device.waitForIdle(IDLE_TIMEOUT_MS)

        assertNotNull(
            "inputSearch disappeared after typing — SearchScreen may have crashed",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "inputSearch")), TIMEOUT_MS)
        )
        assertNotNull(
            "items_rv gone — results host disappeared after typing",
            device.findObject(By.res(APP_PACKAGE, "items_rv"))
        )
    }

    // -----------------------------------------------------------------------
    // 3. Toolbar back finishes activity → Home
    // -----------------------------------------------------------------------

    @Test
    fun pressing_system_back_returns_to_home() {
        assertNotNull(
            "inputSearch not visible before back",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "inputSearch")), TIMEOUT_MS)
        )

        device.pressBack()
        device.waitForIdle(IDLE_TIMEOUT_MS)

        assertNotNull(
            "Home logo not visible after pressing back from SearchScreen",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "logo")), TIMEOUT_MS)
        )
    }
}
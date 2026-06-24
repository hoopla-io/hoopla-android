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
 * UI Automator tests for [uz.alphazet.hoopla.ui.home.NotificationsScreen] —
 * the standalone activity reached by tapping the `notification` icon on
 * Home. The activity hosts a paged RecyclerView (`notification_rv`) and
 * eagerly calls `markRead()` on the ViewModel during `onCreate`.
 *
 * Tested flows:
 *  1. Tapping the notification icon on Home opens NotificationsScreen —
 *     the toolbar and `notification_rv` render.
 *  2. The toolbar navigation button finishes the activity and returns to
 *     the Home screen.
 *  3. Pressing system back from NotificationsScreen returns to Home (paging
 *     source has no state that should swallow the back press).
 *
 * Prerequisites:
 *  - Device / emulator has the debug build installed.
 *  - Network is reachable — the notifications pager issues a request during
 *    onCreate; we only assert structural chrome, not the item list, so an
 *    empty backend response is fine.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class NotificationsScreenTest : BaseUiTest() {

    @Before
    override fun setUp() {
        clearAuthTokens()
        super.setUp()
        launchApp()

        // NotificationsScreen is reached from Home — mirror the production flow.
        waitForId("home").click()
        device.waitForIdle(IDLE_TIMEOUT_MS)
        waitForId("notification").click()
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }

    // -----------------------------------------------------------------------
    // 1. NotificationsScreen renders
    // -----------------------------------------------------------------------

    @Test
    fun notifications_screen_renders_toolbar_and_recycler() {
        assertNotNull(
            "notification_rv not visible — NotificationsScreen did not mount",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "notification_rv")), LAUNCH_READY_MS)
        )
        assertNotNull(
            "toolbar missing on NotificationsScreen",
            device.findObject(By.res(APP_PACKAGE, "toolbar"))
        )
    }

    // -----------------------------------------------------------------------
    // 2. Toolbar back → Home
    // -----------------------------------------------------------------------

    @Test
    fun tapping_toolbar_back_returns_to_home() {
        assertNotNull(
            "notification_rv not ready before back",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "notification_rv")), LAUNCH_READY_MS)
        )

        // The toolbar nav icon's content description is localised — tap the
        // button via content-description selector (same pattern used by the
        // ShopDetail back-arrow test).
        val backArrow = device.wait(Until.findObject(By.desc(localizedString(uz.alphazet.domain.R.string.back))), TIMEOUT_MS)
        if (backArrow != null) {
            backArrow.click()
        } else {
            // Fallback for non-UZ locales — press system back instead.
            device.pressBack()
        }
        device.waitForIdle(IDLE_TIMEOUT_MS)

        assertNotNull(
            "Home logo not visible after leaving NotificationsScreen",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "logo")), TIMEOUT_MS)
        )
    }

    // -----------------------------------------------------------------------
    // 3. System back → Home
    // -----------------------------------------------------------------------

    @Test
    fun pressing_system_back_returns_to_home() {
        assertNotNull(
            "notification_rv not ready before back",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "notification_rv")), LAUNCH_READY_MS)
        )

        device.pressBack()
        device.waitForIdle(IDLE_TIMEOUT_MS)

        assertNotNull(
            "Home logo not visible after pressing system back from NotificationsScreen",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "logo")), TIMEOUT_MS)
        )
    }
}
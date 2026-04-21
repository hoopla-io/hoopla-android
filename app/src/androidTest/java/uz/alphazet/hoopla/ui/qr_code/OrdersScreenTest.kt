package uz.alphazet.hoopla.ui.qr_code

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import uz.alphazet.hoopla.ui.BaseUiTest

/**
 * UI Automator tests for [uz.alphazet.hoopla.ui.qr_code.OrdersScreen] — the
 * bottom-nav "orders" tab that lists the user's previous orders via a
 * PagingDataAdapter.
 *
 * Two distinct states are exercised:
 *
 *  - **Logged-out guard:** tapping `orders` with an empty access token pops
 *    the sign-in dialog (already covered by
 *    [uz.alphazet.hoopla.ui.MainActivityTest]). We confirm here that
 *    OrdersScreen itself does NOT mount — the guard is wired up at the
 *    bottom-nav listener level, not inside the fragment.
 *
 *  - **Logged-in chrome:** seeding a non-empty `accessToken` lets the bottom
 *    nav pass the guard and the OrdersScreen fragment inflates. The backend
 *    will reject the fake token with 401, so the paging list ends up empty
 *    and `empty_state` becomes visible — but `header_layout`, `order_rv` and
 *    `swipe_refresh_layout` are all in the hierarchy either way, which is
 *    what we assert.
 *
 * Each test seeds its own prefs AFTER the inherited [setUp] runs so that the
 * logged-out and logged-in cases stay independent.
 *
 * Prerequisites:
 *  - Device / emulator has the debug build installed.
 *  - Network is reachable — paging requests the order-history endpoint on
 *    fragment create; we don't assert on list contents.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class OrdersScreenTest : BaseUiTest() {

    // -----------------------------------------------------------------------
    // 1. Logged-out: orders tab guard prevents OrdersScreen from mounting
    // -----------------------------------------------------------------------

    @Test
    fun orders_tab_does_not_mount_when_logged_out() {
        clearAuthTokens()
        launchApp()

        waitForId("orders").click()
        device.waitForIdle(IDLE_TIMEOUT_MS)

        // order_rv is OrdersScreen's list host — if the guard worked, the
        // fragment never mounted and order_rv is absent.
        val mounted = device.wait(
            Until.hasObject(By.res(APP_PACKAGE, "order_rv")),
            IDLE_TIMEOUT_MS
        )
        assertFalse(
            "order_rv visible while logged out — MainActivity guard did not block navigation",
            mounted
        )

        // Dismiss the sign-in dialog so the test leaves the app in a clean state.
        device.pressBack()
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }

    // -----------------------------------------------------------------------
    // 2. Logged-in: OrdersScreen chrome renders (header, order_rv, swipe)
    // -----------------------------------------------------------------------

    @Test
    fun orders_screen_renders_chrome_when_logged_in() {
        seedFakeAccessToken()
        launchApp()

        waitForId("orders").click()
        device.waitForIdle(IDLE_TIMEOUT_MS)

        assertNotNull(
            "order_rv not visible after tapping Orders — fragment did not mount",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "order_rv")), LAUNCH_READY_MS)
        )
        assertNotNull(
            "header_layout missing on OrdersScreen",
            device.findObject(By.res(APP_PACKAGE, "header_layout"))
        )
        assertNotNull(
            "swipe_refresh_layout missing on OrdersScreen",
            device.findObject(By.res(APP_PACKAGE, "swipe_refresh_layout"))
        )
        assertNotNull(
            "header_title missing — order_history toolbar label did not inflate",
            device.findObject(By.res(APP_PACKAGE, "header_title"))
        )
    }

    // -----------------------------------------------------------------------
    // 3. Logged-in: bottom nav persists on OrdersScreen
    // -----------------------------------------------------------------------

    @Test
    fun bottom_nav_persists_on_orders_screen() {
        seedFakeAccessToken()
        launchApp()

        waitForId("orders").click()
        device.waitForIdle(IDLE_TIMEOUT_MS)

        assertNotNull(
            "order_rv not ready",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "order_rv")), LAUNCH_READY_MS)
        )
        assertNotNull(
            "bottom_nav disappeared on Orders tab — user cannot return to Home",
            device.findObject(By.res(APP_PACKAGE, "bottom_nav"))
        )
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Seeds a non-empty `accessToken` so MainActivity's orders-tab guard
     * lets the tap through. The token is deliberately invalid — the paging
     * backend will respond with 401 — but that's fine: we only assert on
     * the fragment chrome, not on list contents.
     */
    private fun seedFakeAccessToken() {
        InstrumentationRegistry.getInstrumentation().targetContext
            .getSharedPreferences("app_cache", Context.MODE_PRIVATE)
            .edit()
            .putString("accessToken", "ui-test-fake-token")
            .putString("refreshToken", "ui-test-fake-refresh")
            .commit()
    }
}
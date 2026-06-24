package uz.alphazet.hoopla.ui.qr_code

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
 * UI Automator tests for [OrderInfoScreen] — the order detail screen opened
 * from [OrdersScreen]. It reads the order id from the `id` extra, fetches
 * `getOrderInfo(id)` on create, and renders the order's drink image, status,
 * modifiers list and (for PendingPayment orders) cancel / continue actions.
 *
 * Tested flows:
 *  1. Launching OrderInfoScreen directly via Intent with `id = -1` still
 *     renders the layout chrome — toolbar, image, name, status, info_rv.
 *     The backend will 404 on id=-1 but the activity survives.
 *  2. Toolbar back button finishes the activity.
 *
 * Prerequisites:
 *  - Device / emulator has the debug build installed.
 *  - No auth / valid order id required for chrome assertions.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class OrderInfoScreenTest : BaseUiTest() {

    @Before
    override fun setUp() {
        clearAuthTokens()
        super.setUp()
        launchOrderInfoScreen(orderId = -1)
    }

    // -----------------------------------------------------------------------
    // 1. OrderInfoScreen renders core chrome
    // -----------------------------------------------------------------------

    @Test
    fun order_info_screen_renders_layout_chrome() {
        assertNotNull(
            "toolbar missing on OrderInfoScreen",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "toolbar")), LAUNCH_READY_MS)
        )
        assertNotNull(
            "image missing — drink image view did not inflate",
            device.findObject(By.res(APP_PACKAGE, "image"))
        )
        assertNotNull(
            "name missing — drink name label did not inflate",
            device.findObject(By.res(APP_PACKAGE, "name"))
        )
        assertNotNull(
            "status missing — order status label did not inflate",
            device.findObject(By.res(APP_PACKAGE, "status"))
        )
        assertNotNull(
            "info_rv missing — modifiers RecyclerView did not inflate",
            device.findObject(By.res(APP_PACKAGE, "info_rv"))
        )
    }

    // -----------------------------------------------------------------------
    // 2. Toolbar back finishes activity
    // -----------------------------------------------------------------------

    @Test
    fun tapping_toolbar_back_finishes_activity() {
        assertNotNull(
            "toolbar not ready before back",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "toolbar")), LAUNCH_READY_MS)
        )

        val backArrow = device.wait(Until.findObject(By.desc(localizedString(uz.alphazet.domain.R.string.back))), TIMEOUT_MS)
        if (backArrow != null) {
            backArrow.click()
        } else {
            device.pressBack()
        }
        device.waitForIdle(IDLE_TIMEOUT_MS)

        val stillVisible = device.wait(
            Until.hasObject(By.res(APP_PACKAGE, "info_rv")),
            IDLE_TIMEOUT_MS
        )
        assertFalse(
            "info_rv still visible after back — activity did not finish",
            stillVisible
        )
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Launches [OrderInfoScreen] directly with an [orderId] extra. */
    private fun launchOrderInfoScreen(orderId: Int) {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val intent = Intent().apply {
            setClassName(APP_PACKAGE, "uz.alphazet.hoopla.ui.qr_code.OrderInfoScreen")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra("id", orderId)
        }
        ctx.startActivity(intent)

        assertNotNull(
            "OrderInfoScreen did not launch within ${LAUNCH_READY_MS}ms",
            device.wait(Until.hasObject(By.pkg(APP_PACKAGE).depth(0)), LAUNCH_READY_MS)
        )
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }
}
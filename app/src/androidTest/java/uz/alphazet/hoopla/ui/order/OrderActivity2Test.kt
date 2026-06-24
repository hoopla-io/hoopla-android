package uz.alphazet.hoopla.ui.order

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
 * UI Automator tests for [OrderActivity2] — an alternate drink order flow
 * that uses a single `modifications_rv` grouped by modifier type instead of
 * the four separate size/sugar/milk/syrup RecyclerViews in [OrderActivity].
 *
 * Tested flows:
 *  1. Launching OrderActivity2 directly via Intent with `shop_id = -1`
 *     inflates the layout chrome — toolbar, image, modifications_rv,
 *     size_selector and the `order` CTA.
 *  2. Toolbar back finishes the activity.
 *
 * Prerequisites:
 *  - Device / emulator has the debug build installed.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class OrderActivity2Test : BaseUiTest() {

    @Before
    override fun setUp() {
        clearAuthTokens()
        super.setUp()
        launchOrderActivity2()
    }

    // -----------------------------------------------------------------------
    // 1. OrderActivity2 renders layout chrome
    // -----------------------------------------------------------------------

    @Test
    fun order_activity2_renders_layout_chrome() {
        assertNotNull(
            "toolbar missing on OrderActivity2",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "toolbar")), LAUNCH_READY_MS)
        )
        assertNotNull(
            "image missing — drink image ImageView did not inflate",
            device.findObject(By.res(APP_PACKAGE, "image"))
        )
        assertNotNull(
            "modifications_rv missing — drink modifications list did not inflate",
            device.findObject(By.res(APP_PACKAGE, "modifications_rv"))
        )
        assertNotNull(
            "size_selector missing — size selector control did not inflate",
            device.findObject(By.res(APP_PACKAGE, "size_selector"))
        )
        assertNotNull(
            "order CTA missing — user cannot submit the order",
            device.findObject(By.res(APP_PACKAGE, "order"))
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
            Until.hasObject(By.res(APP_PACKAGE, "modifications_rv")),
            IDLE_TIMEOUT_MS
        )
        assertFalse(
            "modifications_rv still visible after back — activity did not finish",
            stillVisible
        )
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private fun launchOrderActivity2() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val intent = Intent().apply {
            setClassName(APP_PACKAGE, "uz.alphazet.hoopla.ui.order.OrderActivity2")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra("shop_id", -1)
            putExtra("shop_name", "ui-test-shop")
        }
        ctx.startActivity(intent)

        assertNotNull(
            "OrderActivity2 did not launch within ${LAUNCH_READY_MS}ms",
            device.wait(Until.hasObject(By.pkg(APP_PACKAGE).depth(0)), LAUNCH_READY_MS)
        )
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }
}
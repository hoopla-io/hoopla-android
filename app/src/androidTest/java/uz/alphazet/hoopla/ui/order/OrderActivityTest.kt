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
 * UI Automator tests for [OrderActivity] — the drink-customisation screen
 * reached from [uz.alphazet.hoopla.ui.shop_details.ShopDetailActivity] when
 * the user taps a product. It reads `SHOP_ID`, `SHOP_NAME` and the
 * `DRINK_DATA` parcelable extra, then calls `validateOrder(shopId, drinkId)`
 * to hydrate the size / sugar / milk / syrup spinners.
 *
 * Tested flows:
 *  1. Launching OrderActivity directly via Intent (without the parcelable
 *     DRINK_DATA — id defaults to -1) still inflates the layout. The
 *     backend `validateOrder(-1, -1)` call will fail, but the size / sugar
 *     / milk / syrup adapters and the `order` CTA are all part of the
 *     inflated hierarchy and assertable.
 *  2. Toolbar back button finishes the activity.
 *
 * Launching a fully-driven end-to-end OrderActivity test requires auth + a
 * real shop + a real drink to produce a valid OrderDetails response from
 * `validateOrder`. That full flow is not feasible from a UI test alone and
 * is intentionally out of scope here.
 *
 * Prerequisites:
 *  - Device / emulator has the debug build installed.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class OrderActivityTest : BaseUiTest() {

    @Before
    override fun setUp() {
        clearAuthTokens()
        super.setUp()
        launchOrderActivity()
    }

    // -----------------------------------------------------------------------
    // 1. OrderActivity renders layout chrome
    // -----------------------------------------------------------------------

    @Test
    fun order_activity_renders_layout_chrome() {
        assertNotNull(
            "toolbar missing on OrderActivity",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "toolbar")), LAUNCH_READY_MS)
        )
        assertNotNull(
            "image missing — drink image view did not inflate",
            device.findObject(By.res(APP_PACKAGE, "image"))
        )
        assertNotNull(
            "name missing — drink name TextView did not inflate",
            device.findObject(By.res(APP_PACKAGE, "name"))
        )
        assertNotNull(
            "sizes missing — size RecyclerView did not inflate",
            device.findObject(By.res(APP_PACKAGE, "sizes"))
        )
        assertNotNull(
            "sugars missing — sugar RecyclerView did not inflate",
            device.findObject(By.res(APP_PACKAGE, "sugars"))
        )
        assertNotNull(
            "milk_types missing — milk RecyclerView did not inflate",
            device.findObject(By.res(APP_PACKAGE, "milk_types"))
        )
        assertNotNull(
            "syrups missing — syrup RecyclerView did not inflate",
            device.findObject(By.res(APP_PACKAGE, "syrups"))
        )
        assertNotNull(
            "order CTA button missing — user cannot submit the order",
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
            Until.hasObject(By.res(APP_PACKAGE, "sizes")),
            IDLE_TIMEOUT_MS
        )
        assertFalse(
            "sizes RecyclerView still visible after back — activity did not finish",
            stillVisible
        )
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Launches [OrderActivity] directly with `shop_id = -1`. The
     * `validateOrder(-1, -1)` call will fail against the backend, but the
     * layout is inflated before the response arrives so chrome assertions
     * are still valid.
     */
    private fun launchOrderActivity() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val intent = Intent().apply {
            setClassName(APP_PACKAGE, "uz.alphazet.hoopla.ui.order.OrderActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra("shop_id", -1)
            putExtra("shop_name", "ui-test-shop")
        }
        ctx.startActivity(intent)

        assertNotNull(
            "OrderActivity did not launch within ${LAUNCH_READY_MS}ms",
            device.wait(Until.hasObject(By.pkg(APP_PACKAGE).depth(0)), LAUNCH_READY_MS)
        )
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }
}
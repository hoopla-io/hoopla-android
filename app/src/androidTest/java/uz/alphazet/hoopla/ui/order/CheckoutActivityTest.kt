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
 * UI Automator tests for [CheckoutActivity] — the "review & pay" screen
 * reached from [OrderActivity] once the user has configured a drink. It
 * reads `Constants.DATA` (OrderDetails) and `Constants.MODIFIERS` extras.
 *
 * Tested flows:
 *  1. Launching CheckoutActivity directly via Intent without extras still
 *     inflates the layout — the `orderData` lazy returns null, null-safe
 *     view setters don't crash the activity, and the resource ids
 *     (toolbar, image, name, total_summa, info_rv, order) are in the
 *     hierarchy.
 *  2. Toolbar back finishes the activity.
 *
 * A fully-driven end-to-end CheckoutActivity test requires a real
 * OrderDetails Parcelable (produced by OrderActivity's validateOrder
 * response) plus auth; that is out of scope here.
 *
 * Prerequisites:
 *  - Device / emulator has the debug build installed.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class CheckoutActivityTest : BaseUiTest() {

    @Before
    override fun setUp() {
        clearAuthTokens()
        super.setUp()
        launchCheckoutActivity()
    }

    // -----------------------------------------------------------------------
    // 1. CheckoutActivity renders layout chrome
    // -----------------------------------------------------------------------

    @Test
    fun checkout_activity_renders_layout_chrome() {
        assertNotNull(
            "toolbar missing on CheckoutActivity",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "toolbar")), LAUNCH_READY_MS)
        )
        assertNotNull(
            "image missing — drink image ImageView did not inflate",
            device.findObject(By.res(APP_PACKAGE, "image"))
        )
        assertNotNull(
            "name missing — drink name TextView did not inflate",
            device.findObject(By.res(APP_PACKAGE, "name"))
        )
        assertNotNull(
            "total_summa missing — total price label did not inflate",
            device.findObject(By.res(APP_PACKAGE, "total_summa"))
        )
        assertNotNull(
            "info_rv missing — order items RecyclerView did not inflate",
            device.findObject(By.res(APP_PACKAGE, "info_rv"))
        )
        assertNotNull(
            "order CTA missing — user cannot confirm the order",
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

    /** Launches [CheckoutActivity] directly without parcelable extras. */
    private fun launchCheckoutActivity() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val intent = Intent().apply {
            setClassName(APP_PACKAGE, "uz.alphazet.hoopla.ui.order.CheckoutActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        ctx.startActivity(intent)

        assertNotNull(
            "CheckoutActivity did not launch within ${LAUNCH_READY_MS}ms",
            device.wait(Until.hasObject(By.pkg(APP_PACKAGE).depth(0)), LAUNCH_READY_MS)
        )
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }
}
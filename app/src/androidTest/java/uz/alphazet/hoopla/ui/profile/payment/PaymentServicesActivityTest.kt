package uz.alphazet.hoopla.ui.profile.payment

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
 * UI Automator tests for [PaymentServicesActivity] — the grid of third-party
 * top-up providers reached from the profile tab (or as a redirect from
 * [SubscriptionActivity] when the backend flags a precondition-required
 * response).
 *
 * Tested flows:
 *  1. Launching PaymentServicesActivity directly via Intent renders the
 *     toolbar and the `subscription_rv` grid (yes — the layout reuses the
 *     `subscription_rv` id for the payment services list).
 *  2. Toolbar back button finishes the activity.
 *
 * Prerequisites:
 *  - Device / emulator has the debug build installed.
 *  - Chrome assertions do not require auth — the layout inflates regardless
 *    of the backend response.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class PaymentServicesActivityTest : BaseUiTest() {

    @Before
    override fun setUp() {
        clearAuthTokens()
        super.setUp()
        launchPaymentServicesActivity()
    }

    // -----------------------------------------------------------------------
    // 1. PaymentServicesActivity renders core elements
    // -----------------------------------------------------------------------

    @Test
    fun payment_services_activity_renders_toolbar_and_grid() {
        assertNotNull(
            "subscription_rv not visible — PaymentServicesActivity did not mount",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "subscription_rv")), LAUNCH_READY_MS)
        )
        assertNotNull(
            "toolbar missing on PaymentServicesActivity",
            device.findObject(By.res(APP_PACKAGE, "toolbar"))
        )
        assertNotNull(
            "swipe_refresh_layout missing — users cannot refresh the provider list",
            device.findObject(By.res(APP_PACKAGE, "swipe_refresh_layout"))
        )
    }

    // -----------------------------------------------------------------------
    // 2. Toolbar back finishes activity
    // -----------------------------------------------------------------------

    @Test
    fun tapping_toolbar_back_finishes_activity() {
        assertNotNull(
            "subscription_rv not ready before back",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "subscription_rv")), LAUNCH_READY_MS)
        )

        val backArrow = device.wait(Until.findObject(By.desc("Orqaga")), TIMEOUT_MS)
        if (backArrow != null) {
            backArrow.click()
        } else {
            device.pressBack()
        }
        device.waitForIdle(IDLE_TIMEOUT_MS)

        val stillVisible = device.wait(
            Until.hasObject(By.res(APP_PACKAGE, "subscription_rv")),
            IDLE_TIMEOUT_MS
        )
        assertFalse(
            "subscription_rv still visible after back — activity did not finish",
            stillVisible
        )
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Launches [PaymentServicesActivity] directly via Intent. */
    private fun launchPaymentServicesActivity() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val intent = Intent().apply {
            setClassName(
                APP_PACKAGE,
                "uz.alphazet.hoopla.ui.profile.payment.PaymentServicesActivity"
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        ctx.startActivity(intent)

        assertNotNull(
            "PaymentServicesActivity did not launch within ${LAUNCH_READY_MS}ms",
            device.wait(Until.hasObject(By.pkg(APP_PACKAGE).depth(0)), LAUNCH_READY_MS)
        )
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }
}
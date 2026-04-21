package uz.alphazet.hoopla.ui.profile.subscriptions

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
 * UI Automator tests for [SubscriptionActivity] — the subscription plans
 * screen reached from ProfileScreen. It fetches `getSubscriptions()` on
 * create; in logged-out state the backend returns 401 and the list stays
 * empty, which keeps the "coming soon" lottie visible.
 *
 * Tested flows:
 *  1. Launching SubscriptionActivity directly via Intent renders the
 *     toolbar and the paged RecyclerView (`subscription_rv`).
 *  2. The SwipeRefreshLayout container is present so users can pull to
 *     refresh.
 *  3. Toolbar back button (`By.desc("Orqaga")`) finishes the activity.
 *
 * Prerequisites:
 *  - Device / emulator has the debug build installed.
 *  - No auth token required for chrome assertions — a logged-out 401 still
 *    inflates the layout.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SubscriptionActivityTest : BaseUiTest() {

    @Before
    override fun setUp() {
        clearAuthTokens()
        super.setUp()
        launchSubscriptionActivity()
    }

    // -----------------------------------------------------------------------
    // 1. SubscriptionActivity renders core elements
    // -----------------------------------------------------------------------

    @Test
    fun subscription_activity_renders_toolbar_and_recycler() {
        assertNotNull(
            "subscription_rv not visible — SubscriptionActivity did not mount",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "subscription_rv")), LAUNCH_READY_MS)
        )
        assertNotNull(
            "toolbar missing on SubscriptionActivity",
            device.findObject(By.res(APP_PACKAGE, "toolbar"))
        )
    }

    // -----------------------------------------------------------------------
    // 2. SwipeRefreshLayout is present
    // -----------------------------------------------------------------------

    @Test
    fun swipe_refresh_layout_is_present() {
        assertNotNull(
            "swipe_refresh_layout missing — users cannot pull-to-refresh the list",
            device.wait(
                Until.findObject(By.res(APP_PACKAGE, "swipe_refresh_layout")),
                LAUNCH_READY_MS
            )
        )
    }

    // -----------------------------------------------------------------------
    // 3. Toolbar back finishes activity
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
            // Fallback for non-UZ locales — press system back instead.
            device.pressBack()
        }
        device.waitForIdle(IDLE_TIMEOUT_MS)

        // Activity finished — subscription_rv should be gone. Use hasObject
        // for a definitive negative assertion.
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

    /** Launches [SubscriptionActivity] directly via Intent. */
    private fun launchSubscriptionActivity() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val intent = Intent().apply {
            setClassName(
                APP_PACKAGE,
                "uz.alphazet.hoopla.ui.profile.subscriptions.SubscriptionActivity"
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        ctx.startActivity(intent)

        assertNotNull(
            "SubscriptionActivity did not launch within ${LAUNCH_READY_MS}ms",
            device.wait(Until.hasObject(By.pkg(APP_PACKAGE).depth(0)), LAUNCH_READY_MS)
        )
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }
}
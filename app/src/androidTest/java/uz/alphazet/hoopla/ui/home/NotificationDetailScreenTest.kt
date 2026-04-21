package uz.alphazet.hoopla.ui.home

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
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
 * UI Automator tests for [NotificationDetailScreen] — the standalone
 * activity that shows a single notification's hero image, title and
 * description. It reads the notification id from the `id` extra and calls
 * `viewModel.getNotificationDetail(id)` in onCreate.
 *
 * Tested flows:
 *  1. Launching the activity directly via Intent with `id = -1` still
 *     inflates the layout — toolbar, image, name and desc are all
 *     present regardless of the backend response.
 *
 * Prerequisites:
 *  - Device / emulator has the debug build installed.
 *  - No auth / valid id required for chrome assertions.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class NotificationDetailScreenTest : BaseUiTest() {

    @Before
    override fun setUp() {
        clearAuthTokens()
        super.setUp()
        launchNotificationDetailScreen(notificationId = -1)
    }

    // -----------------------------------------------------------------------
    // 1. Layout chrome renders
    // -----------------------------------------------------------------------

    @Test
    fun notification_detail_screen_renders_layout_chrome() {
        assertNotNull(
            "toolbar missing on NotificationDetailScreen",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "toolbar")), LAUNCH_READY_MS)
        )
        assertNotNull(
            "image missing — hero ImageView did not inflate",
            device.findObject(By.res(APP_PACKAGE, "image"))
        )
        assertNotNull(
            "name missing — notification title TextView did not inflate",
            device.findObject(By.res(APP_PACKAGE, "name"))
        )
        assertNotNull(
            "desc missing — notification description TextView did not inflate",
            device.findObject(By.res(APP_PACKAGE, "desc"))
        )
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Launches [NotificationDetailScreen] directly with an id extra. */
    private fun launchNotificationDetailScreen(notificationId: Int) {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val intent = Intent().apply {
            setClassName(APP_PACKAGE, "uz.alphazet.hoopla.ui.home.NotificationDetailScreen")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            putExtra("id", notificationId)
        }
        ctx.startActivity(intent)

        assertNotNull(
            "NotificationDetailScreen did not launch within ${LAUNCH_READY_MS}ms",
            device.wait(Until.hasObject(By.pkg(APP_PACKAGE).depth(0)), LAUNCH_READY_MS)
        )
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }
}
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
 * UI Automator tests for [uz.alphazet.hoopla.ui.home.StoryViewerActivity].
 *
 * Tested flows:
 *  1. Tapping a story bubble opens the viewer (pager + progress container visible).
 *  2. The close button dismisses the viewer back to Home.
 *  3. Tapping the right half keeps the viewer open (next-slide intent, never finishes).
 *  4. Tapping the left half on the first slide keeps the viewer open
 *     (prev is a no-op at index 0, must not dismiss).
 *  5. A long-press emits onLongPress (chrome fade + animator pause) but does not
 *     dismiss; release restores chrome.
 *  6. A horizontal swipe drives ViewPager2 paging but never dismisses the viewer.
 *  7. A vertical swipe past the dismiss threshold returns the user to Home.
 *
 * Prerequisites:
 *  - Stories endpoint reachable; rail must contain at least one [story_card].
 *  - Debug build installed on a connected device or emulator.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class StoryViewerActivityTest : BaseUiTest() {

    @Before
    override fun setUp() {
        super.setUp()
        launchApp()

        // Default destination is Home, but tap explicitly so the test is robust
        // against any tab the launcher may have restored.
        waitForId("home").click()
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }

    // -----------------------------------------------------------------------
    // 1. Tapping a story bubble opens the viewer
    // -----------------------------------------------------------------------

    @Test
    fun tapping_story_opens_viewer_with_pager_and_progress() {
        openFirstStory()

        assertNotNull(
            "pager not visible — StoryViewerActivity didn't open",
            device.findObject(By.res(APP_PACKAGE, "pager"))
        )
        assertNotNull(
            "progress_container not visible inside StoryViewerActivity",
            device.findObject(By.res(APP_PACKAGE, "progress_container"))
        )

        closeViewer()
    }

    // -----------------------------------------------------------------------
    // 2. Close button dismisses the viewer
    // -----------------------------------------------------------------------

    @Test
    fun close_button_returns_to_home() {
        openFirstStory()

        waitForId("btn_close").click()
        device.waitForIdle(IDLE_TIMEOUT_MS)

        assertNotNull(
            "logo not visible — close button did not return to Home",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "logo")), TIMEOUT_MS)
        )
    }

    // -----------------------------------------------------------------------
    // 3. Right-tap keeps the viewer visible
    // -----------------------------------------------------------------------

    @Test
    fun right_tap_keeps_viewer_visible() {
        openFirstStory()
        val pager = waitForId("pager")
        val bounds = pager.visibleBounds

        // Tap mid-vertical on the right quarter — the right tap zone.
        device.click(bounds.right - bounds.width() / 4, bounds.centerY())
        device.waitForIdle(IDLE_TIMEOUT_MS)

        assertNotNull(
            "pager disappeared after right-tap — viewer finished unexpectedly",
            device.findObject(By.res(APP_PACKAGE, "pager"))
        )

        closeViewer()
    }

    // -----------------------------------------------------------------------
    // 4. Left-tap on first slide must not dismiss
    // -----------------------------------------------------------------------

    @Test
    fun left_tap_on_first_slide_does_not_dismiss_viewer() {
        openFirstStory()
        val pager = waitForId("pager")
        val bounds = pager.visibleBounds

        device.click(bounds.left + bounds.width() / 4, bounds.centerY())
        device.waitForIdle(IDLE_TIMEOUT_MS)

        assertNotNull(
            "pager disappeared after left-tap on first slide — prev should be a no-op",
            device.findObject(By.res(APP_PACKAGE, "pager"))
        )

        closeViewer()
    }

    // -----------------------------------------------------------------------
    // 5. Long-press pauses + hides chrome but does not dismiss
    // -----------------------------------------------------------------------

    @Test
    fun long_press_does_not_dismiss_viewer() {
        openFirstStory()
        val pager = waitForId("pager")

        // longClick holds for ViewConfiguration.getLongPressTimeout() (~500ms),
        // long enough to trigger GestureDetector.onLongPress and the chrome fade.
        pager.longClick()
        device.waitForIdle(IDLE_TIMEOUT_MS)

        assertNotNull(
            "pager disappeared after long-press — onLongPress should not finish the activity",
            device.findObject(By.res(APP_PACKAGE, "pager"))
        )

        closeViewer()
    }

    // -----------------------------------------------------------------------
    // 6. Horizontal swipe pages within the viewer but does not dismiss
    // -----------------------------------------------------------------------

    @Test
    fun horizontal_swipe_keeps_viewer_visible() {
        openFirstStory()
        val pager = waitForId("pager")
        val bounds = pager.visibleBounds

        // Right → left swipe across the pager; ViewPager2 either pages forward
        // (cube transition) or no-ops if there's only one slide. Either way the
        // viewer must remain.
        device.swipe(
            bounds.right - 100, bounds.centerY(),
            bounds.left + 100, bounds.centerY(),
            30
        )
        device.waitForIdle(IDLE_TIMEOUT_MS)

        assertNotNull(
            "pager disappeared after horizontal swipe — viewer finished unexpectedly",
            device.findObject(By.res(APP_PACKAGE, "pager"))
        )

        closeViewer()
    }

    // -----------------------------------------------------------------------
    // 7. Vertical swipe-down past the dismiss threshold returns to Home
    // -----------------------------------------------------------------------

    @Test
    fun swipe_down_dismisses_viewer() {
        openFirstStory()
        val pager = waitForId("pager")
        val bounds = pager.visibleBounds

        // Pure vertical drag from upper portion to near-bottom — clears the
        // height/4 threshold and triggers the settle-to-finish animation.
        device.swipe(
            bounds.centerX(), bounds.top + bounds.height() / 4,
            bounds.centerX(), bounds.bottom - 80,
            30
        )
        device.waitForIdle(IDLE_TIMEOUT_MS)

        assertNotNull(
            "logo not visible — swipe-down did not dismiss the viewer",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "logo")), TIMEOUT_MS)
        )
    }

    // -----------------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------------

    private fun openFirstStory() {
        val story = device.wait(
            Until.findObject(By.res(APP_PACKAGE, "story_card")),
            LAUNCH_READY_MS
        )
        assertNotNull(
            "No story_card visible — stories rail is empty or stories endpoint is unreachable",
            story
        )
        story!!.click()

        assertNotNull(
            "pager not visible — story tap did not launch StoryViewerActivity",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "pager")), LAUNCH_READY_MS)
        )
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }

    private fun closeViewer() {
        val close = device.findObject(By.res(APP_PACKAGE, "btn_close"))
        if (close != null) {
            close.click()
        } else {
            device.pressBack()
        }
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }
}
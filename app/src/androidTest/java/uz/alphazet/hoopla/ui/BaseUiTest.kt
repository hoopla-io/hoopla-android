package uz.alphazet.hoopla.ui

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Before

/**
 * Base class for all UI Automator tests.
 *
 * Provides:
 *  - [device] — the singleton [UiDevice]
 *  - [launchApp] — presses Home, then launches the app from scratch and waits
 *    for the main window to appear
 *  - [waitForId] / [waitForText] — concise helpers that fail the test with a
 *    clear message when an element is not found within [TIMEOUT_MS]
 *
 * IMPORTANT: These tests require a connected device or running emulator with the
 * app installed.  Run them via:
 *   ./gradlew :app:connectedDebugAndroidTest
 */
abstract class BaseUiTest {

    protected lateinit var device: UiDevice
        private set

    @Before
    open fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Dismiss any system dialogs (e.g. "App crashed") that could block tests.
        device.pressHome()
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }

    protected fun launchApp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val intent = context.packageManager
            .getLaunchIntentForPackage(APP_PACKAGE)!!
            .apply { addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK) }

        context.startActivity(intent)

        // Wait until any window from our package appears.
        val launched = device.wait(Until.hasObject(By.pkg(APP_PACKAGE).depth(0)), TIMEOUT_MS)
        assertNotNull("App did not launch within ${TIMEOUT_MS}ms", launched)
    }

    /** Returns the object or fails the test if it is not visible within [TIMEOUT_MS]. */
    protected fun waitForId(resourceId: String, timeout: Long = TIMEOUT_MS): UiObject2 {
        val selector = By.res(APP_PACKAGE, resourceId)
        val found = device.wait(Until.findObject(selector), timeout)
        assertNotNull("View with id '$resourceId' not found within ${timeout}ms", found)
        return found!!
    }

    /** Returns the object or fails the test if it is not visible within [TIMEOUT_MS]. */
    protected fun waitForText(text: String, timeout: Long = TIMEOUT_MS): UiObject2 {
        val found = device.wait(Until.findObject(By.text(text)), timeout)
        assertNotNull("View with text '$text' not found within ${timeout}ms", found)
        return found!!
    }

    /** Returns the object or fails if text is not found as a substring. */
    protected fun waitForTextContains(substring: String, timeout: Long = TIMEOUT_MS): UiObject2 {
        val found = device.wait(Until.findObject(By.textContains(substring)), timeout)
        assertNotNull("No view containing text '$substring' found within ${timeout}ms", found)
        return found!!
    }

    companion object {
        const val APP_PACKAGE = "uz.alphazet.hoopla"
        const val TIMEOUT_MS = 5_000L
        const val IDLE_TIMEOUT_MS = 1_000L
    }
}
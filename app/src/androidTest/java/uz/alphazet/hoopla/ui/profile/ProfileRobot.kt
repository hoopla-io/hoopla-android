package uz.alphazet.hoopla.ui.profile

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import uz.alphazet.hoopla.ui.BaseUiTest.Companion.APP_PACKAGE
import uz.alphazet.hoopla.ui.BaseUiTest.Companion.IDLE_TIMEOUT_MS
import uz.alphazet.hoopla.ui.BaseUiTest.Companion.TIMEOUT_MS

/**
 * Robot for the Profile screen UI Automator tests.
 *
 * Encapsulates navigation and element assertions so [ProfileScreenTest] can
 * express intent without repeating raw selector boilerplate.
 *
 * Every mutating function returns `this` for chaining:
 *   robot.navigateToProfile().assertLanguagesRowVisible()
 */
class ProfileRobot(private val device: UiDevice) {

    /** Taps the profile bottom-nav tab and waits for the screen to settle. */
    fun navigateToProfile() = apply {
        val tab = device.wait(Until.findObject(By.res(APP_PACKAGE, "profile")), TIMEOUT_MS)
        assertNotNull("Profile tab not found in bottom navigation", tab)
        tab!!.click()
        device.wait(Until.hasObject(By.res(APP_PACKAGE, "swipe_refresh_layout")), TIMEOUT_MS)
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }

    /**
     * Finds header_title by resource ID and returns its text.
     * Fails the test if the view is not present.
     */
    fun readHeaderText(): String {
        val view = device.wait(Until.findObject(By.res(APP_PACKAGE, "header_title")), TIMEOUT_MS)
        assertNotNull("header_title view not found on profile screen", view)
        return view!!.text ?: ""
    }

    /** Asserts [header_title] shows [expected]. */
    fun assertHeaderText(expected: String) = apply {
        assertEquals(
            "header_title text mismatch",
            expected,
            readHeaderText()
        )
    }

    /**
     * If the login button is present (logged-out device), asserts it is enabled.
     * Silently passes on a logged-in device where the button is absent.
     */
    fun assertLoginButtonEnabledIfPresent() = apply {
        val btn = device.wait(Until.findObject(By.res(APP_PACKAGE, "login")), TIMEOUT_MS)
        if (btn != null) assertTrue("Login button must be enabled", btn.isEnabled)
    }

    fun assertLanguagesRowVisible() = apply {
        assertNotNull(
            "Languages settings row not found",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "languages")), TIMEOUT_MS)
        )
    }

    fun assertPrivacyPolicyRowVisible() = apply {
        assertNotNull(
            "Privacy Policy row not found",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "privacyPolicy")), TIMEOUT_MS)
        )
    }

    fun assertOrdersTabVisible() = apply {
        assertNotNull(
            "Orders tab not found",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "orders")), TIMEOUT_MS)
        )
    }
}
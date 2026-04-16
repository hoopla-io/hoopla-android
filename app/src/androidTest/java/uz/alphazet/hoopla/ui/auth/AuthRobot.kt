package uz.alphazet.hoopla.ui.auth

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import uz.alphazet.hoopla.ui.BaseUiTest.Companion.APP_PACKAGE
import uz.alphazet.hoopla.ui.BaseUiTest.Companion.IDLE_TIMEOUT_MS
import uz.alphazet.hoopla.ui.BaseUiTest.Companion.TIMEOUT_MS

/**
 * Robot for the authentication screen (phone input + Continue button).
 *
 * Encapsulates all UiDevice interactions so [AuthFlowTest] can express intent
 * without repeating raw selector boilerplate across tests.
 *
 * Every mutating function returns `this` for chaining:
 *   robot.typePhone("901234567").assertContinueEnabled()
 */
class AuthRobot(private val device: UiDevice) {

    /** Taps the phone input and types [digits] (9-digit local part). */
    fun typePhone(digits: String) = apply {
        val input = device.wait(Until.findObject(By.res(APP_PACKAGE, "inputPhone")), TIMEOUT_MS)
        assertNotNull("inputPhone not found", input)
        input!!.click()
        device.waitForIdle(IDLE_TIMEOUT_MS)
        input.text = digits
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }

    /** Clears the phone input field. */
    fun clearPhone() = apply {
        val input = device.wait(Until.findObject(By.res(APP_PACKAGE, "inputPhone")), TIMEOUT_MS)
        assertNotNull("inputPhone not found", input)
        input!!.clear()
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }

    /** Asserts the "hoopla" title and phone input are both visible. */
    fun assertAuthScreenVisible() = apply {
        assertNotNull(
            "Title 'hoopla' not visible on auth screen",
            device.wait(Until.findObject(By.text("hoopla")), TIMEOUT_MS)
        )
        assertNotNull(
            "Phone input not visible on auth screen",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "inputPhone")), TIMEOUT_MS)
        )
    }

    /** Asserts the Continue (btSend) button is enabled. */
    fun assertContinueEnabled() = apply {
        val btn = device.wait(Until.findObject(By.res(APP_PACKAGE, "btSend")), TIMEOUT_MS)
        assertNotNull("btSend not found", btn)
        assertTrue("Continue button must be enabled", btn!!.isEnabled)
    }

    /** Asserts the Continue (btSend) button is disabled. */
    fun assertContinueDisabled() = apply {
        val btn = device.wait(Until.findObject(By.res(APP_PACKAGE, "btSend")), TIMEOUT_MS)
        assertNotNull("btSend not found", btn)
        assertFalse("Continue button must be disabled", btn!!.isEnabled)
    }
}
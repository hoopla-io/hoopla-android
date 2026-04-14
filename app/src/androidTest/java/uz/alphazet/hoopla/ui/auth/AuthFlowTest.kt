package uz.alphazet.hoopla.ui.auth

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
 * UI Automator tests for the authentication flow ([AuthActivity]).
 *
 * Launches [AuthActivity] directly so the tests are isolated from
 * [uz.alphazet.hoopla.ui.MainActivity].
 *
 * Tested flows:
 *  1. Auth screen is visible — title "hoopla" and phone input are present.
 *  2. Continue button is disabled when no phone number is entered.
 *  3. Typing a valid 9-digit suffix enables the Continue button.
 *  4. Clearing the phone input disables the Continue button again.
 *  5. Typing a short (incomplete) number keeps Continue disabled.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class AuthFlowTest : BaseUiTest() {

    @Before
    override fun setUp() {
        super.setUp()
        launchAuthActivity()
    }

    private fun launchAuthActivity() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val intent = Intent(context, AuthActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)
        device.wait(Until.hasObject(By.pkg(APP_PACKAGE).depth(0)), TIMEOUT_MS)
    }

    // -----------------------------------------------------------------------
    // 1. Auth screen elements are visible
    // -----------------------------------------------------------------------

    @Test
    fun auth_screen_shows_title_and_phone_input() {
        // Title label "hoopla"
        val title = device.wait(Until.findObject(By.text("hoopla")), TIMEOUT_MS)
        assertNotNull("Title 'hoopla' not visible on auth screen", title)

        // Phone input field
        val phoneInput = device.wait(Until.findObject(By.res(APP_PACKAGE, "inputPhone")), TIMEOUT_MS)
        assertNotNull("Phone input not visible on auth screen", phoneInput)
    }

    // -----------------------------------------------------------------------
    // 2. Continue button is disabled initially
    // -----------------------------------------------------------------------

    @Test
    fun continue_button_is_disabled_before_phone_entry() {
        val continueBtn = waitForId("btSend")
        assertFalse("Continue button must be disabled before entering a phone number",
            continueBtn.isEnabled)
    }

    // -----------------------------------------------------------------------
    // 3. Valid phone number enables Continue
    // -----------------------------------------------------------------------

    @Test
    fun typing_valid_phone_enables_continue_button() {
        val phoneInput = waitForId("inputPhone")
        // The mask prepends "998 "; we type the 9-digit local part.
        // MaskedEditText accepts raw digit input.
        phoneInput.click()
        device.waitForIdle(IDLE_TIMEOUT_MS)
        phoneInput.text = "901234567"
        device.waitForIdle(IDLE_TIMEOUT_MS)

        val continueBtn = waitForId("btSend")
        // After entering 9 digits the button should be enabled
        assertNotNull("Continue button not found after phone input", continueBtn)
        // Note: MaskedEditText triggers doOnTextChanged which enables/disables the button.
        // If the button is still disabled here it means the mask did not accept the text —
        // that is a product bug, not a test bug.
        assert(continueBtn.isEnabled) {
            "Continue button should be enabled after entering a valid 9-digit phone suffix"
        }
    }

    // -----------------------------------------------------------------------
    // 4. Clearing the phone disables Continue again
    // -----------------------------------------------------------------------

    @Test
    fun clearing_phone_disables_continue_button() {
        val phoneInput = waitForId("inputPhone")
        phoneInput.click()
        phoneInput.text = "901234567"
        device.waitForIdle(IDLE_TIMEOUT_MS)

        // Clear the field
        phoneInput.clear()
        device.waitForIdle(IDLE_TIMEOUT_MS)

        val continueBtn = waitForId("btSend")
        assertFalse("Continue button must be disabled after clearing phone input",
            continueBtn.isEnabled)
    }

    // -----------------------------------------------------------------------
    // 5. Incomplete phone number keeps Continue disabled
    // -----------------------------------------------------------------------

    @Test
    fun incomplete_phone_number_keeps_continue_button_disabled() {
        val phoneInput = waitForId("inputPhone")
        phoneInput.click()
        // Only 4 digits — too short
        phoneInput.text = "9012"
        device.waitForIdle(IDLE_TIMEOUT_MS)

        val continueBtn = waitForId("btSend")
        assertFalse("Continue button must remain disabled for a partial phone number",
            continueBtn.isEnabled)
    }
}
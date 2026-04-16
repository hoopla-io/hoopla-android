package uz.alphazet.hoopla.ui.auth

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
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
 * Each test delegates UI interactions to [AuthRobot], keeping the test body
 * focused on intent rather than selector boilerplate.
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

    private val robot by lazy { AuthRobot(device) }

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
        // Wait for the phone input to be fully drawn, not just the package window —
        // ensures all subsequent tests can interact with inputPhone immediately.
        device.wait(Until.hasObject(By.res(APP_PACKAGE, "inputPhone")), TIMEOUT_MS)
    }

    // -----------------------------------------------------------------------
    // 1. Auth screen elements are visible
    // -----------------------------------------------------------------------

    @Test
    fun auth_screen_shows_title_and_phone_input() {
        robot.assertAuthScreenVisible()
    }

    // -----------------------------------------------------------------------
    // 2. Continue button is disabled initially
    // -----------------------------------------------------------------------

    @Test
    fun continue_button_is_disabled_before_phone_entry() {
        robot.assertContinueDisabled()
    }

    // -----------------------------------------------------------------------
    // 3. Valid phone number enables Continue
    // -----------------------------------------------------------------------

    @Test
    fun typing_valid_phone_enables_continue_button() {
        // The mask prepends "+998 "; we type the 9-digit local part.
        // Note: MaskedEditText triggers doOnTextChanged which enables/disables
        // the button. If still disabled, the mask did not accept the text —
        // that is a product bug, not a test bug.
        robot.typePhone("901234567").assertContinueEnabled()
    }

    // -----------------------------------------------------------------------
    // 4. Clearing the phone disables Continue again
    // -----------------------------------------------------------------------

    @Test
    fun clearing_phone_disables_continue_button() {
        robot.typePhone("901234567").clearPhone().assertContinueDisabled()
    }

    // -----------------------------------------------------------------------
    // 5. Incomplete phone number keeps Continue disabled
    // -----------------------------------------------------------------------

    @Test
    fun incomplete_phone_number_keeps_continue_button_disabled() {
        robot.typePhone("9012").assertContinueDisabled()
    }
}
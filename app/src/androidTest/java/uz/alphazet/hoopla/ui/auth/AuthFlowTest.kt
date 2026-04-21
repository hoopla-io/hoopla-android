package uz.alphazet.hoopla.ui.auth

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uz.alphazet.hoopla.ui.BaseUiTest

/**
 * UI Automator tests for the auth flow — [AuthActivity] hosting
 * [AuthScreen] (phone entry) and [ConfirmPhoneNumberScreen] (5-digit SMS
 * code entry).
 *
 * Test phone number: **+998 90 047-24-00** (raw digits `900472400`).
 * Test SMS code:     **`12345`** — any 5-digit value; the backend is
 * expected to reject it, which lets us assert the error branch.
 *
 * Tested flows:
 *  1. [AuthActivity] launches and shows the phone-input screen.
 *  2. `btSend` is disabled until the full 9-digit local part has been typed,
 *     then becomes enabled.
 *  3. Typing the full number and tapping `btSend` navigates to
 *     [ConfirmPhoneNumberScreen] — the PIN-view appears.
 *  4. The confirm screen shows the formatted phone in its header.
 *  5. Entering an invalid 5-digit code triggers the confirm request and
 *     surfaces `errorTextCode`.
 *  6. Pressing back on the confirm screen returns to the phone-input screen.
 *
 * Prerequisites:
 *  - Device / emulator has the debug build installed.
 *  - Network is reachable — flows 3–6 depend on the backend responding to
 *    `sendSms` / `confirmSms`.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class AuthFlowTest : BaseUiTest() {

    @Before
    override fun setUp() {
        clearAuthTokens()
        super.setUp()
        launchAuthActivity()
    }

    // -----------------------------------------------------------------------
    // 1. AuthActivity launches
    // -----------------------------------------------------------------------

    @Test
    fun auth_activity_launches_with_phone_input_screen() {
        assertNotNull(
            "inputPhone not visible — AuthActivity did not mount AuthScreen",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "inputPhone")), TIMEOUT_MS)
        )
        assertNotNull(
            "btSend not visible on phone-input screen",
            device.findObject(By.res(APP_PACKAGE, "btSend"))
        )
    }

    // -----------------------------------------------------------------------
    // 2. btSend is disabled until the full phone number is entered
    // -----------------------------------------------------------------------

    @Test
    fun btSend_is_disabled_until_full_phone_number_entered() {
        val inputPhone = waitForId("inputPhone")
        val btSend = waitForId("btSend")

        // Initially disabled (empty phone).
        assertTrue("btSend should be disabled with an empty phone", !btSend.isEnabled)

        inputPhone.click()
        device.waitForIdle(IDLE_TIMEOUT_MS)

        // Partial input — still disabled.
        typeText("9004")
        device.waitForIdle(IDLE_TIMEOUT_MS)
        assertTrue("btSend should remain disabled for a partial phone number", !btSend.isEnabled)

        // Complete the number — must become enabled.
        typeText("72400")
        device.waitForIdle(IDLE_TIMEOUT_MS)
        assertTrue(
            "btSend must be enabled after 9 digits have been entered",
            btSend.isEnabled
        )
    }

    // -----------------------------------------------------------------------
    // 3. Full phone + tap send → navigates to confirm screen
    // -----------------------------------------------------------------------

    @Test
    fun entering_phone_and_tapping_send_navigates_to_confirm_screen() {
        typePhoneAndTapSend()

        // PinView on the confirm screen proves navigation succeeded.
        assertNotNull(
            "inputCode (PinView) not found — did not navigate to ConfirmPhoneNumberScreen",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "inputCode")), LAUNCH_READY_MS)
        )
        assertNotNull(
            "btConfirm not visible on confirm screen",
            device.findObject(By.res(APP_PACKAGE, "btConfirm"))
        )
    }

    // -----------------------------------------------------------------------
    // 4. Confirm screen displays the formatted phone number
    // -----------------------------------------------------------------------

    @Test
    fun confirm_screen_shows_formatted_phone_number() {
        typePhoneAndTapSend()

        val phoneLabel = device.wait(
            Until.findObject(By.res(APP_PACKAGE, "phoneNumber")),
            LAUNCH_READY_MS
        )
        assertNotNull("phoneNumber header not found on confirm screen", phoneLabel)

        val text = phoneLabel!!.text.orEmpty()
        assertTrue(
            "phoneNumber header must contain the dialled number, was: '$text'",
            text.contains("900") && text.contains("47")
        )
    }

    // -----------------------------------------------------------------------
    // 5. Invalid 5-digit code → errorTextCode becomes visible
    // -----------------------------------------------------------------------

    @Test
    fun entering_invalid_sms_code_shows_error_text() {
        typePhoneAndTapSend()

        val inputCode = device.wait(
            Until.findObject(By.res(APP_PACKAGE, "inputCode")),
            LAUNCH_READY_MS
        )
        assertNotNull("inputCode not ready for input", inputCode)
        inputCode!!.click()
        device.waitForIdle(IDLE_TIMEOUT_MS)

        // Any 5-digit value — the backend is expected to reject it. Entering
        // the 5th digit auto-triggers btConfirm via `doAfterTextChanged`.
        typeText("12345")

        val error = device.wait(
            Until.findObject(By.res(APP_PACKAGE, "errorTextCode")),
            LAUNCH_READY_MS
        )
        assertNotNull(
            "errorTextCode must become visible after an invalid confirmation code",
            error
        )
    }

    // -----------------------------------------------------------------------
    // 6. Back navigation from confirm screen returns to phone input
    // -----------------------------------------------------------------------

    @Test
    fun back_from_confirm_screen_returns_to_phone_input() {
        typePhoneAndTapSend()

        // Ensure we actually landed on the confirm screen first.
        assertNotNull(
            "Did not reach confirm screen before back navigation",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "inputCode")), LAUNCH_READY_MS)
        )

        // Use the in-screen back button (backImg) — this calls
        // requireActivity().onBackPressed() which pops the confirm fragment.
        waitForId("backImg").click()
        device.waitForIdle(IDLE_TIMEOUT_MS)

        assertNotNull(
            "inputPhone not visible after pressing back on confirm screen",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "inputPhone")), TIMEOUT_MS)
        )
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Launches [AuthActivity] directly so tests don't depend on the profile tab. */
    private fun launchAuthActivity() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val intent = Intent().apply {
            setClassName(APP_PACKAGE, "uz.alphazet.hoopla.ui.auth.AuthActivity")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        ctx.startActivity(intent)

        assertNotNull(
            "AuthActivity did not launch within ${LAUNCH_READY_MS}ms",
            device.wait(Until.hasObject(By.pkg(APP_PACKAGE).depth(0)), LAUNCH_READY_MS)
        )
        // Wait for the phone input field to be fully rendered.
        assertNotNull(
            "inputPhone not ready after AuthActivity launch",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "inputPhone")), LAUNCH_READY_MS)
        )
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }

    /**
     * Uses `adb shell input text` so MaskedEditText / PinView receive real
     * per-character keystrokes — this keeps their TextWatchers in sync with
     * the mask and the 5-digit auto-submit respectively.
     */
    private fun typeText(text: String) {
        device.executeShellCommand("input text $text")
    }

    /** Types the test phone number and presses the send CTA. */
    private fun typePhoneAndTapSend() {
        val inputPhone = waitForId("inputPhone")
        inputPhone.click()
        device.waitForIdle(IDLE_TIMEOUT_MS)

        // Raw digits of +998 90 047-24-00.
        typeText("900472400")
        device.waitForIdle(IDLE_TIMEOUT_MS)

        val btSend = waitForId("btSend")
        assertTrue(
            "btSend did not become enabled — phone input may not be registering",
            btSend.isEnabled
        )
        btSend.click()
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }
}
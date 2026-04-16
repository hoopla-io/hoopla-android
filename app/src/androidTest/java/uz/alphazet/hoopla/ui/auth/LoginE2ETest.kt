package uz.alphazet.hoopla.ui.auth

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uz.alphazet.hoopla.ui.BaseUiTest

/**
 * End-to-end login flow using a real test account.
 *
 * Phone  : +998 90 047-24-00  (typed as raw digits: 900472400)
 * OTP    : any 5-digit code delivered via SMS — entered manually below.
 *
 * Flow:
 *   1. Launch the app.
 *   2. Navigate to the Profile tab → tap "Войти" (Login).
 *      OR launch AuthActivity directly from the bottom-nav guard.
 *   3. Type the phone number into the masked EditText.
 *   4. Tap "Продолжить" (Continue) to request the SMS.
 *   5. Wait for the OTP screen (PinView with 5 boxes).
 *   6. Type the 5-digit OTP code — PinView auto-submits on the 5th digit.
 *   7. Assert that the app returns to MainActivity and the bottom nav is visible,
 *      which confirms a successful login (AuthActivity finishes on success).
 *
 * IMPORTANT:
 *  - The device must have a real internet connection so the SMS API call succeeds.
 *  - You must enter the received SMS code in [OTP_CODE] before running.
 *    Alternatively run the test without step 6 to stop at the OTP screen and
 *    enter the code manually — the test will wait up to [OTP_WAIT_MS].
 *  - If the account is already logged in, the test skips the auth flow and
 *    asserts the logged-in state directly.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class LoginE2ETest : BaseUiTest() {

    companion object {
        /** Raw 9-digit suffix for +998 90 047-24-00 */
        private const val PHONE_DIGITS = "900472400"

        /**
         * Replace with the actual 5-digit OTP received on the device's SIM.
         * The PinView auto-submits when all 5 digits are entered so no button
         * tap is needed after typing the code.
         *
         * When this value is the placeholder "12345", [full_login_flow_with_real_phone_and_sms_code]
         * is automatically skipped via [assumeTrue] so CI does not fail on an invalid code.
         */
        private const val OTP_CODE = "12345"

        /** Extra time to wait for the OTP screen to appear after the SMS request. */
        private const val OTP_SCREEN_WAIT_MS = 10_000L

        /**
         * Time budget for the login success transition.
         * The server call + token storage can take a few seconds.
         */
        private const val LOGIN_SUCCESS_WAIT_MS = 10_000L
    }

    @Before
    override fun setUp() {
        // Clear session tokens so every test starts in the logged-out state.
        // Keys match the property names used by PrefDelegates.stringNullable() in AppCacheImpl.
        InstrumentationRegistry.getInstrumentation().targetContext
            .getSharedPreferences("app_cache", Context.MODE_PRIVATE)
            .edit()
            .remove("accessToken")
            .remove("refreshToken")
            .commit()

        super.setUp()
        launchApp()
    }

    // -----------------------------------------------------------------------
    // Full login flow
    // -----------------------------------------------------------------------

    @Test
    fun full_login_flow_with_real_phone_and_sms_code() {
        // Skip automatically when OTP_CODE is still the placeholder — prevents CI
        // failures on every run where no real SMS code has been supplied.
        assumeTrue(
            "Skipped: OTP_CODE is still the placeholder '12345' — replace it with a real received code to run this test",
            OTP_CODE != "12345"
        )

        // ── Step 1: open the auth screen ──────────────────────────────────
        openAuthScreen()

        // ── Step 2: enter the phone number ───────────────────────────────
        val phoneInput = waitForId("inputPhone")
        phoneInput.click()
        device.waitForIdle(IDLE_TIMEOUT_MS)

        // MaskedEditText mask: +998 (##) ###-##-##
        // Type only the 9-digit local part; the mask and country prefix are fixed.
        phoneInput.text = PHONE_DIGITS
        device.waitForIdle(IDLE_TIMEOUT_MS)

        // ── Step 3: tap Continue ──────────────────────────────────────────
        val continueBtn = waitForId("btSend")
        assertTrue("Continue button must be enabled after entering phone",
            continueBtn.isEnabled)
        continueBtn.click()

        // ── Step 4: wait for the OTP screen ──────────────────────────────
        val otpInput = device.wait(
            Until.findObject(By.res(APP_PACKAGE, "inputCode")),
            OTP_SCREEN_WAIT_MS
        )
        assertNotNull(
            "OTP screen (inputCode) did not appear within ${OTP_SCREEN_WAIT_MS}ms — " +
                    "check that the SMS request succeeded and the network is available",
            otpInput
        )

        // ── Step 5: enter the OTP code ────────────────────────────────────
        // PinView (com.chaos.view.PinView) accepts direct text input.
        // doAfterTextChanged fires on every character; the screen auto-confirms
        // when all 5 digits are present (see ConfirmPhoneNumberScreen.initialize).
        otpInput.click()
        device.waitForIdle(IDLE_TIMEOUT_MS)
        otpInput.text = OTP_CODE
        device.waitForIdle(IDLE_TIMEOUT_MS)

        // ── Step 6: wait for successful login ────────────────────────────
        // On success, AuthActivity calls finish() and MainActivity is in the
        // foreground.  The bottom navigation is the definitive sign of success.
        val bottomNav = device.wait(
            Until.findObject(By.res(APP_PACKAGE, "bottom_nav")),
            LOGIN_SUCCESS_WAIT_MS
        )
        assertNotNull(
            "Bottom navigation not found after login — either the OTP was wrong " +
                    "or the server did not return a token within ${LOGIN_SUCCESS_WAIT_MS}ms",
            bottomNav
        )
    }

    // -----------------------------------------------------------------------
    // Individual step tests (useful for debugging the flow in isolation)
    // -----------------------------------------------------------------------

    @Test
    fun phone_input_accepts_number_and_enables_continue() {
        openAuthScreen()

        val phoneInput = waitForId("inputPhone")
        phoneInput.click()
        phoneInput.text = PHONE_DIGITS
        device.waitForIdle(IDLE_TIMEOUT_MS)

        val continueBtn = waitForId("btSend")
        assertTrue(
            "Continue button must be enabled after entering '$PHONE_DIGITS'",
            continueBtn.isEnabled
        )
    }

    @Test
    fun sms_request_leads_to_otp_screen() {
        openAuthScreen()

        val phoneInput = waitForId("inputPhone")
        phoneInput.click()
        phoneInput.text = PHONE_DIGITS
        device.waitForIdle(IDLE_TIMEOUT_MS)
        waitForId("btSend").click()

        val otpScreen = device.wait(
            Until.findObject(By.res(APP_PACKAGE, "inputCode")),
            OTP_SCREEN_WAIT_MS
        )
        assertNotNull(
            "OTP screen did not appear after tapping Continue — " +
                    "verify network connectivity and that the phone is reachable",
            otpScreen
        )

        // The phone number should be displayed on the confirmation screen
        val phoneLabel = device.wait(
            Until.findObject(By.res(APP_PACKAGE, "phoneNumber")),
            TIMEOUT_MS
        )
        assertNotNull("Phone number label not found on OTP screen", phoneLabel)
    }

    @Test
    fun back_button_on_otp_screen_returns_to_phone_input() {
        openAuthScreen()

        val phoneInput = waitForId("inputPhone")
        phoneInput.click()
        phoneInput.text = PHONE_DIGITS
        device.waitForIdle(IDLE_TIMEOUT_MS)
        waitForId("btSend").click()

        // Wait for OTP screen — must appear or the SMS request failed.
        val otpInput = device.wait(
            Until.findObject(By.res(APP_PACKAGE, "inputCode")),
            OTP_SCREEN_WAIT_MS
        )
        assertNotNull(
            "OTP screen did not appear within ${OTP_SCREEN_WAIT_MS}ms — " +
                    "check network connectivity and SMS rate limits",
            otpInput
        )

        // Tap the back arrow
        val backBtn = device.wait(Until.findObject(By.res(APP_PACKAGE, "backImg")), TIMEOUT_MS)
        assertNotNull("Back button not found on OTP screen", backBtn)
        backBtn!!.click()
        device.waitForIdle(IDLE_TIMEOUT_MS)

        // Should return to the phone input screen
        val phoneInputAgain = device.wait(
            Until.findObject(By.res(APP_PACKAGE, "inputPhone")),
            TIMEOUT_MS
        )
        assertNotNull("Phone input screen did not reappear after pressing back", phoneInputAgain)
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    /**
     * Navigates to AuthActivity from wherever the app currently is.
     *
     * Strategy:
     *  1. If [inputPhone] is already on screen (AuthActivity already open) → done.
     *  2. If the bottom nav is visible → tap Profile → tap "Войти".
     *  3. If none of the above → press back until we find a known anchor.
     */
    private fun openAuthScreen() {
        // Already on the auth screen?
        val alreadyOnAuth = device.findObject(By.res(APP_PACKAGE, "inputPhone"))
        if (alreadyOnAuth != null) return

        // Navigate via Profile → Login button
        val profileTab = device.wait(Until.findObject(By.res(APP_PACKAGE, "profile")), TIMEOUT_MS)
        if (profileTab != null) {
            profileTab.click()
            device.waitForIdle(IDLE_TIMEOUT_MS)

            val loginBtn = device.wait(Until.findObject(By.res(APP_PACKAGE, "login")), TIMEOUT_MS)
            if (loginBtn != null) {
                loginBtn.click()
                // Wait for AuthActivity to open
                device.wait(Until.findObject(By.res(APP_PACKAGE, "inputPhone")), TIMEOUT_MS)
                return
            }
        }

        // Fallback: the orders tab triggers the sign-in dialog (logged-out guard).
        // The "Sign in" button text varies by language; try all three supported values.
        val ordersTab = device.findObject(By.res(APP_PACKAGE, "orders"))
        ordersTab?.click()
        device.waitForIdle(IDLE_TIMEOUT_MS)
        val signInDialogBtn = listOf("Войти", "Sign In", "Kirish")
            .mapNotNull { text -> device.findObject(By.text(text)) }
            .firstOrNull()
            ?: device.wait(Until.findObject(By.res(APP_PACKAGE, "btSignIn")), TIMEOUT_MS)
        signInDialogBtn?.click()
        device.wait(Until.findObject(By.res(APP_PACKAGE, "inputPhone")), TIMEOUT_MS)
    }
}
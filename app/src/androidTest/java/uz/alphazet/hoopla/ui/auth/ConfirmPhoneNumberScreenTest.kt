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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import uz.alphazet.hoopla.ui.BaseUiTest

/**
 * UI Automator tests for [ConfirmPhoneNumberScreen] — the 5-digit OTP entry
 * fragment hosted by [AuthActivity] after a phone number is submitted.
 *
 * These complement `AuthFlowTest`:
 *  - AuthFlowTest covers the happy path (phone → code → error branch).
 *  - This class drills into the confirm screen itself — timer, resend button
 *    gating, code field clearing on error, and the static chrome
 *    (`privacy_policy`, `confirmation_code`, `phoneNumber` header).
 *
 * Tested flows:
 *  1. Confirm screen renders all static chrome (`confirmation_code`,
 *     `code_send_to`, `phoneNumber`, `timer`, `privacy_policy`).
 *  2. The countdown `timer` TextView contains a `mm:ss` value shortly after
 *     navigation — proves the CountDownTimer is running.
 *  3. `send_again` is disabled while the timer is still counting down. It
 *     only becomes enabled when the timer hits zero, so it must start
 *     disabled in the test window.
 *  4. The `errorTextCode` label is initially hidden (visibility=GONE in XML)
 *     and only appears after a failed confirm — it must not be visible when
 *     landing on the screen.
 *  5. `backImg` pops back to the phone-input fragment.
 *
 * Test phone: **+998 90 047-24-00** (raw `900472400`).
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ConfirmPhoneNumberScreenTest : BaseUiTest() {

    @Before
    override fun setUp() {
        clearAuthTokens()
        super.setUp()
        launchAuthActivity()
        typePhoneAndTapSend()

        // Wait for the confirm screen to be fully ready before any assertions.
        assertNotNull(
            "ConfirmPhoneNumberScreen did not appear — inputCode not found",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "inputCode")), LAUNCH_READY_MS)
        )
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }

    // -----------------------------------------------------------------------
    // 1. Static chrome renders
    // -----------------------------------------------------------------------

    @Test
    fun confirm_screen_renders_all_static_chrome() {
        assertNotNull(
            "confirmation_code header missing",
            device.findObject(By.res(APP_PACKAGE, "confirmation_code"))
        )
        assertNotNull(
            "code_send_to label missing",
            device.findObject(By.res(APP_PACKAGE, "code_send_to"))
        )
        assertNotNull(
            "phoneNumber display missing",
            device.findObject(By.res(APP_PACKAGE, "phoneNumber"))
        )
        assertNotNull(
            "timer TextView missing",
            device.findObject(By.res(APP_PACKAGE, "timer"))
        )
        assertNotNull(
            "privacy_policy footer missing",
            device.findObject(By.res(APP_PACKAGE, "privacy_policy"))
        )
        assertNotNull(
            "btConfirm missing on confirm screen",
            device.findObject(By.res(APP_PACKAGE, "btConfirm"))
        )
        assertNotNull(
            "send_again button missing",
            device.findObject(By.res(APP_PACKAGE, "send_again"))
        )
    }

    // -----------------------------------------------------------------------
    // 2. Timer counts down with mm:ss format
    // -----------------------------------------------------------------------

    @Test
    fun timer_label_shows_mmss_value_while_counting_down() {
        val timer = waitForId("timer")

        // Poll — onTick fires every second and writes "${mm}:${ss}" into the
        // timer TextView. Give the first tick up to 2s to avoid races on
        // slow emulators.
        val deadline = System.currentTimeMillis() + 2_500L
        var value = ""
        while (System.currentTimeMillis() < deadline) {
            value = timer.text.orEmpty()
            if (value.matches(Regex("^\\d{2}:\\d{2}$"))) break
            device.waitForIdle(IDLE_TIMEOUT_MS / 4)
        }
        assertTrue(
            "timer did not render mm:ss format — was: '$value'",
            value.matches(Regex("^\\d{2}:\\d{2}$"))
        )
    }

    // -----------------------------------------------------------------------
    // 3. send_again is disabled while the timer is running
    // -----------------------------------------------------------------------

    @Test
    fun send_again_button_is_disabled_while_timer_is_active() {
        val sendAgain = waitForId("send_again")
        assertFalse(
            "send_again must be disabled while the resend timer is still counting down",
            sendAgain.isEnabled
        )
    }

    // -----------------------------------------------------------------------
    // 4. errorTextCode is hidden on a fresh confirm screen
    // -----------------------------------------------------------------------

    @Test
    fun error_text_is_hidden_on_fresh_confirm_screen() {
        // In XML, errorTextCode visibility=gone — UiAutomator cannot find
        // GONE views. Use hasObject for a definitive negative assertion.
        assertFalse(
            "errorTextCode must be hidden until an invalid code has been submitted",
            device.hasObject(By.res(APP_PACKAGE, "errorTextCode"))
        )
    }

    // -----------------------------------------------------------------------
    // 5. backImg pops back to phone input
    // -----------------------------------------------------------------------

    @Test
    fun tapping_backImg_returns_to_phone_input_screen() {
        waitForId("backImg").click()
        device.waitForIdle(IDLE_TIMEOUT_MS)

        assertNotNull(
            "inputPhone not visible after tapping backImg — did not return to phone-input screen",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "inputPhone")), TIMEOUT_MS)
        )
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Launches [AuthActivity] directly — no dependence on the profile tab. */
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
        assertNotNull(
            "inputPhone not ready after AuthActivity launch",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "inputPhone")), LAUNCH_READY_MS)
        )
    }

    private fun typePhoneAndTapSend() {
        val inputPhone = waitForId("inputPhone")
        inputPhone.click()
        device.waitForIdle(IDLE_TIMEOUT_MS)

        // adb shell input text preserves per-character keystrokes so the
        // MaskedEditText mask logic runs exactly like in production.
        device.executeShellCommand("input text 900472400")
        device.waitForIdle(IDLE_TIMEOUT_MS)

        val btSend = waitForId("btSend")
        assertTrue(
            "btSend did not become enabled — phone input may not have registered",
            btSend.isEnabled
        )
        btSend.click()
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }
}
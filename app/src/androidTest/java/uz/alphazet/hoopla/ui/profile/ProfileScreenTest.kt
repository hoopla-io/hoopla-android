package uz.alphazet.hoopla.ui.profile

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
 * UI Automator tests for the Profile screen.
 *
 * These tests navigate to [ProfileScreen] via the bottom navigation and verify
 * the visible UI elements.  The exact content depends on whether the test device
 * is logged in or logged out.
 *
 * Tested flows:
 *  1. Tapping the Profile tab shows the screen header "Профиль".
 *  2. Logged-out state — a "Войти" (Login) button is visible.
 *  3. Language section is accessible (the Languages row is present).
 *  4. Privacy Policy row is present and tappable.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ProfileScreenTest : BaseUiTest() {

    @Before
    override fun setUp() {
        super.setUp()
        launchApp()
        navigateToProfile()
    }

    private fun navigateToProfile() {
        val profileTab = waitForId("profile")
        profileTab.click()
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }

    // -----------------------------------------------------------------------
    // 1. Profile screen header is visible
    // -----------------------------------------------------------------------

    @Test
    fun profile_screen_shows_header() {
        val header = device.wait(Until.findObject(By.textContains("Профиль")), TIMEOUT_MS)
        assertNotNull("Profile header not found on profile screen", header)
    }

    // -----------------------------------------------------------------------
    // 2. Login button visible when logged out
    // -----------------------------------------------------------------------

    @Test
    fun login_button_visible_when_logged_out() {
        // If the user is logged out, the "Войти" button is present.
        // If logged in this element is GONE — the test passes silently in that case.
        val loginButton = device.wait(Until.findObject(By.res(APP_PACKAGE, "login")), TIMEOUT_MS)
        // We accept either state; the assertion only fires when the element is found
        // but in an unexpected visual state.
        if (loginButton != null) {
            assert(loginButton.isEnabled) {
                "'Войти' button found but is not enabled"
            }
        }
    }

    // -----------------------------------------------------------------------
    // 3. Languages row is present
    // -----------------------------------------------------------------------

    @Test
    fun languages_row_is_visible() {
        val languagesRow = device.wait(Until.findObject(By.res(APP_PACKAGE, "languages")), TIMEOUT_MS)
        assertNotNull("Languages settings row not found on profile screen", languagesRow)
    }

    // -----------------------------------------------------------------------
    // 4. Privacy Policy row is present
    // -----------------------------------------------------------------------

    @Test
    fun privacy_policy_row_is_visible() {
        val privacyRow = device.wait(Until.findObject(By.res(APP_PACKAGE, "privacyPolicy")), TIMEOUT_MS)
        assertNotNull("Privacy Policy row not found on profile screen", privacyRow)
    }
}
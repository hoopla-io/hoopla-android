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
 * UI Automator tests for [uz.alphazet.hoopla.ui.profile.ProfileScreen].
 *
 * Tested flows (all in the **logged-out** state — [setUp] calls
 * [clearAuthTokens] so the profile screen's first `getUser()` response is a
 * 401 that triggers `onUnauthorizedException`):
 *
 *  1. Tapping the Profile tab reveals the profile header.
 *  2. The [SwipeRefreshLayout] container is rendered.
 *  3. After the 401 settles, the unauth "login" CTA is visible — the
 *     top-bar "logout" button is hidden.
 *  4. Tapping the "languages" row opens the select-language bottom sheet
 *     (asserted by the three language rows — uzbek / russian / english).
 *  5. Tapping "login" launches [uz.alphazet.hoopla.ui.auth.AuthActivity]
 *     (asserted by the phone-input screen's send button).
 *
 * Prerequisites:
 *  - Device / emulator has the debug build installed.
 *  - Network is reachable — flows 3 and 5 depend on the backend returning
 *    401 for the no-token request.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ProfileScreenTest : BaseUiTest() {

    @Before
    override fun setUp() {
        // Guarantee logged-out state so onUnauthorizedException fires and the
        // unauth group (login button) becomes visible.
        clearAuthTokens()
        super.setUp()
        launchApp()

        // Navigate to the Profile tab for every test.
        waitForId("profile").click()
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }

    // -----------------------------------------------------------------------
    // 1. Profile screen renders
    // -----------------------------------------------------------------------

    @Test
    fun profile_tab_opens_and_header_is_visible() {
        val header = device.wait(
            Until.findObject(By.res(APP_PACKAGE, "header_title")),
            TIMEOUT_MS
        )
        assertNotNull("ProfileScreen header_title not found after tapping Profile tab", header)
    }

    // -----------------------------------------------------------------------
    // 2. Pull-to-refresh container is part of the layout
    // -----------------------------------------------------------------------

    @Test
    fun swipe_refresh_layout_is_rendered() {
        val swipe = device.wait(
            Until.findObject(By.res(APP_PACKAGE, "swipe_refresh_layout")),
            TIMEOUT_MS
        )
        assertNotNull("swipe_refresh_layout is not part of the inflated ProfileScreen", swipe)
    }

    // -----------------------------------------------------------------------
    // 3. Logged-out state — login button appears, logout is hidden
    // -----------------------------------------------------------------------

    @Test
    fun logged_out_state_shows_login_button_and_hides_logout() {
        // The unauth group is invisible in XML and only becomes visible after
        // the first getUser() call receives 401 — give the network request
        // the same budget we give MainActivity on launch.
        val login = device.wait(
            Until.findObject(By.res(APP_PACKAGE, "login")),
            LAUNCH_READY_MS
        )
        assertNotNull(
            "login button must be visible in logged-out state (no 401 response received?)",
            login
        )

        // logout lives in the top bar and is GONE when onUnauthorizedException fires.
        val logoutVisible = device.hasObject(By.res(APP_PACKAGE, "logout"))
        org.junit.Assert.assertFalse(
            "logout button must be GONE when the user is not authenticated",
            logoutVisible
        )
    }

    // -----------------------------------------------------------------------
    // 4. Languages row opens the select-language bottom sheet
    // -----------------------------------------------------------------------

    @Test
    fun tapping_languages_opens_select_language_bottom_sheet() {
        waitForId("languages").click()

        // The bottom sheet's three rows prove the dialog is rendered and the
        // click wiring in ProfileScreen.onClick is intact.
        val uzbek = device.wait(Until.findObject(By.res(APP_PACKAGE, "uzbek")), TIMEOUT_MS)
        assertNotNull("uzbek language row not found — select-language bottom sheet did not open", uzbek)

        assertNotNull(
            "russian language row not found",
            device.findObject(By.res(APP_PACKAGE, "russian"))
        )
        assertNotNull(
            "english language row not found",
            device.findObject(By.res(APP_PACKAGE, "english"))
        )

        // Leave the screen clean for subsequent tests.
        device.pressBack()
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }

    // -----------------------------------------------------------------------
    // 5. Tapping login launches AuthActivity
    // -----------------------------------------------------------------------

    @Test
    fun tapping_login_opens_auth_activity() {
        // Wait for the unauth CTA to become visible (depends on 401 response).
        val login = device.wait(
            Until.findObject(By.res(APP_PACKAGE, "login")),
            LAUNCH_READY_MS
        )
        assertNotNull("login button not visible — cannot tap it", login)

        login.click()

        // The phone-input screen is the first destination in AuthActivity.
        // btSend is the "send OTP" CTA; inputPhone is the EditText. Either
        // proves the auth flow opened.
        val bt = device.wait(
            Until.findObject(By.res(APP_PACKAGE, "btSend")),
            TIMEOUT_MS
        )
        assertNotNull(
            "AuthActivity phone-input screen (btSend) not found after tapping login",
            bt
        )

        // Pop back to Profile so subsequent tests start cleanly.
        device.pressBack()
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }
}
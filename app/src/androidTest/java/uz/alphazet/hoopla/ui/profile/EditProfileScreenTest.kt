package uz.alphazet.hoopla.ui.profile

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
 * UI Automator tests for [EditProfileScreen] — the standalone activity that
 * lets the user edit their display name, birthday and gender. It loads
 * `viewModel.editMe()` on create and pushes changes through `updateMe()`.
 *
 * Tested flows:
 *  1. Launching EditProfileScreen directly via Intent renders the form
 *     chrome — toolbar, name input, gender spinner, birthday field, submit
 *     button and the delete-account action.
 *  2. `btSend` starts disabled (the activity calls `binding.btSend.disable()`
 *     immediately in onCreate before user data returns).
 *  3. Toolbar back button finishes the activity.
 *
 * Prerequisites:
 *  - Device / emulator has the debug build installed.
 *  - No auth is required for chrome assertions — the layout inflates before
 *    the `editMe()` response arrives.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class EditProfileScreenTest : BaseUiTest() {

    @Before
    override fun setUp() {
        clearAuthTokens()
        super.setUp()
        launchEditProfileScreen()
    }

    // -----------------------------------------------------------------------
    // 1. Form chrome renders
    // -----------------------------------------------------------------------

    @Test
    fun edit_profile_screen_renders_all_form_chrome() {
        assertNotNull(
            "toolbar missing on EditProfileScreen",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "toolbar")), LAUNCH_READY_MS)
        )
        assertNotNull(
            "input_name_layout missing — name field did not inflate",
            device.findObject(By.res(APP_PACKAGE, "input_name_layout"))
        )
        assertNotNull(
            "input_name missing — name EditText did not inflate",
            device.findObject(By.res(APP_PACKAGE, "input_name"))
        )
        assertNotNull(
            "gender_spinner missing on EditProfileScreen",
            device.findObject(By.res(APP_PACKAGE, "gender_spinner"))
        )
        assertNotNull(
            "input_birth missing — birthday field did not inflate",
            device.findObject(By.res(APP_PACKAGE, "input_birth"))
        )
        assertNotNull(
            "btSend missing on EditProfileScreen",
            device.findObject(By.res(APP_PACKAGE, "btSend"))
        )
        assertNotNull(
            "bt_delete_account missing — user cannot deactivate account",
            device.findObject(By.res(APP_PACKAGE, "bt_delete_account"))
        )
    }

    // -----------------------------------------------------------------------
    // 2. btSend starts disabled
    // -----------------------------------------------------------------------

    @Test
    fun btSend_is_disabled_on_initial_render() {
        val btSend = waitForId("btSend")
        assertFalse(
            "btSend must start disabled — user has not edited the form yet",
            btSend.isEnabled
        )
    }

    // -----------------------------------------------------------------------
    // 3. Toolbar back finishes activity
    // -----------------------------------------------------------------------

    @Test
    fun tapping_toolbar_back_finishes_activity() {
        assertNotNull(
            "input_name not ready before back",
            device.wait(Until.findObject(By.res(APP_PACKAGE, "input_name")), LAUNCH_READY_MS)
        )

        val backArrow = device.wait(Until.findObject(By.desc("Orqaga")), TIMEOUT_MS)
        if (backArrow != null) {
            backArrow.click()
        } else {
            device.pressBack()
        }
        device.waitForIdle(IDLE_TIMEOUT_MS)

        val stillVisible = device.wait(
            Until.hasObject(By.res(APP_PACKAGE, "input_name")),
            IDLE_TIMEOUT_MS
        )
        assertFalse(
            "input_name still visible after back — activity did not finish",
            stillVisible
        )
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Launches [EditProfileScreen] directly via Intent. */
    private fun launchEditProfileScreen() {
        val ctx: Context = ApplicationProvider.getApplicationContext()
        val intent = Intent().apply {
            setClassName(
                APP_PACKAGE,
                "uz.alphazet.hoopla.ui.profile.EditProfileScreen"
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        ctx.startActivity(intent)

        assertNotNull(
            "EditProfileScreen did not launch within ${LAUNCH_READY_MS}ms",
            device.wait(Until.hasObject(By.pkg(APP_PACKAGE).depth(0)), LAUNCH_READY_MS)
        )
        device.waitForIdle(IDLE_TIMEOUT_MS)
    }
}
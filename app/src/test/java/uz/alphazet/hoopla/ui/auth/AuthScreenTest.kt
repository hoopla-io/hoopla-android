package uz.alphazet.hoopla.ui.auth

import android.view.View
import androidx.fragment.app.testing.launchFragmentInContainer
import com.google.android.material.textfield.TextInputLayout
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koin.test.KoinTestRule
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.LoginSessionData
import uz.alphazet.domain.cache.AppCache
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.TestApp
import uz.alphazet.hoopla.rules.MainDispatcherRule

/**
 * Robolectric unit tests for [AuthScreen].
 *
 * [TestApp] replaces [uz.alphazet.hoopla.App] so MapKit and the production Koin
 * graph are never initialised. [KoinTestRule] provides a minimal graph with a
 * mocked [AuthVM] and [AppCache] per test.
 *
 * What is tested here:
 *  - Fragment inflates without crashing.
 *  - Send button starts disabled (no phone text entered).
 *  - Clicking the send button with an invalid phone shows a validation error on
 *    the phone [TextInputLayout] and does NOT call [AuthVM.sendSms].
 *  - [AuthScreen.showLoading] / [AuthScreen.hideLoading] toggle phone-input
 *    clickability (and exercise the third-party CircularProgressButton
 *    animation without crashing under Robolectric).
 *
 * What is NOT tested here (left to integration / Espresso tests):
 *  - Actual phone-number validation via the MaskedEditText (third-party).
 *  - The valid-phone → [AuthVM.sendSms] happy path (needs MaskedEditText wired).
 *  - Navigation to [ConfirmPhoneNumberScreen] from `collectSendSmsResource`
 *    (requires [AuthActivity] host).
 *  - [AuthScreen.initialize] calls `inputPhone.requestFocus()`; focus behaviour
 *    in `launchFragmentInContainer` without a real window is flaky.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = TestApp::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AuthScreenTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // Kept empty so the fragment never navigates during tests. AuthScreen only
    // collects sendSmsFlow — resendSmsFlow is consumed by ConfirmPhoneNumberScreen.
    private val sendSmsFlow = MutableSharedFlow<UIResource<LoginSessionData>>(replay = 0)

    private val authVM: AuthVM = mockk(relaxed = true) {
        every { sendSmsFlow } returns this@AuthScreenTest.sendSmsFlow
    }

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(module {
            viewModel<AuthVM> { authVM }
            single<AppCache> { mockk(relaxed = true) }
        })
    }

    // -----------------------------------------------------------------------

    @Test
    fun fragment_inflates_without_crash() {
        launchFragmentInContainer<AuthScreen>(themeResId = R.style.Theme_Hoopla)
    }

    @Test
    fun send_button_is_disabled_initially() {
        val scenario = launchFragmentInContainer<AuthScreen>(themeResId = R.style.Theme_Hoopla)
        scenario.onFragment { fragment ->
            val btSend = fragment.requireView().findViewById<View>(R.id.btSend)
            assertFalse(
                "Send button must be disabled before any phone text is entered",
                btSend.isEnabled
            )
        }
    }

    /**
     * With an empty phone, [AuthScreen.onClick] on `btSend` must set the error
     * string on the phone [TextInputLayout] and must NOT call [AuthVM.sendSms].
     * Calling `fragment.onClick(view)` directly bypasses the disabled-click
     * guard on `btSend`, so we can exercise the validation branch deterministically
     * without driving the third-party MaskedEditText.
     */
    @Test
    fun click_send_with_invalid_phone_sets_layout_error_and_does_not_call_sendSms() {
        val scenario = launchFragmentInContainer<AuthScreen>(themeResId = R.style.Theme_Hoopla)

        scenario.onFragment { fragment ->
            val root = fragment.requireView()
            val btSend = root.findViewById<View>(R.id.btSend)
            val inputPhoneLayout = root.findViewById<TextInputLayout>(R.id.inputPhoneLayout)

            fragment.onClick(btSend)

            val expected =
                fragment.getString(uz.alphazet.domain.R.string.phone_number_is_not_valid)
            assertNotNull("Phone layout must have an error set on invalid submit", inputPhoneLayout.error)
            assertEquals(expected, inputPhoneLayout.error.toString())
            io.mockk.verify(exactly = 0) { authVM.sendSms(any()) }
        }
    }

    @Test
    fun showLoading_disables_phone_input_clickability() {
        val scenario = launchFragmentInContainer<AuthScreen>(themeResId = R.style.Theme_Hoopla)
        scenario.onFragment { fragment ->
            fragment.showLoading()
            val inputPhone = fragment.requireView().findViewById<View>(R.id.inputPhone)
            assertFalse(
                "Phone input must not be clickable while loading",
                inputPhone.isClickable
            )
        }
    }

    @Test
    fun hideLoading_re_enables_phone_input_clickability() {
        val scenario = launchFragmentInContainer<AuthScreen>(themeResId = R.style.Theme_Hoopla)
        scenario.onFragment { fragment ->
            // Simulate a loading cycle: show then hide.
            fragment.showLoading()
            fragment.hideLoading()
            val inputPhone = fragment.requireView().findViewById<View>(R.id.inputPhone)
            assertTrue(
                "Phone input must be clickable again after hideLoading()",
                inputPhone.isClickable
            )
        }
    }
}
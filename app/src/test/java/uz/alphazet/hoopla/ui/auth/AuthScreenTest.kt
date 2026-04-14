package uz.alphazet.hoopla.ui.auth

import android.view.View
import androidx.fragment.app.testing.launchFragmentInContainer
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Assert.assertFalse
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
 * [TestApp] is used instead of [uz.alphazet.hoopla.App] so that MapKit and the
 * production Koin graph are never initialised.  [KoinTestRule] provides a minimal
 * Koin context with a mocked [AuthVM] and [AppCache] for each test.
 *
 * What is tested here:
 *  - The fragment inflates without crashing.
 *  - The send button starts disabled (no phone text entered).
 *  - [AuthScreen.showLoading] disables the phone input.
 *  - [AuthScreen.hideLoading] re-enables the phone input.
 *
 * What is NOT tested here (left to integration / Espresso tests):
 *  - Actual phone-number validation via the masked EditText (third-party behaviour).
 *  - Navigation to [ConfirmPhoneNumberScreen] (requires [AuthActivity] host).
 *  - The [CircularProgressButton] animation (third-party, no-op in Robolectric).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = TestApp::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AuthScreenTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // Backing flows — remain empty so the fragment never navigates during tests.
    private val sendSmsFlow = MutableSharedFlow<UIResource<LoginSessionData>>(replay = 0)
    private val resendSmsFlow = MutableSharedFlow<UIResource<LoginSessionData>>(replay = 0)

    private val authVM: AuthVM = mockk(relaxed = true) {
        every { sendSmsFlow } returns this@AuthScreenTest.sendSmsFlow
        every { resendSmsFlow } returns this@AuthScreenTest.resendSmsFlow
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
            assertFalse("Send button must be disabled before any phone text is entered",
                btSend.isEnabled)
        }
    }

    @Test
    fun showLoading_disables_phone_input_clickability() {
        val scenario = launchFragmentInContainer<AuthScreen>(themeResId = R.style.Theme_Hoopla)
        scenario.onFragment { fragment ->
            fragment.showLoading()
            val inputPhone = fragment.requireView().findViewById<View>(R.id.inputPhone)
            assertFalse("Phone input must not be clickable while loading",
                inputPhone.isClickable)
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
            assert(inputPhone.isClickable) {
                "Phone input must be clickable again after hideLoading()"
            }
        }
    }
}

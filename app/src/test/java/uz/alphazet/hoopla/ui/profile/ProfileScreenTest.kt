package uz.alphazet.hoopla.ui.profile

import android.view.View
import android.widget.TextView
import androidx.fragment.app.testing.launchFragmentInContainer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import org.koin.test.KoinTestRule
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.UserData
import uz.alphazet.domain.cache.AppCache
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.TestApp
import uz.alphazet.hoopla.rules.MainDispatcherRule

/**
 * Robolectric unit tests for [ProfileScreen].
 *
 * Covers:
 *  - Fragment inflation.
 *  - [ProfileVM.getUser] is called on view creation.
 *  - Success state → auth group is visible, user name is displayed.
 *  - [ProfileScreen.onUnauthorizedException] shows the unauthenticated group.
 *  - Pull-to-refresh triggers a second [ProfileVM.getUser] call.
 *
 * Koin is provided per-test via [KoinTestRule] with mocked [ProfileVM] and [AppCache].
 * The [userFlow] StateFlow is pre-seeded with the desired state **before** launching the
 * fragment so that [collectLatest] fires the correct branch during [initialize].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = TestApp::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileScreenTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val userFlow =
        MutableStateFlow<UIResource<UserData>>(UIResource.Loading)

    private val profileVM: ProfileVM = mockk(relaxed = true) {
        every { userDataFlow } returns userFlow
    }

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(module {
            viewModel<ProfileVM> { profileVM }
            single<AppCache> { mockk(relaxed = true) }
        })
    }

    // -----------------------------------------------------------------------

    @Test
    fun fragment_inflates_without_crash() {
        launchFragmentInContainer<ProfileScreen>(themeResId = R.style.Theme_Hoopla)
    }

    @Test
    fun getUser_is_called_once_on_view_created() {
        launchFragmentInContainer<ProfileScreen>(themeResId = R.style.Theme_Hoopla)
        // initialize() calls viewModel.getUser() exactly once
        verify(exactly = 1) { profileVM.getUser() }
    }

    /**
     * When [userFlow] already holds a [UIResource.Success] value at launch time,
     * [collectLatest] immediately runs the success branch, shows [R.id.authGroup],
     * hides [R.id.unAuthGroup], and sets the user's name.
     */
    @Test
    fun success_state_shows_auth_group_and_user_name() {
        // UserData(name, phoneNumber, balance, cashbackBalance, currency, gender,
        //          dateOfBirth, userId, subscription, unreadNotifications)
        val userData = UserData("Ali", null, 0.0, 0.0, "UZS", null, null, 1, null, 0)
        // Pre-seed before launch so collectLatest picks it up in initialize()
        userFlow.value = UIResource.Success(userData)

        val scenario = launchFragmentInContainer<ProfileScreen>(themeResId = R.style.Theme_Hoopla)

        scenario.onFragment { fragment ->
            val root = fragment.requireView()
            assertEquals(
                "authGroup must be VISIBLE on success",
                View.VISIBLE,
                root.findViewById<View>(R.id.authGroup).visibility
            )
            assertEquals(
                "unAuthGroup must be GONE on success",
                View.GONE,
                root.findViewById<View>(R.id.unAuthGroup).visibility
            )
            assertEquals(
                "User name must be displayed",
                "Ali",
                root.findViewById<TextView>(R.id.name).text.toString()
            )
        }
    }

    /**
     * [ProfileScreen.onUnauthorizedException] is the override that reacts to 401 errors.
     * Calling it directly verifies the auth/unauth group visibility toggle without
     * needing a live network call.
     */
    @Test
    fun unauthorized_exception_shows_unauth_group_and_hides_auth_group() {
        val scenario = launchFragmentInContainer<ProfileScreen>(themeResId = R.style.Theme_Hoopla)

        scenario.onFragment { fragment ->
            fragment.onUnauthorizedException("Unauthorized", 401)

            val root = fragment.requireView()
            assertEquals(
                "unAuthGroup must be VISIBLE after unauthorized",
                View.VISIBLE,
                root.findViewById<View>(R.id.unAuthGroup).visibility
            )
            assertEquals(
                "authGroup must be GONE after unauthorized",
                View.GONE,
                root.findViewById<View>(R.id.authGroup).visibility
            )
            assertEquals(
                "logout button must be GONE after unauthorized",
                View.GONE,
                root.findViewById<View>(R.id.logout).visibility
            )
        }
    }

    /**
     * Pull-to-refresh calls [ProfileScreen.onRefresh] which delegates to
     * [ProfileVM.getUser].  Combined with the call in [initialize], the VM
     * method should have been invoked exactly twice.
     */
    @Test
    fun pull_to_refresh_calls_getUser_a_second_time() {
        val scenario = launchFragmentInContainer<ProfileScreen>(themeResId = R.style.Theme_Hoopla)

        scenario.onFragment { fragment ->
            fragment.onRefresh()
        }

        verify(exactly = 2) { profileVM.getUser() }
    }
}

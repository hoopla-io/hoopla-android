package uz.alphazet.hoopla.ui.profile

import android.view.View
import android.widget.TextView
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
import uz.alphazet.data.models.SubscriptionData
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
 *  - Balance text is rendered with the currency suffix.
 *  - Subscription branch: with a subscription the active-tariff card is shown
 *    and the "select tariff" CTA is hidden; without, the CTA is shown and the
 *    card is hidden.
 *  - [ProfileScreen.onUnauthorizedException] shows the unauthenticated group.
 *  - [ProfileScreen.showLoading] / [ProfileScreen.hideLoading] toggle the
 *    [SwipeRefreshLayout] indicator.
 *  - Pull-to-refresh triggers a second [ProfileVM.getUser] call.
 *
 * Koin is provided per-test via [KoinTestRule] with a mocked [ProfileVM] and
 * [AppCache]. The `userDataFlow` [MutableStateFlow] is pre-seeded with the
 * desired state **before** launching the fragment so that `collectLatest`
 * fires the correct branch during `initialize`.
 *
 * What is NOT tested here (left to integration / Espresso tests):
 *  - Click → navigation intents (edit, login, top-up, subscription).
 *  - Logout confirmation DialogFragment flow.
 *  - Language BottomSheet.
 *  - Exact phone/balance formatting (covered by their own extension unit tests).
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

    // Helpers --------------------------------------------------------------

    private fun user(
        name: String? = "Ali",
        phone: String? = null,
        balance: Double? = 0.0,
        currency: String? = "UZS",
        subscription: SubscriptionData? = null,
    ) = UserData(
        name, phone, balance, 0.0, currency, null, null, 1, subscription, 0
    )

    // -----------------------------------------------------------------------

    @Test
    fun fragment_inflates_without_crash() {
        launchFragmentInContainer<ProfileScreen>(themeResId = R.style.Theme_Hoopla)
    }

    @Test
    fun getUser_is_called_once_on_view_created() {
        launchFragmentInContainer<ProfileScreen>(themeResId = R.style.Theme_Hoopla)
        verify(exactly = 1) { profileVM.getUser() }
    }

    /**
     * When `userDataFlow` already holds a [UIResource.Success] value at launch,
     * `collectLatest` runs the success branch: `authGroup` VISIBLE,
     * `unAuthGroup` GONE, name TextView populated.
     */
    @Test
    fun success_state_shows_auth_group_and_user_name() {
        userFlow.value = UIResource.Success(user(name = "Ali"))

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
     * Balance text is composed as `formatToPrice() + " $currency"`. We don't
     * reassert the price-formatting rules here (those are covered by the
     * extension's own tests) — we just confirm the currency suffix is present.
     */
    @Test
    fun success_state_sets_balance_text_with_currency() {
        userFlow.value = UIResource.Success(user(balance = 12_500.0, currency = "UZS"))

        val scenario = launchFragmentInContainer<ProfileScreen>(themeResId = R.style.Theme_Hoopla)

        scenario.onFragment { fragment ->
            val balance = fragment.requireView().findViewById<TextView>(R.id.balance).text.toString()
            assertTrue(
                "Balance text must end with the currency suffix, was: '$balance'",
                balance.endsWith(" UZS")
            )
        }
    }

    /**
     * With a non-null [SubscriptionData] → the active-tariff card is shown,
     * the "select tariff" CTA is hidden, and the subscription name is rendered.
     */
    @Test
    fun success_with_subscription_shows_active_tariff_card_and_hides_select_tariff() {
        val sub = SubscriptionData(id = 1, name = "Routine", endDate = null, endDateUnix = null)
        userFlow.value = UIResource.Success(user(subscription = sub))

        val scenario = launchFragmentInContainer<ProfileScreen>(themeResId = R.style.Theme_Hoopla)

        scenario.onFragment { fragment ->
            val root = fragment.requireView()
            assertEquals(
                "active_tariff_info_card must be VISIBLE when user has a subscription",
                View.VISIBLE,
                root.findViewById<View>(R.id.active_tariff_info_card).visibility
            )
            assertEquals(
                "select_tariff CTA must be GONE when user has a subscription",
                View.GONE,
                root.findViewById<View>(R.id.select_tariff).visibility
            )
            assertEquals(
                "Routine",
                root.findViewById<TextView>(R.id.subscription_name).text.toString()
            )
        }
    }

    /**
     * With a null [SubscriptionData] → the opposite: active-tariff card hidden,
     * "select tariff" CTA visible.
     */
    @Test
    fun success_without_subscription_shows_select_tariff_and_hides_active_tariff_card() {
        userFlow.value = UIResource.Success(user(subscription = null))

        val scenario = launchFragmentInContainer<ProfileScreen>(themeResId = R.style.Theme_Hoopla)

        scenario.onFragment { fragment ->
            val root = fragment.requireView()
            assertEquals(
                "active_tariff_info_card must be GONE when user has no subscription",
                View.GONE,
                root.findViewById<View>(R.id.active_tariff_info_card).visibility
            )
            assertEquals(
                "select_tariff CTA must be VISIBLE when user has no subscription",
                View.VISIBLE,
                root.findViewById<View>(R.id.select_tariff).visibility
            )
        }
    }

    /**
     * [ProfileScreen.onUnauthorizedException] is the override that reacts to 401
     * errors. Calling it directly verifies the auth/unauth group visibility
     * toggle without needing a live network call.
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
     * [ProfileScreen] overrides `showLoading` / `hideLoading` to drive the
     * [SwipeRefreshLayout] indicator with no isResumed guard, so we can assert
     * the refreshing flag directly.
     */
    @Test
    fun showLoading_starts_swipe_refresh_indicator() {
        val scenario = launchFragmentInContainer<ProfileScreen>(themeResId = R.style.Theme_Hoopla)

        scenario.onFragment { fragment ->
            fragment.showLoading()
            val swipe = fragment.requireView()
                .findViewById<SwipeRefreshLayout>(R.id.swipe_refresh_layout)
            assertTrue("SwipeRefreshLayout must be refreshing after showLoading()", swipe.isRefreshing)
        }
    }

    @Test
    fun hideLoading_stops_swipe_refresh_indicator() {
        val scenario = launchFragmentInContainer<ProfileScreen>(themeResId = R.style.Theme_Hoopla)

        scenario.onFragment { fragment ->
            fragment.showLoading()
            fragment.hideLoading()
            val swipe = fragment.requireView()
                .findViewById<SwipeRefreshLayout>(R.id.swipe_refresh_layout)
            assertFalse("SwipeRefreshLayout must stop refreshing after hideLoading()", swipe.isRefreshing)
        }
    }

    /**
     * Pull-to-refresh calls [ProfileScreen.onRefresh] which delegates to
     * [ProfileVM.getUser]. Combined with the call in `initialize`, the VM
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
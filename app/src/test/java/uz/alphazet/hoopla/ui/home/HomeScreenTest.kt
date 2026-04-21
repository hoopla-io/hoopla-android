package uz.alphazet.hoopla.ui.home

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
import uz.alphazet.data.models.CategoryData
import uz.alphazet.data.models.FeedbackDetail
import uz.alphazet.data.models.UserData
import uz.alphazet.domain.cache.AppCache
import uz.alphazet.domain.permission.PermissionManager
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.TestApp
import uz.alphazet.hoopla.rules.MainDispatcherRule

/**
 * Robolectric unit tests for [HomeScreen].
 *
 * Covers:
 *  - Fragment inflation.
 *  - [HomeVM.getPendingFeedbacks], [HomeVM.getUser] and [HomeVM.getCategories] are
 *    called on view creation.
 *  - [collectUserData] — unread-notification badge visibility / formatting
 *    (hidden at 0, numeric at 1..99, "99+" above 99).
 *  - [collectCategories] — non-empty list makes the categories RecyclerView visible;
 *    empty list leaves it hidden.
 *  - Pull-to-refresh re-invokes [HomeVM.getCategories].
 *
 * Koin is provided per-test via [KoinTestRule] with a mocked [HomeVM],
 * [PermissionManager] and [AppCache]. StateFlows are pre-seeded **before** launching
 * the fragment so that [collectLatest] fires the correct branch during [initialize].
 * The [PermissionManager] is a relaxed mock — its `onAllow` callback is never
 * invoked, which prevents the location-services code-path from running during tests.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = TestApp::class)
@OptIn(ExperimentalCoroutinesApi::class)
class HomeScreenTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val pendingFeedbackFlow =
        MutableStateFlow<UIResource<FeedbackDetail>>(UIResource.Loading)
    private val userDataFlow =
        MutableStateFlow<UIResource<UserData>>(UIResource.Loading)
    private val categoriesFlow =
        MutableStateFlow<UIResource<List<CategoryData>>>(UIResource.Loading)

    private val homeVM: HomeVM = mockk(relaxed = true) {
        every { pendingFeedbackFlow } returns this@HomeScreenTest.pendingFeedbackFlow
        every { userDataFlow } returns this@HomeScreenTest.userDataFlow
        every { categoriesFlow } returns this@HomeScreenTest.categoriesFlow
    }

    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(module {
            viewModel<HomeVM> { homeVM }
            single<PermissionManager> { mockk(relaxed = true) }
            single<AppCache> { mockk(relaxed = true) }
        })
    }

    // -----------------------------------------------------------------------

    @Test
    fun fragment_inflates_without_crash() {
        launchFragmentInContainer<HomeScreen>(themeResId = R.style.Theme_Hoopla)
    }

    @Test
    fun initialize_calls_getPendingFeedbacks_getUser_getCategories_once_each() {
        launchFragmentInContainer<HomeScreen>(themeResId = R.style.Theme_Hoopla)

        verify(exactly = 1) { homeVM.getPendingFeedbacks() }
        verify(exactly = 1) { homeVM.getUser() }
        verify(exactly = 1) { homeVM.getCategories() }
    }

    /**
     * [UserData.unreadNotifications] == 0 → badge stays GONE.
     */
    @Test
    fun user_with_zero_unread_hides_notification_badge() {
        val user = UserData("Ali", null, 0.0, 0.0, "UZS", null, null, 1, null, 0)
        userDataFlow.value = UIResource.Success(user)

        val scenario = launchFragmentInContainer<HomeScreen>(themeResId = R.style.Theme_Hoopla)

        scenario.onFragment { fragment ->
            val badge = fragment.requireView().findViewById<View>(R.id.notification_badge)
            assertEquals(
                "Notification badge must stay GONE when there are no unread notifications",
                View.GONE,
                badge.visibility
            )
        }
    }

    /**
     * 0 < unread ≤ 99 → badge VISIBLE and shows the numeric count.
     */
    @Test
    fun user_with_unread_notifications_shows_badge_with_count() {
        val user = UserData("Ali", null, 0.0, 0.0, "UZS", null, null, 1, null, 7)
        userDataFlow.value = UIResource.Success(user)

        val scenario = launchFragmentInContainer<HomeScreen>(themeResId = R.style.Theme_Hoopla)

        scenario.onFragment { fragment ->
            val badge = fragment.requireView().findViewById<TextView>(R.id.notification_badge)
            assertEquals(
                "Notification badge must be VISIBLE when unread > 0",
                View.VISIBLE,
                badge.visibility
            )
            assertEquals("7", badge.text.toString())
        }
    }

    /**
     * unread > 99 → badge shows "99+".
     */
    @Test
    fun user_with_more_than_99_unread_shows_99_plus() {
        val user = UserData("Ali", null, 0.0, 0.0, "UZS", null, null, 1, null, 150)
        userDataFlow.value = UIResource.Success(user)

        val scenario = launchFragmentInContainer<HomeScreen>(themeResId = R.style.Theme_Hoopla)

        scenario.onFragment { fragment ->
            val badge = fragment.requireView().findViewById<TextView>(R.id.notification_badge)
            assertEquals(View.VISIBLE, badge.visibility)
            assertEquals("99+", badge.text.toString())
        }
    }

    /**
     * Non-empty category list → categories RecyclerView becomes VISIBLE.
     */
    @Test
    fun non_empty_categories_make_categories_rv_visible() {
        val cats = listOf(CategoryData(1, "Coffee", null), CategoryData(2, "Tea", null))
        categoriesFlow.value = UIResource.Success(cats)

        val scenario = launchFragmentInContainer<HomeScreen>(themeResId = R.style.Theme_Hoopla)

        scenario.onFragment { fragment ->
            val rv = fragment.requireView().findViewById<View>(R.id.categories_rv)
            assertEquals(
                "categories_rv must be VISIBLE when categories list is non-empty",
                View.VISIBLE,
                rv.visibility
            )
        }
    }

    /**
     * Empty category list → categories RecyclerView stays GONE (XML default).
     */
    @Test
    fun empty_categories_keep_categories_rv_gone() {
        categoriesFlow.value = UIResource.Success(emptyList())

        val scenario = launchFragmentInContainer<HomeScreen>(themeResId = R.style.Theme_Hoopla)

        scenario.onFragment { fragment ->
            val rv = fragment.requireView().findViewById<View>(R.id.categories_rv)
            assertEquals(
                "categories_rv must stay GONE when categories list is empty",
                View.GONE,
                rv.visibility
            )
        }
    }

    /**
     * Pull-to-refresh calls [HomeScreen.onRefresh] which re-invokes
     * [HomeVM.getCategories]. Combined with the call in [initialize], the VM
     * method should have been invoked exactly twice.
     */
    @Test
    fun pull_to_refresh_calls_getCategories_a_second_time() {
        val scenario = launchFragmentInContainer<HomeScreen>(themeResId = R.style.Theme_Hoopla)

        scenario.onFragment { fragment ->
            fragment.onRefresh()
        }

        verify(exactly = 2) { homeVM.getCategories() }
    }
}
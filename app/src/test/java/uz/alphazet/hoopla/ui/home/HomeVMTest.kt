package uz.alphazet.hoopla.ui.home

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.CategoryData
import uz.alphazet.data.models.FeedbackDetail
import uz.alphazet.data.models.LoyaltyItemData
import uz.alphazet.data.models.ShopItemData
import uz.alphazet.data.models.UserData
import uz.alphazet.domain.network.RemoteException
import uz.alphazet.domain.repositories.CategoryRepo
import uz.alphazet.domain.repositories.HomeRepo
import uz.alphazet.domain.repositories.OrdersRepo
import uz.alphazet.domain.repositories.ProfileRepo
import uz.alphazet.domain.repositories.StoryRepo
import uz.alphazet.hoopla.rules.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class HomeVMTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val homeRepo: HomeRepo = mockk()
    private val profileRepo: ProfileRepo = mockk()
    private val categoryRepo: CategoryRepo = mockk()
    private val storyRepo: StoryRepo = mockk()
    private val ordersRepo: OrdersRepo = mockk()

    private val vm by lazy { HomeVM(homeRepo, profileRepo, categoryRepo, storyRepo, ordersRepo) }

    // --- getUser (Pattern B — StateFlow) ----------------------------------

    @Test
    fun getUser_updates_userDataFlow_from_loading_to_success() = runTest {
        val user = UserData("Ali", "998901234567", 1000.0, 200.0, "UZS", "M", null, 1, null, 0)
        coEvery { profileRepo.getMe() } returns UIResource.Success(user)

        vm.userDataFlow.test {
            assertEquals(UIResource.Loading, awaitItem()) // initial
            vm.getUser()
            val result = awaitItem()
            assertTrue(result is UIResource.Success)
            assertEquals("Ali", (result as UIResource.Success).data?.name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getUser_updates_userDataFlow_to_error() = runTest {
        coEvery { profileRepo.getMe() } returns UIResource.Error(RemoteException("network", -1))

        vm.userDataFlow.test {
            assertEquals(UIResource.Loading, awaitItem())
            vm.getUser()
            assertTrue(awaitItem() is UIResource.Error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- getCategories (Pattern B) ----------------------------------------

    @Test
    fun getCategories_updates_categoriesFlow_to_success() = runTest {
        val cats = listOf(CategoryData(1, "Coffee", null), CategoryData(2, "Tea", null))
        coEvery { categoryRepo.getCategories() } returns UIResource.Success(cats)

        vm.categoriesFlow.test {
            assertEquals(UIResource.Loading, awaitItem())
            vm.getCategories()
            val result = awaitItem()
            assertTrue(result is UIResource.Success)
            assertEquals(2, (result as UIResource.Success).data?.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- getPendingFeedbacks (Pattern B) ----------------------------------

    @Test
    fun getPendingFeedbacks_updates_pendingFeedbackFlow() = runTest {
        val feedback = FeedbackDetail(12.5, 0.0, "Latte", 99, "DELIVERED", "Hoopla", 25000.0, null, null, "Hoopla Central", null)
        coEvery { homeRepo.getPendingFeedbacks() } returns UIResource.Success(feedback)

        vm.pendingFeedbackFlow.test {
            assertEquals(UIResource.Loading, awaitItem())
            vm.getPendingFeedbacks()
            val result = awaitItem()
            assertTrue(result is UIResource.Success)
            assertEquals("Latte", (result as UIResource.Success).data?.drinkName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- getLoyaltyCard (Pattern C — suspend returning SharedFlow) --------

    @Test
    fun getLoyaltyCard_returns_success_via_shared_flow() = runTest {
        val cards = listOf(LoyaltyItemData(0, true, false), LoyaltyItemData(1, false, true))
        coEvery { homeRepo.getLoyaltyCard() } returns flowOf(UIResource.Success(cards))

        val flow = vm.getLoyaltyCard()
        flow.test {
            val result = awaitItem()
            assertTrue(result is UIResource.Success)
            assertEquals(2, (result as UIResource.Success).data?.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- getNearShops (Pattern C) -----------------------------------------

    @Test
    fun getNearShops_returns_shops_for_given_coords() = runTest {
        val shops = listOf(ShopItemData(7, "Hoopla", null, 0.4, null))
        coEvery { homeRepo.getNearShops(41.3, 69.2, "Hoopla", null) } returns
            flowOf(UIResource.Success(shops))

        val flow = vm.getNearShops(41.3, 69.2, "Hoopla")
        flow.test {
            val result = awaitItem()
            assertTrue(result is UIResource.Success)
            assertEquals("Hoopla", (result as UIResource.Success).data?.first()?.name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getNearShops_null_params_pass_through() = runTest {
        coEvery { homeRepo.getNearShops(any(), any(), null, null) } returns
            flowOf(UIResource.Success(emptyList()))

        val flow = vm.getNearShops(41.3, 69.2, null)
        flow.test {
            assertTrue(awaitItem() is UIResource.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- submitFeedback (Pattern C) ---------------------------------------

    @Test
    fun submitFeedback_returns_success() = runTest {
        coEvery { homeRepo.submitFeedback(77, 5, "great") } returns
            flowOf(UIResource.Success(Any()))

        val flow = vm.submitFeedback(77, 5, "great")
        flow.test {
            assertTrue(awaitItem() is UIResource.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

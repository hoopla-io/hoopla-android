package uz.alphazet.hoopla.ui.order

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.UserData
import uz.alphazet.data.models.order.CheckOutInfo
import uz.alphazet.data.models.order.ModifierItemData
import uz.alphazet.data.models.order.OrderDetails
import uz.alphazet.data.models.order.OrderInfoData
import uz.alphazet.domain.repositories.OrderRepo
import uz.alphazet.domain.repositories.ProfileRepo
import uz.alphazet.hoopla.rules.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class OrderVMTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val orderRepo: OrderRepo = mockk()
    private val profileRepo: ProfileRepo = mockk()

    private val userData = UserData("Ali", null, 1000.0, 100.0, "UZS", null, null, 1, null, 0)

    // vm is lazy so we can set up mocks for the init block first
    private val vm by lazy {
        coEvery { profileRepo.getMe() } returns UIResource.Success(userData)
        OrderVM(orderRepo, profileRepo)
    }

    // --- init block -------------------------------------------------------

    @Test
    fun init_triggers_getUser_and_populates_userDataFlow() = runTest {
        coEvery { profileRepo.getMe() } returns UIResource.Success(userData)
        val vm = OrderVM(orderRepo, profileRepo)

        vm.userDataFlow.test {
            // StateFlow initial is Loading; init block fires getUser which
            // sets it to Success before we even see the first item (unconfined).
            val first = awaitItem()
            // With UnconfinedTestDispatcher the launch runs eagerly: we may
            // get Loading or Success depending on timing. Accept either and
            // assert at least one Success arrives.
            if (first is UIResource.Loading) {
                assertTrue(awaitItem() is UIResource.Success)
            } else {
                assertTrue(first is UIResource.Success)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- getUser (Pattern B) ----------------------------------------------

    @Test
    fun getUser_updates_userDataFlow_to_success() = runTest {
        // Create a fresh local VM so mock setup is fully controlled.
        coEvery { profileRepo.getMe() } returns UIResource.Success(userData)
        val localVm = OrderVM(orderRepo, profileRepo)

        // Change mock and call getUser() *before* the Turbine collector starts, so
        // the StateFlow already holds Success("Bek") when we subscribe.
        coEvery { profileRepo.getMe() } returns UIResource.Success(userData.copy(name = "Bek"))
        localVm.getUser()

        localVm.userDataFlow.test {
            val item = awaitItem()
            // Drain any residual Loading then assert the Success
            val result = if (item is UIResource.Loading) awaitItem() else item
            assertTrue(result is UIResource.Success)
            assertEquals("Bek", (result as UIResource.Success).data?.name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- validateOrder (Pattern C) ----------------------------------------

    @Test
    fun validateOrder_returns_order_details() = runTest {
        val drink = OrderDetails.Drink(5, "Latte", 25000.0, null)
        val shop = OrderDetails.Shop(3, "Hoopla")
        val mod = OrderDetails.Modification(emptyList(), emptyList(), emptyList(), emptyList())
        val details = OrderDetails(mod, drink, shop, null, null, null)

        coEvery { orderRepo.validateOrder(3, 5) } returns flowOf(UIResource.Success(details))

        val flow = vm.validateOrder(3, 5)
        flow.test {
            val result = awaitItem()
            assertTrue(result is UIResource.Success)
            assertEquals("Latte", (result as UIResource.Success).data?.drink?.name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- createOrder (Pattern C) ------------------------------------------

    @Test
    fun createOrder_returns_order_info() = runTest {
        val info = OrderInfoData(101, "Hoopla", "Hoopla Central", null, null, "Latte", "pending")
        coEvery { orderRepo.createOrder(3, 5, any(), any()) } returns flowOf(UIResource.Success(info))

        val flow = vm.createOrder(3, 5, arrayListOf())
        flow.test {
            val result = awaitItem()
            assertTrue(result is UIResource.Success)
            assertEquals("pending", (result as UIResource.Success).data?.orderStatus)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun createOrder_forwards_comment_to_repo() = runTest {
        val info = OrderInfoData(101, "Hoopla", "Hoopla Central", null, null, "Latte", "pending")
        coEvery { orderRepo.createOrder(3, 5, any(), "no sugar") } returns
            flowOf(UIResource.Success(info))

        val flow = vm.createOrder(3, 5, arrayListOf(), "no sugar")
        flow.test {
            val result = awaitItem()
            assertTrue(result is UIResource.Success)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { orderRepo.createOrder(3, 5, any(), "no sugar") }
    }

    // --- createOrderRahmat (Pattern C) ------------------------------------

    @Test
    fun createOrderRahmat_returns_checkout_info() = runTest {
        val checkout = CheckOutInfo(29000, "https://pay/co", "app://pay", "2030-01-01", 55, "https://s/x")
        coEvery { orderRepo.createOrderRahmat(3, 5, any(), false, 0.0, any()) } returns
            flowOf(UIResource.Success(checkout))

        val flow = vm.createOrderRahmat(3, 5, arrayListOf(), false, 0.0)
        flow.test {
            val result = awaitItem()
            assertTrue(result is UIResource.Success)
            assertEquals("https://pay/co", (result as UIResource.Success).data?.checkout_url)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun createOrderRahmat_forwards_comment_to_repo() = runTest {
        val checkout = CheckOutInfo(29000, "https://pay/co", "app://pay", "2030-01-01", 55, "https://s/x")
        coEvery { orderRepo.createOrderRahmat(3, 5, any(), false, 0.0, "extra hot") } returns
            flowOf(UIResource.Success(checkout))

        val flow = vm.createOrderRahmat(3, 5, arrayListOf(), false, 0.0, "extra hot")
        flow.test {
            val result = awaitItem()
            assertTrue(result is UIResource.Success)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { orderRepo.createOrderRahmat(3, 5, any(), false, 0.0, "extra hot") }
    }
}

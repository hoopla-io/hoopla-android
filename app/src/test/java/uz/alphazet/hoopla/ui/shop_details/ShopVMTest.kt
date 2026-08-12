package uz.alphazet.hoopla.ui.shop_details

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
import uz.alphazet.data.models.ShopData
import uz.alphazet.data.models.order.OrderDetails
import uz.alphazet.domain.network.RemoteException
import uz.alphazet.domain.repositories.OrderRepo
import uz.alphazet.domain.repositories.ShopRepo
import uz.alphazet.hoopla.rules.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class ShopVMTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repo: ShopRepo = mockk()
    private val orderRepo: OrderRepo = mockk()
    private val vm by lazy { ShopVM(repo, orderRepo) }

    @Test
    fun getShopDetail_returns_shop_data() = runTest {
        val shop = ShopData(
            id = 3,
            partnerId = 1,
            name = "Hoopla Central",
            pictureUrl = null,
            canAcceptOrders = true,
            location = null,
            phoneNumbers = emptyList(),
            workingHours = emptyList(),
            pictures = emptyList(),
            drinks = emptyList(),
            urls = emptyList(),
        )
        coEvery { repo.getShopDetail(3) } returns flowOf(UIResource.Success(shop))

        val flow = vm.getShopDetail(3)
        flow.test {
            val result = awaitItem()
            assertTrue(result is UIResource.Success)
            assertEquals("Hoopla Central", (result as UIResource.Success).data?.name)
            assertTrue(result.data?.canAcceptOrders == true)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getShopDetail_returns_error_when_repo_fails() = runTest {
        coEvery { repo.getShopDetail(999) } returns flowOf(
            UIResource.Error(RemoteException("not found", 404))
        )

        val flow = vm.getShopDetail(999)
        flow.test {
            val result = awaitItem()
            assertTrue(result is UIResource.Error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /**
     * The menu grid asks this before adding a drink, to find out whether it has options that have
     * to be picked first. It goes through the order repo rather than OrderVM, whose init block
     * would fire a profile request from every shop screen.
     */
    @Test
    fun validateOrder_forwards_to_the_order_repo() = runTest {
        val details = OrderDetails(
            modifications = OrderDetails.Modification(null, null, null, null),
            drink = OrderDetails.Drink(id = 5, name = "Espresso", amount = 12000.0, imageUrl = null),
            shop = OrderDetails.Shop(id = 3, name = "Hoopla Central"),
            validatedAt = null,
            validatedAtUnix = null,
            cashbackPercent = null
        )
        coEvery { orderRepo.validateOrder(3, 5) } returns flowOf(UIResource.Success(details))

        vm.validateOrder(3, 5).test {
            val result = awaitItem()
            assertTrue(result is UIResource.Success)
            assertEquals("Espresso", (result as UIResource.Success).data?.drink?.name)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { orderRepo.validateOrder(3, 5) }
    }

    @Test
    fun validateOrder_propagates_an_error() = runTest {
        coEvery { orderRepo.validateOrder(any(), any()) } returns flowOf(
            UIResource.Error(RemoteException("not found", 404))
        )

        vm.validateOrder(3, 99).test {
            assertTrue(awaitItem() is UIResource.Error)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

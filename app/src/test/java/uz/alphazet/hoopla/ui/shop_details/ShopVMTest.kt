package uz.alphazet.hoopla.ui.shop_details

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
import uz.alphazet.data.models.ShopData
import uz.alphazet.domain.network.RemoteException
import uz.alphazet.domain.repositories.ShopRepo
import uz.alphazet.hoopla.rules.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class ShopVMTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repo: ShopRepo = mockk()
    private val vm by lazy { ShopVM(repo) }

    @Test
    fun getShopDetail_returns_shop_data() = runTest {
        val shop = ShopData(3, 1, "Hoopla Central", true, null, emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
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
}

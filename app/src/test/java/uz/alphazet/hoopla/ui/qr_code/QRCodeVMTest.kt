package uz.alphazet.hoopla.ui.qr_code

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
import uz.alphazet.data.models.DailyDrinksStatData
import uz.alphazet.data.models.QRCodeAccessData
import uz.alphazet.data.models.UserData
import uz.alphazet.data.models.order.OrderInfo
import uz.alphazet.domain.repositories.OrderHistoryDataSource
import uz.alphazet.domain.repositories.ProfileRepo
import uz.alphazet.domain.repositories.QRCodeRepo
import uz.alphazet.hoopla.rules.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class QRCodeVMTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repo: QRCodeRepo = mockk()
    private val profileRepo: ProfileRepo = mockk()
    // dataSource is only used for paging (out of scope), use relaxed mock to
    // avoid constructor-call issues
    private val dataSource: OrderHistoryDataSource = mockk(relaxed = true)

    private val vm by lazy { QRCodeVM(repo, profileRepo, dataSource) }

    // --- generateQRCode (Pattern B) ---------------------------------------

    @Test
    fun generateQRCode_updates_qrCodeDataFlow() = runTest {
        val qrData = QRCodeAccessData("qr-token-abc", 1_893_456_000L)
        coEvery { repo.generateQRCode() } returns UIResource.Success(qrData)

        vm.qrCodeDataFlow.test {
            assertEquals(UIResource.Loading, awaitItem())
            vm.generateQRCode()
            val result = awaitItem()
            assertTrue(result is UIResource.Success)
            assertEquals("qr-token-abc", (result as UIResource.Success).data?.qrCode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- getDrinksStat (Pattern B) ----------------------------------------

    @Test
    fun getDrinksStat_updates_drinksStatDataFlow() = runTest {
        val stat = DailyDrinksStatData(available = 8, used = 2)
        coEvery { repo.getDrinksStat() } returns UIResource.Success(stat)

        vm.drinksStatDataFlow.test {
            assertEquals(UIResource.Loading, awaitItem())
            vm.getDrinksStat()
            val result = awaitItem()
            assertTrue(result is UIResource.Success)
            assertEquals(8, (result as UIResource.Success).data?.available)
            assertEquals(2, result.data?.used)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- getUser (Pattern B) ----------------------------------------------

    @Test
    fun getUser_updates_userDataFlow() = runTest {
        val user = UserData("Ali", null, 1000.0, 100.0, "UZS", null, null, 1, null, 0)
        coEvery { profileRepo.getMe() } returns UIResource.Success(user)

        vm.userDataFlow.test {
            assertEquals(UIResource.Loading, awaitItem())
            vm.getUser()
            val result = awaitItem()
            assertTrue(result is UIResource.Success)
            assertEquals("Ali", (result as UIResource.Success).data?.name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- getOrderInfo (Pattern B) ----------------------------------------

    @Test
    fun getOrderInfo_updates_orderInfoFlow() = runTest {
        val info = OrderInfo(50.0, 0.0, null, null, "Latte", null, 7, emptyList(), "completed", 25000.0, null, null, "Hoopla")
        coEvery { repo.getOrderInfo(7) } returns UIResource.Success(info)

        vm.orderInfoFlow.test {
            assertEquals(UIResource.Loading, awaitItem())
            vm.getOrderInfo(7)
            val result = awaitItem()
            assertTrue(result is UIResource.Success)
            assertEquals(7, (result as UIResource.Success).data?.id)
            assertEquals("completed", result.data?.orderStatus)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- cancelOrder (Pattern C) -----------------------------------------

    @Test
    fun cancelOrder_returns_success() = runTest {
        coEvery { repo.cancelOrder(9) } returns flowOf(UIResource.Success(Any()))

        val flow = vm.cancelOrder(9)
        flow.test {
            assertTrue(awaitItem() is UIResource.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

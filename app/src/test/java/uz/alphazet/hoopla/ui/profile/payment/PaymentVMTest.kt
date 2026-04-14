package uz.alphazet.hoopla.ui.profile.payment

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
import uz.alphazet.data.models.PaymentServiceCheckOutData
import uz.alphazet.data.models.PaymentServiceItemData
import uz.alphazet.domain.network.RemoteException
import uz.alphazet.domain.repositories.PaymentServiceRepo
import uz.alphazet.hoopla.rules.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentVMTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repo: PaymentServiceRepo = mockk()
    private val vm by lazy { PaymentVM(repo) }

    // --- getPaymentServices (Pattern C) -----------------------------------

    @Test
    fun getPaymentServices_returns_service_list() = runTest {
        val services = listOf(
            PaymentServiceItemData(1, "https://img/click.png", "Click"),
            PaymentServiceItemData(2, "https://img/payme.png", "Payme")
        )
        coEvery { repo.getPaymentServices() } returns flowOf(UIResource.Success(services))

        val flow = vm.getPaymentServices()
        flow.test {
            val result = awaitItem()
            assertTrue(result is UIResource.Success)
            assertEquals(2, (result as UIResource.Success).data?.size)
            assertEquals("Click", result.data?.first()?.name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- topUpViaPayService (Pattern A — MutableSharedFlow) ---------------

    @Test
    fun topUpViaPayService_emits_loading_then_success() = runTest {
        val checkout = PaymentServiceCheckOutData("https://click.uz/co")
        coEvery { repo.topUpViaPayService(1, 50000.0) } returns UIResource.Success(checkout)

        vm.topUpDataFlow.test {
            vm.topUpViaPayService(1, 50000.0)
            assertEquals(UIResource.Loading, awaitItem())
            val result = awaitItem()
            assertTrue(result is UIResource.Success)
            assertEquals("https://click.uz/co", (result as UIResource.Success).data?.checkoutUrl)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun topUpViaPayService_emits_loading_then_error_on_402() = runTest {
        coEvery { repo.topUpViaPayService(any(), any()) } returns UIResource.Error(
            RemoteException("payment required", 402)
        )

        vm.topUpDataFlow.test {
            vm.topUpViaPayService(1, 10.0)
            assertEquals(UIResource.Loading, awaitItem())
            val result = awaitItem()
            assertTrue(result is UIResource.Error)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

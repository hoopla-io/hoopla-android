package uz.alphazet.domain.repositories

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import uz.alphazet.data.BaseResponseData
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.PaymentServiceCheckOutData
import uz.alphazet.data.models.PaymentServiceItemData
import uz.alphazet.data.services.PaymentService
import uz.alphazet.domain.network.PaymentException
import uz.alphazet.domain.network.UnauthorizedException

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentServiceRepoTest {

    private val dispatcher = StandardTestDispatcher()
    private val api: PaymentService = mockk()
    private val repo = PaymentServiceRepo(api)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun getPaymentServices_success_emits_list() = runTest(dispatcher) {
        val items = listOf(
            PaymentServiceItemData(id = 1, logoUrl = "http://logo/1", name = "Click"),
            PaymentServiceItemData(id = 2, logoUrl = "http://logo/2", name = "Payme")
        )
        coEvery { api.getPaymentServices() } returns Response.success(wrap(items))

        repo.getPaymentServices().test {
            val item = awaitItem()
            assertTrue(item is UIResource.Success)
            assertEquals(items, (item as UIResource.Success).data)
            awaitComplete()
        }
        coVerify(exactly = 1) { api.getPaymentServices() }
    }

    @Test
    fun getPaymentServices_401_emits_unauthorized_error() = runTest(dispatcher) {
        coEvery { api.getPaymentServices() } returns Response.error(
            401,
            """{"message":"expired"}""".toResponseBody("application/json".toMediaType())
        )

        repo.getPaymentServices().test {
            val item = awaitItem()
            assertTrue(item is UIResource.Error)
            assertTrue((item as UIResource.Error).throwable is UnauthorizedException)
            awaitComplete()
        }
    }

    @Test
    fun topUpViaPayService_success_returns_checkout_url() = runTest(dispatcher) {
        val checkout = PaymentServiceCheckOutData(checkoutUrl = "https://pay/checkout")
        coEvery { api.topUpViaPayService(1, 5000.0) } returns Response.success(wrap(checkout))

        val result = repo.topUpViaPayService(id = 1, amount = 5000.0)

        assertTrue(result is UIResource.Success)
        assertEquals("https://pay/checkout", (result as UIResource.Success).data!!.checkoutUrl)
        coVerify(exactly = 1) { api.topUpViaPayService(1, 5000.0) }
    }

    @Test
    fun topUpViaPayService_forwards_id_and_amount_to_service() = runTest(dispatcher) {
        coEvery { api.topUpViaPayService(any(), any()) } returns Response.success(wrap(null))

        repo.topUpViaPayService(id = 88, amount = 123.45)

        coVerify(exactly = 1) { api.topUpViaPayService(88, 123.45) }
    }

    @Test
    fun topUpViaPayService_402_returns_payment_exception() = runTest(dispatcher) {
        val body = """
            {
              "data": {"amount":1500,"checkout_url":"https://pay/1","deeplink":"app://pay",
                      "expires_at":"2030-01-01","order_id":77,"short_link":"http://s"},
              "message":"payment required","status":false,"code":402,"meta":null
            }
        """.trimIndent()
        coEvery { api.topUpViaPayService(any(), any()) } returns Response.error(
            402,
            body.toResponseBody("application/json".toMediaType())
        )

        val result = repo.topUpViaPayService(id = 1, amount = 1500.0)

        assertTrue(result is UIResource.Error)
        assertTrue((result as UIResource.Error).throwable is PaymentException)
    }

    private fun <T> wrap(data: T?): BaseResponseData<T> = BaseResponseData(
        data = data,
        message = "ok",
        status = true,
        code = 200,
        meta = null
    )
}
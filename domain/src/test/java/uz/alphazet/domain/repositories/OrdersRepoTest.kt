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
import uz.alphazet.data.models.DailyDrinksStatData
import uz.alphazet.data.models.QRCodeAccessData
import uz.alphazet.data.models.order.OrderInfo
import uz.alphazet.data.services.OrdersService
import uz.alphazet.domain.network.ConflictException
import uz.alphazet.domain.network.NotFoundException
import uz.alphazet.domain.network.UnauthorizedException

@OptIn(ExperimentalCoroutinesApi::class)
class OrdersRepoTest {

    private val dispatcher = StandardTestDispatcher()
    private val api: OrdersService = mockk()
    private val repo = OrdersRepo(api)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun generateQRCode_success_returns_qr_data() = runTest(dispatcher) {
        val payload = QRCodeAccessData(token = "QR-123", expiresAt = 1_700_000_000L, orderId = 1)
        coEvery { api.generateQRCode() } returns Response.success(wrap(payload))

        val result = repo.generateQRCode()

        assertTrue(result is UIResource.Success)
        assertEquals(payload, (result as UIResource.Success).data)
        coVerify(exactly = 1) { api.generateQRCode() }
    }

    @Test
    fun generateQRCode_401_returns_unauthorized() = runTest(dispatcher) {
        coEvery { api.generateQRCode() } returns Response.error(
            401,
            """{"message":"expired"}""".toResponseBody("application/json".toMediaType())
        )

        val result = repo.generateQRCode()

        assertTrue(result is UIResource.Error)
        assertTrue((result as UIResource.Error).throwable is UnauthorizedException)
    }

    @Test
    fun getOrderInfo_success_returns_info_and_forwards_id() = runTest(dispatcher) {
        val info = OrderInfo(
            cashbackEarned = 100.0,
            cashbackUsed = 0.0,
            checkoutUrl = null,
            drinkImageUrl = null,
            drinkName = "Latte",
            fiscalLink = null,
            id = 55,
            items = emptyList(),
            orderStatus = "PAID",
            productPrice = 15_000.0,
            purchasedAt = "2026-04-20",
            purchasedAtUnix = 1_700_000_000L,
            shopName = "Hoopla"
        )
        coEvery { api.getOrderInfo(55) } returns Response.success(wrap(info))

        val result = repo.getOrderInfo(55)

        assertTrue(result is UIResource.Success)
        assertEquals(info, (result as UIResource.Success).data)
        coVerify(exactly = 1) { api.getOrderInfo(55) }
    }

    @Test
    fun getOrderInfo_404_returns_not_found() = runTest(dispatcher) {
        coEvery { api.getOrderInfo(any()) } returns Response.error(
            404,
            """{"message":"no order"}""".toResponseBody("application/json".toMediaType())
        )

        val result = repo.getOrderInfo(42)

        assertTrue(result is UIResource.Error)
        assertTrue((result as UIResource.Error).throwable is NotFoundException)
    }

    @Test
    fun cancelOrder_success_emits_success_and_forwards_id() = runTest(dispatcher) {
        coEvery { api.cancelOrder(9) } returns Response.success(wrap<Any>(null))

        repo.cancelOrder(9).test {
            val item = awaitItem()
            assertTrue(item is UIResource.Success)
            awaitComplete()
        }
        coVerify(exactly = 1) { api.cancelOrder(9) }
    }

    @Test
    fun cancelOrder_409_emits_conflict_error() = runTest(dispatcher) {
        coEvery { api.cancelOrder(any()) } returns Response.error(
            409,
            """{"message":"already processed"}""".toResponseBody("application/json".toMediaType())
        )

        repo.cancelOrder(9).test {
            val item = awaitItem()
            assertTrue(item is UIResource.Error)
            assertTrue((item as UIResource.Error).throwable is ConflictException)
            awaitComplete()
        }
    }

    @Test
    fun getDrinksStat_success_returns_stats() = runTest(dispatcher) {
        val stat = DailyDrinksStatData(available = 3, used = 2)
        coEvery { api.getDrinksStat() } returns Response.success(wrap(stat))

        val result = repo.getDrinksStat()

        assertTrue(result is UIResource.Success)
        assertEquals(stat, (result as UIResource.Success).data)
        coVerify(exactly = 1) { api.getDrinksStat() }
    }

    private fun <T> wrap(data: T?): BaseResponseData<T> = BaseResponseData(
        data = data,
        message = "ok",
        status = true,
        code = 200,
        meta = null
    )
}
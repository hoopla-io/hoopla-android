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
import uz.alphazet.data.models.ShopData
import uz.alphazet.data.services.ShopService
import uz.alphazet.domain.network.NotFoundException
import uz.alphazet.domain.network.ServerErrorException

@OptIn(ExperimentalCoroutinesApi::class)
class ShopRepoTest {

    private val dispatcher = StandardTestDispatcher()
    private val service: ShopService = mockk()
    private val repo = ShopRepo(service)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun getShopDetail_success_emits_success_with_shop() = runTest(dispatcher) {
        val shop = ShopData(
            id = 42,
            partnerId = 7,
            name = "Hoopla Chilonzor",
            pictureUrl = null,
            canAcceptOrders = true,
            location = null,
            phoneNumbers = null,
            workingHours = null,
            pictures = null,
            drinks = null,
            urls = null
        )
        coEvery { service.getShopDetail(42) } returns Response.success(wrap(shop))

        repo.getShopDetail(42).test {
            val item = awaitItem()
            assertTrue(item is UIResource.Success)
            assertEquals(shop, (item as UIResource.Success).data)
            awaitComplete()
        }
        coVerify(exactly = 1) { service.getShopDetail(42) }
    }

    @Test
    fun getShopDetail_passes_shopId_to_service() = runTest(dispatcher) {
        coEvery { service.getShopDetail(any()) } returns Response.success(wrap(null))

        repo.getShopDetail(999).test {
            awaitItem()
            awaitComplete()
        }
        coVerify(exactly = 1) { service.getShopDetail(999) }
    }

    @Test
    fun getShopDetail_404_emits_not_found_error() = runTest(dispatcher) {
        coEvery { service.getShopDetail(any()) } returns Response.error(
            404,
            """{"message":"shop not found"}""".toResponseBody("application/json".toMediaType())
        )

        repo.getShopDetail(1).test {
            val item = awaitItem()
            assertTrue(item is UIResource.Error)
            assertTrue((item as UIResource.Error).throwable is NotFoundException)
            awaitComplete()
        }
    }

    @Test
    fun getShopDetail_500_emits_server_error() = runTest(dispatcher) {
        coEvery { service.getShopDetail(any()) } returns Response.error(
            500,
            """{"message":"boom"}""".toResponseBody("application/json".toMediaType())
        )

        repo.getShopDetail(1).test {
            val item = awaitItem()
            assertTrue(item is UIResource.Error)
            assertTrue((item as UIResource.Error).throwable is ServerErrorException)
            awaitComplete()
        }
    }

    private fun <T> wrap(data: T?): BaseResponseData<T> = BaseResponseData(
        data = data,
        message = "ok",
        status = true,
        code = 200,
        meta = null
    )
}
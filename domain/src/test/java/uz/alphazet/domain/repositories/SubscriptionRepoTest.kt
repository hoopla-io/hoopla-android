package uz.alphazet.domain.repositories

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Response
import uz.alphazet.data.BaseResponseData
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.SubscriptionItemData
import uz.alphazet.data.services.SubscriptionService
import uz.alphazet.domain.network.PaymentException
import uz.alphazet.domain.network.UnauthorizedException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SubscriptionRepoTest {

    private val dispatcher = StandardTestDispatcher()
    private val api: SubscriptionService = mockk()
    private val repo = SubscriptionRepo(api)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun getSubscriptions_success_emits_list() = runTest(dispatcher) {
        val plans = listOf(
            SubscriptionItemData(
                currency = "UZS", days = 30, id = 1, name = "Basic",
                price = 49_000.0, features = emptyList()
            )
        )
        coEvery { api.getSubscriptions() } returns Response.success(wrap(plans))

        repo.getSubscriptions().test {
            val item = awaitItem()
            assertTrue(item is UIResource.Success)
            assertEquals(plans, (item as UIResource.Success).data)
            awaitComplete()
        }
        coVerify(exactly = 1) { api.getSubscriptions() }
    }

    @Test
    fun getSubscriptions_401_emits_unauthorized_error() = runTest(dispatcher) {
        coEvery { api.getSubscriptions() } returns Response.error(
            401,
            """{"message":"expired"}""".toResponseBody("application/json".toMediaType())
        )

        repo.getSubscriptions().test {
            val item = awaitItem()
            assertTrue(item is UIResource.Error)
            assertTrue((item as UIResource.Error).throwable is UnauthorizedException)
            awaitComplete()
        }
    }

    @Test
    fun buySubscription_sends_subscriptionId_in_json_body() = runTest(dispatcher) {
        val bodySlot = slot<RequestBody>()
        coEvery { api.buySubscription(capture(bodySlot)) } returns Response.success(wrap<Any>(null))

        repo.buySubscription(subscriptionId = 42).test { awaitItem(); awaitComplete() }

        val json = JSONObject(bodySlot.captured.asString())
        assertEquals(42, json.getInt("subscriptionId"))
    }

    @Test
    fun buySubscription_success_emits_success() = runTest(dispatcher) {
        coEvery { api.buySubscription(any()) } returns Response.success(wrap<Any>(null))

        repo.buySubscription(1).test {
            val item = awaitItem()
            assertTrue(item is UIResource.Success)
            awaitComplete()
        }
    }

    @Test
    fun buySubscription_402_emits_payment_required_error() = runTest(dispatcher) {
        val body = """
            {
              "data": {"amount":49000,"checkout_url":"https://pay/sub","deeplink":"app://pay",
                      "expires_at":"2030-01-01","order_id":1,"short_link":"http://s"},
              "message":"payment required","status":false,"code":402,"meta":null
            }
        """.trimIndent()
        coEvery { api.buySubscription(any()) } returns Response.error(
            402,
            body.toResponseBody("application/json".toMediaType())
        )

        repo.buySubscription(1).test {
            val item = awaitItem()
            assertTrue(item is UIResource.Error)
            assertTrue((item as UIResource.Error).throwable is PaymentException)
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

    private fun RequestBody.asString(): String =
        Buffer().also { writeTo(it) }.readUtf8()
}
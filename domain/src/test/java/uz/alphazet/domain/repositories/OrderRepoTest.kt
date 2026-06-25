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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Response
import uz.alphazet.data.BaseResponseData
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.order.CheckOutInfo
import uz.alphazet.data.models.order.ModifierItemData
import uz.alphazet.data.models.order.OrderDetails
import uz.alphazet.data.models.order.OrderInfoData
import uz.alphazet.data.services.OrderService
import uz.alphazet.domain.network.PaymentException
import uz.alphazet.domain.network.UnauthorizedException
import uz.alphazet.domain.network.ValidationException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class OrderRepoTest {

    private val dispatcher = StandardTestDispatcher()
    private val service: OrderService = mockk()
    private val repo = OrderRepo(service)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- validateOrder ---

    @Test
    fun validateOrder_sends_shopId_and_drinkId_in_json_body() = runTest(dispatcher) {
        val slot = slot<RequestBody>()
        coEvery { service.validateOrder(capture(slot)) } returns Response.success(
            wrap(sampleOrderDetails())
        )

        repo.validateOrder(shopId = 7, drinkId = 123)
            .test { awaitItem(); awaitComplete() }

        val json = JSONObject(slot.captured.asString())
        assertEquals(7, json.getInt("shopId"))
        assertEquals(123, json.getInt("drinkId"))
        coVerify(exactly = 1) { service.validateOrder(any()) }
    }

    @Test
    fun validateOrder_success_emits_order_details() = runTest(dispatcher) {
        val details = sampleOrderDetails()
        coEvery { service.validateOrder(any()) } returns Response.success(wrap(details))

        repo.validateOrder(1, 2).test {
            val item = awaitItem()
            assertTrue(item is UIResource.Success)
            assertEquals(details, (item as UIResource.Success).data)
            awaitComplete()
        }
    }

    @Test
    fun validateOrder_422_emits_validation_error() = runTest(dispatcher) {
        coEvery { service.validateOrder(any()) } returns Response.error(
            422,
            """{"message":"invalid"}""".toResponseBody("application/json".toMediaType())
        )

        repo.validateOrder(1, 2).test {
            val item = awaitItem()
            assertTrue(item is UIResource.Error)
            assertTrue((item as UIResource.Error).throwable is ValidationException)
            awaitComplete()
        }
    }

    // --- createOrder ---

    @Test
    fun createOrder_serialises_modifiers_array() = runTest(dispatcher) {
        val slot = slot<RequestBody>()
        coEvery { service.createOrder(capture(slot)) } returns Response.success(
            wrap(OrderInfoData(1, "Hoopla", "Chilonzor", "now", 1L, "Latte", "PENDING"))
        )

        val modifiers = arrayListOf(
            ModifierItemData(
                modifierId = "m1", modifierGroupId = "g1",
                modifierKey = "size", modifierPrice = 2_000.0
            ),
            ModifierItemData(
                modifierId = "m2", modifierGroupId = "g2",
                modifierKey = "syrup", modifierPrice = 1_500.0
            )
        )

        repo.createOrder(shopId = 5, drinkId = 77, modifiers = modifiers)
            .test { awaitItem(); awaitComplete() }

        val json = JSONObject(slot.captured.asString())
        assertEquals(5, json.getInt("shopId"))
        assertEquals(77, json.getInt("drinkId"))
        val arr = json.getJSONArray("modifiers")
        assertEquals(2, arr.length())
        val first = arr.getJSONObject(0)
        assertEquals("m1", first.getString("modifierId"))
        assertEquals("g1", first.getString("modifierGroupId"))
        assertEquals("size", first.getString("modifierKey"))
        assertEquals(2_000.0, first.getDouble("modifierPrice"), 0.0)
    }

    @Test
    fun createOrder_includes_comment_when_provided() = runTest(dispatcher) {
        val slot = slot<RequestBody>()
        coEvery { service.createOrder(capture(slot)) } returns Response.success(
            wrap(OrderInfoData(1, null, null, null, null, null, null))
        )

        repo.createOrder(shopId = 1, drinkId = 2, modifiers = arrayListOf(), comment = "  no sugar  ")
            .test { awaitItem(); awaitComplete() }

        val json = JSONObject(slot.captured.asString())
        assertEquals("no sugar", json.getString("comment"))
    }

    @Test
    fun createOrder_omits_comment_when_blank_or_null() = runTest(dispatcher) {
        val slot = slot<RequestBody>()
        coEvery { service.createOrder(capture(slot)) } returns Response.success(
            wrap(OrderInfoData(1, null, null, null, null, null, null))
        )

        repo.createOrder(shopId = 1, drinkId = 2, modifiers = arrayListOf(), comment = "   ")
            .test { awaitItem(); awaitComplete() }

        assertFalse(JSONObject(slot.captured.asString()).has("comment"))
    }

    @Test
    fun createOrder_with_empty_modifiers_sends_empty_array() = runTest(dispatcher) {
        val slot = slot<RequestBody>()
        coEvery { service.createOrder(capture(slot)) } returns Response.success(
            wrap(OrderInfoData(1, null, null, null, null, null, null))
        )

        repo.createOrder(shopId = 1, drinkId = 2, modifiers = arrayListOf())
            .test { awaitItem(); awaitComplete() }

        val json = JSONObject(slot.captured.asString())
        assertEquals(0, json.getJSONArray("modifiers").length())
    }

    @Test
    fun createOrder_success_emits_order_info() = runTest(dispatcher) {
        val info = OrderInfoData(1, "Hoopla", "Chilonzor", "now", 1L, "Latte", "PENDING")
        coEvery { service.createOrder(any()) } returns Response.success(wrap(info))

        repo.createOrder(1, 2, arrayListOf()).test {
            val item = awaitItem()
            assertTrue(item is UIResource.Success)
            assertEquals(info, (item as UIResource.Success).data)
            awaitComplete()
        }
    }

    @Test
    fun createOrder_401_emits_unauthorized_error() = runTest(dispatcher) {
        coEvery { service.createOrder(any()) } returns Response.error(
            401,
            """{"message":"expired"}""".toResponseBody("application/json".toMediaType())
        )

        repo.createOrder(1, 2, arrayListOf()).test {
            val item = awaitItem()
            assertTrue(item is UIResource.Error)
            assertTrue((item as UIResource.Error).throwable is UnauthorizedException)
            awaitComplete()
        }
    }

    // --- createOrderRahmat ---

    @Test
    fun createOrderRahmat_includes_cashback_fields_and_modifiers() = runTest(dispatcher) {
        val slot = slot<RequestBody>()
        coEvery { service.createOrderRahmat(capture(slot)) } returns Response.success(
            wrap(CheckOutInfo(1500, "http://co", "app://co", "exp", 1, "http://s"))
        )

        val modifiers = arrayListOf(
            ModifierItemData(
                modifierId = "m1", modifierGroupId = "g1",
                modifierKey = "size", modifierPrice = 2_000.0
            )
        )

        repo.createOrderRahmat(
            shopId = 9,
            drinkId = 4,
            modifiers = modifiers,
            useCashback = true,
            cashbackAmount = 500.0
        ).test { awaitItem(); awaitComplete() }

        val json = JSONObject(slot.captured.asString())
        assertEquals(9, json.getInt("shopId"))
        assertEquals(4, json.getInt("drinkId"))
        assertEquals(true, json.getBoolean("use_cashback"))
        assertEquals(500.0, json.getDouble("cashback_amount"), 0.0)
        assertEquals(1, json.getJSONArray("modifiers").length())
    }

    @Test
    fun createOrderRahmat_includes_comment_when_provided() = runTest(dispatcher) {
        val slot = slot<RequestBody>()
        coEvery { service.createOrderRahmat(capture(slot)) } returns Response.success(
            wrap(CheckOutInfo(null, null, null, null, null, null))
        )

        repo.createOrderRahmat(
            shopId = 1, drinkId = 2, modifiers = arrayListOf(),
            useCashback = false, cashbackAmount = 0.0, comment = "  extra hot  "
        ).test { awaitItem(); awaitComplete() }

        assertEquals("extra hot", JSONObject(slot.captured.asString()).getString("comment"))
    }

    @Test
    fun createOrderRahmat_omits_comment_when_blank_or_null() = runTest(dispatcher) {
        val slot = slot<RequestBody>()
        coEvery { service.createOrderRahmat(capture(slot)) } returns Response.success(
            wrap(CheckOutInfo(null, null, null, null, null, null))
        )

        repo.createOrderRahmat(1, 2, arrayListOf(), useCashback = false, cashbackAmount = 0.0)
            .test { awaitItem(); awaitComplete() }

        assertFalse(JSONObject(slot.captured.asString()).has("comment"))
    }

    @Test
    fun createOrderRahmat_with_useCashback_false_serialises_false() = runTest(dispatcher) {
        val slot = slot<RequestBody>()
        coEvery { service.createOrderRahmat(capture(slot)) } returns Response.success(
            wrap(CheckOutInfo(null, null, null, null, null, null))
        )

        repo.createOrderRahmat(1, 2, arrayListOf(), useCashback = false, cashbackAmount = 0.0)
            .test { awaitItem(); awaitComplete() }

        val json = JSONObject(slot.captured.asString())
        assertEquals(false, json.getBoolean("use_cashback"))
        assertEquals(0.0, json.getDouble("cashback_amount"), 0.0)
    }

    @Test
    fun createOrderRahmat_success_emits_checkout_info() = runTest(dispatcher) {
        val checkout = CheckOutInfo(1500, "http://co", "app://co", "exp", 1, "http://s")
        coEvery { service.createOrderRahmat(any()) } returns Response.success(wrap(checkout))

        repo.createOrderRahmat(1, 2, arrayListOf(), true, 0.0).test {
            val item = awaitItem()
            assertTrue(item is UIResource.Success)
            assertEquals(checkout, (item as UIResource.Success).data)
            awaitComplete()
        }
    }

    @Test
    fun createOrderRahmat_402_emits_payment_error() = runTest(dispatcher) {
        val body = """
            {
              "data": {"amount":1500,"checkout_url":"https://pay/co","deeplink":"app://pay",
                      "expires_at":"2030-01-01","order_id":1,"short_link":"http://s"},
              "message":"payment required","status":false,"code":402,"meta":null
            }
        """.trimIndent()
        coEvery { service.createOrderRahmat(any()) } returns Response.error(
            402,
            body.toResponseBody("application/json".toMediaType())
        )

        repo.createOrderRahmat(1, 2, arrayListOf(), true, 500.0).test {
            val item = awaitItem()
            assertTrue(item is UIResource.Error)
            assertTrue((item as UIResource.Error).throwable is PaymentException)
            awaitComplete()
        }
    }

    private fun sampleOrderDetails(): OrderDetails = OrderDetails(
        modifications = OrderDetails.Modification(null, null, null, null),
        drink = OrderDetails.Drink(id = 123, name = "Latte", amount = 15_000.0, imageUrl = null),
        shop = OrderDetails.Shop(id = 7, name = "Hoopla"),
        validatedAt = "now",
        validatedAtUnix = 1,
        cashbackPercent = null
    )

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
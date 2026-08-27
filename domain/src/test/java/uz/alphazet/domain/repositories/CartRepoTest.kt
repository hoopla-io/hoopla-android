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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Response
import uz.alphazet.data.BaseResponseData
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.cart.CartCountData
import uz.alphazet.data.models.cart.CartData
import uz.alphazet.data.models.cart.CartItemData
import uz.alphazet.data.models.order.CheckOutInfo
import uz.alphazet.data.models.order.ModifierItemData
import uz.alphazet.data.services.CartService
import uz.alphazet.domain.network.ConflictException
import uz.alphazet.domain.network.PaymentException
import uz.alphazet.domain.network.UnauthorizedException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CartRepoTest {

    private val dispatcher = StandardTestDispatcher()
    private val service: CartService = mockk()
    private val repo = CartRepo(service)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- getCart (returns the resource directly, for the screen's StateFlow) ---

    @Test
    fun getCart_success_returns_the_cart() = runTest(dispatcher) {
        val cart = sampleCart()
        coEvery { service.getCart() } returns Response.success(wrap(cart))

        val result = repo.getCart()

        assertTrue(result is UIResource.Success)
        assertEquals(cart, (result as UIResource.Success).data)
    }

    @Test
    fun getCart_401_returns_unauthorized_error() = runTest(dispatcher) {
        coEvery { service.getCart() } returns Response.error(
            401,
            """{"message":"expired"}""".toResponseBody("application/json".toMediaType())
        )

        val result = repo.getCart()

        assertTrue(result is UIResource.Error)
        assertTrue((result as UIResource.Error).throwable is UnauthorizedException)
    }

    @Test
    fun getCartCount_emits_the_count() = runTest(dispatcher) {
        coEvery { service.getCartCount() } returns Response.success(wrap(CartCountData(3)))

        repo.getCartCount().test {
            val item = awaitItem()
            assertTrue(item is UIResource.Success)
            assertEquals(3, (item as UIResource.Success).data?.count)
            awaitComplete()
        }
    }

    // --- addItem ---

    @Test
    fun addItem_serialises_shop_drink_quantity_and_modifiers() = runTest(dispatcher) {
        val slot = slot<RequestBody>()
        coEvery { service.addItem(capture(slot)) } returns Response.success(wrap(sampleCart()))

        val modifiers = arrayListOf(
            ModifierItemData(
                modifierId = "m1", modifierGroupId = "g1",
                modifierKey = "milk", modifierPrice = 5_000.0
            )
        )

        repo.addItem(shopId = 3, drinkId = 5, quantity = 2, modifiers = modifiers)
            .test { awaitItem(); awaitComplete() }

        val json = JSONObject(slot.captured.asString())
        assertEquals(3, json.getInt("shopId"))
        assertEquals(5, json.getInt("drinkId"))
        assertEquals(2, json.getInt("quantity"))

        val arr = json.getJSONArray("modifiers")
        assertEquals(1, arr.length())
        val first = arr.getJSONObject(0)
        assertEquals("m1", first.getString("modifierId"))
        assertEquals("g1", first.getString("modifierGroupId"))
        assertEquals("milk", first.getString("modifierKey"))
        assertEquals(5_000.0, first.getDouble("modifierPrice"), 0.0)

        coVerify(exactly = 1) { service.addItem(any()) }
    }

    @Test
    fun addItem_with_no_modifiers_sends_an_empty_array() = runTest(dispatcher) {
        val slot = slot<RequestBody>()
        coEvery { service.addItem(capture(slot)) } returns Response.success(wrap(sampleCart()))

        repo.addItem(1, 2, 1, arrayListOf()).test { awaitItem(); awaitComplete() }

        assertEquals(0, JSONObject(slot.captured.asString()).getJSONArray("modifiers").length())
    }

    /** The cross-shop conflict is the only signal the API gives, so it must survive mapping. */
    @Test
    fun addItem_409_emits_a_conflict_error() = runTest(dispatcher) {
        coEvery { service.addItem(any()) } returns Response.error(
            409,
            """{"message":"cart belongs to another shop"}"""
                .toResponseBody("application/json".toMediaType())
        )

        repo.addItem(1, 2, 1, arrayListOf()).test {
            val item = awaitItem()
            assertTrue(item is UIResource.Error)
            val throwable = (item as UIResource.Error).throwable
            assertTrue(throwable is ConflictException)
            assertEquals("cart belongs to another shop", throwable.message)
            awaitComplete()
        }
    }

    // --- quantity / removal ---

    @Test
    fun updateItemQuantity_sends_the_quantity_and_the_item_id() = runTest(dispatcher) {
        val slot = slot<RequestBody>()
        coEvery { service.updateItemQuantity(eq(11), capture(slot)) } returns
            Response.success(wrap(sampleCart()))

        repo.updateItemQuantity(itemId = 11, quantity = 3)
            .test { awaitItem(); awaitComplete() }

        assertEquals(3, JSONObject(slot.captured.asString()).getInt("quantity"))
        coVerify(exactly = 1) { service.updateItemQuantity(11, any()) }
    }

    /** Stepping a line below one is expressed as quantity 0, not as a delete call. */
    @Test
    fun updateItemQuantity_allows_zero_to_drop_the_line() = runTest(dispatcher) {
        val slot = slot<RequestBody>()
        coEvery { service.updateItemQuantity(any(), capture(slot)) } returns
            Response.success(wrap(sampleCart(items = emptyList())))

        repo.updateItemQuantity(itemId = 11, quantity = 0)
            .test { awaitItem(); awaitComplete() }

        assertEquals(0, JSONObject(slot.captured.asString()).getInt("quantity"))
    }

    @Test
    fun removeItem_calls_the_service_with_the_item_id() = runTest(dispatcher) {
        coEvery { service.removeItem(11) } returns Response.success(wrap(sampleCart()))

        repo.removeItem(11).test { awaitItem(); awaitComplete() }

        coVerify(exactly = 1) { service.removeItem(11) }
    }

    @Test
    fun clearCart_calls_the_service() = runTest(dispatcher) {
        coEvery { service.clearCart() } returns Response.success(wrap<Any>(null))

        repo.clearCart().test {
            assertTrue(awaitItem() is UIResource.Success)
            awaitComplete()
        }

        coVerify(exactly = 1) { service.clearCart() }
    }

    // --- promocode ---

    @Test
    fun applyPromo_trims_the_code() = runTest(dispatcher) {
        val slot = slot<RequestBody>()
        coEvery { service.applyPromo(capture(slot)) } returns Response.success(wrap(sampleCart()))

        repo.applyPromo("  SUMMER25  ").test { awaitItem(); awaitComplete() }

        assertEquals("SUMMER25", JSONObject(slot.captured.asString()).getString("code"))
    }

    @Test
    fun removePromo_calls_the_service() = runTest(dispatcher) {
        coEvery { service.removePromo() } returns Response.success(wrap(sampleCart()))

        repo.removePromo().test { awaitItem(); awaitComplete() }

        coVerify(exactly = 1) { service.removePromo() }
    }

    // --- comment ---

    @Test
    fun setComment_trims_the_note() = runTest(dispatcher) {
        val slot = slot<RequestBody>()
        coEvery { service.setComment(capture(slot)) } returns Response.success(wrap(sampleCart()))

        repo.setComment("  no sugar  ").test { awaitItem(); awaitComplete() }

        assertEquals("no sugar", JSONObject(slot.captured.asString()).getString("comment"))
    }

    /**
     * Clearing the note must reach the server as an explicit null. Dropping the key would send
     * `{}` and silently leave the old comment on the cart.
     */
    @Test
    fun setComment_sends_an_explicit_null_when_cleared() = runTest(dispatcher) {
        val slot = slot<RequestBody>()
        coEvery { service.setComment(capture(slot)) } returns Response.success(wrap(sampleCart()))

        repo.setComment("   ").test { awaitItem(); awaitComplete() }

        val json = JSONObject(slot.captured.asString())
        assertTrue(json.has("comment"))
        assertTrue(json.isNull("comment"))
    }

    @Test
    fun setComment_sends_an_explicit_null_for_a_null_note() = runTest(dispatcher) {
        val slot = slot<RequestBody>()
        coEvery { service.setComment(capture(slot)) } returns Response.success(wrap(sampleCart()))

        repo.setComment(null).test { awaitItem(); awaitComplete() }

        assertTrue(JSONObject(slot.captured.asString()).isNull("comment"))
    }

    // --- checkout ---

    @Test
    fun checkout_sends_the_cashback_choice() = runTest(dispatcher) {
        val slot = slot<RequestBody>()
        coEvery { service.checkout(capture(slot)) } returns Response.success(
            wrap(CheckOutInfo(55_000, "https://pay/abc", null, null, 42, null))
        )

        repo.checkout(useCashback = true, cashbackAmount = 2_000.0)
            .test { awaitItem(); awaitComplete() }

        val json = JSONObject(slot.captured.asString())
        assertTrue(json.getBoolean("use_cashback"))
        assertEquals(2_000.0, json.getDouble("cashback_amount"), 0.0)
    }

    @Test
    fun checkout_success_emits_the_check_out_info() = runTest(dispatcher) {
        val info = CheckOutInfo(55_000, "https://pay/abc", "hoopla://pay/abc", null, 42, null)
        coEvery { service.checkout(any()) } returns Response.success(wrap(info))

        repo.checkout(false, 0.0).test {
            val item = awaitItem()
            assertTrue(item is UIResource.Success)
            assertEquals(info, (item as UIResource.Success).data)
            awaitComplete()
        }
    }

    /** Payment still needed: the same 402 hand-off the single-item checkout already uses. */
    @Test
    fun checkout_402_emits_a_payment_error_carrying_the_checkout_url() = runTest(dispatcher) {
        coEvery { service.checkout(any()) } returns Response.error(
            402,
            """{"data":{"checkout_url":"https://pay/abc","deeplink":"hoopla://pay/abc"},
                |"message":"payment required"}""".trimMargin()
                .toResponseBody("application/json".toMediaType())
        )

        repo.checkout(false, 0.0).test {
            val item = awaitItem()
            assertTrue(item is UIResource.Error)
            val throwable = (item as UIResource.Error).throwable
            assertTrue(throwable is PaymentException)
            assertEquals("https://pay/abc", (throwable as PaymentException).errorData?.checkoutUrl)
            awaitComplete()
        }
    }

    @Test
    fun checkout_409_emits_a_conflict_error() = runTest(dispatcher) {
        coEvery { service.checkout(any()) } returns Response.error(
            409,
            """{"message":"shop is closed"}""".toResponseBody("application/json".toMediaType())
        )

        repo.checkout(false, 0.0).test {
            val item = awaitItem()
            assertTrue(item is UIResource.Error)
            assertTrue((item as UIResource.Error).throwable is ConflictException)
            awaitComplete()
        }
    }

    @Test
    fun a_null_data_body_still_succeeds_with_null() = runTest(dispatcher) {
        coEvery { service.getCart() } returns Response.success(wrap<CartData>(null))

        val result = repo.getCart()

        assertTrue(result is UIResource.Success)
        assertNull((result as UIResource.Success).data)
    }

    // --- helpers ---

    private fun sampleCart(items: List<CartItemData> = listOf(sampleItem())) = CartData(
        id = 1,
        shopId = 3,
        partnerId = 9,
        status = "active",
        promoCode = null,
        comment = null,
        items = items,
        subtotal = 55_000.0,
        promoDiscount = 0.0,
        total = 55_000.0
    )

    private fun sampleItem() = CartItemData(
        id = 11,
        drinkId = 5,
        name = "Cappuccino",
        imageUrl = "https://img/cappuccino.png",
        quantity = 2,
        unitPrice = 25_000.0,
        lineTotal = 50_000.0,
        modifiers = emptyList()
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

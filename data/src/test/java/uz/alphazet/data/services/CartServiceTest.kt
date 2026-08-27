package uz.alphazet.data.services

import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class CartServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: CartService

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        service = retrofitFor(server)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun jsonBody(json: String) =
        json.toRequestBody("application/json".toMediaType())

    private val cartJson = """
        {
          "data": {
            "id": 1, "shopId": 3, "partnerId": 9, "status": "active",
            "promoCode": null, "comment": null,
            "items": [
              { "id": 11, "drinkId": 5, "name": "Cappuccino",
                "imageUrl": "https://img/cappuccino.png", "quantity": 2,
                "unitPrice": 25000.0, "lineTotal": 50000.0,
                "modifiers": [ { "name": "Oat milk", "price": 5000.0 } ] }
            ],
            "subtotal": 55000.0, "promoDiscount": 0, "total": 55000.0
          },
          "message": "ok", "status": true, "code": 200, "meta": null
        }
    """.trimIndent()

    @Test
    fun getCart_gets_cart_and_parses_items_and_totals() = runTest {
        server.enqueue(mockOk(cartJson))

        val response = service.getCart()

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/v1/user/cart", recorded.path)

        val cart = response.body()?.data
        assertNotNull(cart)
        assertEquals(3, cart!!.shopId)
        assertEquals(55000.0, cart.total!!, 0.0)
        assertEquals(1, cart.items!!.size)

        val item = cart.items!!.first()
        assertEquals(11, item.id)
        assertEquals("Cappuccino", item.name)
        assertEquals("https://img/cappuccino.png", item.imageUrl)
        assertEquals(2, item.quantity)
        assertEquals(50000.0, item.lineTotal!!, 0.0)
        assertEquals("Oat milk", item.modifiers!!.first().name)
        assertEquals(5000.0, item.modifiers!!.first().price!!, 0.0)
    }

    @Test
    fun getCartCount_parses_the_count() = runTest {
        server.enqueue(
            mockOk("""{"data":{"count":4},"message":"ok","status":true,"code":200,"meta":null}""")
        )

        val response = service.getCartCount()

        assertEquals("/v1/user/cart/count", server.takeRequest().path)
        assertEquals(4, response.body()?.data?.count)
    }

    @Test
    fun addItem_posts_the_body_to_cart_items() = runTest {
        server.enqueue(mockOk(cartJson))

        val body = """{"shopId":3,"drinkId":5,"quantity":1,"modifiers":[]}"""
        service.addItem(jsonBody(body))

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/v1/user/cart/items", recorded.path)
        assertEquals(body, recorded.body.readUtf8())
    }

    @Test
    fun addItem_surfaces_the_cross_shop_conflict_as_409() = runTest {
        server.enqueue(mockError(409, """{"message":"cart belongs to another shop"}"""))

        val response = service.addItem(jsonBody("""{"shopId":4,"drinkId":5,"quantity":1}"""))

        assertEquals(409, response.code())
        assertNull(response.body())
    }

    @Test
    fun updateItemQuantity_patches_the_item_path() = runTest {
        server.enqueue(mockOk(cartJson))

        val body = """{"quantity":0}"""
        service.updateItemQuantity(11, jsonBody(body))

        val recorded = server.takeRequest()
        assertEquals("PATCH", recorded.method)
        assertEquals("/v1/user/cart/items/11", recorded.path)
        assertEquals(body, recorded.body.readUtf8())
    }

    @Test
    fun removeItem_deletes_the_item_path() = runTest {
        server.enqueue(mockOk(cartJson))

        service.removeItem(11)

        val recorded = server.takeRequest()
        assertEquals("DELETE", recorded.method)
        assertEquals("/v1/user/cart/items/11", recorded.path)
    }

    @Test
    fun clearCart_deletes_the_cart() = runTest {
        server.enqueue(mockOk("""{"data":null,"message":"ok","status":true,"code":200,"meta":null}"""))

        service.clearCart()

        val recorded = server.takeRequest()
        assertEquals("DELETE", recorded.method)
        assertEquals("/v1/user/cart", recorded.path)
    }

    @Test
    fun applyPromo_posts_and_returns_the_repriced_cart() = runTest {
        server.enqueue(
            mockOk(
                cartJson.replace("\"promoCode\": null", "\"promoCode\": \"SUMMER25\"")
                    .replace("\"promoDiscount\": 0", "\"promoDiscount\": 5000.0")
            )
        )

        service.applyPromo(jsonBody("""{"code":"SUMMER25"}"""))
            .body()?.data.let { cart ->
                assertEquals("SUMMER25", cart?.promoCode)
                assertEquals(5000.0, cart?.promoDiscount!!, 0.0)
            }

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/v1/user/cart/promo", recorded.path)
    }

    @Test
    fun removePromo_deletes_the_promo_path() = runTest {
        server.enqueue(mockOk(cartJson))

        service.removePromo()

        val recorded = server.takeRequest()
        assertEquals("DELETE", recorded.method)
        assertEquals("/v1/user/cart/promo", recorded.path)
    }

    @Test
    fun setComment_posts_to_the_comment_path() = runTest {
        server.enqueue(mockOk(cartJson))

        val body = """{"comment":"no sugar"}"""
        service.setComment(jsonBody(body))

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/v1/user/cart/comment", recorded.path)
        assertEquals(body, recorded.body.readUtf8())
    }

    @Test
    fun checkout_posts_to_cart_checkout_and_parses_check_out_info() = runTest {
        server.enqueue(
            mockOk(
                """
                {
                  "data": {
                    "amount": 55000,
                    "checkout_url": "https://pay.example/abc",
                    "deeplink": "hoopla://pay/abc",
                    "expires_at": "2026-08-04T12:00:00Z",
                    "order_id": 42,
                    "short_link": "https://h.uz/abc"
                  },
                  "message": "ok", "status": true, "code": 200, "meta": null
                }
                """.trimIndent()
            )
        )

        val response = service.checkout(jsonBody("""{"use_cashback":false,"cashback_amount":0}"""))

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/v1/user/orders/cart-checkout", recorded.path)

        val info = response.body()?.data
        assertNotNull(info)
        assertEquals(42, info!!.order_id)
        assertEquals("https://pay.example/abc", info.checkout_url)
        assertEquals("hoopla://pay/abc", info.deeplink)
    }

}

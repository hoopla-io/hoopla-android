package uz.alphazet.data.services

import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OrderServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: OrderService

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

    @Test
    fun validateOrder_posts_to_validate_order_and_parses_order_details() = runTest {
        server.enqueue(
            mockOk(
                """
                {
                  "data": {
                    "modifications": {
                      "size": [{"modificationId":"s1","modificationGroupId":"g1","modificationName":"Large","modificationKey":"large","modificationPrice":1000.0}],
                      "sugar": [],
                      "milk": [],
                      "syrup": []
                    },
                    "drink": {"id": 5, "name": "Latte", "amount": 25000.0, "imageUrl": "https://img/latte.png"},
                    "shop": {"id": 3, "name": "Hoopla Central"},
                    "validatedAt": "2026-01-01T10:00:00Z",
                    "validatedAtUnix": 1735725600
                  },
                  "message": "ok", "status": true, "code": 200, "meta": null
                }
                """.trimIndent()
            )
        )

        val body = """{"shopId":3,"drinkId":5}"""
        val response = service.validateOrder(jsonBody(body))

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/v1/user/orders/validate-order", recorded.path)
        assertEquals(body, recorded.body.readUtf8())

        val details = response.body()?.data
        assertNotNull(details)
        assertEquals("Latte", details!!.drink?.name)
        assertEquals(25000.0, details.drink!!.amount!!, 0.0)
        assertEquals("Hoopla Central", details.shop?.name)
        val sizes = details.modifications.size
        assertNotNull(sizes)
        assertEquals(1, sizes!!.size)
        assertEquals("Large", sizes[0]?.modificationName)
        assertEquals(1000.0, sizes[0]?.modificationPrice!!, 0.0)
    }

    @Test
    fun createOrder_posts_body_and_parses_order_info() = runTest {
        server.enqueue(
            mockOk(
                """
                {
                  "data": {
                    "id": 101,
                    "partnerName": "Hoopla",
                    "shopName": "Hoopla Central",
                    "purchasedAt": "2026-01-01",
                    "purchasedAtUnix": 1735725600,
                    "drinkName": "Latte",
                    "orderStatus": "pending"
                  },
                  "message": "ok", "status": true, "code": 200, "meta": null
                }
                """.trimIndent()
            )
        )

        val body = """{"shopId":3,"drinkId":5,"modifications":[]}"""
        val response = service.createOrder(jsonBody(body))

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/v1/user/orders/create", recorded.path)
        assertEquals(body, recorded.body.readUtf8())

        val order = response.body()?.data
        assertNotNull(order)
        assertEquals(101, order!!.id)
        assertEquals("Latte", order.drinkName)
        assertEquals("pending", order.orderStatus)
    }

    @Test
    fun createOrderRahmat_posts_and_parses_checkout_info() = runTest {
        server.enqueue(
            mockOk(
                """
                {
                  "data": {
                    "amount": 29000,
                    "checkout_url": "https://rahmat.pay/co",
                    "deeplink": "rahmat://pay",
                    "expires_at": "2030-01-01",
                    "order_id": 55,
                    "short_link": "https://r.link/x"
                  },
                  "message": "ok", "status": true, "code": 200, "meta": null
                }
                """.trimIndent()
            )
        )

        val body = """{"orderId":55}"""
        val response = service.createOrderRahmat(jsonBody(body))

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/v1/user/orders/create-rahmat", recorded.path)

        val checkout = response.body()?.data
        assertNotNull(checkout)
        assertEquals(29000, checkout!!.amount)
        assertEquals("https://rahmat.pay/co", checkout.checkout_url)
        assertEquals(55, checkout.order_id)
    }

    @Test
    fun getPaymentStatus_uses_get_at_create_rahmat_path() = runTest {
        server.enqueue(
            mockOk("""{"data":null,"message":"ok","status":true,"code":200,"meta":null}""")
        )

        service.getPaymentStatus()

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/v1/user/orders/create-rahmat", recorded.path)
    }

    @Test
    fun error_422_is_surfaced_as_unsuccessful_response() = runTest {
        server.enqueue(mockError(422, """{"message":"shop closed"}"""))

        val response = service.createOrder(jsonBody("{}"))

        assertFalse(response.isSuccessful)
        assertEquals(422, response.code())
        assertTrue(response.errorBody()?.string()?.contains("shop closed") == true)
    }
}
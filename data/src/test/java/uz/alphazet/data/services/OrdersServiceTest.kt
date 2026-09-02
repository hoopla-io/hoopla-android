package uz.alphazet.data.services

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class OrdersServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: OrdersService

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        service = retrofitFor(server)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun generateQRCode_gets_expected_path_and_parses_qr_data() = runTest {
        server.enqueue(
            mockOk(
                """
                {
                  "data": {"token": "qr-token-abc", "expiresAt": 1893456000, "orderId": 501},
                  "message": "ok", "status": true, "code": 200, "meta": null
                }
                """.trimIndent()
            )
        )

        val response = service.generateQRCode()

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/v1/user/generate-qr-code", recorded.path)

        val qr = response.body()?.data
        assertNotNull(qr)
        assertEquals("qr-token-abc", qr!!.token)
        assertEquals(1_893_456_000L, qr.expiresAt)
        assertEquals(501, qr.orderId)
    }

    @Test
    fun getOrders_sends_pagination_params_and_parses_order_list() = runTest {
        server.enqueue(
            mockOk(
                """
                {
                  "data": [
                    {
                      "id": 1,
                      "shopName": "Hoopla",
                      "shopIconUrl": null,
                      "drinkName": "Latte",
                      "orderStatus": "completed",
                      "productPrice": 25000.0,
                      "purchasedAt": "2026-01-01",
                      "purchasedAtUnix": 1735725600,
                      "cashback_earned": 500.0,
                      "cashback_used": 0.0,
                      "checkout_url": null,
                      "fiscalLink": null
                    }
                  ],
                  "message": "ok", "status": true, "code": 200, "meta": null
                }
                """.trimIndent()
            )
        )

        val response = service.getOrders(page = 1, itemsPerPage = 20)

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/v1/user/orders/orders-list?page=1&itemsPerPage=20", recorded.path)

        val orders = response.body()?.data
        assertNotNull(orders)
        assertEquals(1, orders!!.size)
        assertEquals(1, orders[0].id)
        assertEquals("Latte", orders[0].drinkName)
        assertEquals("completed", orders[0].orderStatus)
        assertEquals(500.0, orders[0].cashbackEarned!!, 0.0)
        assertEquals(0.0, orders[0].cashbackUsed!!, 0.0)
        assertNull(orders[0].checkoutUrl)
    }

    @Test
    fun getOrderHistory_sends_pagination_params_and_parses_nested_drinks() = runTest {
        server.enqueue(
            mockOk(
                """
                {
                  "code": 200,
                  "message": "ok!",
                  "data": [
                    {
                      "id": 415,
                      "shopName": "The Chai",
                      "shopIconUrl": "https://files.hoopla.uz/images/shop.jpg",
                      "drinks": [
                        {
                          "drinkId": 409,
                          "drinkName": "Green Tea",
                          "drinkPrice": 500,
                          "status": "ordered",
                          "drinkImageUrl": "https://files.hoopla.uz/images/green-tea.jpg"
                        },
                        {
                          "drinkId": 367,
                          "drinkName": "Latte",
                          "drinkPrice": 200,
                          "status": "ordered",
                          "drinkImageUrl": "https://files.hoopla.uz/images/latte.jpg"
                        }
                      ],
                      "purchasedAt": "2026-08-23T08:52:03.803756Z",
                      "purchasedAtUnix": 1787475123,
                      "cashback_earned": 0,
                      "hasFeedback": false
                    }
                  ],
                  "status": true, "meta": null
                }
                """.trimIndent()
            )
        )

        val response = service.getOrderHistory(page = 1, itemsPerPage = 10)

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/v1/user/orders/history?page=1&itemsPerPage=10", recorded.path)

        val orders = response.body()?.data
        assertNotNull(orders)
        assertEquals(1, orders!!.size)

        val order = orders[0]
        assertEquals(415, order.id)
        assertEquals("The Chai", order.shopName)
        assertEquals(1_787_475_123L, order.purchasedAtUnix)
        assertEquals(false, order.hasFeedback)
        assertEquals(2, order.drinks!!.size)
        assertEquals("Green Tea", order.drinks!![0].drinkName)
        assertEquals(500.0, order.drinks!![0].drinkPrice!!, 0.0)
        assertEquals("ordered", order.drinks!![1].status)
        // No order-level price or status in this payload: both are derived from the drinks.
        assertEquals(700.0, order.totalPrice, 0.0)
        assertEquals("ordered", order.commonStatus)
    }

    @Test
    fun getOrderHistory_reports_no_common_status_when_drinks_disagree() = runTest {
        server.enqueue(
            mockOk(
                """
                {
                  "data": [
                    {
                      "id": 416,
                      "shopName": "The Chai",
                      "shopIconUrl": null,
                      "drinks": [
                        {"drinkId": 1, "drinkName": "Latte", "drinkPrice": 200, "status": "completed", "drinkImageUrl": null},
                        {"drinkId": 2, "drinkName": "Tea", "drinkPrice": 100, "status": "cancelled", "drinkImageUrl": null}
                      ],
                      "purchasedAt": "2026-08-23T08:52:03.803756Z",
                      "purchasedAtUnix": 1787475123,
                      "cashback_earned": 0,
                      "hasFeedback": false
                    }
                  ],
                  "message": "ok", "status": true, "code": 200, "meta": null
                }
                """.trimIndent()
            )
        )

        val order = service.getOrderHistory(page = 1, itemsPerPage = 10).body()?.data?.first()
        server.takeRequest()

        assertNotNull(order)
        assertNull(order!!.commonStatus)
        assertEquals(300.0, order.totalPrice, 0.0)
    }

    @Test
    fun getOrderInfo_resolves_path_param_and_parses_order_info() = runTest {
        server.enqueue(
            mockOk(
                """
                {
                  "data": {
                    "cashback_earned": 300.0,
                    "cashback_used": 100.0,
                    "checkout_url": null,
                    "drinkImageUrl": "https://img/latte.png",
                    "drinkName": "Latte",
                    "fiscalLink": null,
                    "id": 7,
                    "items": [
                      {"item_type": "size", "name": "Large", "price": 500.0}
                    ],
                    "orderStatus": "completed",
                    "productPrice": 25000.0,
                    "purchasedAt": "2026-01-01",
                    "purchasedAtUnix": 1735725600,
                    "shopName": "Hoopla"
                  },
                  "message": "ok", "status": true, "code": 200, "meta": null
                }
                """.trimIndent()
            )
        )

        val response = service.getOrderInfo(id = 7)

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/v1/user/orders/7", recorded.path)

        val info = response.body()?.data
        assertNotNull(info)
        assertEquals(7, info!!.id)
        assertEquals("Latte", info.drinkName)
        assertEquals(300.0, info.cashbackEarned!!, 0.0)
        assertEquals(1, info.items?.size)
        assertEquals("Large", info.items!![0]?.name)
        assertEquals("size", info.items[0]?.itemType)
    }

    @Test
    fun cancelOrder_posts_to_path_with_id() = runTest {
        server.enqueue(
            mockOk("""{"data":null,"message":"cancelled","status":true,"code":200,"meta":null}""")
        )

        service.cancelOrder(id = 9)

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/v1/user/orders/9/cancel", recorded.path)
    }

    @Test
    fun getDrinksStat_gets_drinks_stat_path_and_parses_stat() = runTest {
        server.enqueue(
            mockOk(
                """
                {
                  "data": {"available": 8, "used": 2},
                  "message": "ok", "status": true, "code": 200, "meta": null
                }
                """.trimIndent()
            )
        )

        val response = service.getDrinksStat()

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/v1/user/orders/drinks-stat", recorded.path)

        val stat = response.body()?.data
        assertNotNull(stat)
        assertEquals(8, stat!!.available)
        assertEquals(2, stat.used)
    }
}
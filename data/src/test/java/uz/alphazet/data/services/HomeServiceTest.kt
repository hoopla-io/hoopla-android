package uz.alphazet.data.services

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HomeServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: HomeService

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
    fun getLoyaltyCard_hits_expected_path_and_parses_list() = runTest {
        server.enqueue(
            mockOk(
                """
                {
                  "data": [
                    {"drinkIndex": 0, "isFilled": true,  "isFree": false},
                    {"drinkIndex": 1, "isFilled": false, "isFree": true}
                  ],
                  "message": "ok", "status": true, "code": 200, "meta": null
                }
                """.trimIndent()
            )
        )

        val response = service.getLoyaltyCard()

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/v1/user/orders/loyalty-card", recorded.path)

        val items = response.body()?.data
        assertNotNull(items)
        assertEquals(2, items!!.size)
        assertTrue(items[0].isFilled)
        assertFalse(items[0].isFree)
        assertTrue(items[1].isFree)
    }

    @Test
    fun getNearShops_includes_all_query_params_when_supplied() = runTest {
        server.enqueue(
            mockOk(
                """
                {
                  "data": [
                    {
                      "shopId": 7, "name": "Hoopla", "pictureUrl": null,
                      "distance": 0.42,
                      "location": {"lat": 41.3, "lng": 69.2}
                    }
                  ],
                  "message": "ok", "status": true, "code": 200, "meta": null
                }
                """.trimIndent()
            )
        )

        val response = service.getNearShops(
            lat = 41.3,
            long = 69.2,
            name = "Hoopla",
            categoryId = 5
        )

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        // Query-param order is determined by the service method signature.
        assertEquals(
            "/v1/shops/near-shops?lat=41.3&long=69.2&name=Hoopla&categoryId=5",
            recorded.path
        )

        val shops = response.body()?.data
        assertNotNull(shops)
        assertEquals(1, shops!!.size)
        assertEquals(7, shops.first().shopId)
        assertEquals(0.42, shops.first().distance!!, 0.0)
    }

    @Test
    fun getNearShops_omits_null_optional_query_params() = runTest {
        server.enqueue(
            mockOk(
                """{"data":[], "message":"ok","status":true,"code":200,"meta":null}"""
            )
        )

        service.getNearShops(lat = 41.3, long = 69.2, name = null, categoryId = null)

        val recorded = server.takeRequest()
        // Retrofit drops @Query params whose value is null — "name" and
        // "categoryId" must be absent from the URL.
        assertEquals("/v1/shops/near-shops?lat=41.3&long=69.2", recorded.path)
    }

    @Test
    fun getPendingFeedbacks_parses_snake_case_fields() = runTest {
        server.enqueue(
            mockOk(
                """
                {
                  "data": {
                    "cashback_earned": 12.5,
                    "cashback_used": 4.0,
                    "drinkName": "Latte",
                    "id": 99,
                    "orderStatus": "DELIVERED",
                    "partnerName": "Hoopla",
                    "productPrice": 25000.0,
                    "purchasedAt": "2026-01-01",
                    "purchasedAtUnix": 1735689600,
                    "shopName": "Hoopla Central",
                    "drinkImage": "https://img/latte.png"
                  },
                  "message": "ok", "status": true, "code": 200, "meta": null
                }
                """.trimIndent()
            )
        )

        val response = service.getPendingFeedbacks()

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/v1/orders/feedbacks/pending", recorded.path)

        val feedback = response.body()?.data
        assertNotNull(feedback)
        assertEquals(12.5, feedback!!.cashbackEarned!!, 0.0)
        assertEquals(4.0, feedback.cashbackUsed!!, 0.0)
        assertEquals("Latte", feedback.drinkName)
        assertEquals(99, feedback.id)
    }

    @Test
    fun submitFeedback_posts_map_body_as_json_at_path_with_id() = runTest {
        server.enqueue(
            mockOk("""{"data":null,"message":"ok","status":true,"code":200,"meta":null}""")
        )

        val body: Map<String, Any> = mapOf(
            "rating" to 5,
            "comment" to "great"
        )

        service.submitFeedback(id = 77, body = body)

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/v1/orders/feedbacks/77/feedback", recorded.path)

        val sentBody = recorded.body.readUtf8()
        // Gson serializes the Map; assert both fields are present — order is
        // insertion-order for LinkedHashMap (what mapOf returns), so a simple
        // substring check keeps the test resilient if anything in the
        // serializer changes whitespace.
        assertTrue(sentBody.contains("\"rating\":5"))
        assertTrue(sentBody.contains("\"comment\":\"great\""))
    }

    @Test
    fun null_data_in_envelope_yields_null_body_data() = runTest {
        server.enqueue(
            mockOk("""{"data":null,"message":"empty","status":true,"code":200,"meta":null}""")
        )

        val response = service.getLoyaltyCard()

        assertTrue(response.isSuccessful)
        assertNull(response.body()?.data)
        assertEquals("empty", response.body()?.message)
    }
}
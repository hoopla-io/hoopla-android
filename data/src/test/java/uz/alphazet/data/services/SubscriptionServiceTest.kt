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

class SubscriptionServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: SubscriptionService

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
    fun getSubscriptions_hits_subscriptions_path_and_parses_list() = runTest {
        server.enqueue(
            mockOk(
                """
                {
                  "data": [
                    {
                      "currency": "UZS",
                      "days": 30,
                      "id": 1,
                      "name": "Gold",
                      "price": 49900.0,
                      "features": [
                        {"id": 10, "feature": "Unlimited coffee"},
                        {"id": 11, "feature": "Free delivery"}
                      ]
                    },
                    {
                      "currency": "UZS",
                      "days": 7,
                      "id": 2,
                      "name": "Silver",
                      "price": 19900.0,
                      "features": []
                    }
                  ],
                  "message": "ok", "status": true, "code": 200, "meta": null
                }
                """.trimIndent()
            )
        )

        val response = service.getSubscriptions()

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/v1/subscriptions", recorded.path)

        val subscriptions = response.body()?.data
        assertNotNull(subscriptions)
        assertEquals(2, subscriptions!!.size)

        val gold = subscriptions[0]
        assertEquals(1, gold.id)
        assertEquals("Gold", gold.name)
        assertEquals(49900.0, gold.price!!, 0.0)
        assertEquals(30, gold.days)
        assertEquals("UZS", gold.currency)
        assertEquals(2, gold.features?.size)
        assertEquals("Unlimited coffee", gold.features!![0]?.feature)
        assertEquals(10, gold.features[0]?.id)

        val silver = subscriptions[1]
        assertEquals(0, silver.features?.size)
    }

    @Test
    fun buySubscription_posts_body_to_buy_endpoint() = runTest {
        server.enqueue(
            mockOk("""{"data":null,"message":"subscribed","status":true,"code":200,"meta":null}""")
        )

        val body = """{"subscriptionId":1,"paymentServiceId":2}"""
        val response = service.buySubscription(
            body.toRequestBody("application/json".toMediaType())
        )

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/v1/subscriptions/buy", recorded.path)
        assertEquals(body, recorded.body.readUtf8())

        assertTrue(response.isSuccessful)
        assertEquals("subscribed", response.body()?.message)
    }

    @Test
    fun getSubscriptions_returns_empty_list_when_data_is_empty_array() = runTest {
        server.enqueue(
            mockOk("""{"data":[],"message":"ok","status":true,"code":200,"meta":null}""")
        )

        val response = service.getSubscriptions()

        assertEquals(0, response.body()?.data?.size)
    }

    @Test
    fun buySubscription_402_is_surfaced_as_unsuccessful() = runTest {
        server.enqueue(mockError(402, """{"message":"payment required"}"""))

        val response = service.buySubscription(
            "{}".toRequestBody("application/json".toMediaType())
        )

        assertFalse(response.isSuccessful)
        assertEquals(402, response.code())
    }
}
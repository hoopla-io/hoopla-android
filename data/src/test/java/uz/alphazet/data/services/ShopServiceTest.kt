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

class ShopServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: ShopService

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
    fun getShopDetail_encodes_shop_id_as_query_param() = runTest {
        server.enqueue(
            mockOk(
                """
                {
                  "data": {
                    "id": 3,
                    "partnerId": 1,
                    "name": "Hoopla Central",
                    "canAcceptOrders": true,
                    "location": {"lat": 41.3, "lng": 69.2},
                    "phoneNumbers": [{"phoneNumber": "+998901234567"}],
                    "workingHours": [
                      {"weekDay": "Monday", "openAt": "09:00", "closeAt": "22:00"}
                    ],
                    "pictures": [{"pictureUrl": "https://img/shop.png"}],
                    "drinks": [],
                    "urls": []
                  },
                  "message": "ok", "status": true, "code": 200, "meta": null
                }
                """.trimIndent()
            )
        )

        val response = service.getShopDetail(shopId = 3)

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/v1/shops/shop?shopId=3", recorded.path)

        val shop = response.body()?.data
        assertNotNull(shop)
        assertEquals(3, shop!!.id)
        assertEquals("Hoopla Central", shop.name)
        assertTrue(shop.canAcceptOrders!!)
        assertEquals(41.3, shop.location?.lat!!, 0.0)
        assertEquals(69.2, shop.location?.lng!!, 0.0)
    }

    @Test
    fun getShopDetail_parses_working_hours() = runTest {
        server.enqueue(
            mockOk(
                """
                {
                  "data": {
                    "id": 3, "partnerId": 1, "name": "Shop",
                    "canAcceptOrders": false, "location": null,
                    "phoneNumbers": [], "pictures": [], "drinks": [], "urls": [],
                    "workingHours": [
                      {"weekDay": "Monday",    "openAt": "09:00", "closeAt": "21:00"},
                      {"weekDay": "Saturday",  "openAt": "10:00", "closeAt": "20:00"},
                      {"weekDay": "Sunday",    "openAt": null,    "closeAt": null}
                    ]
                  },
                  "message": "ok", "status": true, "code": 200, "meta": null
                }
                """.trimIndent()
            )
        )

        val response = service.getShopDetail(shopId = 3)
        val hours = response.body()?.data?.workingHours

        assertNotNull(hours)
        assertEquals(3, hours!!.size)
        assertEquals("Monday",   hours[0]?.weekDay)
        assertEquals("09:00",    hours[0]?.openAt)
        assertEquals("21:00",    hours[0]?.closeAt)
        assertEquals("Saturday", hours[1]?.weekDay)
        assertNull(hours[2]?.openAt)
    }

    @Test
    fun getShopDetail_canAcceptOrders_false_is_preserved() = runTest {
        server.enqueue(
            mockOk(
                """
                {
                  "data": {
                    "id": 5, "partnerId": 2, "name": "Closed",
                    "canAcceptOrders": false, "location": null,
                    "phoneNumbers": [], "workingHours": [],
                    "pictures": [], "drinks": [], "urls": []
                  },
                  "message": "ok", "status": true, "code": 200, "meta": null
                }
                """.trimIndent()
            )
        )

        val shop = service.getShopDetail(shopId = 5).body()?.data
        assertFalse(shop?.canAcceptOrders!!)
    }

    @Test
    fun getShopDetail_404_is_surfaced_as_unsuccessful() = runTest {
        server.enqueue(mockError(404, """{"message":"not found"}"""))

        val response = service.getShopDetail(shopId = 999)

        assertFalse(response.isSuccessful)
        assertEquals(404, response.code())
    }

    /**
     * The detail toolbar's share action is gated on this field, so a shop that comes back
     * without one has to stay null rather than fall back to an empty string.
     */
    @Test
    fun getShopDetail_parses_share_url_and_leaves_it_null_when_absent() = runTest {
        server.enqueue(
            mockOk(
                """
                {
                  "data": {
                    "id": 3, "partnerId": 1, "name": "Hoopla Central",
                    "canAcceptOrders": true, "location": null,
                    "phoneNumbers": [], "workingHours": [],
                    "pictures": [], "drinks": [], "urls": [],
                    "shareUrl": "https://hoopla.uz/s/3"
                  },
                  "message": "ok", "status": true, "code": 200, "meta": null
                }
                """.trimIndent()
            )
        )
        server.enqueue(
            mockOk(
                """
                {
                  "data": {
                    "id": 4, "partnerId": 1, "name": "No Link",
                    "canAcceptOrders": true, "location": null,
                    "phoneNumbers": [], "workingHours": [],
                    "pictures": [], "drinks": [], "urls": []
                  },
                  "message": "ok", "status": true, "code": 200, "meta": null
                }
                """.trimIndent()
            )
        )

        assertEquals(
            "https://hoopla.uz/s/3",
            service.getShopDetail(shopId = 3).body()?.data?.shareUrl
        )
        assertNull(service.getShopDetail(shopId = 4).body()?.data?.shareUrl)
    }
}

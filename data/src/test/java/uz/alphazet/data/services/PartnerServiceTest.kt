package uz.alphazet.data.services

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PartnerServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: PartnerService

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        service = retrofitFor(server)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    /**
     * A null name must be omitted rather than sent as `name=null` — the whole catalogue is the
     * point of this call when a partner is known only by its id.
     */
    @Test
    fun getPartners_without_a_name_asks_for_the_whole_list() = runTest {
        server.enqueue(
            mockOk(
                """
                {
                  "data": [
                    {"id": 62, "name": "Alibi coffee", "logoUrl": "https://img/alibi.png"}
                  ],
                  "message": "ok!", "status": true, "code": 200, "meta": null
                }
                """.trimIndent()
            )
        )

        val response = service.getPartners(null)

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/v1/partners/list", recorded.path)

        val partners = response.body()?.data
        assertEquals(1, partners?.size)
        assertEquals(62, partners?.first()?.id)
        assertEquals("Alibi coffee", partners?.first()?.name)
        assertEquals("https://img/alibi.png", partners?.first()?.logoUrl)
        // The list endpoint never sends a description, whatever the model allows.
        assertNull(partners?.first()?.description)
    }

    @Test
    fun getPartners_sends_the_name_filter() = runTest {
        server.enqueue(mockOk("""{"data": [], "message": "ok!", "status": true, "code": 200, "meta": null}"""))

        service.getPartners("Alibi")

        assertEquals("/v1/partners/list?name=Alibi", server.takeRequest().path)
    }

    @Test
    fun getPartnerShops_encodes_the_partner_id_and_location() = runTest {
        server.enqueue(
            mockOk(
                """
                {
                  "data": [
                    {
                      "shopId": 100, "partnerId": 62, "name": "alibi coffee",
                      "logoUrl": "https://img/alibi.png", "pictureUrl": "https://img/shop.jpg",
                      "distance": 4.24, "acceptingOrders": true, "pausedUntil": null,
                      "location": {"lat": 41.289104, "lng": 69.227424}
                    }
                  ],
                  "message": "ok!", "status": true, "code": 200, "meta": null
                }
                """.trimIndent()
            )
        )

        val response = service.getPartnerShops(partnerId = 62, lat = 41.3, long = 69.2)

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/v1/partners/shops?partnerId=62&lat=41.3&long=69.2", recorded.path)

        val shop = response.body()?.data?.first()
        assertEquals(100, shop?.shopId)
        assertEquals(62, shop?.partnerId)
        assertEquals(4.24, shop?.distance!!, 0.001)
        assertTrue(shop.acceptingOrders == true)
        // The endpoint never sends a rating, so the card's rating pill stays hidden here.
        assertNull(shop.rating)
    }

    /** A brand with no active shops answers 200 with an empty list, not an error. */
    @Test
    fun getPartnerShops_omits_an_absent_location_and_accepts_no_shops() = runTest {
        server.enqueue(mockOk("""{"data": [], "message": "ok!", "status": true, "code": 200, "meta": null}"""))

        val response = service.getPartnerShops(partnerId = 1, lat = null, long = null)

        assertEquals("/v1/partners/shops?partnerId=1", server.takeRequest().path)
        assertEquals(emptyList<Any>(), response.body()?.data)
    }
}

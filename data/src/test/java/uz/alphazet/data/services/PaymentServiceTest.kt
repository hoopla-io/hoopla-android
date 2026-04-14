package uz.alphazet.data.services

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class PaymentServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: PaymentService

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
    fun getPaymentServices_hits_services_path_and_parses_list() = runTest {
        server.enqueue(
            mockOk(
                """
                {
                  "data": [
                    {"id": 1, "logoUrl": "https://img/click.png", "name": "Click"},
                    {"id": 2, "logoUrl": "https://img/payme.png", "name": "Payme"}
                  ],
                  "message": "ok", "status": true, "code": 200, "meta": null
                }
                """.trimIndent()
            )
        )

        val response = service.getPaymentServices()

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/v1/user/pay/services", recorded.path)

        val services = response.body()?.data
        assertNotNull(services)
        assertEquals(2, services!!.size)
        assertEquals(1, services[0].id)
        assertEquals("Click", services[0].name)
        assertEquals("https://img/click.png", services[0].logoUrl)
    }

    @Test
    fun topUpViaPayService_encodes_id_and_amount_as_query_params() = runTest {
        server.enqueue(
            mockOk(
                """
                {
                  "data": {"checkoutUrl": "https://click.uz/co?token=abc"},
                  "message": "ok", "status": true, "code": 200, "meta": null
                }
                """.trimIndent()
            )
        )

        val response = service.topUpViaPayService(id = 1, amount = 50000.0)

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/v1/user/pay/top-up?id=1&amount=50000.0", recorded.path)

        val checkout = response.body()?.data
        assertNotNull(checkout)
        assertEquals("https://click.uz/co?token=abc", checkout!!.checkoutUrl)
    }

    @Test
    fun topUpViaPayService_decimal_amount_is_encoded_correctly() = runTest {
        server.enqueue(
            mockOk(
                """{"data":{"checkoutUrl":"https://p/co"},"message":"ok","status":true,"code":200,"meta":null}"""
            )
        )

        service.topUpViaPayService(id = 2, amount = 12500.5)

        val recorded = server.takeRequest()
        assertEquals("/v1/user/pay/top-up?id=2&amount=12500.5", recorded.path)
    }

    @Test
    fun error_402_is_surfaced_as_unsuccessful() = runTest {
        server.enqueue(mockError(402, """{"message":"insufficient funds"}"""))

        val response = service.topUpViaPayService(id = 1, amount = 100.0)

        assertFalse(response.isSuccessful)
        assertEquals(402, response.code())
    }
}
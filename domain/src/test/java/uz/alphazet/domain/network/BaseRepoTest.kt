package uz.alphazet.domain.network

import app.cash.turbine.test
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import uz.alphazet.data.BaseResponseData
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.order.PaymentRequiredExceptionData
import java.net.ConnectException
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class BaseRepoTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo = TestRepo()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- success paths ----------------------------------------------------

    @Test
    fun handle_returns_success_with_data() = runTest(dispatcher) {
        val result = repo.runHandle { Response.success(wrap("hello")) }

        assertTrue(result is UIResource.Success)
        assertEquals("hello", (result as UIResource.Success).data)
    }

    @Test
    fun handle_returns_success_with_null_data() = runTest(dispatcher) {
        val result = repo.runHandle { Response.success(wrap<String>(null)) }

        assertTrue(result is UIResource.Success)
        assertNull((result as UIResource.Success).data)
    }

    @Test
    fun handleFlow_emits_success() = runTest(dispatcher) {
        val flow: Flow<UIResource<String>> = repo.runHandleFlow {
            Response.success(wrap("flowed"))
        }

        flow.test {
            val item = awaitItem()
            assertTrue(item is UIResource.Success)
            assertEquals("flowed", (item as UIResource.Success).data)
            awaitComplete()
        }
    }

    @Test
    fun handleX_returns_whole_response_wrapper() = runTest(dispatcher) {
        val body = wrap("payload")

        val result = repo.runHandleX { Response.success(body) }

        assertTrue(result is UIResource.Success)
        val wrapper = (result as UIResource.Success).data
        assertNotNull(wrapper)
        assertEquals("payload", wrapper!!.data)
        assertEquals("ok", wrapper.message)
    }

    // --- HTTP error code → exception mapping ------------------------------

    @Test
    fun code_400_maps_to_bad_request() = runTest(dispatcher) {
        assertError<BadRequestException>(400, errorJson("bad body"))
    }

    @Test
    fun code_401_maps_to_unauthorized() = runTest(dispatcher) {
        assertError<UnauthorizedException>(401, errorJson("unauth"))
    }

    @Test
    fun code_402_maps_to_payment_with_parsed_error_data() = runTest(dispatcher) {
        val paymentJson = """
            {
                "data": {
                    "amount": 1500,
                    "checkout_url": "https://pay.example/co",
                    "deeplink": "app://pay",
                    "expires_at": "2030-01-01",
                    "order_id": 77,
                    "short_link": "http://s"
                },
                "message": "payment required",
                "status": false,
                "code": 402,
                "meta": null
            }
        """.trimIndent()

        val result = repo.runHandle<Unit> {
            Response.error(402, paymentJson.toBody())
        }

        assertTrue(result is UIResource.Error)
        val err = (result as UIResource.Error).throwable
        assertTrue(err is PaymentException)
        val payment = err as PaymentException
        assertEquals(402, payment.code)
        assertEquals("payment required", payment.message)
        val data: PaymentRequiredExceptionData? = payment.errorData
        assertNotNull(data)
        assertEquals(1500, data!!.amount)
        assertEquals(77, data.orderId)
        assertEquals("app://pay", data.deeplink)
    }

    @Test
    fun code_403_maps_to_forbidden() = runTest(dispatcher) {
        assertError<ForbiddenException>(403, errorJson("forbidden"))
    }

    @Test
    fun code_404_maps_to_not_found() = runTest(dispatcher) {
        assertError<NotFoundException>(404, errorJson("missing"))
    }

    @Test
    fun code_409_maps_to_conflict() = runTest(dispatcher) {
        assertError<ConflictException>(409, errorJson("conflict"))
    }

    @Test
    fun code_422_maps_to_validation() = runTest(dispatcher) {
        assertError<ValidationException>(422, errorJson("invalid"))
    }

    @Test
    fun code_428_maps_to_precondition_required() = runTest(dispatcher) {
        assertError<PreconditionRequiredException>(428, errorJson("need"))
    }

    @Test
    fun code_429_maps_to_too_many_requests() = runTest(dispatcher) {
        assertError<TooManyRequestException>(429, errorJson("slow"))
    }

    @Test
    fun code_500_maps_to_server_error() = runTest(dispatcher) {
        assertError<ServerErrorException>(500, errorJson("boom"))
    }

    @Test
    fun code_599_maps_to_server_error() = runTest(dispatcher) {
        assertError<ServerErrorException>(599, errorJson("boom"))
    }

    @Test
    fun unmapped_code_falls_through_to_remote_exception() = runTest(dispatcher) {
        val result = repo.runHandle<Unit> {
            Response.error(418, errorJson("teapot").toBody())
        }

        assertTrue(result is UIResource.Error)
        val err = (result as UIResource.Error).throwable
        // Explicit class check — subclasses would pass instanceof RemoteException,
        // so assert the concrete base type here.
        assertEquals(RemoteException::class.java, err.javaClass)
        assertEquals(418, (err as RemoteException).code)
        assertEquals("teapot", err.message)
    }

    @Test
    fun malformed_error_body_still_produces_mapped_exception() = runTest(dispatcher) {
        // Not JSON → Gson parse throws internally; BaseRepo must swallow and
        // fall back to the message field of the Response (blank here).
        val result = repo.runHandle<Unit> {
            Response.error(400, "not-json".toBody())
        }

        assertTrue(result is UIResource.Error)
        val err = (result as UIResource.Error).throwable
        assertTrue(err is BadRequestException)
        assertEquals(400, (err as BadRequestException).code)
    }

    // --- device-level exceptions ------------------------------------------

    @Test
    fun unknown_host_maps_to_connection_error() = runTest(dispatcher) {
        val result = repo.runHandle<Unit> { throw UnknownHostException("no dns") }

        assertTrue(result is UIResource.Error)
        val err = (result as UIResource.Error).throwable
        assertTrue(err is ConnectionErrorException)
        assertEquals("no dns", err.message)
    }

    @Test
    fun connect_exception_maps_to_connection_error() = runTest(dispatcher) {
        val result = repo.runHandle<Unit> { throw ConnectException("refused") }

        assertTrue(result is UIResource.Error)
        assertTrue((result as UIResource.Error).throwable is ConnectionErrorException)
    }

    @Test
    fun unknown_exception_maps_to_remote_exception_with_code_minus_one() = runTest(dispatcher) {
        val result = repo.runHandle<Unit> { throw RuntimeException("weird") }

        assertTrue(result is UIResource.Error)
        val err = (result as UIResource.Error).throwable
        assertEquals(RemoteException::class.java, err.javaClass)
        assertEquals(-1, (err as RemoteException).code)
        assertEquals("weird", err.message)
    }

    // --- helpers ----------------------------------------------------------

    private suspend inline fun <reified E : Throwable> assertError(
        code: Int,
        body: String,
    ) {
        val result = repo.runHandle<Unit> {
            Response.error(code, body.toBody())
        }
        assertTrue("expected Error, got $result", result is UIResource.Error)
        val err = (result as UIResource.Error).throwable
        assertTrue(
            "expected ${E::class.java.simpleName}, got ${err.javaClass.simpleName}",
            err is E
        )
    }

    private fun <T> wrap(data: T?): BaseResponseData<T> = BaseResponseData(
        data = data,
        message = "ok",
        status = true,
        code = 200,
        meta = null
    )

    private fun errorJson(message: String): String =
        Gson().toJson(mapOf("message" to message))

    private fun String.toBody(): ResponseBody =
        this.toResponseBody("application/json".toMediaType())

    /** Exposes the protected BaseRepo helpers for test use. */
    private class TestRepo : BaseRepo() {
        suspend fun <T> runHandle(
            callback: suspend () -> Response<BaseResponseData<T>>
        ): UIResource<T> = handle(callback)

        suspend fun <T> runHandleFlow(
            callback: suspend () -> Response<BaseResponseData<T>>
        ): Flow<UIResource<T>> = handleFlow(callback)

        suspend fun <T> runHandleX(
            callback: suspend () -> Response<BaseResponseData<T>>
        ): UIResource<BaseResponseData<T>> = handleX(callback)
    }
}

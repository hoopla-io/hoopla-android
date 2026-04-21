package uz.alphazet.domain.repositories

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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Response
import uz.alphazet.data.BaseResponseData
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.ConfirmSMSData
import uz.alphazet.data.models.LoginSessionData
import uz.alphazet.data.services.AuthService
import uz.alphazet.domain.network.BadRequestException
import uz.alphazet.domain.network.UnauthorizedException
import uz.alphazet.domain.network.ValidationException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AuthRepoTest {

    private val dispatcher = StandardTestDispatcher()
    private val authService: AuthService = mockk()
    private val repo = AuthRepo(authService)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- sendSMS ---

    @Test
    fun sendSMS_sends_phoneNumber_in_json_body() = runTest(dispatcher) {
        val bodySlot = slot<RequestBody>()
        coEvery { authService.sendSMS(capture(bodySlot)) } returns Response.success(
            wrap(LoginSessionData("+998900472400", 120, "sess-1"))
        )

        val result = repo.sendSMS("+998900472400")

        assertTrue(result is UIResource.Success)
        val json = JSONObject(bodySlot.captured.asString())
        assertEquals("+998900472400", json.getString("phoneNumber"))
        coVerify(exactly = 1) { authService.sendSMS(any()) }
    }

    @Test
    fun sendSMS_success_returns_session_data() = runTest(dispatcher) {
        val session = LoginSessionData("+998900472400", 120, "sess-1")
        coEvery { authService.sendSMS(any()) } returns Response.success(wrap(session))

        val result = repo.sendSMS("+998900472400")

        assertTrue(result is UIResource.Success)
        assertEquals(session, (result as UIResource.Success).data)
    }

    @Test
    fun sendSMS_422_returns_validation_error() = runTest(dispatcher) {
        coEvery { authService.sendSMS(any()) } returns Response.error(
            422,
            """{"message":"invalid phone"}""".toResponseBody("application/json".toMediaType())
        )

        val result = repo.sendSMS("bad")

        assertTrue(result is UIResource.Error)
        assertTrue((result as UIResource.Error).throwable is ValidationException)
    }

    @Test
    fun sendSMS_429_returns_error() = runTest(dispatcher) {
        coEvery { authService.sendSMS(any()) } returns Response.error(
            429,
            """{"message":"too many"}""".toResponseBody("application/json".toMediaType())
        )

        val result = repo.sendSMS("+998900472400")

        assertTrue(result is UIResource.Error)
    }

    // --- resendSMS ---

    @Test
    fun resendSMS_sends_sessionId_in_json_body() = runTest(dispatcher) {
        val bodySlot = slot<RequestBody>()
        coEvery { authService.resendSMS(capture(bodySlot)) } returns Response.success(
            wrap(LoginSessionData(null, 120, "sess-1"))
        )

        repo.resendSMS("sess-1")

        val json = JSONObject(bodySlot.captured.asString())
        assertEquals("sess-1", json.getString("sessionId"))
    }

    @Test
    fun resendSMS_success_returns_session_data() = runTest(dispatcher) {
        val session = LoginSessionData(null, 60, "sess-2")
        coEvery { authService.resendSMS(any()) } returns Response.success(wrap(session))

        val result = repo.resendSMS("sess-2")

        assertTrue(result is UIResource.Success)
        assertEquals(session, (result as UIResource.Success).data)
    }

    // --- confirmSMS ---

    @Test
    fun confirmSMS_sends_code_and_sessionId_in_json_body() = runTest(dispatcher) {
        val bodySlot = slot<RequestBody>()
        coEvery { authService.confirmSMS(capture(bodySlot)) } returns Response.success(
            wrap(
                ConfirmSMSData(
                    userId = 9L, isNewUser = false, phoneNumber = "+998900472400",
                    jwt = ConfirmSMSData.JwtData("access", "refresh", 1_700_000_000L)
                )
            )
        )

        repo.confirmSMS(code = 54321, sessionId = "sess-1")

        val json = JSONObject(bodySlot.captured.asString())
        assertEquals(54321, json.getInt("code"))
        assertEquals("sess-1", json.getString("sessionId"))
    }

    @Test
    fun confirmSMS_success_returns_jwt_payload() = runTest(dispatcher) {
        val confirm = ConfirmSMSData(
            userId = 9L, isNewUser = true, phoneNumber = "+998900472400",
            jwt = ConfirmSMSData.JwtData("access-tok", "refresh-tok", 1_700_000_000L)
        )
        coEvery { authService.confirmSMS(any()) } returns Response.success(wrap(confirm))

        val result = repo.confirmSMS(code = 12345, sessionId = "sess-1")

        assertTrue(result is UIResource.Success)
        assertEquals(confirm, (result as UIResource.Success).data)
    }

    @Test
    fun confirmSMS_400_returns_bad_request_error() = runTest(dispatcher) {
        coEvery { authService.confirmSMS(any()) } returns Response.error(
            400,
            """{"message":"wrong code"}""".toResponseBody("application/json".toMediaType())
        )

        val result = repo.confirmSMS(code = 12345, sessionId = "sess-1")

        assertTrue(result is UIResource.Error)
        assertTrue((result as UIResource.Error).throwable is BadRequestException)
    }

    @Test
    fun confirmSMS_401_returns_unauthorized_error() = runTest(dispatcher) {
        coEvery { authService.confirmSMS(any()) } returns Response.error(
            401,
            """{"message":"expired"}""".toResponseBody("application/json".toMediaType())
        )

        val result = repo.confirmSMS(code = 12345, sessionId = "sess-1")

        assertTrue(result is UIResource.Error)
        assertTrue((result as UIResource.Error).throwable is UnauthorizedException)
    }

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
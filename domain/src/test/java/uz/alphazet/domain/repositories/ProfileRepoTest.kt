package uz.alphazet.domain.repositories

import app.cash.turbine.test
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Response
import uz.alphazet.data.BaseResponseData
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.UserData
import uz.alphazet.data.services.ProfileService
import uz.alphazet.domain.cache.AppCache
import uz.alphazet.domain.network.UnauthorizedException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ProfileRepoTest {

    private val dispatcher = StandardTestDispatcher()
    private val api: ProfileService = mockk()
    private val appCache: AppCache = mockk(relaxed = true)
    private val repo = ProfileRepo(api, appCache)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- getMe ---

    @Test
    fun getMe_success_returns_user() = runTest(dispatcher) {
        val user = sampleUser()
        coEvery { api.getMe() } returns Response.success(wrap(user))

        val result = repo.getMe()

        assertTrue(result is UIResource.Success)
        assertEquals(user, (result as UIResource.Success).data)
    }

    @Test
    fun getMe_401_returns_unauthorized_error() = runTest(dispatcher) {
        coEvery { api.getMe() } returns Response.error(
            401,
            """{"message":"expired"}""".toResponseBody("application/json".toMediaType())
        )

        val result = repo.getMe()

        assertTrue(result is UIResource.Error)
        assertTrue((result as UIResource.Error).throwable is UnauthorizedException)
    }

    // --- editMe ---

    @Test
    fun editMe_success_returns_user() = runTest(dispatcher) {
        val user = sampleUser()
        coEvery { api.editMe() } returns Response.success(wrap(user))

        val result = repo.editMe()

        assertTrue(result is UIResource.Success)
        assertEquals(user, (result as UIResource.Success).data)
    }

    // --- updateMe: conditional JSON payload ---

    @Test
    fun updateMe_includes_all_three_keys_when_all_non_empty() = runTest(dispatcher) {
        val slot = slot<RequestBody>()
        coEvery { api.updateMe(capture(slot)) } returns Response.success(wrap<Any>(null))

        repo.updateMe(name = "Ali", gender = "male", dateOfBirth = "1998-01-01")
            .test { awaitItem(); awaitComplete() }

        val json = JSONObject(slot.captured.asString())
        assertEquals("Ali", json.getString("name"))
        assertEquals("male", json.getString("gender"))
        assertEquals("1998-01-01", json.getString("dateOfBirth"))
    }

    @Test
    fun updateMe_omits_null_and_empty_keys() = runTest(dispatcher) {
        val slot = slot<RequestBody>()
        coEvery { api.updateMe(capture(slot)) } returns Response.success(wrap<Any>(null))

        repo.updateMe(name = "Ali", gender = null, dateOfBirth = "")
            .test { awaitItem(); awaitComplete() }

        val json = JSONObject(slot.captured.asString())
        assertEquals("Ali", json.getString("name"))
        assertFalse("null gender must be omitted", json.has("gender"))
        assertFalse("empty dateOfBirth must be omitted", json.has("dateOfBirth"))
    }

    @Test
    fun updateMe_with_all_empty_sends_empty_json_object() = runTest(dispatcher) {
        val slot = slot<RequestBody>()
        coEvery { api.updateMe(capture(slot)) } returns Response.success(wrap<Any>(null))

        repo.updateMe(name = null, gender = "", dateOfBirth = null)
            .test { awaitItem(); awaitComplete() }

        val json = JSONObject(slot.captured.asString())
        assertEquals(0, json.length())
    }

    @Test
    fun updateMe_success_emits_success() = runTest(dispatcher) {
        coEvery { api.updateMe(any()) } returns Response.success(wrap<Any>(null))

        repo.updateMe("Ali", "male", "1998-01-01").test {
            val item = awaitItem()
            assertTrue(item is UIResource.Success)
            awaitComplete()
        }
    }

    // --- logout / deactivate ---

    @Test
    fun logout_success_emits_success() = runTest(dispatcher) {
        coEvery { api.logout(any()) } returns Response.success(wrap<Any>(null))

        repo.logout().test {
            val item = awaitItem()
            assertTrue(item is UIResource.Success)
            awaitComplete()
        }
        coVerify(exactly = 1) { api.logout(any()) }
    }

    @Test
    fun logout_401_emits_unauthorized_error() = runTest(dispatcher) {
        coEvery { api.logout(any()) } returns Response.error(
            401,
            """{"message":"expired"}""".toResponseBody("application/json".toMediaType())
        )

        repo.logout().test {
            val item = awaitItem()
            assertTrue(item is UIResource.Error)
            assertTrue((item as UIResource.Error).throwable is UnauthorizedException)
            awaitComplete()
        }
    }

    @Test
    fun deactivate_success_emits_success() = runTest(dispatcher) {
        coEvery { api.deactivate() } returns Response.success(wrap<Any>(null))

        repo.deactivate().test {
            val item = awaitItem()
            assertTrue(item is UIResource.Success)
            awaitComplete()
        }
        coVerify(exactly = 1) { api.deactivate() }
    }

    private fun sampleUser() = UserData(
        name = "Ali",
        phoneNumber = "+998900472400",
        balance = 10_000.0,
        cashbackBalance = 500.0,
        currency = "UZS",
        gender = "male",
        dateOfBirth = "1998-01-01",
        userId = 1,
        subscription = null,
        unreadNotifications = 0
    )

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
package uz.alphazet.domain.repositories

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import uz.alphazet.data.BaseResponseData
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.NotificationDetail
import uz.alphazet.data.services.NotificationService
import uz.alphazet.domain.network.NotFoundException
import uz.alphazet.domain.network.UnauthorizedException

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationRepoTest {

    private val dispatcher = StandardTestDispatcher()
    private val service: NotificationService = mockk()
    private val repo = NotificationRepo(service)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun getNotificationDetail_success_returns_detail() = runTest(dispatcher) {
        val detail = NotificationDetail(
            countReads = 5,
            createdAt = "2026-04-20",
            files = null,
            isNew = false,
            notificationDescription = "New promo",
            notificationId = 11,
            notificationTitle = "Hi",
            shareUrl = null,
            url = null
        )
        coEvery { service.getNotificationDetail(11, "ru") } returns Response.success(wrap(detail))

        val result = repo.getNotificationDetail(11)

        assertTrue(result is UIResource.Success)
        assertEquals(detail, (result as UIResource.Success).data)
        coVerify(exactly = 1) { service.getNotificationDetail(11, "ru") }
    }

    @Test
    fun getNotificationDetail_forwards_language_parameter() = runTest(dispatcher) {
        coEvery { service.getNotificationDetail(any(), any()) } returns Response.success(wrap(null))

        repo.getNotificationDetail(id = 7, language = "uz")

        coVerify(exactly = 1) { service.getNotificationDetail(7, "uz") }
    }

    @Test
    fun getNotificationDetail_uses_russian_by_default() = runTest(dispatcher) {
        coEvery { service.getNotificationDetail(any(), any()) } returns Response.success(wrap(null))

        repo.getNotificationDetail(id = 3)

        coVerify(exactly = 1) { service.getNotificationDetail(3, "ru") }
    }

    @Test
    fun getNotificationDetail_404_returns_not_found_error() = runTest(dispatcher) {
        coEvery { service.getNotificationDetail(any(), any()) } returns Response.error(
            404,
            """{"message":"nope"}""".toResponseBody("application/json".toMediaType())
        )

        val result = repo.getNotificationDetail(99)

        assertTrue(result is UIResource.Error)
        assertTrue((result as UIResource.Error).throwable is NotFoundException)
    }

    @Test
    fun markRead_success_emits_success() = runTest(dispatcher) {
        coEvery { service.markRead() } returns Response.success(wrap<Any>(null))

        repo.markRead().test {
            val item = awaitItem()
            assertTrue(item is UIResource.Success)
            awaitComplete()
        }
        coVerify(exactly = 1) { service.markRead() }
    }

    @Test
    fun markRead_401_emits_unauthorized_error() = runTest(dispatcher) {
        coEvery { service.markRead() } returns Response.error(
            401,
            """{"message":"expired"}""".toResponseBody("application/json".toMediaType())
        )

        repo.markRead().test {
            val item = awaitItem()
            assertTrue(item is UIResource.Error)
            assertTrue((item as UIResource.Error).throwable is UnauthorizedException)
            awaitComplete()
        }
    }

    private fun <T> wrap(data: T?): BaseResponseData<T> = BaseResponseData(
        data = data,
        message = "ok",
        status = true,
        code = 200,
        meta = null
    )
}
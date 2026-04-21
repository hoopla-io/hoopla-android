package uz.alphazet.domain.repositories

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
import uz.alphazet.data.models.CategoryData
import uz.alphazet.data.services.CategoryService
import uz.alphazet.domain.network.BadRequestException
import uz.alphazet.domain.network.ConnectionErrorException
import uz.alphazet.domain.network.UnauthorizedException
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryRepoTest {

    private val dispatcher = StandardTestDispatcher()
    private val service: CategoryService = mockk()
    private val repo = CategoryRepo(service)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun getCategories_success_returns_success_with_list() = runTest(dispatcher) {
        val categories = listOf(
            CategoryData(id = 1, name = "Coffee", imageUrl = "http://img/1"),
            CategoryData(id = 2, name = "Tea", imageUrl = "http://img/2")
        )
        coEvery { service.getCategories() } returns Response.success(wrap(categories))

        val result = repo.getCategories()

        assertTrue(result is UIResource.Success)
        assertEquals(categories, (result as UIResource.Success).data)
        coVerify(exactly = 1) { service.getCategories() }
    }

    @Test
    fun getCategories_empty_list_returns_success_empty() = runTest(dispatcher) {
        coEvery { service.getCategories() } returns Response.success(wrap(emptyList()))

        val result = repo.getCategories()

        assertTrue(result is UIResource.Success)
        assertTrue((result as UIResource.Success).data!!.isEmpty())
    }

    @Test
    fun getCategories_401_returns_unauthorized_error() = runTest(dispatcher) {
        coEvery { service.getCategories() } returns Response.error(
            401,
            """{"message":"expired"}""".toResponseBody("application/json".toMediaType())
        )

        val result = repo.getCategories()

        assertTrue(result is UIResource.Error)
        assertTrue((result as UIResource.Error).throwable is UnauthorizedException)
    }

    @Test
    fun getCategories_400_returns_bad_request_error() = runTest(dispatcher) {
        coEvery { service.getCategories() } returns Response.error(
            400,
            """{"message":"bad"}""".toResponseBody("application/json".toMediaType())
        )

        val result = repo.getCategories()

        assertTrue(result is UIResource.Error)
        assertTrue((result as UIResource.Error).throwable is BadRequestException)
    }

    @Test
    fun getCategories_no_network_returns_connection_error() = runTest(dispatcher) {
        coEvery { service.getCategories() } throws UnknownHostException("no dns")

        val result = repo.getCategories()

        assertTrue(result is UIResource.Error)
        assertTrue((result as UIResource.Error).throwable is ConnectionErrorException)
    }

    private fun <T> wrap(data: T?): BaseResponseData<T> = BaseResponseData(
        data = data,
        message = "ok",
        status = true,
        code = 200,
        meta = null
    )
}
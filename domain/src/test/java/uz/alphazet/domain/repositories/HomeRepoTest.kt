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
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import uz.alphazet.data.BaseResponseData
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.FeedbackDetail
import uz.alphazet.data.models.LoyaltyItemData
import uz.alphazet.data.models.ShopItemData
import uz.alphazet.data.services.HomeService
import uz.alphazet.domain.network.NotFoundException
import uz.alphazet.domain.network.UnauthorizedException

@OptIn(ExperimentalCoroutinesApi::class)
class HomeRepoTest {

    private val dispatcher = StandardTestDispatcher()
    private val homeService: HomeService = mockk()
    private val repo = HomeRepo(homeService)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- getLoyaltyCard ---

    @Test
    fun getLoyaltyCard_success_emits_list() = runTest(dispatcher) {
        val cards = listOf(
            LoyaltyItemData(drinkIndex = 0, isFilled = true, isFree = false),
            LoyaltyItemData(drinkIndex = 1, isFilled = false, isFree = false)
        )
        coEvery { homeService.getLoyaltyCard() } returns Response.success(wrap(cards))

        repo.getLoyaltyCard().test {
            val item = awaitItem()
            assertTrue(item is UIResource.Success)
            assertEquals(cards, (item as UIResource.Success).data)
            awaitComplete()
        }
    }

    @Test
    fun getLoyaltyCard_401_emits_unauthorized() = runTest(dispatcher) {
        coEvery { homeService.getLoyaltyCard() } returns Response.error(
            401,
            """{"message":"expired"}""".toResponseBody("application/json".toMediaType())
        )

        repo.getLoyaltyCard().test {
            val item = awaitItem()
            assertTrue(item is UIResource.Error)
            assertTrue((item as UIResource.Error).throwable is UnauthorizedException)
            awaitComplete()
        }
    }

    // --- getNearShops ---

    @Test
    fun getNearShops_forwards_all_query_params() = runTest(dispatcher) {
        coEvery { homeService.getNearShops(any(), any(), any(), any()) } returns
            Response.success(wrap(emptyList<ShopItemData>()))

        repo.getNearShops(lat = 41.3, long = 69.2, name = "hoopla", categoryId = 5)
            .test { awaitItem(); awaitComplete() }

        coVerify(exactly = 1) { homeService.getNearShops(41.3, 69.2, "hoopla", 5) }
    }

    @Test
    fun getNearShops_defaults_categoryId_to_null() = runTest(dispatcher) {
        coEvery { homeService.getNearShops(any(), any(), any(), any()) } returns
            Response.success(wrap(emptyList<ShopItemData>()))

        repo.getNearShops(lat = 41.3, long = 69.2, name = null)
            .test { awaitItem(); awaitComplete() }

        coVerify(exactly = 1) { homeService.getNearShops(41.3, 69.2, null, null) }
    }

    @Test
    fun getNearShops_success_emits_shop_list() = runTest(dispatcher) {
        val shops = listOf(
            ShopItemData(shopId = 1, name = "A", pictureUrl = null, distance = 0.5, location = null)
        )
        coEvery { homeService.getNearShops(any(), any(), any(), any()) } returns
            Response.success(wrap(shops))

        repo.getNearShops(0.0, 0.0, null).test {
            val item = awaitItem()
            assertTrue(item is UIResource.Success)
            assertEquals(shops, (item as UIResource.Success).data)
            awaitComplete()
        }
    }

    // --- getPendingFeedbacks ---

    @Test
    fun getPendingFeedbacks_success_returns_detail() = runTest(dispatcher) {
        val detail = FeedbackDetail(
            cashbackEarned = 0.0, cashbackUsed = 0.0, drinkName = "Latte",
            id = 101, orderStatus = "DELIVERED", partnerName = "Hoopla",
            productPrice = 15_000.0, purchasedAt = "2026-04-20",
            purchasedAtUnix = 1_700_000_000L, shopName = "Chilonzor", drinkImage = null
        )
        coEvery { homeService.getPendingFeedbacks() } returns Response.success(wrap(detail))

        val result = repo.getPendingFeedbacks()

        assertTrue(result is UIResource.Success)
        assertEquals(detail, (result as UIResource.Success).data)
    }

    @Test
    fun getPendingFeedbacks_404_returns_not_found() = runTest(dispatcher) {
        coEvery { homeService.getPendingFeedbacks() } returns Response.error(
            404,
            """{"message":"none"}""".toResponseBody("application/json".toMediaType())
        )

        val result = repo.getPendingFeedbacks()

        assertTrue(result is UIResource.Error)
        assertTrue((result as UIResource.Error).throwable is NotFoundException)
    }

    // --- submitFeedback ---

    @Test
    fun submitFeedback_with_comment_includes_both_keys_in_body() = runTest(dispatcher) {
        val bodySlot = slot<Map<String, Any>>()
        coEvery { homeService.submitFeedback(any(), capture(bodySlot)) } returns
            Response.success(wrap<Any>(null))

        repo.submitFeedback(orderId = 12, rating = 5, comment = "Great coffee!")
            .test { awaitItem(); awaitComplete() }

        assertEquals(5, bodySlot.captured["rating"])
        assertEquals("Great coffee!", bodySlot.captured["comment"])
        coVerify(exactly = 1) { homeService.submitFeedback(12, any()) }
    }

    @Test
    fun submitFeedback_without_comment_omits_comment_key() = runTest(dispatcher) {
        val bodySlot = slot<Map<String, Any>>()
        coEvery { homeService.submitFeedback(any(), capture(bodySlot)) } returns
            Response.success(wrap<Any>(null))

        repo.submitFeedback(orderId = 12, rating = 4, comment = null)
            .test { awaitItem(); awaitComplete() }

        assertEquals(4, bodySlot.captured["rating"])
        assertFalse("comment must not be in body when null", bodySlot.captured.containsKey("comment"))
    }

    @Test
    fun submitFeedback_with_blank_comment_omits_comment_key() = runTest(dispatcher) {
        val bodySlot = slot<Map<String, Any>>()
        coEvery { homeService.submitFeedback(any(), capture(bodySlot)) } returns
            Response.success(wrap<Any>(null))

        repo.submitFeedback(orderId = 12, rating = 3, comment = "   ")
            .test { awaitItem(); awaitComplete() }

        assertFalse("blank comment must be treated as absent", bodySlot.captured.containsKey("comment"))
    }

    @Test
    fun submitFeedback_401_emits_unauthorized_error() = runTest(dispatcher) {
        coEvery { homeService.submitFeedback(any(), any()) } returns Response.error(
            401,
            """{"message":"expired"}""".toResponseBody("application/json".toMediaType())
        )

        repo.submitFeedback(orderId = 1, rating = 5, comment = "x").test {
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
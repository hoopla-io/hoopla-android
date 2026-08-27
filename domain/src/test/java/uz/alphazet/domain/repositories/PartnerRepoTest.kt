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
import uz.alphazet.data.models.PartnerItemData
import uz.alphazet.data.models.ShopItemData
import uz.alphazet.data.services.PartnerService
import uz.alphazet.domain.network.NotFoundException
import uz.alphazet.domain.network.ServerErrorException

@OptIn(ExperimentalCoroutinesApi::class)
class PartnerRepoTest {

    private val dispatcher = StandardTestDispatcher()
    private val service: PartnerService = mockk()
    private val repo = PartnerRepo(service)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun getPartner_picks_the_matching_brand_out_of_the_list() = runTest(dispatcher) {
        coEvery { service.getPartners(null) } returns Response.success(
            wrap(listOf(partner(62, "Alibi coffee"), partner(71, "Miran Coffee")))
        )

        repo.getPartner(71).test {
            val item = awaitItem()
            assertTrue(item is UIResource.Success)
            assertEquals("Miran Coffee", (item as UIResource.Success).data?.name)
            awaitComplete()
        }
        // The lookup must not filter server-side — the name is exactly what it does not know.
        coVerify(exactly = 1) { service.getPartners(null) }
    }

    /**
     * The list only carries brands with at least one active shop, so a partner deactivated since
     * a story link was authored is genuinely absent — an error, not an empty success.
     */
    @Test
    fun getPartner_emits_not_found_when_the_id_is_absent() = runTest(dispatcher) {
        coEvery { service.getPartners(null) } returns Response.success(
            wrap(listOf(partner(62, "Alibi coffee")))
        )

        repo.getPartner(999).test {
            val item = awaitItem()
            assertTrue(item is UIResource.Error)
            assertTrue((item as UIResource.Error).throwable is NotFoundException)
            awaitComplete()
        }
    }

    @Test
    fun getPartner_emits_not_found_when_the_list_is_empty() = runTest(dispatcher) {
        coEvery { service.getPartners(null) } returns Response.success(wrap(emptyList()))

        repo.getPartner(1).test {
            val item = awaitItem()
            assertTrue(item is UIResource.Error)
            assertTrue((item as UIResource.Error).throwable is NotFoundException)
            awaitComplete()
        }
    }

    @Test
    fun getPartner_propagates_a_list_failure_unchanged() = runTest(dispatcher) {
        coEvery { service.getPartners(null) } returns Response.error(
            500,
            """{"message":"boom"}""".toResponseBody("application/json".toMediaType())
        )

        repo.getPartner(62).test {
            val item = awaitItem()
            assertTrue(item is UIResource.Error)
            assertTrue((item as UIResource.Error).throwable is ServerErrorException)
            awaitComplete()
        }
    }

    @Test
    fun getPartnerShops_forwards_the_id_and_location() = runTest(dispatcher) {
        val shop = ShopItemData(
            shopId = 100,
            name = "alibi coffee",
            pictureUrl = null,
            distance = 4.24,
            location = null,
            partnerId = 62,
        )
        coEvery { service.getPartnerShops(62, 41.3, 69.2) } returns Response.success(
            wrap(listOf(shop))
        )

        repo.getPartnerShops(62, 41.3, 69.2).test {
            val item = awaitItem()
            assertTrue(item is UIResource.Success)
            assertEquals(listOf(shop), (item as UIResource.Success).data)
            awaitComplete()
        }
        coVerify(exactly = 1) { service.getPartnerShops(62, 41.3, 69.2) }
    }

    /** A brand with no active shops is a live case — an empty list, never an error. */
    @Test
    fun getPartnerShops_success_with_no_shops_is_not_an_error() = runTest(dispatcher) {
        coEvery { service.getPartnerShops(any(), any(), any()) } returns Response.success(
            wrap(emptyList<ShopItemData>())
        )

        repo.getPartnerShops(1, null, null).test {
            val item = awaitItem()
            assertTrue(item is UIResource.Success)
            assertEquals(emptyList<ShopItemData>(), (item as UIResource.Success).data)
            awaitComplete()
        }
    }

    private fun partner(id: Int, name: String) = PartnerItemData(
        description = null,
        id = id,
        logoUrl = "https://files.hoopla.uz/images/$id.png",
        name = name
    )

    private fun <T> wrap(data: T?): BaseResponseData<T> = BaseResponseData(
        data = data,
        message = "ok",
        status = true,
        code = 200,
        meta = null
    )
}

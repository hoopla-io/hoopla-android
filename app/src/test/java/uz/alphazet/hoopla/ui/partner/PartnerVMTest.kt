package uz.alphazet.hoopla.ui.partner

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.PartnerItemData
import uz.alphazet.data.models.ShopItemData
import uz.alphazet.domain.network.NotFoundException
import uz.alphazet.domain.repositories.PartnerRepo
import uz.alphazet.hoopla.rules.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class PartnerVMTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repo: PartnerRepo = mockk()
    private val vm by lazy { PartnerVM(repo) }

    @Test
    fun getPartner_returns_the_brand() = runTest {
        val partner = PartnerItemData(
            description = null,
            id = 71,
            logoUrl = "https://files.hoopla.uz/images/71.png",
            name = "Miran Coffee"
        )
        coEvery { repo.getPartner(71) } returns flowOf(UIResource.Success(partner))

        vm.getPartner(71).test {
            val result = awaitItem()
            assertTrue(result is UIResource.Success)
            assertEquals("Miran Coffee", (result as UIResource.Success).data?.name)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { repo.getPartner(71) }
    }

    /** The header is optional: a brand the list no longer carries must not fail the screen. */
    @Test
    fun getPartner_propagates_not_found() = runTest {
        coEvery { repo.getPartner(any()) } returns flowOf(
            UIResource.Error(NotFoundException("partner 999 not found", 404))
        )

        vm.getPartner(999).test {
            assertTrue(awaitItem() is UIResource.Error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getPartnerShops_returns_the_shops_of_the_brand() = runTest {
        val shop = ShopItemData(
            shopId = 100,
            name = "alibi coffee",
            pictureUrl = null,
            distance = 4.24,
            location = null,
            partnerId = 62,
        )
        coEvery { repo.getPartnerShops(62, 41.3, 69.2) } returns flowOf(
            UIResource.Success(listOf(shop))
        )

        vm.getPartnerShops(62, 41.3, 69.2).test {
            val result = awaitItem()
            assertTrue(result is UIResource.Success)
            assertEquals(1, (result as UIResource.Success).data?.size)
            assertEquals(100, result.data?.first()?.shopId)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { repo.getPartnerShops(62, 41.3, 69.2) }
    }

    /** No fix in hand: the request still goes out, with the screen's fallback coordinates. */
    @Test
    fun getPartnerShops_forwards_the_coordinates_it_was_given() = runTest {
        coEvery { repo.getPartnerShops(any(), any(), any()) } returns flowOf(
            UIResource.Success(emptyList())
        )

        vm.getPartnerShops(62, null, null).test {
            assertTrue(awaitItem() is UIResource.Success)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { repo.getPartnerShops(62, null, null) }
    }
}

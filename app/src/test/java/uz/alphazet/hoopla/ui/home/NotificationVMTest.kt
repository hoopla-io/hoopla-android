package uz.alphazet.hoopla.ui.home

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.NotificationDetail
import uz.alphazet.domain.cache.AppCache
import uz.alphazet.domain.repositories.NotificationDataSource
import uz.alphazet.domain.repositories.NotificationRepo
import uz.alphazet.hoopla.rules.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationVMTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val dataSource: NotificationDataSource = mockk(relaxed = true)
    private val repo: NotificationRepo = mockk()
    private val cache: AppCache = mockk()

    private val vm by lazy {
        every { cache.lang } returns "ru"
        NotificationVM(dataSource, repo, cache)
    }

    // --- getNotificationDetail (Pattern B) --------------------------------

    @Test
    fun getNotificationDetail_updates_detailFlow_to_success() = runTest {
        val detail = NotificationDetail(5, "2026-01-01", null, false, "Full desc", 42, "Important", null, null)
        coEvery { repo.getNotificationDetail(42, "ru") } returns UIResource.Success(detail)

        vm.notificationDetailFlow.test {
            assertEquals(UIResource.Loading, awaitItem())
            vm.getNotificationDetail(42)
            val result = awaitItem()
            assertTrue(result is UIResource.Success)
            assertEquals(42, (result as UIResource.Success).data?.notificationId)
            assertEquals("Important", result.data?.notificationTitle)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- markRead ---------------------------------------------------------

    @Test
    fun markRead_completes_without_error() = runTest {
        coEvery { repo.markRead() } returns flowOf(UIResource.Success(Any()))

        // markRead() launches a coroutine that collects the flow. We just
        // assert it doesn't throw and the VM state remains intact.
        vm.markRead()

        // notificationDetailFlow stays at Loading since we didn't call getDetail
        vm.notificationDetailFlow.test {
            assertEquals(UIResource.Loading, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}

package uz.alphazet.hoopla.ui.profile.subscriptions

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.FeatureItemData
import uz.alphazet.data.models.SubscriptionItemData
import uz.alphazet.domain.network.PaymentException
import uz.alphazet.domain.repositories.SubscriptionRepo
import uz.alphazet.hoopla.rules.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class SubscriptionVMTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repo: SubscriptionRepo = mockk()
    private val vm by lazy { SubscriptionVM(repo) }

    // --- getSubscriptions (Pattern C) -------------------------------------

    @Test
    fun getSubscriptions_returns_list_with_features() = runTest {
        val subs = listOf(
            SubscriptionItemData(
                "UZS", 30, 1, "Gold", 49900.0,
                listOf(FeatureItemData(10, "Unlimited coffee"))
            )
        )
        coEvery { repo.getSubscriptions() } returns flowOf(UIResource.Success(subs))

        val flow = vm.getSubscriptions()
        flow.test {
            val result = awaitItem()
            assertTrue(result is UIResource.Success)
            val data = (result as UIResource.Success).data
            assertEquals(1, data?.size)
            assertEquals("Gold", data?.first()?.name)
            assertEquals("Unlimited coffee", data?.first()?.features?.first()?.feature)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- buySubscription (Pattern C) --------------------------------------

    @Test
    fun buySubscription_returns_success() = runTest {
        coEvery { repo.buySubscription(1) } returns flowOf(UIResource.Success(Any()))

        val flow = vm.buySubscription(1)
        flow.test {
            assertTrue(awaitItem() is UIResource.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun buySubscription_returns_error_when_repo_fails_with_402() = runTest {
        coEvery { repo.buySubscription(999) } returns flowOf(
            UIResource.Error(PaymentException(null, "payment required", 402))
        )

        val flow = vm.buySubscription(999)
        flow.test {
            val result = awaitItem()
            assertTrue(result is UIResource.Error)
            assertTrue((result as UIResource.Error).throwable is PaymentException)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

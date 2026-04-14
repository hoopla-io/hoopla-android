package uz.alphazet.hoopla.ui.profile

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
import uz.alphazet.data.models.UserData
import uz.alphazet.domain.repositories.ProfileRepo
import uz.alphazet.hoopla.rules.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileVMTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repo: ProfileRepo = mockk()
    private val vm by lazy { ProfileVM(repo) }

    private val userData = UserData("Ali", "998901234567", 1500.0, 200.0, "UZS", "M", null, 1, null, 0)

    // --- getUser (Pattern B) ----------------------------------------------

    @Test
    fun getUser_updates_userDataFlow_to_success() = runTest {
        coEvery { repo.getMe() } returns UIResource.Success(userData)

        vm.userDataFlow.test {
            assertEquals(UIResource.Loading, awaitItem())
            vm.getUser()
            val result = awaitItem()
            assertTrue(result is UIResource.Success)
            assertEquals("Ali", (result as UIResource.Success).data?.name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun editMe_updates_userDataFlow_to_success() = runTest {
        coEvery { repo.editMe() } returns UIResource.Success(userData)

        vm.userDataFlow.test {
            assertEquals(UIResource.Loading, awaitItem())
            vm.editMe()
            val result = awaitItem()
            assertTrue(result is UIResource.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- updateMe (Pattern C) ---------------------------------------------

    @Test
    fun updateMe_returns_success_via_shared_flow() = runTest {
        coEvery { repo.updateMe("Alisher", "M", null) } returns flowOf(UIResource.Success(Any()))

        val flow = vm.updateMe("Alisher", "M", null)
        flow.test {
            assertTrue(awaitItem() is UIResource.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- logout (Pattern C) -----------------------------------------------

    @Test
    fun logout_returns_success_via_shared_flow() = runTest {
        coEvery { repo.logout() } returns flowOf(UIResource.Success(Any()))

        val flow = vm.logout()
        flow.test {
            assertTrue(awaitItem() is UIResource.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- deactivate (Pattern C) -------------------------------------------

    @Test
    fun deactivate_returns_success_via_shared_flow() = runTest {
        coEvery { repo.deactivate() } returns flowOf(UIResource.Success(Any()))

        val flow = vm.deactivate()
        flow.test {
            assertTrue(awaitItem() is UIResource.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

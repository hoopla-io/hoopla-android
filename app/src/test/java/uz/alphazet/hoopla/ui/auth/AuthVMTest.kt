package uz.alphazet.hoopla.ui.auth

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.ConfirmSMSData
import uz.alphazet.data.models.LoginSessionData
import uz.alphazet.domain.network.RemoteException
import uz.alphazet.domain.repositories.AuthRepo
import uz.alphazet.hoopla.rules.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class AuthVMTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepo: AuthRepo = mockk()

    private val vm by lazy { AuthVM(authRepo) }

    // --- sendSms ----------------------------------------------------------

    @Test
    fun sendSms_emits_loading_then_success() = runTest {
        val session = LoginSessionData("998901234567", 60, "sess-abc")
        coEvery { authRepo.sendSMS("998901234567") } returns UIResource.Success(session)

        vm.sendSmsFlow.test {
            vm.sendSms("998901234567")
            assertEquals(UIResource.Loading, awaitItem())
            val result = awaitItem()
            assertTrue(result is UIResource.Success)
            assertEquals("sess-abc", (result as UIResource.Success).data?.sessionId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun sendSms_emits_loading_then_error() = runTest {
        val error = RemoteException("server error", 500)
        coEvery { authRepo.sendSMS(any()) } returns UIResource.Error(error)

        vm.sendSmsFlow.test {
            vm.sendSms("998901234567")
            assertEquals(UIResource.Loading, awaitItem())
            val result = awaitItem()
            assertTrue(result is UIResource.Error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- resendSms --------------------------------------------------------

    @Test
    fun resendSms_emits_loading_then_success() = runTest {
        val session = LoginSessionData("998901234567", 60, "sess-xyz")
        coEvery { authRepo.resendSMS("sess-abc") } returns UIResource.Success(session)

        vm.resendSmsFlow.test {
            vm.resendSms("sess-abc")
            assertEquals(UIResource.Loading, awaitItem())
            val result = awaitItem()
            assertTrue(result is UIResource.Success)
            assertEquals("sess-xyz", (result as UIResource.Success).data?.sessionId)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- confirmSMS -------------------------------------------------------

    @Test
    fun confirmSMS_emits_loading_then_success() = runTest {
        val jwt = ConfirmSMSData.JwtData("a.b.c", "r.s.t", 1_893_456_000L)
        val confirmData = ConfirmSMSData(42L, false, "998901234567", jwt)
        coEvery { authRepo.confirmSMS(1234, "sess-abc") } returns UIResource.Success(confirmData)

        vm.confirmSmsFlow.test {
            vm.confirmSMS(1234, "sess-abc")
            assertEquals(UIResource.Loading, awaitItem())
            val result = awaitItem()
            assertTrue(result is UIResource.Success)
            val data = (result as UIResource.Success).data
            assertEquals("a.b.c", data?.jwt?.accessToken)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun confirmSMS_emits_loading_then_error() = runTest {
        coEvery { authRepo.confirmSMS(any(), any()) } returns UIResource.Error(
            RemoteException("unauthorized", 401)
        )

        vm.confirmSmsFlow.test {
            vm.confirmSMS(0, "bad-session")
            assertEquals(UIResource.Loading, awaitItem())
            assertTrue(awaitItem() is UIResource.Error)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

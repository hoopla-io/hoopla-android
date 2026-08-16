package uz.alphazet.hoopla.ui.auth

import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.ConfirmSMSData
import uz.alphazet.data.models.LoginSessionData
import uz.alphazet.domain.cache.AppCache
import uz.alphazet.domain.repositories.AuthRepo
import uz.alphazet.domain.repositories.PushTokenRepo
import uz.alphazet.domain.ui.BaseVM
import uz.alphazet.domain.ui.load
import uz.alphazet.domain.utils.awaitToken
import uz.alphazet.domain.utils.log

class AuthVM(
    private val authRepo: AuthRepo,
    private val pushTokenRepo: PushTokenRepo,
    private val appCache: AppCache
) : BaseVM() {

    private val sendSmsEmitter: MutableSharedFlow<UIResource<LoginSessionData>> =
        MutableSharedFlow()
    val sendSmsFlow: SharedFlow<UIResource<LoginSessionData>> get() = sendSmsEmitter

    private val resendSmsEmitter: MutableSharedFlow<UIResource<LoginSessionData>> =
        MutableSharedFlow()
    val resendSmsFlow: SharedFlow<UIResource<LoginSessionData>> get() = resendSmsEmitter

    private val confirmSmsEmitter: MutableSharedFlow<UIResource<ConfirmSMSData>> =
        MutableSharedFlow()
    val confirmSmsFlow: SharedFlow<UIResource<ConfirmSMSData>> get() = confirmSmsEmitter


    fun sendSms(phone: String) {
        launch {
            sendSmsEmitter.load { authRepo.sendSMS(phone) }
        }
    }

    fun resendSms(sessionId: String) {
        launch {
            resendSmsEmitter.load { authRepo.resendSMS(sessionId) }
        }
    }

    fun confirmSMS(code: Int, sessionId: String) {
        launch {
            confirmSmsEmitter.load { authRepo.confirmSMS(code, sessionId) }
        }
    }

    // Best-effort: upload the FCM token right after a successful login so the
    // backend can start sending order-status pushes to this device/session.
    // Failures are swallowed - this must never block or fail the login flow.
    fun registerPushToken() {
        launch {
            runCatching {
                val token = FirebaseMessaging.getInstance().awaitToken()
                appCache.fcmToken = token
                pushTokenRepo.registerPushToken(token)
            }.onFailure { it.log("AuthVM.registerPushToken") }
        }
    }

}
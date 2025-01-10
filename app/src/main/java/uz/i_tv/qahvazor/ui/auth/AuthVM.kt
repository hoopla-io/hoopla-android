package uz.i_tv.qahvazor.ui.auth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import uz.i_tv.data.UIResource
import uz.i_tv.data.models.AccessTokenData
import uz.i_tv.data.models.ConfirmSMSData
import uz.i_tv.data.models.LoginSessionData
import uz.i_tv.domain.repositories.AuthRepo
import uz.i_tv.domain.ui.BaseVM
import uz.i_tv.domain.ui.load

class AuthVM(private val authRepo: AuthRepo) : BaseVM() {

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

}
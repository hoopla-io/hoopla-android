package uz.alphazet.hoopla.ui.profile

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.shareIn
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.UserData
import uz.alphazet.domain.cache.AppCache
import uz.alphazet.domain.repositories.ProfileRepo
import uz.alphazet.domain.repositories.PushTokenRepo
import uz.alphazet.domain.ui.BaseVM
import uz.alphazet.domain.ui.load
import uz.alphazet.domain.utils.log

class ProfileVM(
    private val repo: ProfileRepo,
    private val pushTokenRepo: PushTokenRepo,
    private val appCache: AppCache
) : BaseVM() {

    private val userDataEmitter: MutableStateFlow<UIResource<UserData>> =
        MutableStateFlow(UIResource.Loading)
    val userDataFlow: StateFlow<UIResource<UserData>> get() = userDataEmitter

    fun getUser() {
        launch { userDataEmitter.load { repo.getMe() } }
    }

    fun editMe() {
        launch { userDataEmitter.load { repo.editMe() } }
    }

    suspend fun updateMe(
        name: String?,
        gender: String?,
        dateOfBirth: String?
    ): SharedFlow<UIResource<Any>> {
        return repo.updateMe(name, gender, dateOfBirth)
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)
    }

    suspend fun logout(): SharedFlow<UIResource<Any>> {
        unregisterPushTokenBestEffort()
        return repo.logout()
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)
    }

    // Best-effort: tell the backend to stop sending pushes to this device's
    // token before the session is torn down. Fire-and-forget - must not block
    // or fail the logout flow itself.
    private fun unregisterPushTokenBestEffort() {
        val token = appCache.fcmToken
        if (token.isNullOrEmpty()) return

        launch {
            runCatching { pushTokenRepo.unregisterPushToken(token) }
                .onFailure { it.log("ProfileVM.unregisterPushToken") }
        }
    }

    suspend fun deactivate(): SharedFlow<UIResource<Any>> {
        return repo.deactivate()
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)
    }

}
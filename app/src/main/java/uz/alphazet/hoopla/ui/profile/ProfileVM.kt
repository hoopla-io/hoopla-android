package uz.alphazet.hoopla.ui.profile

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.shareIn
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.UserData
import uz.alphazet.domain.repositories.ProfileRepo
import uz.alphazet.domain.ui.BaseVM
import uz.alphazet.domain.ui.load

class ProfileVM(private val repo: ProfileRepo) : BaseVM() {

    private val userDataEmitter: MutableStateFlow<UIResource<UserData>> =
        MutableStateFlow(UIResource.Loading)
    val userDataFlow: StateFlow<UIResource<UserData>> get() = userDataEmitter

    fun getUser() {
        launch {
            userDataEmitter.load { repo.getMe() }
        }
    }

    suspend fun logout(): SharedFlow<UIResource<Any>> {
        return repo.logout()
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)
    }

}
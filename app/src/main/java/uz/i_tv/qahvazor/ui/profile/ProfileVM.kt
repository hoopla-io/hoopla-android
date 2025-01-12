package uz.i_tv.qahvazor.ui.profile

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.shareIn
import uz.i_tv.data.UIResource
import uz.i_tv.data.models.UserData
import uz.i_tv.domain.repositories.ProfileRepo
import uz.i_tv.domain.ui.BaseVM
import uz.i_tv.domain.ui.load

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
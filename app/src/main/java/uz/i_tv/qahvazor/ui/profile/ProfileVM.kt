package uz.i_tv.qahvazor.ui.profile

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import uz.i_tv.data.UIResource
import uz.i_tv.data.models.UserData
import uz.i_tv.domain.repositories.ProfileRepo
import uz.i_tv.domain.ui.BaseVM

class ProfileVM(private val repo: ProfileRepo) : BaseVM() {

    suspend fun getMe(): SharedFlow<UIResource<UserData>> {
        return repo.getMe()
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)
    }

    suspend fun logout(): SharedFlow<UIResource<Any>> {
        return repo.logout()
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)
    }

}
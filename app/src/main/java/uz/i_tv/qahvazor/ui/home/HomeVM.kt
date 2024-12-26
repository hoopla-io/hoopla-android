package uz.i_tv.qahvazor.ui.home

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import uz.i_tv.data.UIResource
import uz.i_tv.data.models.CompanyItemData
import uz.i_tv.domain.repositories.HomeRepo
import uz.i_tv.domain.ui.BaseVM

class HomeVM(private val homeRepo: HomeRepo) : BaseVM() {

    suspend fun getCompanies(): SharedFlow<UIResource<List<CompanyItemData>>> {
        return homeRepo.getCompanies()
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)
    }

}
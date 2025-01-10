package uz.i_tv.domain.repositories

import uz.i_tv.data.services.ProfileService
import uz.i_tv.domain.network.BaseRepo

class ProfileRepo(private val api: ProfileService) : BaseRepo() {

    suspend fun getMe() = handleFlow {
        api.getMe()
    }

    suspend fun logout() = handleFlow {
        api.logout()
    }

}
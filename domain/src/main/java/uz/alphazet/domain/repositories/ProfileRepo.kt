package uz.alphazet.domain.repositories

import uz.alphazet.data.services.ProfileService
import uz.alphazet.domain.network.BaseRepo

class ProfileRepo(private val api: ProfileService) : BaseRepo() {

    suspend fun getMe() = handle {
        api.getMe()
    }

    suspend fun logout() = handleFlow {
        api.logout()
    }

}
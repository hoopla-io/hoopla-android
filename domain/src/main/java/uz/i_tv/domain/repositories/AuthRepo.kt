package uz.i_tv.domain.repositories

import uz.i_tv.data.services.AuthService
import uz.i_tv.domain.network.BaseRepo

class AuthRepo(private val authService: AuthService) : BaseRepo() {

    suspend fun login(phoneNumber: String) = handle {
        authService.login(phoneNumber)
    }

}
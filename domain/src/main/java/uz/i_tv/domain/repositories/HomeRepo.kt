package uz.i_tv.domain.repositories

import uz.i_tv.data.services.HomeService
import uz.i_tv.domain.network.BaseRepo

class HomeRepo(private val homeService: HomeService) : BaseRepo() {

    suspend fun getPartners() = handleFlow {
        homeService.getPartners()
    }

}
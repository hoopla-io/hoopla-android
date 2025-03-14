package uz.alphazet.domain.repositories

import uz.alphazet.data.services.HomeService
import uz.alphazet.domain.network.BaseRepo

class HomeRepo(private val homeService: HomeService) : BaseRepo() {

    suspend fun getPartners() = handleFlow {
        homeService.getPartners()
    }

    suspend fun getNearShops(lat: Double, long: Double, name: String?) = handleFlow {
        homeService.getNearShops(lat, long, name)
    }

}
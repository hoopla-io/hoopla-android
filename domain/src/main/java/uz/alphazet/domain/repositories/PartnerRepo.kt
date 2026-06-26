package uz.alphazet.domain.repositories

import uz.alphazet.data.services.PartnerService
import uz.alphazet.domain.network.BaseRepo

class PartnerRepo(private val partnerService: PartnerService) : BaseRepo() {

    suspend fun getPartners(name: String?) = handleFlow {
        partnerService.getPartners(name)
    }

    suspend fun getPartnerShops(partnerId: Int, lat: Double?, long: Double?) = handleFlow {
        partnerService.getPartnerShops(partnerId, lat, long)
    }

}
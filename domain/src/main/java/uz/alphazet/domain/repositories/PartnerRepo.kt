package uz.alphazet.domain.repositories

import uz.alphazet.data.services.PartnerService
import uz.alphazet.domain.network.BaseRepo

class PartnerRepo(private val companyService: PartnerService) : BaseRepo() {

    suspend fun getPartnerDetail(partnerId: Int) = handleFlow {
        companyService.getPartnerDetail(partnerId)
    }

    suspend fun getPartnerShops(partnerId: Int) = handleFlow {
        companyService.getPartnerShops(partnerId)
    }

}
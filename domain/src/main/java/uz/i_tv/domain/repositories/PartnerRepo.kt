package uz.i_tv.domain.repositories

import uz.i_tv.data.services.PartnerService
import uz.i_tv.domain.network.BaseRepo

class PartnerRepo(private val companyService: PartnerService) : BaseRepo() {

    suspend fun getPartnerDetail(partnerId: Int) = handleFlow {
        companyService.getPartnerDetail(partnerId)
    }

    suspend fun getPartnerShops(partnerId: Int) = handleFlow {
        companyService.getPartnerShops(partnerId)
    }

}
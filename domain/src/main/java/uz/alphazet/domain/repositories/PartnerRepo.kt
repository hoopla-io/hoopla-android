package uz.alphazet.domain.repositories

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.PartnerItemData
import uz.alphazet.data.services.PartnerService
import uz.alphazet.domain.network.BaseRepo
import uz.alphazet.domain.network.NotFoundException

class PartnerRepo(private val partnerService: PartnerService) : BaseRepo() {

    suspend fun getPartners(name: String?) = handleFlow {
        partnerService.getPartners(name)
    }

    suspend fun getPartnerShops(partnerId: Int, lat: Double?, long: Double?) = handleFlow {
        partnerService.getPartnerShops(partnerId, lat, long)
    }

    /**
     * One partner by id. There is no partner-detail endpoint, so the brand is picked out of the
     * unfiltered list — a small, un-paginated payload of every partner that has an active shop.
     * A partner that is not in it (deactivated since the link was authored) comes back as a
     * [NotFoundException] rather than an empty success, so the caller can tell the two apart.
     */
    suspend fun getPartner(partnerId: Int): Flow<UIResource<PartnerItemData>> =
        getPartners(null).map { resource ->
            when (resource) {
                is UIResource.Success ->
                    resource.data?.firstOrNull { it.id == partnerId }
                        ?.let { UIResource.Success(it) }
                        ?: UIResource.Error(NotFoundException("partner $partnerId not found", 404))

                is UIResource.Error -> resource
                UIResource.Loading -> UIResource.Loading
            }
        }

}

package uz.alphazet.domain.repositories

import uz.alphazet.data.services.ShopService
import uz.alphazet.domain.network.BaseRepo

class ShopRepo(private val shopService: ShopService) : BaseRepo() {

    suspend fun getShopDetail(shopId: Int) = handleFlow {
        shopService.getShopDetail(shopId)
    }

}
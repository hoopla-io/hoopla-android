package uz.i_tv.domain.repositories

import uz.i_tv.data.services.ShopService
import uz.i_tv.domain.network.BaseRepo

class ShopRepo(private val shopService: ShopService) : BaseRepo() {

    suspend fun getShopDetail(shopId: Int) = handleFlow {
        shopService.getShopDetail(shopId)
    }

}
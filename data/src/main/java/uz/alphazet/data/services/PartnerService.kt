package uz.alphazet.data.services

import retrofit2.http.GET
import retrofit2.http.Query
import uz.alphazet.data.BaseResponse
import uz.alphazet.data.models.PartnerItemData
import uz.alphazet.data.models.ShopItemData

interface PartnerService {

    /**
     * Search/list partners (brands). [name] is an optional case-insensitive
     * substring filter; absent = all partners that have at least one active shop.
     * Sorted best-rated first.
     */
    @GET("v1/partners/list")
    suspend fun getPartners(
        @Query("name") name: String?
    ): BaseResponse<List<PartnerItemData>>

    /**
     * Active shops of the given partner. With both [lat] and [long] each shop
     * carries a distance (km) and the list is sorted nearest-first; otherwise
     * sorted by name with distance 0.
     */
    @GET("v1/partners/shops")
    suspend fun getPartnerShops(
        @Query("partnerId") partnerId: Int,
        @Query("lat") lat: Double?,
        @Query("long") long: Double?,
    ): BaseResponse<List<ShopItemData>>

}
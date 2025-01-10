package uz.i_tv.data.services

import retrofit2.http.GET
import uz.i_tv.data.BaseResponse
import uz.i_tv.data.models.PartnerItemData

interface HomeService {

    @GET("v1/partners")
    suspend fun getPartners(): BaseResponse<List<PartnerItemData>>

}
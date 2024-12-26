package uz.i_tv.data.services

import retrofit2.http.GET
import uz.i_tv.data.BaseResponse
import uz.i_tv.data.models.CompanyItemData

interface HomeService {

    @GET("v1/company/list")
    suspend fun getCompanies(): BaseResponse<List<CompanyItemData>>

}
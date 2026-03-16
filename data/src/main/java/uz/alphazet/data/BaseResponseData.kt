package uz.alphazet.data

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import retrofit2.Response

//typealias Any = Response<Any>
typealias AnyResponse = Response<BaseResponseData<Any>>
typealias BaseResponse<T> = Response<BaseResponseData<T>>

@Keep
data class BaseResponseData<T>(
    @SerializedName("data")
    val data: T?,
    @SerializedName("message")
    val message: String,
    @SerializedName("status")
    val status: Boolean,
    @SerializedName("code")
    val code: Int,
    @SerializedName("meta")
    val meta: MetaData?
) {
    @Keep
    data class MetaData(
        @SerializedName("totalItems")
        val totalItems: Int?,
        @SerializedName("itemsPerPage")
        val itemsPerPage: Int?,
        @SerializedName("currentPage")
        val currentPage: Int?,
        @SerializedName("lastPage")
        val lastPage: Int?
    )
}

@Keep
data class BaseErrorResponse(
    @SerializedName("message")
    val message: String? = null
)




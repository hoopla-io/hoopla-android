package uz.alphazet.data

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import retrofit2.Response
import kotlin.math.ceil

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
        @SerializedName("totalPages")
        val totalPages: Int =
            ceil((totalItems?.toDouble() ?: 0.0) / (currentPage?.toDouble() ?: 0.0)).toInt()
    )
}

@Keep
data class BaseErrorResponse(
    @SerializedName("message")
    val message: String? = null
)




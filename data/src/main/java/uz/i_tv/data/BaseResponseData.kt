package uz.i_tv.data

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
    val code: Int

)

@Keep
data class BaseErrorResponse(

    @SerializedName("message")
    val message: String

)




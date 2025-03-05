package uz.alphazet.domain.network

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import retrofit2.Response
import uz.alphazet.data.BaseErrorResponse
import uz.alphazet.data.BaseResponseData
import uz.alphazet.data.UIResource
import java.net.ConnectException
import java.net.UnknownHostException

abstract class BaseRepo {

    protected suspend fun <T> handleFlow(
        callback: suspend () -> Response<BaseResponseData<T>>
    ): Flow<UIResource<T>> = flow { emit(handle(callback)) }

    protected suspend fun <T> handle(
        callback: suspend () -> Response<BaseResponseData<T>>
    ): UIResource<T> {
        return withContext(IO) {
            try {
                val response: Response<BaseResponseData<T>> = callback()

                val body: BaseResponseData<T>? = response.body()
                val data: T? = body?.data
                val message: String = body?.message ?: ""

                if (response.isSuccessful) {
                    UIResource.Success(data)
                } else UIResource.Error(
                    throwException(
                        response.code(),
                        response.errorBody()?.string(),
                        message
                    )
                )
            } catch (e: Exception) {
                UIResource.Error(handleDeviceException(e))
            }

        }
    }

    protected suspend fun <T> handleX(
        callback: suspend () -> Response<BaseResponseData<T>>
    ): UIResource<BaseResponseData<T>> {
        return withContext(IO) {
            try {
                val response: Response<BaseResponseData<T>> = callback()

                val body: BaseResponseData<T>? = response.body()
                val data: T? = body?.data
                val message: String = body?.message ?: ""

                if (response.isSuccessful) {
                    UIResource.Success(body)
                } else UIResource.Error(
                    throwException(
                        response.code(),
                        response.errorBody()?.string(),
                        message
                    )
                )
            } catch (e: Exception) {
                UIResource.Error(handleDeviceException(e))
            }

        }
    }

    private fun throwException(
        code: Int,
        errorBodyJson: String?,
        message: String?
    ): Throwable {

        val errorData: BaseErrorResponse? = try {
            Gson().fromJson<BaseErrorResponse?>(
                errorBodyJson,
                object : TypeToken<BaseErrorResponse>() {}.type
            )
        } catch (e: Exception) {
            null
        }
        throw return when (code) {
            401 -> UnauthorizedException(errorData?.message ?: message, code)
            402 -> PaymentException(errorData?.message ?: message, code)

            422 -> ValidationException(errorData?.message ?: message, code)
            400 -> BadRequestException(errorData?.message ?: message, code)
            403 -> ForbiddenException(errorData?.message ?: message, code)
            409 -> ConflictException(errorData?.message ?: message, code)
            404 -> NotFoundException(errorData?.message ?: message, code)
            429 -> TooManyRequestException(errorData?.message ?: message, code)
            in 500..599 -> ServerErrorException(errorData?.message ?: message, code)

            //check other cases

            else -> RemoteException(code = code, message = errorData?.message ?: message)
        }
    }

    private fun handleDeviceException(e: Throwable): Throwable {
        return when (e) {
            is UnknownHostException,
            is ConnectException -> ConnectionErrorException(e.message, e.hashCode())

            else -> RemoteException(e.message, -1)
        }
    }


}
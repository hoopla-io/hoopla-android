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
import uz.alphazet.data.models.order.PaymentRequiredExceptionData
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

        // Not every endpoint wraps its reason in the usual {"message": ...} envelope — the
        // scheduled-pickup 409s answer with a bare sentence. Without this fallback the
        // customer would get a blank toast instead of an actionable "pick another time".
        val reason = errorData?.message?.takeIf { it.isNotBlank() }
            ?: message?.takeIf { it.isNotBlank() }
            ?: errorBodyJson.asPlainTextReason()

        throw return when (code) {
            400 -> BadRequestException(reason, code)
            401 -> UnauthorizedException(reason, code)
            402 -> {
                val errorBody = try {
                    Gson().fromJson<BaseResponseData<PaymentRequiredExceptionData>>(
                        errorBodyJson,
                        object :
                            TypeToken<BaseResponseData<PaymentRequiredExceptionData>>() {}.type
                    )
                } catch (e: Throwable) {
                    null
                }
                PaymentException(errorBody?.data, reason, code)
            }

            403 -> ForbiddenException(reason, code)
            404 -> NotFoundException(reason, code)
            409 -> ConflictException(reason, code)
            422 -> ValidationException(reason, code)
            428 -> PreconditionRequiredException(reason, code)
            429 -> TooManyRequestException(reason, code)
            in 500..599 -> ServerErrorException(reason, code)

            //check other cases

            else -> RemoteException(code = code, message = reason)
        }
    }

    /**
     * The raw error body, when it looks like a human-readable reason rather than a structure
     * we failed to parse. JSON, arrays and HTML error pages are rejected — showing those to a
     * customer is worse than showing nothing.
     */
    private fun String?.asPlainTextReason(): String? {
        val raw = this?.trim() ?: return null
        if (raw.isEmpty() || raw.length > MAX_PLAIN_TEXT_REASON) return null
        if (raw.startsWith("{") || raw.startsWith("[") || raw.startsWith("<")) return null
        return raw
    }

    private companion object {
        /** Longer than this and it is a page or a stack trace, not a message for a customer. */
        const val MAX_PLAIN_TEXT_REASON = 300
    }

    private fun handleDeviceException(e: Throwable): Throwable {
        return when (e) {
            is UnknownHostException,
            is ConnectException -> ConnectionErrorException(e.message, e.hashCode())

            else -> RemoteException(e.message, -1)
        }
    }


}
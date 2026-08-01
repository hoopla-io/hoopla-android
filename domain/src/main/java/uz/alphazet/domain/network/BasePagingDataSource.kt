package uz.alphazet.domain.network

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import uz.alphazet.data.BaseErrorResponse
import uz.alphazet.data.BaseResponse
import uz.alphazet.data.BaseResponseData
import uz.alphazet.data.models.order.PaymentRequiredExceptionData
import java.net.ConnectException
import java.net.UnknownHostException

abstract class BasePagingDataSource<Value : Any> : PagingSource<Int, Value>() {

    override fun getRefreshKey(state: PagingState<Int, Value>): Int? {
        val anchorPosition = state.anchorPosition ?: return null
        val page = state.closestPageToPosition(anchorPosition) ?: return null
        return page.prevKey?.plus(1) ?: page.nextKey?.minus(1)
    }

    protected suspend fun handle(
        body: suspend () -> BaseResponse<List<Value>>
    ): LoadResult<Int, Value> {
        return try {
            val response: BaseResponse<List<Value>> = body()
            handleResource(response)
        } catch (e: Exception) {
            LoadResult.Error(handleDeviceException(e))
        }
    }

    private fun handleResource(response: BaseResponse<List<Value>>): LoadResult<Int, Value> {

        val res: BaseResponseData<List<Value>>? = response.body()
        val data = res?.data

        val message = response.message()

        val errorData = BaseErrorResponse()

        return if (response.isSuccessful) {
            if (data != null) {
                val page = res.meta?.currentPage ?: 0
                val allPageCount = res.meta?.lastPage ?: 0

                val nextKey = if (page >= allPageCount) null else page + 1
                val prevKey = if (page == 1) null else page - 1

                LoadResult.Page(data, prevKey, nextKey)
            } else LoadResult.Error(NullPointerException(errorData.message))
        } else LoadResult.Error(
            throwException(
                response.code(),
                response.errorBody()?.string(),
                message
            )
        )
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

        // Mirrors BaseRepo: not every endpoint wraps its reason in the usual
        // {"message": ...} envelope. Here [message] is the HTTP reason phrase ("Conflict"),
        // so a sentence the server actually wrote outranks it — it is the only one of the
        // two that tells the customer what to do about it.
        val reason = errorData?.message?.takeIf { it.isNotBlank() }
            ?: errorBodyJson.asPlainTextReason()
            ?: message?.takeIf { it.isNotBlank() }

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

    private fun handleDeviceException(e: Throwable): Throwable {
        return when (e) {
            is UnknownHostException, is NullPointerException,
            is ConnectException -> ConnectionErrorException(e.message, e.hashCode())

            else -> RemoteException(e.message, -1)
        }
    }

    private companion object {
        /** Longer than this and it is a page or a stack trace, not a message for a customer. */
        const val MAX_PLAIN_TEXT_REASON = 300
    }
}
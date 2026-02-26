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
                val allPageCount = res.meta?.totalPages ?: 0

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
        throw return when (code) {
            400 -> BadRequestException(errorData?.message ?: message, code)
            401 -> UnauthorizedException(errorData?.message ?: message, code)
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
                PaymentException(errorBody?.data, errorData?.message ?: message, code)
            }

            403 -> ForbiddenException(errorData?.message ?: message, code)
            404 -> NotFoundException(errorData?.message ?: message, code)
            409 -> ConflictException(errorData?.message ?: message, code)
            422 -> ValidationException(errorData?.message ?: message, code)
            428 -> PreconditionRequiredException(errorData?.message ?: message, code)
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
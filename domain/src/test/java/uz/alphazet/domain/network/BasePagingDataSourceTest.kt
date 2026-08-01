package uz.alphazet.domain.network

import androidx.paging.PagingSource
import com.google.gson.Gson
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import uz.alphazet.data.BaseResponse
import uz.alphazet.data.BaseResponseData

/**
 * Covers how a paged endpoint's error body turns into a message the customer sees.
 *
 * Unlike [BaseRepo], the fallback `message` here is the HTTP reason phrase ("Conflict"),
 * which is never actionable — so a sentence the server actually wrote has to outrank it.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BasePagingDataSourceTest {

    private val source = TestSource()

    @Test
    fun plain_text_error_body_becomes_the_exception_message() = runTest {
        val reason = "pickup time must be at least 10 minutes from now"

        val error = source.loadError(409, body = reason, httpMessage = "Conflict")

        assertTrue(error is ConflictException)
        assertEquals(reason, error.message)
    }

    @Test
    fun json_error_body_still_wins_over_everything_else() = runTest {
        val error = source.loadError(
            409,
            body = Gson().toJson(mapOf("message" to "already claimed")),
            httpMessage = "Conflict"
        )

        assertEquals("already claimed", error.message)
    }

    @Test
    fun markup_error_body_falls_back_to_the_http_reason_phrase() = runTest {
        val error = source.loadError(
            500,
            body = "<html><body>502 Bad Gateway</body></html>",
            httpMessage = "Internal Server Error"
        )

        assertTrue(error is ServerErrorException)
        assertEquals("Internal Server Error", error.message)
    }

    @Test
    fun oversized_plain_text_error_body_falls_back_to_the_http_reason_phrase() = runTest {
        val error = source.loadError(400, body = "x".repeat(301), httpMessage = "Bad Request")

        assertTrue(error is BadRequestException)
        assertEquals("Bad Request", error.message)
    }

    @Test
    fun a_plain_text_reason_survives_on_every_mapped_status() = runTest {
        assertEquals("token gone", source.loadError(401, "token gone").message)
        assertEquals("not yours", source.loadError(403, "not yours").message)
        assertEquals("no such page", source.loadError(404, "no such page").message)
        assertEquals("slow down", source.loadError(429, "slow down").message)
    }

    @Test
    fun a_successful_page_still_carries_its_data_and_keys() = runTest {
        val result = source.runHandle {
            Response.success(
                BaseResponseData(
                    data = listOf("a", "b"),
                    message = "ok",
                    status = true,
                    code = 200,
                    meta = null
                )
            )
        }

        assertTrue(result is PagingSource.LoadResult.Page)
        assertEquals(listOf("a", "b"), (result as PagingSource.LoadResult.Page).data)
    }

    // --- helpers ----------------------------------------------------------

    /** Exposes the protected `handle` helper and unwraps the mapped throwable for tests. */
    private class TestSource : BasePagingDataSource<String>() {

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, String> =
            LoadResult.Page(emptyList(), null, null)

        suspend fun runHandle(
            body: suspend () -> BaseResponse<List<String>>
        ): LoadResult<Int, String> = handle(body)

        /**
         * Retrofit's own `Response.error(code, body)` hardcodes the reason phrase, so the raw
         * response is built here to keep the JSON-vs-text-vs-status precedence observable.
         */
        suspend fun loadError(
            code: Int,
            body: String,
            httpMessage: String = "Conflict"
        ): Throwable {
            val raw = okhttp3.Response.Builder()
                .code(code)
                .message(httpMessage)
                .protocol(Protocol.HTTP_1_1)
                .request(Request.Builder().url("http://localhost/orders").build())
                .build()
            val response: BaseResponse<List<String>> = Response.error(
                body.toResponseBody("application/json".toMediaType()),
                raw
            )

            val result = handle { response }
            assertTrue("expected Error, got $result", result is LoadResult.Error)
            return (result as LoadResult.Error).throwable
        }
    }
}

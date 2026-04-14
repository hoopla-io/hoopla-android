package uz.alphazet.data

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BaseResponseDataTest {

    private val gson = Gson()

    @Test
    fun deserializes_success_envelope_with_string_data() {
        val json = """
            {
                "data": "hello",
                "message": "ok",
                "status": true,
                "code": 200,
                "meta": null
            }
        """.trimIndent()

        val type = object : TypeToken<BaseResponseData<String>>() {}.type
        val parsed: BaseResponseData<String> = gson.fromJson(json, type)

        assertEquals("hello", parsed.data)
        assertEquals("ok", parsed.message)
        assertTrue(parsed.status)
        assertEquals(200, parsed.code)
        assertNull(parsed.meta)
    }

    @Test
    fun deserializes_envelope_with_null_data() {
        val json = """
            {
                "data": null,
                "message": "gone",
                "status": false,
                "code": 404,
                "meta": null
            }
        """.trimIndent()

        val type = object : TypeToken<BaseResponseData<String>>() {}.type
        val parsed: BaseResponseData<String> = gson.fromJson(json, type)

        assertNull(parsed.data)
        assertEquals("gone", parsed.message)
        assertFalse(parsed.status)
        assertEquals(404, parsed.code)
    }

    @Test
    fun deserializes_envelope_with_nested_meta() {
        val json = """
            {
                "data": [1, 2, 3],
                "message": "ok",
                "status": true,
                "code": 200,
                "meta": {
                    "totalItems": 42,
                    "itemsPerPage": 10,
                    "currentPage": 3,
                    "lastPage": 5
                }
            }
        """.trimIndent()

        val type = object : TypeToken<BaseResponseData<List<Int>>>() {}.type
        val parsed: BaseResponseData<List<Int>> = gson.fromJson(json, type)

        assertEquals(listOf(1, 2, 3), parsed.data)
        val meta = parsed.meta
        assertNotNull(meta)
        assertEquals(42, meta!!.totalItems)
        assertEquals(10, meta.itemsPerPage)
        assertEquals(3, meta.currentPage)
        assertEquals(5, meta.lastPage)
    }

    @Test
    fun meta_data_tolerates_missing_fields() {
        val json = """{ "totalItems": 7 }"""

        val meta: BaseResponseData.MetaData = gson.fromJson(json, BaseResponseData.MetaData::class.java)

        assertEquals(7, meta.totalItems)
        assertNull(meta.itemsPerPage)
        assertNull(meta.currentPage)
        assertNull(meta.lastPage)
    }

    @Test
    fun base_error_response_deserializes_message() {
        val json = """{ "message": "validation failed" }"""

        val err = gson.fromJson(json, BaseErrorResponse::class.java)

        assertEquals("validation failed", err.message)
    }

    @Test
    fun base_error_response_tolerates_missing_message() {
        val json = """{ }"""

        val err = gson.fromJson(json, BaseErrorResponse::class.java)

        assertNull(err.message)
    }

    @Test
    fun envelope_round_trips_through_gson() {
        val original = BaseResponseData(
            data = "payload",
            message = "ok",
            status = true,
            code = 200,
            meta = BaseResponseData.MetaData(
                totalItems = 100,
                itemsPerPage = 20,
                currentPage = 1,
                lastPage = 5
            )
        )

        val roundTripped: BaseResponseData<String> = gson.fromJson(
            gson.toJson(original),
            object : TypeToken<BaseResponseData<String>>() {}.type
        )

        assertEquals(original, roundTripped)
    }
}
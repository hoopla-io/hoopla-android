package uz.alphazet.data.services

import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProfileServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: ProfileService

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        service = retrofitFor(server)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun getMe_parses_full_user_payload() = runTest {
        server.enqueue(
            mockOk(
                """
                {
                  "data": {
                    "name": "Ali",
                    "phoneNumber": "998901234567",
                    "balance": 1500.0,
                    "cashback_balance": 250.5,
                    "currency": "UZS",
                    "gender": "M",
                    "dateOfBirth": "1990-01-01",
                    "userId": 42,
                    "subscription": null,
                    "unreadNotifications": 2
                  },
                  "message": "ok", "status": true, "code": 200, "meta": null
                }
                """.trimIndent()
            )
        )

        val response = service.getMe()

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/v1/user/get-me", recorded.path)

        val user = response.body()?.data
        assertNotNull(user)
        assertEquals("Ali", user!!.name)
        assertEquals(42, user.userId)
        assertEquals(250.5, user.cashbackBalance!!, 0.0)
    }

    @Test
    fun editMe_uses_get_at_edit_me_path() = runTest {
        server.enqueue(
            mockOk(
                """{"data":null,"message":"ok","status":true,"code":200,"meta":null}"""
            )
        )

        service.editMe()

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/v1/user/edit-me", recorded.path)
    }

    @Test
    fun logout_is_post_and_returns_envelope() = runTest {
        server.enqueue(
            mockOk("""{"data":null,"message":"bye","status":true,"code":200,"meta":null}""")
        )

        val response = service.logout()

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/v1/user/logout", recorded.path)
        assertTrue(response.isSuccessful)
        assertEquals("bye", response.body()?.message)
    }

    @Test
    fun updateMe_sends_put_with_request_body() = runTest {
        server.enqueue(
            mockOk("""{"data":null,"message":"ok","status":true,"code":200,"meta":null}""")
        )

        val body = """{"name":"Ali","gender":"M"}"""
        service.updateMe(body.toRequestBody("application/json".toMediaType()))

        val recorded = server.takeRequest()
        assertEquals("PUT", recorded.method)
        assertEquals("/v1/user/update-me", recorded.path)
        assertEquals(body, recorded.body.readUtf8())
    }

    @Test
    fun deactivate_uses_delete_verb() = runTest {
        server.enqueue(
            mockOk("""{"data":null,"message":"ok","status":true,"code":200,"meta":null}""")
        )

        service.deactivate()

        val recorded = server.takeRequest()
        assertEquals("DELETE", recorded.method)
        assertEquals("/v1/user/deactivate", recorded.path)
    }

    @Test
    fun error_response_surfaces_raw_error_body() = runTest {
        server.enqueue(mockError(422, """{"message":"validation failed"}"""))

        val response = service.getMe()

        assertFalse(response.isSuccessful)
        assertEquals(422, response.code())
        val errorBody = response.errorBody()?.string()
        assertNotNull(errorBody)
        assertTrue(errorBody!!.contains("validation failed"))
    }
}
package uz.alphazet.data.services

import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: AuthService

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
    fun sendSMS_posts_to_login_endpoint_and_parses_session() = runTest {
        server.enqueue(
            mockOk(
                """
                {
                  "data": {
                    "phoneNumber": "998901234567",
                    "sessionExpiresAt": 60,
                    "sessionId": "sess-abc"
                  },
                  "message": "ok",
                  "status": true,
                  "code": 200,
                  "meta": null
                }
                """.trimIndent()
            )
        )

        val json = """{"phoneNumber":"998901234567"}"""
        val response = service.sendSMS(json.toRequestBody("application/json".toMediaType()))

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/v1/auth/login", recorded.path)
        assertEquals(json, recorded.body.readUtf8())

        assertTrue(response.isSuccessful)
        val session = response.body()?.data
        assertNotNull(session)
        assertEquals("998901234567", session!!.phoneNumber)
        assertEquals("sess-abc", session.sessionId)
        assertEquals(60, session.sessionExpiresAt)
    }

    @Test
    fun resendSMS_posts_to_resend_sms_endpoint() = runTest {
        server.enqueue(
            mockOk(
                """
                {
                  "data": {
                    "phoneNumber": "998901234567",
                    "sessionExpiresAt": 60,
                    "sessionId": "sess-xyz"
                  },
                  "message": "ok", "status": true, "code": 200, "meta": null
                }
                """.trimIndent()
            )
        )

        service.resendSMS("{}".toRequestBody("application/json".toMediaType()))

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/v1/auth/resend-sms", recorded.path)
    }

    @Test
    fun confirmSMS_parses_nested_jwt_payload() = runTest {
        server.enqueue(
            mockOk(
                """
                {
                  "data": {
                    "userId": 42,
                    "isNewUser": false,
                    "phoneNumber": "998901234567",
                    "jwt": {
                      "accessToken": "a.b.c",
                      "refreshToken": "r.s.t",
                      "expireAt": 1893456000
                    }
                  },
                  "message": "ok", "status": true, "code": 200, "meta": null
                }
                """.trimIndent()
            )
        )

        val result = service.confirmSMS(
            "{\"code\":\"1234\"}".toRequestBody("application/json".toMediaType())
        )

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/v1/auth/confirm-sms", recorded.path)

        val data = result.body()?.data
        assertNotNull(data)
        assertEquals(42L, data!!.userId)
        assertEquals("a.b.c", data.jwt.accessToken)
        assertEquals(1_893_456_000L, data.jwt.expireAt)
    }

    @Test
    fun refreshToken_uses_patch_and_sends_refresh_token_as_query_param() = runTest {
        server.enqueue(
            mockOk(
                """
                {
                  "data": {
                    "accessToken": "new-access",
                    "refreshToken": "new-refresh",
                    "expireAt": 1893456000
                  },
                  "message": "ok", "status": true, "code": 200, "meta": null
                }
                """.trimIndent()
            )
        )

        val response = service.refreshToken(refreshToken = "r.s.t")

        val recorded = server.takeRequest()
        assertEquals("PATCH", recorded.method)
        assertEquals("/v1/user/refresh-token?refreshToken=r.s.t", recorded.path)

        val tokens = response.body()?.data
        assertNotNull(tokens)
        assertEquals("new-access", tokens!!.accessToken)
        assertEquals("new-refresh", tokens.refreshToken)
    }

    @Test
    fun non_2xx_response_is_surfaced_unparsed() = runTest {
        server.enqueue(mockError(401, """{"message":"unauthorized"}"""))

        val response = service.refreshToken("expired")

        assertEquals(401, response.code())
        assertTrue(!response.isSuccessful)
        val errorJson = response.errorBody()?.string()
        assertNotNull(errorJson)
        assertTrue(errorJson!!.contains("unauthorized"))
    }
}
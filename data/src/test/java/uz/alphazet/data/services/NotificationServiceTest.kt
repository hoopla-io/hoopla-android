package uz.alphazet.data.services

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: NotificationService

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
    fun getNotifications_sends_pagination_and_language_query_params() = runTest {
        server.enqueue(
            mockOk("""{"data":[],"message":"ok","status":true,"code":200,"meta":null}""")
        )

        service.getNotifications(page = 2, itemsPerPage = 10, language = "en")

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/v1/notifications/list?page=2&itemsPerPage=10&language=en", recorded.path)
    }

    @Test
    fun getNotifications_parses_list_with_nested_files() = runTest {
        server.enqueue(
            mockOk(
                """
                {
                  "data": [
                    {
                      "createdAt": "2026-01-01T10:00:00Z",
                      "files": {"imageUrl": "https://img/n1.png"},
                      "isNew": true,
                      "notificationDescription": "desc",
                      "notificationId": 10,
                      "notificationTitle": "title",
                      "url": null
                    }
                  ],
                  "message": "ok", "status": true, "code": 200, "meta": null
                }
                """.trimIndent()
            )
        )

        val response = service.getNotifications(page = 1, itemsPerPage = 20, language = "ru")

        val notifications = response.body()?.data
        assertNotNull(notifications)
        assertEquals(1, notifications!!.size)
        assertEquals(10, notifications[0].notificationId)
        assertTrue(notifications[0].isNew!!)
        assertEquals("https://img/n1.png", notifications[0].files?.imageUrl)
    }

    @Test
    fun getNotificationDetail_sends_notification_id_and_language() = runTest {
        server.enqueue(
            mockOk(
                """
                {
                  "data": {
                    "countReads": 5,
                    "createdAt": "2026-01-01",
                    "files": {"imageUrl": "https://img/d.png"},
                    "isNew": false,
                    "notificationDescription": "full description",
                    "notificationId": 42,
                    "notificationTitle": "Important",
                    "shareUrl": "https://share/42",
                    "url": null
                  },
                  "message": "ok", "status": true, "code": 200, "meta": null
                }
                """.trimIndent()
            )
        )

        val response = service.getNotificationDetail(notificationId = 42, language = "uz")

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/v1/notifications/show?notificationId=42&language=uz", recorded.path)

        val detail = response.body()?.data
        assertNotNull(detail)
        assertEquals(42, detail!!.notificationId)
        assertEquals(5, detail.countReads)
        assertFalse(detail.isNew!!)
        assertEquals("https://share/42", detail.shareUrl)
        assertEquals("https://img/d.png", detail.files?.imageUrl)
    }

    @Test
    fun markRead_uses_post_at_mark_read_endpoint() = runTest {
        server.enqueue(
            mockOk("""{"data":null,"message":"ok","status":true,"code":200,"meta":null}""")
        )

        service.markRead()

        val recorded = server.takeRequest()
        assertEquals("POST", recorded.method)
        assertEquals("/v1/notifications/mark-read", recorded.path)
    }
}
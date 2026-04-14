package uz.alphazet.data.services

import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class CategoryServiceTest {

    private lateinit var server: MockWebServer
    private lateinit var service: CategoryService

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
    fun getCategories_hits_expected_path_with_get() = runTest {
        server.enqueue(
            mockOk("""{"data":[],"message":"ok","status":true,"code":200,"meta":null}""")
        )

        service.getCategories()

        val recorded = server.takeRequest()
        assertEquals("GET", recorded.method)
        assertEquals("/v1/categories/list", recorded.path)
    }

    @Test
    fun getCategories_parses_list_of_categories() = runTest {
        server.enqueue(
            mockOk(
                """
                {
                  "data": [
                    {"id": 1, "name": "Coffee",    "imageUrl": "https://img/coffee.png"},
                    {"id": 2, "name": "Cold Brew", "imageUrl": null}
                  ],
                  "message": "ok", "status": true, "code": 200, "meta": null
                }
                """.trimIndent()
            )
        )

        val response = service.getCategories()

        val categories = response.body()?.data
        assertNotNull(categories)
        assertEquals(2, categories!!.size)

        assertEquals(1, categories[0].id)
        assertEquals("Coffee", categories[0].name)
        assertEquals("https://img/coffee.png", categories[0].imageUrl)

        assertEquals(2, categories[1].id)
        assertNull(categories[1].imageUrl)
    }

    @Test
    fun getCategories_returns_empty_list_on_empty_data() = runTest {
        server.enqueue(
            mockOk("""{"data":[],"message":"no data","status":true,"code":200,"meta":null}""")
        )

        val response = service.getCategories()

        assertEquals(0, response.body()?.data?.size)
    }
}
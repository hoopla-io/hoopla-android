package uz.alphazet.domain.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.alphazet.data.UIResource

class UIResourceTest {

    @Test
    fun success_holds_non_null_data() {
        val resource: UIResource<String> = UIResource.Success("payload")

        assertTrue(resource is UIResource.Success<String>)
        assertEquals("payload", (resource as UIResource.Success<String>).data)
    }

    @Test
    fun success_permits_null_data() {
        val resource: UIResource<String> = UIResource.Success(null)

        assertNull((resource as UIResource.Success<String>).data)
    }

    @Test
    fun error_retains_throwable() {
        val cause = IllegalStateException("boom")

        val resource: UIResource<Any> = UIResource.Error(cause)

        assertSame(cause, (resource as UIResource.Error).throwable)
    }

    @Test
    fun loading_is_singleton() {
        val a: UIResource<Any> = UIResource.Loading
        val b: UIResource<String> = UIResource.Loading

        assertSame(a, b)
    }

    @Test
    fun fold_covers_all_branches() {
        fun <T> describe(r: UIResource<T>): String = when (r) {
            is UIResource.Success -> "success"
            is UIResource.Error -> "error"
            is UIResource.Loading -> "loading"
        }

        assertEquals("success", describe(UIResource.Success("x")))
        assertEquals("error", describe(UIResource.Error(RuntimeException())))
        assertEquals("loading", describe(UIResource.Loading))
    }
}

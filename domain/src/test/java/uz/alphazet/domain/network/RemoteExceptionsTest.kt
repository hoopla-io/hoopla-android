package uz.alphazet.domain.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.alphazet.data.models.order.PaymentRequiredExceptionData

class RemoteExceptionsTest {

    @Test
    fun remote_exception_round_trips_message_and_code() {
        val ex = RemoteException("oops", 418)

        assertEquals("oops", ex.message)
        assertEquals(418, ex.code)
        assertTrue(ex is Exception)
    }

    @Test
    fun unauthorized_is_remote_exception() {
        val ex = UnauthorizedException("no", 401)

        assertTrue(ex is RemoteException)
        assertEquals(401, ex.code)
    }

    @Test
    fun bad_request_is_remote_exception() {
        val ex = BadRequestException("bad", 400)

        assertTrue(ex is RemoteException)
        assertEquals(400, ex.code)
    }

    @Test
    fun validation_is_remote_exception() {
        val ex = ValidationException("invalid", 422)

        assertTrue(ex is RemoteException)
        assertEquals(422, ex.code)
    }

    @Test
    fun forbidden_is_remote_exception() {
        val ex = ForbiddenException("denied", 403)

        assertTrue(ex is RemoteException)
        assertEquals(403, ex.code)
    }

    @Test
    fun not_found_is_remote_exception() {
        val ex = NotFoundException("nope", 404)

        assertTrue(ex is RemoteException)
        assertEquals(404, ex.code)
    }

    @Test
    fun conflict_is_remote_exception() {
        val ex = ConflictException("dup", 409)

        assertTrue(ex is RemoteException)
        assertEquals(409, ex.code)
    }

    @Test
    fun too_many_requests_is_remote_exception() {
        val ex = TooManyRequestException("slow down", 429)

        assertTrue(ex is RemoteException)
        assertEquals(429, ex.code)
    }

    @Test
    fun precondition_required_is_remote_exception() {
        val ex = PreconditionRequiredException("needed", 428)

        assertTrue(ex is RemoteException)
        assertEquals(428, ex.code)
    }

    @Test
    fun server_error_is_remote_exception() {
        val ex = ServerErrorException("boom", 500)

        assertTrue(ex is RemoteException)
        assertEquals(500, ex.code)
    }

    @Test
    fun connection_error_is_remote_exception() {
        val ex = ConnectionErrorException("offline", -1)

        assertTrue(ex is RemoteException)
        assertEquals(-1, ex.code)
    }

    @Test
    fun payment_exception_carries_error_data() {
        val data = PaymentRequiredExceptionData(
            amount = 1000,
            checkoutUrl = "https://pay",
            deeplink = "app://pay",
            expiresAt = "2030-01-01",
            orderId = 42,
            shortLink = "http://s"
        )

        val ex = PaymentException(data, "pay up", 402)

        assertTrue(ex is RemoteException)
        assertEquals(402, ex.code)
        assertSame(data, ex.errorData)
    }

    @Test
    fun payment_exception_allows_null_error_data() {
        val ex = PaymentException(null, "pay up", 402)

        assertNull(ex.errorData)
    }

    @Test
    fun message_may_be_null() {
        val ex = NotFoundException(null, 404)

        assertNull(ex.message)
    }
}

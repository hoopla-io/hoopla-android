package uz.i_tv.domain.network

open class RemoteException(
    override val message: String?,
    open val code: Int
) : Exception(message)

//401
class UnauthorizedException(
    override val message: String?,
    override val code: Int
) : RemoteException(message, code)

//402
class PaymentException(
    override val message: String?,
    override val code: Int
) : RemoteException(message, code)

//422
class ValidationException(
    override val message: String?,
    override val code: Int,
) : RemoteException(message, code)

//400
class BadRequestException(
    override val message: String?,
    override val code: Int,
) : RemoteException(message, code)

//403
class ForbiddenException(
    override val message: String?,
    override val code: Int,
) : RemoteException(message, code)

//409
class ConflictException(
    override val message: String?,
    override val code: Int,
) : RemoteException(message, code)

//404
class NotFoundException(
    override val message: String?,
    override val code: Int,
) : RemoteException(message, code)

//429
class TooManyRequestException(
    override val message: String?,
    override val code: Int,
) : RemoteException(message, code)

//5..
class ServerErrorException(
    override val message: String?,
    override val code: Int,
) : RemoteException(message, code)

//connection error
class ConnectionErrorException(
    override val message: String?,
    override val code: Int,
) : RemoteException(message, code)


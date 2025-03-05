package uz.alphazet.domain.network

interface RemoteErrorListener {
    fun onRemoteException(message: String?, code: Int)
    fun onUnauthorizedException(message: String?, code: Int)
    fun onPaymentException(message: String?, code: Int)
    fun onValidationException(message: String?, code: Int)
    fun onBadRequestException(message: String?, code: Int)
    fun onForbiddenException(message: String?, code: Int)
    fun onConflictException(message: String?, code: Int)
    fun onNotFoundException(message: String?, code: Int)
    fun onTooManyRequestException(message: String?, code: Int)
    fun onServerErrorException(message: String?, code: Int)
    fun onConnectionErrorException(message: String?)
}
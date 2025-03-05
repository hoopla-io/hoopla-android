package uz.alphazet.data.models

data class LoginSessionData(
    val phoneNumber: String?,
    val sessionExpiresAt: Int?,
    val sessionId: String?
)
package uz.i_tv.data.models

data class LoginSessionData(
    val phoneNumber: String?,
    val sessionExpiresAt: Int?,
    val sessionId: String?
)
package uz.i_tv.data.models

data class ConfirmSMSData(
    val userId: Long,
    val isNewUser: Boolean,
    val phoneNumber: String?,
    val jwt: JwtData
) {
    data class JwtData(
        val accessToken: String?,
        val refreshToken: String?,
        val expireAt: Long?
    )
}
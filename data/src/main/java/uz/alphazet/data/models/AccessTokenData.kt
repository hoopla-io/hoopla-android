package uz.alphazet.data.models

data class AccessTokenData(
    val accessToken: String?,
    val refreshToken: String?,
    val expireAt: Long?
)
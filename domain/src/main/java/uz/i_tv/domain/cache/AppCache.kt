package uz.i_tv.domain.cache

interface AppCache {
    var fcmToken: String?
    var accessToken: String?
    var refreshToken: String?
    var tokenExpireAt: Long
    var lang: String
    var isFirstTime: Boolean

    fun saveTokens(
        accessToken: String?,
        refreshToken: String?,
        tokenExpireAt: Long
    )

    fun clearTokens()

    fun clearData()
}
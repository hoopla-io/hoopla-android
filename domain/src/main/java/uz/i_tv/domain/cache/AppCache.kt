package uz.i_tv.domain.cache

interface AppCache {
    var fcmToken: String?
    var lang: String
    var isFirstTime: Boolean

    fun clearData()
}
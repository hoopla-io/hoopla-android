package uz.alphazet.domain.cache

import android.content.SharedPreferences
import com.zeugmasolutions.localehelper.Locales

class AppCacheImpl constructor(
    private val prefs: SharedPreferences
) : AppCache {
    override var fcmToken: String? by prefs.stringNullable()
    override var accessToken: String? by prefs.stringNullable()
    override var refreshToken: String? by prefs.stringNullable()
    override var tokenExpireAt: Long by prefs.long()
    override var lang: String by prefs.string(Locales.Russian.language)
    override var isFirstTime: Boolean by prefs.boolean(true)

    override fun saveTokens(accessToken: String?, refreshToken: String?, tokenExpireAt: Long) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        this.tokenExpireAt = tokenExpireAt
    }

    override fun clearTokens() {
        this.accessToken = null
        this.refreshToken = null
        this.tokenExpireAt = 0L
    }

    override fun clearData() {
        prefs.edit().clear().apply()
    }
}


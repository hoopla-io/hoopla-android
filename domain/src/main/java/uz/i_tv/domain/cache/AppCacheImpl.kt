package uz.i_tv.domain.cache

import android.content.SharedPreferences
import com.zeugmasolutions.localehelper.Locales

class AppCacheImpl constructor(
    private val prefs: SharedPreferences
) : AppCache {
    override var fcmToken: String? by prefs.stringNullable()
    override var lang: String by prefs.string(Locales.Russian.language)
    override var isFirstTime: Boolean by prefs.boolean(true)

    override fun clearData() {
        prefs.edit().clear().apply()
    }
}


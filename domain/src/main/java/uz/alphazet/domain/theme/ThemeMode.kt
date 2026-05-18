package uz.alphazet.domain.theme

import androidx.appcompat.app.AppCompatDelegate
import uz.alphazet.domain.cache.AppCache

object ThemeModes {
    const val SYSTEM = "system"
    const val LIGHT = "light"
    const val DARK = "dark"
}

fun String.toNightMode(): Int = when (this) {
    ThemeModes.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
    ThemeModes.DARK -> AppCompatDelegate.MODE_NIGHT_YES
    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
}

fun AppCache.applyThemeMode() {
    themeMode = ThemeModes.LIGHT
    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
}
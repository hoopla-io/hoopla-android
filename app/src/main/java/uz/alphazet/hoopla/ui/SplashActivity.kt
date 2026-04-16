package uz.alphazet.hoopla.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.fragment.app.commit
import com.zeugmasolutions.localehelper.LocaleHelper
import org.koin.android.ext.android.inject
import uz.alphazet.domain.cache.AppCache
import uz.alphazet.domain.ui.BaseActivity
import uz.alphazet.domain.utils.gone
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.ui.on_boarding.OnBoardingScreen
import java.util.Locale

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {

    private val cache: AppCache by inject()
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.screen_splash)

        val imageView = findViewById<ImageView>(R.id.logo_img)

        handler.postDelayed({
            when {
                cache.isFirstTime -> {
                    cache.isFirstTime = false
                    val lang = resolveDeviceLang()
                    cache.lang = lang
                    // Sync localehelper so its stored preference matches cache.lang.
                    // LocaleHelper.setLocale only writes to SharedPreferences — no
                    // Activity.recreate() is triggered.
                    LocaleHelper.setLocale(this, Locale.forLanguageTag(lang))

                    imageView.gone()
                    val fragment = OnBoardingScreen()
                    supportFragmentManager.commit(allowStateLoss = true) {
                        replace(R.id.intro_container, fragment)
                    }
                }

                else -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }

        }, 1000)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    /**
     * Maps the device's primary locale to one of the three supported language
     * codes: "uz", "en", or "ru" (fallback).
     *
     * Uses [Locale.getDefault] which reflects the language the user has chosen
     * in Android system settings.
     */
    private fun resolveDeviceLang(): String {
        val deviceLang = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            resources.configuration.locales[0].language
        } else {
            @Suppress("DEPRECATION")
            resources.configuration.locale.language
        }
        return when (deviceLang) {
            "uz" -> "uz"
            "en" -> "en"
            else -> "ru"
        }
    }

}
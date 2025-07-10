package uz.alphazet.hoopla.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.fragment.app.commit
import org.koin.android.ext.android.inject
import uz.alphazet.domain.cache.AppCache
import uz.alphazet.domain.ui.BaseActivity
import uz.alphazet.domain.utils.gone
import uz.alphazet.hoopla.R
import uz.alphazet.hoopla.ui.on_boarding.OnBoardingScreen

@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {

    private val cache: AppCache by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.screen_splash)

        val imageView = findViewById<ImageView>(R.id.logo_img)

        Handler(Looper.getMainLooper()).postDelayed({
            when {
                cache.isFirstTime -> {
                    cache.isFirstTime = false

                    imageView.gone()
                    val fragment = OnBoardingScreen()
                    supportFragmentManager.commit { replace(R.id.intro_container, fragment) }
                    supportFragmentManager.executePendingTransactions()
                }

                else -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }

        }, 1000)
    }

}
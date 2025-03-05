package uz.alphazet.hoopla.ui

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import uz.alphazet.domain.ui.BaseFragment
import uz.alphazet.hoopla.R

@SuppressLint("CustomSplashScreen")
class SplashScreen : BaseFragment(R.layout.screen_splash) {

    override fun initialize() {
        Handler(Looper.getMainLooper()).postDelayed({
            when {
                cache.isFirstTime -> {
                    cache.isFirstTime = false
                    replaceScreen(Screens.onBoardingScreen())
                }

                else -> replaceScreen(Screens.bottomNav())
            }

        }, 1000)
    }

}
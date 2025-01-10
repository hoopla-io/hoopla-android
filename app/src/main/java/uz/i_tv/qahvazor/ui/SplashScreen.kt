package uz.i_tv.qahvazor.ui

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import uz.i_tv.domain.ui.BaseFragment
import uz.i_tv.qahvazor.R

@SuppressLint("CustomSplashScreen")
class SplashScreen : BaseFragment(R.layout.screen_splash) {

    override fun initialize() {
        Handler(Looper.getMainLooper()).postDelayed({
//            when {
//                cache.isFirstTime -> {
//                    cache.isFirstTime = false
//                    replaceScreen(Screens.authScreen())
//                }
//
//                else -> replaceScreen(Screens.bottomNav())
//            }

            replaceScreen(Screens.bottomNav())

//            newRootScreen(ClientScreens.bottomNav())
        }, 1000)
    }

}
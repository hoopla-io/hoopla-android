package uz.i_tv.qahvazor.ui

import com.github.terrakok.cicerone.Screen
import uz.i_tv.domain.ui.BaseRootActivity

class RootActivity : BaseRootActivity() {
    override val rootScreen: Screen = Screens.splashScreen()

    override fun logOut() {
        router.navigateTo(Screens.bottomNav())
    }

    override fun navigateToAuthScreen() {
        router.navigateTo(Screens.authScreen())
    }

}
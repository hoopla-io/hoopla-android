package uz.alphazet.hoopla.ui

import com.github.terrakok.cicerone.Screen
import uz.alphazet.domain.ui.BaseRootActivity

class RootActivity : BaseRootActivity() {
    override val rootScreen: Screen = Screens.splashScreen()

    override fun logOut() {
        router.navigateTo(Screens.bottomNav())
    }

    override fun navigateToAuthScreen() {
        router.navigateTo(Screens.authScreen())
    }

}
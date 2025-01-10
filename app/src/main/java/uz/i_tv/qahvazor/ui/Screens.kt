package uz.i_tv.qahvazor.ui

import com.github.terrakok.cicerone.androidx.FragmentScreen
import uz.i_tv.qahvazor.ui.auth.AuthScreen
import uz.i_tv.qahvazor.ui.auth.ConfirmPhoneNumberScreen
import uz.i_tv.qahvazor.ui.home.HomeScreen
import uz.i_tv.qahvazor.ui.profile.ProfileScreen
import uz.i_tv.qahvazor.ui.qr_code.QRCodeScreen

object Screens {

    fun splashScreen() = FragmentScreen { SplashScreen() }

    private var authScreen: AuthScreen? = null
    fun authScreen(isCurrent: Boolean = false) = FragmentScreen {
        if (isCurrent && authScreen != null) authScreen!! else AuthScreen().also { authScreen = it }
    }

    fun confirmationScreen(sessionId: String, phoneNumberFormatted: String) =
        FragmentScreen { ConfirmPhoneNumberScreen.withArgs(sessionId, phoneNumberFormatted) }

    fun bottomNav(initialPageId: Int = TabContainerFragment.TAB_HOME) =
        FragmentScreen { BottomNavScreen.getNewInstance(initialPageId) }

    object BottomNavPages {
        fun home() = FragmentScreen { HomeScreen() }
        fun qrCode() = FragmentScreen { QRCodeScreen() }
        fun profile() = FragmentScreen { ProfileScreen() }
    }

}
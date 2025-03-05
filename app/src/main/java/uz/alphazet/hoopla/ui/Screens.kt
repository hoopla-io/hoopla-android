package uz.alphazet.hoopla.ui

import com.github.terrakok.cicerone.androidx.FragmentScreen
import uz.alphazet.hoopla.ui.auth.AuthScreen
import uz.alphazet.hoopla.ui.auth.ConfirmPhoneNumberScreen
import uz.alphazet.hoopla.ui.home.HomeScreen
import uz.alphazet.hoopla.ui.on_boarding.OnBoardingScreen
import uz.alphazet.hoopla.ui.profile.ProfileScreen
import uz.alphazet.hoopla.ui.profile.payment.PaymentServicesScreen
import uz.alphazet.hoopla.ui.profile.subscriptions.SubscriptionsScreen
import uz.alphazet.hoopla.ui.qr_code.QRCodeScreen

object Screens {

    fun splashScreen() = FragmentScreen { SplashScreen() }
    fun onBoardingScreen() = FragmentScreen { OnBoardingScreen() }

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

    fun subscriptionsScreen() = FragmentScreen { SubscriptionsScreen() }
    fun paymentServicesScreen() = FragmentScreen { PaymentServicesScreen() }

}
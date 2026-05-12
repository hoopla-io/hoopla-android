package uz.alphazet.hoopla.di

import org.koin.dsl.module
import uz.alphazet.hoopla.ui.auth.AuthVM
import uz.alphazet.hoopla.ui.home.HomeVM
import uz.alphazet.hoopla.ui.home.NotificationVM
import uz.alphazet.hoopla.ui.home.StoryViewerVM
import uz.alphazet.hoopla.ui.order.OrderVM
import uz.alphazet.hoopla.ui.profile.ProfileVM
import uz.alphazet.hoopla.ui.profile.payment.PaymentVM
import uz.alphazet.hoopla.ui.profile.subscriptions.SubscriptionVM
import uz.alphazet.hoopla.ui.qr_code.QRCodeVM
import uz.alphazet.hoopla.ui.shop_details.ShopVM

object AppModule {

    val viewModelModule = module {
        factory { AuthVM(get()) }
        factory { HomeVM(get(), get(), get(), get()) }
        factory { ProfileVM(get()) }
        factory { ShopVM(get()) }
        factory { OrderVM(get(), get()) }
        factory { QRCodeVM(get(), get(), get()) }
        factory { SubscriptionVM(get()) }
        factory { PaymentVM(get()) }
        factory { NotificationVM(get(), get(), get()) }
        factory { StoryViewerVM(get()) }
    }

}
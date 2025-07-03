package uz.alphazet.hoopla.di

import org.koin.dsl.module
import uz.alphazet.hoopla.ui.auth.AuthVM
import uz.alphazet.hoopla.ui.home.HomeVM
import uz.alphazet.hoopla.ui.order.OrderVM
import uz.alphazet.hoopla.ui.partner_details.PartnerVM
import uz.alphazet.hoopla.ui.profile.ProfileVM
import uz.alphazet.hoopla.ui.profile.payment.PaymentVM
import uz.alphazet.hoopla.ui.profile.subscriptions.SubscriptionVM
import uz.alphazet.hoopla.ui.qr_code.QRCodeVM
import uz.alphazet.hoopla.ui.shop_details.ShopVM

object AppModule {

    val viewModelModule = module {
        factory { AuthVM(get()) }
        factory { HomeVM(get()) }
        factory { ProfileVM(get()) }
        factory { PartnerVM(get()) }
        factory { ShopVM(get()) }
        factory { OrderVM(get()) }
        factory { QRCodeVM(get(), get()) }
        factory { SubscriptionVM(get()) }
        factory { PaymentVM(get()) }
    }

}
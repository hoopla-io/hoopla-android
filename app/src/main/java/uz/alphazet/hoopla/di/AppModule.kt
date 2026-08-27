package uz.alphazet.hoopla.di

import org.koin.dsl.module
import uz.alphazet.hoopla.ui.auth.AuthVM
import uz.alphazet.hoopla.ui.cart.CartVM
import uz.alphazet.hoopla.ui.home.HomeVM
import uz.alphazet.hoopla.ui.home.NotificationVM
import uz.alphazet.hoopla.ui.home.StoryViewerVM
import uz.alphazet.hoopla.ui.order.OrderVM
import uz.alphazet.hoopla.ui.profile.ProfileVM
import uz.alphazet.hoopla.ui.profile.devices.DevicesVM
import uz.alphazet.hoopla.ui.profile.giftcard.GiftCardVM
import uz.alphazet.hoopla.ui.profile.payment.PaymentVM
import uz.alphazet.hoopla.ui.partner.PartnerVM
import uz.alphazet.hoopla.ui.profile.subscriptions.SubscriptionVM
import uz.alphazet.hoopla.ui.orders.OrdersVM
import uz.alphazet.hoopla.ui.search.SearchVM
import uz.alphazet.hoopla.ui.shop_details.ShopVM

object AppModule {

    val viewModelModule = module {
        factory { AuthVM(get()) }
        factory { HomeVM(get(), get(), get(), get(), get()) }
        factory { ProfileVM(get()) }
        factory { DevicesVM(get(), get()) }
        factory { GiftCardVM(get()) }
        factory { ShopVM(get(), get()) }
        factory { OrderVM(get(), get()) }
        factory { OrdersVM(get(), get(), get()) }
        factory { SubscriptionVM(get()) }
        factory { PaymentVM(get()) }
        factory { NotificationVM(get(), get(), get()) }
        factory { StoryViewerVM(get()) }
        factory { SearchVM(get()) }
        factory { PartnerVM(get()) }
        factory { CartVM(get(), get(), get(), get()) }
    }

}
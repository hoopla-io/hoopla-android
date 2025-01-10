package uz.i_tv.qahvazor.di

import org.koin.dsl.module
import uz.i_tv.qahvazor.ui.auth.AuthVM
import uz.i_tv.qahvazor.ui.home.HomeVM
import uz.i_tv.qahvazor.ui.partner_details.PartnerVM
import uz.i_tv.qahvazor.ui.profile.ProfileVM
import uz.i_tv.qahvazor.ui.qr_code.QRCodeVM
import uz.i_tv.qahvazor.ui.shop_details.ShopVM

object AppModule {

    val viewModelModule = module {
        factory { AuthVM(get()) }
        factory { HomeVM(get()) }
        factory { ProfileVM(get()) }
        factory { PartnerVM(get()) }
        factory { ShopVM(get()) }
        factory { QRCodeVM(get()) }
    }

}
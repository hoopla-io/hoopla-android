package uz.i_tv.qahvazor.di

import org.koin.dsl.module
import uz.i_tv.qahvazor.ui.auth.AuthVM
import uz.i_tv.qahvazor.ui.home.HomeVM

object AppModule {

    val viewModelModule = module {
        factory { AuthVM() }
        factory { HomeVM(get()) }
    }

}
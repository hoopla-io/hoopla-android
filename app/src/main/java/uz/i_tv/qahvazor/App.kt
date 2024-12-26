package uz.i_tv.qahvazor

import com.zeugmasolutions.localehelper.LocaleAwareApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import uz.i_tv.domain.di.Modules
import uz.i_tv.qahvazor.di.AppModule

class App : LocaleAwareApplication() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@App)
            modules(
                Modules.utilsModule,
                Modules.apiModule,
                Modules.repositoryModule,
                AppModule.viewModelModule
            )
        }

    }

}
package uz.alphazet.hoopla

import com.zeugmasolutions.localehelper.LocaleAwareApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import uz.alphazet.domain.di.Modules
import uz.alphazet.hoopla.di.AppModule

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
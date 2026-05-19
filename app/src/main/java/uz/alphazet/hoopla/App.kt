package uz.alphazet.hoopla

import com.zeugmasolutions.localehelper.LocaleAwareApplication
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import uz.alphazet.domain.cache.AppCache
import uz.alphazet.domain.di.Modules
import uz.alphazet.domain.theme.applyThemeMode
import uz.alphazet.hoopla.di.AppModule

class App : LocaleAwareApplication() {

    private val appCache: AppCache by inject()

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

        appCache.applyThemeMode()

    }

}
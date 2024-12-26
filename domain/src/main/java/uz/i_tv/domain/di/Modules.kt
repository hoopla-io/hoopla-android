package uz.i_tv.domain.di

import android.content.Context
import android.content.SharedPreferences
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import uz.i_tv.data.services.AuthService
import uz.i_tv.data.services.HomeService
import uz.i_tv.domain.BuildConfig
import uz.i_tv.domain.cache.AppCache
import uz.i_tv.domain.cache.AppCacheImpl
import uz.i_tv.domain.network.interceptor.NetworkConnectionInterceptor
import uz.i_tv.domain.repositories.AuthRepo
import uz.i_tv.domain.repositories.HomeRepo
import uz.i_tv.domain.utils.Constants.APP_CACHE
import java.util.concurrent.TimeUnit

object Modules {

    val utilsModule = module {
        single<AppCache> { AppCacheImpl(get()) }
    }

    val apiModule = module {
        single { provideAppCacheSharedPreferences(androidContext()) }
//        single { AppInterceptor() }
        single { provideRetrofit(get()) }
        single { provideOkHttpClient(androidContext()) }
        single { provideAuthService(get()) }
        single { provideHomeService(get()) }
    }

    val repositoryModule = module {
        factory { AuthRepo(get()) }
        factory { HomeRepo(get()) }
    }

    private fun provideAuthService(retrofit: Retrofit) = retrofit.create(AuthService::class.java)
    private fun provideHomeService(retrofit: Retrofit) = retrofit.create(HomeService::class.java)

    private fun provideAppCacheSharedPreferences(
        context: Context
    ): SharedPreferences = context.getSharedPreferences(APP_CACHE, Context.MODE_PRIVATE)

    private fun provideRetrofit(client: OkHttpClient) =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

//    private fun provideOkHttpClient(
//        interceptor: AppInterceptor,
//        context: Context
//    ): OkHttpClient {
//        return OkHttpClient().newBuilder().apply {
//            if (BuildConfig.DEBUG)
//                addInterceptor(ChuckerInterceptor.Builder(context).build())
//        }
//            .addInterceptor(HttpLoggingInterceptor { message -> message.log("HTTP_LOGGING_INTERCEPTOR") }.apply {
//                this.level = HttpLoggingInterceptor.Level.BODY
//            })
//            .addInterceptor(interceptor)
//            .build()
//    }


    private fun provideOkHttpClient(context: Context): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        return OkHttpClient.Builder()
            .addInterceptor(NetworkConnectionInterceptor(context))
            .addInterceptor(loggingInterceptor)
            .writeTimeout(60, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS).build()
    }

    class AppInterceptor() : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {

            val req = chain.request()
            val builder = req.newBuilder()

            return chain.proceed(builder.build())
        }
    }


}
package uz.alphazet.domain.di

import android.content.Context
import android.content.SharedPreferences
import androidx.collection.ArrayMap
import com.chuckerteam.chucker.api.ChuckerInterceptor
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import uz.alphazet.data.BaseResponse
import uz.alphazet.data.models.AccessTokenData
import uz.alphazet.data.services.AuthService
import uz.alphazet.data.services.HomeService
import uz.alphazet.data.services.OrderService
import uz.alphazet.data.services.PaymentService
import uz.alphazet.data.services.ProfileService
import uz.alphazet.data.services.QrCodeService
import uz.alphazet.data.services.ShopService
import uz.alphazet.data.services.SubscriptionService
import uz.alphazet.domain.BuildConfig
import uz.alphazet.domain.cache.AppCache
import uz.alphazet.domain.cache.AppCacheImpl
import uz.alphazet.domain.network.interceptor.NetworkConnectionInterceptor
import uz.alphazet.domain.permission.PermissionManager
import uz.alphazet.domain.permission.PermissionManagerImpl
import uz.alphazet.domain.repositories.AuthRepo
import uz.alphazet.domain.repositories.HomeRepo
import uz.alphazet.domain.repositories.OrderRepo
import uz.alphazet.domain.repositories.PaymentServiceRepo
import uz.alphazet.domain.repositories.ProfileRepo
import uz.alphazet.domain.repositories.QRCodeRepo
import uz.alphazet.domain.repositories.ShopRepo
import uz.alphazet.domain.repositories.SubscriptionRepo
import uz.alphazet.domain.utils.Constants.APP_CACHE
import uz.alphazet.domain.utils.log
import java.util.concurrent.TimeUnit

object Modules {

    val utilsModule = module {
        single<AppCache> { AppCacheImpl(get()) }
        single<PermissionManager> { PermissionManagerImpl(androidContext()) }
    }

    val apiModule = module {
        single { provideAppCacheSharedPreferences(androidContext()) }
//        single { AppInterceptor(get()) }
        single { provideRetrofit(get()) }
        single { provideOkHttpClient(androidContext(), get()) }
        single { provideAuthService(get()) }
        single { provideHomeService(get()) }
        single { provideProfileService(get()) }
        single { provideShopService(get()) }
        single { provideOrderService(get()) }
        single { provideQrCodeService(get()) }
        single { provideSubscriptionService(get()) }
        single { providePaymentService(get()) }
    }

    val repositoryModule = module {
        factory { AuthRepo(get()) }
        factory { HomeRepo(get()) }
        factory { ProfileRepo(get()) }
        factory { ShopRepo(get()) }
        factory { OrderRepo(get()) }
        factory { QRCodeRepo(get()) }
        factory { SubscriptionRepo(get()) }
        factory { PaymentServiceRepo(get()) }
    }

    private fun provideAuthService(retrofit: Retrofit) = retrofit.create(AuthService::class.java)
    private fun provideHomeService(retrofit: Retrofit) = retrofit.create(HomeService::class.java)
    private fun provideProfileService(retrofit: Retrofit) =
        retrofit.create(ProfileService::class.java)

    private fun provideShopService(retrofit: Retrofit) = retrofit.create(ShopService::class.java)
    private fun provideOrderService(retrofit: Retrofit) = retrofit.create(OrderService::class.java)
    private fun provideQrCodeService(retrofit: Retrofit) =
        retrofit.create(QrCodeService::class.java)

    private fun provideSubscriptionService(retrofit: Retrofit) =
        retrofit.create(SubscriptionService::class.java)

    private fun providePaymentService(retrofit: Retrofit) =
        retrofit.create(PaymentService::class.java)

    private fun provideAppCacheSharedPreferences(
        context: Context
    ): SharedPreferences = context.getSharedPreferences(APP_CACHE, Context.MODE_PRIVATE)

    private fun provideRetrofit(client: OkHttpClient) =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()


    private fun provideOkHttpClient(context: Context, appCache: AppCache): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            message.log("HTTP_LOGGING_INTERCEPTOR")
        }
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        return OkHttpClient.Builder().apply {
            if (BuildConfig.DEBUG)
                addInterceptor(ChuckerInterceptor.Builder(context).build())
        }
//            .addInterceptor(HeaderInterceptor())
            .addInterceptor(NetworkConnectionInterceptor(context, appCache))
            .addNetworkInterceptor(NetworkInterceptor())
            .authenticator(TokenAuthenticator(appCache))
            .addInterceptor(loggingInterceptor)
            .writeTimeout(60, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS).build()
    }

    class NetworkInterceptor() : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val response = chain.proceed(chain.request())
            if (response.code == 412)
                return response.newBuilder().code(401).build()
            else
                return response
        }
    }

    class TokenAuthenticator(private val appCache: AppCache) : Authenticator {
        override fun authenticate(route: Route?, response: Response): Request? {
            "authenticate".log("TokenAuthenticator")
            return synchronized(this) {

                val refreshToken = runBlocking { appCache.refreshToken }

                if (!refreshToken.isNullOrEmpty()) {
                    val newTokenResponse = runBlocking {
                        getNewToken(refreshToken)
                    }

                    // Проверяем успешность ответа
                    when (newTokenResponse.code()) {
                        200 -> {
                            newTokenResponse.body()?.let {
                                appCache.saveTokens(
                                    it.data?.accessToken,
                                    it.data?.refreshToken,
                                    it.data?.expireAt ?: 0L
                                )
                            }
                            "saveTokens 200 TokenAuthenticator".log("CustomInterceptor")
                        }

                        401 -> {
                            appCache.clearTokens()
                            "clearTokens ${newTokenResponse.code()} TokenAuthenticator".log("CustomInterceptor")
                        }

                        else -> {
                            "Error refreshing token: ${newTokenResponse.message()} ${newTokenResponse.code()} TokenAuthenticator".log(
                                "CustomInterceptor"
                            )
                        }
                    }

                    response.request.log("TokenAuthenticator")
                    val req = response.request
                    val builder = req.newBuilder()

                    if (appCache.accessToken != null) builder.header(
                        "Authorization",
                        "Bearer ${appCache.accessToken}"
                    )

                    builder.build()
                } else
                    null
            }

        }

        private suspend fun getNewToken(
            refreshToken: String
        ): BaseResponse<AccessTokenData> {

            val okHttpClient = OkHttpClient.Builder()
                .addInterceptor(HttpLoggingInterceptor { message -> message.log("LOGGING_INTERCEPTOR") }.apply {
                    this.level = HttpLoggingInterceptor.Level.BODY
                })
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://api.hoopla.uz/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build()
            val service = retrofit.create(AuthService::class.java)

            val jsonParams: MutableMap<String?, Any?> = ArrayMap()

            jsonParams["refreshToken"] = refreshToken

            val body: RequestBody = RequestBody.create(
                "application/json".toMediaTypeOrNull(),
                (JSONObject(jsonParams)).toString()
            )

            return service.refreshToken(refreshToken)
        }
    }

}
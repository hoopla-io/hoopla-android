package uz.i_tv.domain.network.interceptor

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.collection.ArrayMap
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import okio.IOException
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import uz.i_tv.data.BaseResponse
import uz.i_tv.data.models.AccessTokenData
import uz.i_tv.data.services.AuthService
import uz.i_tv.domain.R
import uz.i_tv.domain.cache.AppCache
import uz.i_tv.domain.network.NetworkStatus
import uz.i_tv.domain.utils.log

class NetworkConnectionInterceptor(
    private val context: Context,
    private val appCache: AppCache
) : Interceptor {

    private val connectivityManager: ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override fun intercept(chain: Interceptor.Chain): Response {
        when (getCurrentNetworkStatus()) {
            NetworkStatus.Disconnected -> {
                Log.d("NETWORK", "Network status: Disconnected")
                throw NoConnectivityException(context)
            }

            else -> {
                Log.d("NETWORK", "Network status: Connected")

                return synchronized(this) {

                    //refresh token zaprosi profile select qilishda ishlatiladi, bu expireAtga togri kepqomasligi kk
                    val contains = chain.request().url.toString().contains("refresh-token")

                    val currentTimeMillis = System.currentTimeMillis()
                    val expireAt = runBlocking { appCache.tokenExpireAt }
                    "currentTimeMillis--${currentTimeMillis / 1000L}".log("NetworkConnectionInterceptor")
                    "expireAt--$expireAt".log("NetworkConnectionInterceptor")
                    if (expireAt != null && expireAt != 0L && !contains) {
                        if ((expireAt - 30L) < currentTimeMillis / 1000L) {

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
                                        "saveTokens 200".log("CustomInterceptor")
                                    }

                                    401 -> {
                                        appCache.clearTokens()
                                        "clearTokens ${newTokenResponse.code()}".log("CustomInterceptor")
                                    }

                                    else -> {
                                        "Error refreshing token: ${newTokenResponse.message()} ${newTokenResponse.code()}".log(
                                            "CustomInterceptor"
                                        )
                                    }
                                }

                            }
                        }
                    }

                    val builder = chain.request().newBuilder()

                    if (appCache.accessToken != null) builder.addHeader(
                        "Authorization",
                        "Bearer ${appCache.accessToken}"
                    )

                    chain.proceed(builder.build())
                }
            }
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
            .baseUrl("http://api.qahvazor.uz/api/")
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

    private fun getCurrentNetworkStatus(): NetworkStatus {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                    ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).let { connected ->
                        when (connected) {
                            true -> NetworkStatus.Connected
                            else -> NetworkStatus.Disconnected
                        }
                    }
            } else {
                NetworkStatus.Connected
            }
        } catch (e: Exception) {
            Log.d("NETWORK", "exception thrown: ${e.localizedMessage}")
            return NetworkStatus.Disconnected
        }
    }
}

class NoConnectivityException(private val context: Context) : IOException() {
    override val message: String
        get() = context.getString(R.string.no_internet_connection)

    override fun getLocalizedMessage(): String {
        return context.getString(R.string.no_internet_connection)
    }
}

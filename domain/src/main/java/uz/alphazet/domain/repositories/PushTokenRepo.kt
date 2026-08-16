package uz.alphazet.domain.repositories

import androidx.collection.ArrayMap
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import uz.alphazet.data.services.PushTokenService
import uz.alphazet.domain.network.BaseRepo
import uz.alphazet.domain.network.DeviceInfoProvider

class PushTokenRepo(
    private val api: PushTokenService,
    private val deviceInfo: DeviceInfoProvider
) : BaseRepo() {

    suspend fun registerPushToken(token: String) = handle {

        val jsonParams: MutableMap<String?, Any?> = ArrayMap()

        jsonParams["token"] = token
        jsonParams["platform"] = deviceInfo.platform
        jsonParams["deviceId"] = deviceInfo.deviceId

        val body: RequestBody = (JSONObject(jsonParams)).toString()
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        api.registerPushToken(body)
    }

    suspend fun unregisterPushToken(token: String) = handle {

        val jsonParams: MutableMap<String?, Any?> = ArrayMap()

        jsonParams["token"] = token

        val body: RequestBody = (JSONObject(jsonParams)).toString()
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        api.unregisterPushToken(body)
    }

}

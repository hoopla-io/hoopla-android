package uz.alphazet.domain.repositories

import androidx.collection.ArrayMap
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import org.json.JSONObject
import uz.alphazet.data.services.AuthService
import uz.alphazet.domain.network.BaseRepo

class AuthRepo(private val authService: AuthService) : BaseRepo() {

    suspend fun sendSMS(phoneNumber: String) = handle {

        val jsonParams: MutableMap<String?, Any?> = ArrayMap()

        jsonParams["phoneNumber"] = phoneNumber

        val body: RequestBody = RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            (JSONObject(jsonParams)).toString()
        )

        authService.sendSMS(body)
    }

    suspend fun resendSMS(sessionId: String) = handle {

        val jsonParams: MutableMap<String?, Any?> = ArrayMap()

        jsonParams["sessionId"] = sessionId

        val body: RequestBody = RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            (JSONObject(jsonParams)).toString()
        )

        authService.resendSMS(body)
    }

    suspend fun confirmSMS(code: Int, sessionId: String) = handle {

        val jsonParams: MutableMap<String?, Any?> = ArrayMap()

        jsonParams["code"] = code
        jsonParams["sessionId"] = sessionId

        val body: RequestBody = RequestBody.create(
            "application/json".toMediaTypeOrNull(),
            (JSONObject(jsonParams)).toString()
        )

        authService.confirmSMS(body)
    }

}
package uz.alphazet.domain.repositories

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import uz.alphazet.data.services.GiftCardService
import uz.alphazet.domain.network.BaseRepo

class GiftCardRepo(private val service: GiftCardService) : BaseRepo() {

    suspend fun redeem(code: String) = handleFlow {
        val requestBody = JSONObject()
            .put("code", code.trim())
            .toString()
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        service.redeem(requestBody)
    }

}
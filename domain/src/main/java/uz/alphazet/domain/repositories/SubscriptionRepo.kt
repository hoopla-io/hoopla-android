package uz.alphazet.domain.repositories

import androidx.collection.ArrayMap
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import org.json.JSONObject
import uz.alphazet.data.services.SubscriptionService
import uz.alphazet.domain.network.BaseRepo

class SubscriptionRepo(private val api: SubscriptionService) : BaseRepo() {

    suspend fun getSubscriptions() = handleFlow {
        api.getSubscriptions()
    }

    suspend fun buySubscription(subscriptionId: Int) = handleFlow {
        val jsonParams: MutableMap<String?, Any?> = ArrayMap()

        jsonParams["subscriptionId"] = subscriptionId

        val body: RequestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            (JSONObject(jsonParams)).toString()
        )
        api.buySubscription(body)
    }

}
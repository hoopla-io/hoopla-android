package uz.alphazet.domain.repositories

import androidx.collection.ArrayMap
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import org.json.JSONObject
import uz.alphazet.data.services.OrderService
import uz.alphazet.domain.network.BaseRepo

class OrderRepo(private val service: OrderService) : BaseRepo() {

    suspend fun validateOrder(shopId: Int, drinkId: Int) = handleFlow {
        val jsonParams: MutableMap<String?, Any?> = ArrayMap()

        jsonParams["shopId"] = shopId
        jsonParams["drinkId"] = drinkId

        val body: RequestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            (JSONObject(jsonParams)).toString()
        )
        service.validateOrder(body)
    }

    suspend fun createOrder(shopId: Int, drinkId: Int, addOnId: String?) = handleFlow {
        val jsonParams: MutableMap<String?, Any?> = ArrayMap()

        jsonParams["shopId"] = shopId
        jsonParams["drinkId"] = drinkId
        if (!addOnId.isNullOrEmpty())
            jsonParams["addOnId"] = addOnId

        val body: RequestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            (JSONObject(jsonParams)).toString()
        )
        service.createOrder(body)
    }

}
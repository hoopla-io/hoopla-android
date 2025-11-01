package uz.alphazet.domain.repositories

import androidx.collection.ArrayMap
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import uz.alphazet.data.models.order.ModifierItemData
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

    suspend fun createOrder(shopId: Int, drinkId: Int, modifiers: ArrayList<ModifierItemData>) =
        handleFlow {
            val json = JSONObject().apply {
                put("shopId", shopId)
                put("drinkId", drinkId)
                put("modifiers", JSONArray().apply {
                    modifiers.forEach {
                        put(JSONObject().apply {
                            put("modifierId", it.modifierId)
                            if (!it.modifierGroupId.isNullOrEmpty())
                                put("modifierGroupId", it.modifierGroupId)
                            put("modifierKey", it.modifierKey)
                            put("modifierPrice", it.modifierPrice)
                        })
                    }
                })
            }

            val requestBody = json.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

            service.createOrder(requestBody)
        }

}
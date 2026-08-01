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

    suspend fun checkPromocode(
        code: String,
        shopId: Int,
        drinkId: Int,
        modifiers: ArrayList<ModifierItemData>
    ) = handleFlow {
        val json = JSONObject().apply {
            put("code", code.trim())
            put("shopId", shopId)
            put("drinkId", drinkId)
            put("modifiers", JSONArray().apply {
                modifiers.forEach {
                    put(JSONObject().apply {
                        put("modifierId", it.modifierId)
                        put("modifierGroupId", it.modifierGroupId)
                        put("modifierKey", it.modifierKey)
                        put("modifierPrice", it.modifierPrice)
                    })
                }
            })
        }

        val requestBody = json.toString()
            .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        service.checkPromocode(requestBody)
    }

    /**
     * [pickupAt] is an RFC3339 instant with an explicit offset (see
     * `uz.alphazet.domain.utils.PickupTime.format`). Omitting it keeps the order ASAP, which
     * is the behaviour every order had before scheduled pickup existed.
     */
    suspend fun createOrder(
        shopId: Int,
        drinkId: Int,
        modifiers: ArrayList<ModifierItemData>,
        comment: String? = null,
        promoCode: String? = null,
        pickupAt: String? = null
    ) =
        handleFlow {
            val json = JSONObject().apply {
                put("shopId", shopId)
                put("drinkId", drinkId)
                if (!comment.isNullOrBlank()) put("comment", comment.trim())
                if (!promoCode.isNullOrBlank()) put("promo_code", promoCode.trim())
                if (!pickupAt.isNullOrBlank()) put("pickup_at", pickupAt.trim())
                put("modifiers", JSONArray().apply {
                    modifiers.forEach {
                        put(JSONObject().apply {
                            put("modifierId", it.modifierId)
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

    /** See [createOrder] for the shape of [pickupAt]; omitting it keeps the order ASAP. */
    suspend fun createOrderRahmat(
        shopId: Int,
        drinkId: Int,
        modifiers: ArrayList<ModifierItemData>,
        useCashback: Boolean,
        cashbackAmount: Double,
        comment: String? = null,
        promoCode: String? = null,
        pickupAt: String? = null
    ) =
        handleFlow {
            val json = JSONObject().apply {
                put("shopId", shopId)
                put("drinkId", drinkId)
                put("use_cashback", useCashback)
                put("cashback_amount", cashbackAmount)
                if (!comment.isNullOrBlank()) put("comment", comment.trim())
                if (!promoCode.isNullOrBlank()) put("promo_code", promoCode.trim())
                if (!pickupAt.isNullOrBlank()) put("pickup_at", pickupAt.trim())
                put("modifiers", JSONArray().apply {
                    modifiers.forEach {
                        put(JSONObject().apply {
                            put("modifierId", it.modifierId)
                            put("modifierGroupId", it.modifierGroupId)
                            put("modifierKey", it.modifierKey)
                            put("modifierPrice", it.modifierPrice)
                        })
                    }
                })
            }

            val requestBody = json.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

            service.createOrderRahmat(requestBody)
        }

}
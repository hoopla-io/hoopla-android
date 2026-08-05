package uz.alphazet.domain.repositories

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import uz.alphazet.data.models.order.ModifierItemData
import uz.alphazet.data.services.CartService
import uz.alphazet.domain.network.BaseRepo

class CartRepo(private val service: CartService) : BaseRepo() {

    /**
     * Returns the resource directly rather than a Flow: the cart screen holds it in a
     * StateFlow and refills it through `load {}`, the same way DevicesVM holds its list.
     */
    suspend fun getCart() = handle { service.getCart() }

    suspend fun getCartCount() = handleFlow { service.getCartCount() }

    /**
     * [modifiers] carries the same selection the single-item order flow builds, so the modifier
     * screens feed both flows unchanged. A 409 here means the cart belongs to another shop.
     */
    suspend fun addItem(
        shopId: Int,
        drinkId: Int,
        quantity: Int,
        modifiers: ArrayList<ModifierItemData>
    ) = handleFlow {
        val json = JSONObject().apply {
            put("shopId", shopId)
            put("drinkId", drinkId)
            put("quantity", quantity)
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

        service.addItem(json.toString().toRequestBody(JSON_MEDIA_TYPE))
    }

    /** A [quantity] of 0 removes the line outright. */
    suspend fun updateItemQuantity(itemId: Int, quantity: Int) = handleFlow {
        val json = JSONObject().apply { put("quantity", quantity) }
        service.updateItemQuantity(itemId, json.toString().toRequestBody(JSON_MEDIA_TYPE))
    }

    suspend fun removeItem(itemId: Int) = handleFlow { service.removeItem(itemId) }

    suspend fun clearCart() = handleFlow { service.clearCart() }

    suspend fun applyPromo(code: String) = handleFlow {
        val json = JSONObject().apply { put("code", code.trim()) }
        service.applyPromo(json.toString().toRequestBody(JSON_MEDIA_TYPE))
    }

    suspend fun removePromo() = handleFlow { service.removePromo() }

    /**
     * A blank [comment] clears the note. `JSONObject.put(String, Any?)` drops the key when the
     * value is null, so an explicit JSON null is written instead — otherwise clearing a comment
     * would silently send `{}` and leave the old note in place.
     */
    suspend fun setComment(comment: String?) = handleFlow {
        val trimmed = comment?.trim()
        val json = JSONObject().apply {
            if (trimmed.isNullOrEmpty()) put("comment", JSONObject.NULL)
            else put("comment", trimmed)
        }
        service.setComment(json.toString().toRequestBody(JSON_MEDIA_TYPE))
    }

    suspend fun checkout(useCashback: Boolean, cashbackAmount: Double) = handleFlow {
        val json = JSONObject().apply {
            put("use_cashback", useCashback)
            put("cashback_amount", cashbackAmount)
        }
        service.checkout(json.toString().toRequestBody(JSON_MEDIA_TYPE))
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaTypeOrNull()
    }

}

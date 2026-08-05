package uz.alphazet.data.services

import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import uz.alphazet.data.BaseResponse
import uz.alphazet.data.models.cart.CartCountData
import uz.alphazet.data.models.cart.CartData
import uz.alphazet.data.models.order.CheckOutInfo

interface CartService {

    @GET("v1/user/cart")
    suspend fun getCart(): BaseResponse<CartData>

    @GET("v1/user/cart/count")
    suspend fun getCartCount(): BaseResponse<CartCountData>

    /**
     * Answers 409 when the cart already holds items from a different shop — that conflict is
     * the only signal there is, so callers react to it rather than pre-checking the cart.
     */
    @POST("v1/user/cart/items")
    suspend fun addItem(@Body body: RequestBody): BaseResponse<CartData>

    /** A quantity of 0 removes the line. */
    @PATCH("v1/user/cart/items/{itemId}")
    suspend fun updateItemQuantity(
        @Path("itemId") itemId: Int,
        @Body body: RequestBody
    ): BaseResponse<CartData>

    @DELETE("v1/user/cart/items/{itemId}")
    suspend fun removeItem(@Path("itemId") itemId: Int): BaseResponse<CartData>

    @DELETE("v1/user/cart")
    suspend fun clearCart(): BaseResponse<Any>

    @POST("v1/user/cart/promo")
    suspend fun applyPromo(@Body body: RequestBody): BaseResponse<CartData>

    @DELETE("v1/user/cart/promo")
    suspend fun removePromo(): BaseResponse<CartData>

    @POST("v1/user/cart/comment")
    suspend fun setComment(@Body body: RequestBody): BaseResponse<CartData>

    /**
     * Same response shape as the single-item checkout: a 402 carries the payment hand-off,
     * a 200 means the order is already paid for (cashback covered it).
     */
    @POST("v1/user/orders/cart-checkout")
    suspend fun checkout(@Body body: RequestBody): BaseResponse<CheckOutInfo>

}

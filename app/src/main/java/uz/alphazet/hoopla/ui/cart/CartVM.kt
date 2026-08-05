package uz.alphazet.hoopla.ui.cart

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.shareIn
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.UserData
import uz.alphazet.data.models.cart.CartCountData
import uz.alphazet.data.models.cart.CartData
import uz.alphazet.data.models.order.CheckOutInfo
import uz.alphazet.data.models.order.ModifierItemData
import uz.alphazet.domain.repositories.CartRepo
import uz.alphazet.domain.repositories.ProfileRepo
import uz.alphazet.domain.repositories.ShopRepo
import uz.alphazet.domain.ui.BaseVM
import uz.alphazet.domain.ui.load

class CartVM(
    private val repo: CartRepo,
    private val profileRepo: ProfileRepo,
    private val shopRepo: ShopRepo
) : BaseVM() {

    private val cartEmitter: MutableStateFlow<UIResource<CartData>> =
        MutableStateFlow(UIResource.Loading)
    val cartFlow: StateFlow<UIResource<CartData>> get() = cartEmitter

    /**
     * drinkId -> picture, built from the shop's menu. The cart response carries no images, so
     * they are resolved from the same menu the customer ordered from. Failure is silent: a cart
     * without pictures is still a usable cart.
     */
    private val drinkImagesEmitter: MutableStateFlow<Map<Int, String>> = MutableStateFlow(emptyMap())
    val drinkImagesFlow: StateFlow<Map<Int, String>> get() = drinkImagesEmitter

    fun loadDrinkImages(shopId: Int) {
        if (shopId <= 0 || drinkImagesEmitter.value.isNotEmpty()) return
        launch {
            val resource = shopRepo.getShopDrinks(shopId).firstOrNull()
            val drinks = (resource as? UIResource.Success)?.data?.categories
                ?.flatMap { it?.drinks.orEmpty() }
                .orEmpty()
            drinkImagesEmitter.value = drinks.mapNotNull { drink ->
                val id = drink.id ?: return@mapNotNull null
                val url = drink.pictureUrl?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                id to url
            }.toMap()
        }
    }

    private val userDataEmitter: MutableStateFlow<UIResource<UserData>> =
        MutableStateFlow(UIResource.Loading)
    val userDataFlow: StateFlow<UIResource<UserData>> get() = userDataEmitter

    /**
     * Re-reads the whole cart. Every mutation ends in a call to this rather than patching a
     * local copy — the server recomputes totals, and it is the only thing that knows them.
     */
    fun getCart() {
        launch { cartEmitter.load { repo.getCart() } }
    }

    fun getUser() {
        launch { userDataEmitter.load { profileRepo.getMe() } }
    }

    suspend fun getCartCount(): SharedFlow<UIResource<CartCountData>> =
        repo.getCartCount().shareIn(viewModelScope, SharingStarted.Lazily, 0)

    suspend fun addItem(
        shopId: Int,
        drinkId: Int,
        quantity: Int,
        modifiers: ArrayList<ModifierItemData>
    ): SharedFlow<UIResource<CartData>> =
        repo.addItem(shopId, drinkId, quantity, modifiers)
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)

    /** A [quantity] of 0 removes the line. */
    suspend fun updateItemQuantity(itemId: Int, quantity: Int): SharedFlow<UIResource<CartData>> =
        repo.updateItemQuantity(itemId, quantity)
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)

    suspend fun removeItem(itemId: Int): SharedFlow<UIResource<CartData>> =
        repo.removeItem(itemId).shareIn(viewModelScope, SharingStarted.Lazily, 0)

    suspend fun clearCart(): SharedFlow<UIResource<Any>> =
        repo.clearCart().shareIn(viewModelScope, SharingStarted.Lazily, 0)

    suspend fun applyPromo(code: String): SharedFlow<UIResource<CartData>> =
        repo.applyPromo(code).shareIn(viewModelScope, SharingStarted.Lazily, 0)

    suspend fun removePromo(): SharedFlow<UIResource<CartData>> =
        repo.removePromo().shareIn(viewModelScope, SharingStarted.Lazily, 0)

    suspend fun setComment(comment: String?): SharedFlow<UIResource<CartData>> =
        repo.setComment(comment).shareIn(viewModelScope, SharingStarted.Lazily, 0)

    /**
     * Saves the note without a result, for the screen going away — a collector tied to the
     * Activity would be torn down before the response landed. Nothing on screen depends on the
     * outcome; the cart is re-read on the next visit either way.
     */
    fun setCommentInBackground(comment: String?) {
        launch { repo.setComment(comment).collect { } }
    }

    /**
     * Commits an edit the customer already sees applied, for the screen going away before the
     * debounce or the undo window ran out. Same reasoning as [setCommentInBackground]: nothing
     * is left to observe the result, and the cart is re-read on the next visit.
     */
    fun updateItemQuantityInBackground(itemId: Int, quantity: Int) {
        launch { repo.updateItemQuantity(itemId, quantity).collect { } }
    }

    fun removeItemInBackground(itemId: Int) {
        launch { repo.removeItem(itemId).collect { } }
    }

    suspend fun checkout(
        useCashback: Boolean,
        cashbackAmount: Double
    ): SharedFlow<UIResource<CheckOutInfo>> =
        repo.checkout(useCashback, cashbackAmount)
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)

}

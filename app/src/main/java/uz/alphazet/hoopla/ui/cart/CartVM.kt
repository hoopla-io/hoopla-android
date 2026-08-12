package uz.alphazet.hoopla.ui.cart

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.UserData
import uz.alphazet.data.models.cart.CartCountData
import uz.alphazet.data.models.cart.CartData
import uz.alphazet.data.models.order.CheckOutInfo
import uz.alphazet.data.models.order.ModifierItemData
import uz.alphazet.domain.cart.CartStore
import uz.alphazet.domain.repositories.CartRepo
import uz.alphazet.domain.repositories.ProfileRepo
import uz.alphazet.domain.repositories.ShopRepo
import uz.alphazet.domain.ui.BaseVM
import uz.alphazet.domain.ui.load

class CartVM(
    private val repo: CartRepo,
    private val profileRepo: ProfileRepo,
    private val shopRepo: ShopRepo,
    private val store: CartStore
) : BaseVM() {

    /**
     * Every cart the server hands back goes into the shared store on its way to the caller, so a
     * screen watching from elsewhere — the menu grid, the nav-bar badge — sees the edit without
     * having to ask for the cart again.
     */
    private fun Flow<UIResource<CartData>>.publishing(): Flow<UIResource<CartData>> =
        onEach { resource -> (resource as? UIResource.Success)?.let { store.publish(it.data) } }

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

    /** The cafe the cart belongs to. Also absent from the cart response. */
    private val shopNameEmitter: MutableStateFlow<String?> = MutableStateFlow(null)
    val shopNameFlow: StateFlow<String?> get() = shopNameEmitter

    /** The shop already resolved, so re-entering the screen does not re-fetch it. */
    private var loadedShopId: Int? = null

    /**
     * Fills in what the cart response leaves out — which cafe this is and what the drinks look
     * like — from the shop the cart belongs to. Callers that do not know the shop up front (the
     * cart tab) can call this again once the cart names it.
     */
    fun loadShopContext(shopId: Int) {
        if (shopId <= 0 || loadedShopId == shopId) return
        loadedShopId = shopId

        launch {
            val drinks = (shopRepo.getShopDrinks(shopId).firstOrNull() as? UIResource.Success)
                ?.data?.categories?.flatMap { it?.drinks.orEmpty() }.orEmpty()
            drinkImagesEmitter.value = drinks.mapNotNull { drink ->
                val id = drink.id ?: return@mapNotNull null
                val url = drink.pictureUrl?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                id to url
            }.toMap()
        }

        launch {
            val shop = (shopRepo.getShopDetail(shopId).firstOrNull() as? UIResource.Success)?.data
            shopNameEmitter.value = shop?.name?.takeIf { it.isNotBlank() }
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
        launch {
            cartEmitter.load { repo.getCart() }
            (cartEmitter.value as? UIResource.Success)?.let { store.publish(it.data) }
        }
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
        repo.addItem(shopId, drinkId, quantity, modifiers).publishing()
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)

    /** A [quantity] of 0 removes the line. */
    suspend fun updateItemQuantity(itemId: Int, quantity: Int): SharedFlow<UIResource<CartData>> =
        repo.updateItemQuantity(itemId, quantity).publishing()
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)

    suspend fun removeItem(itemId: Int): SharedFlow<UIResource<CartData>> =
        repo.removeItem(itemId).publishing().shareIn(viewModelScope, SharingStarted.Lazily, 0)

    suspend fun clearCart(): SharedFlow<UIResource<Any>> =
        repo.clearCart()
            // The clear answers with nothing, so there is no cart to publish — only its absence.
            .onEach { resource -> if (resource is UIResource.Success) store.clear() }
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)

    suspend fun applyPromo(code: String): SharedFlow<UIResource<CartData>> =
        repo.applyPromo(code).publishing().shareIn(viewModelScope, SharingStarted.Lazily, 0)

    suspend fun removePromo(): SharedFlow<UIResource<CartData>> =
        repo.removePromo().publishing().shareIn(viewModelScope, SharingStarted.Lazily, 0)

    suspend fun setComment(comment: String?): SharedFlow<UIResource<CartData>> =
        repo.setComment(comment).publishing().shareIn(viewModelScope, SharingStarted.Lazily, 0)

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
        launch { repo.updateItemQuantity(itemId, quantity).publishing().collect { } }
    }

    fun removeItemInBackground(itemId: Int) {
        launch { repo.removeItem(itemId).publishing().collect { } }
    }

    suspend fun checkout(
        useCashback: Boolean,
        cashbackAmount: Double
    ): SharedFlow<UIResource<CheckOutInfo>> =
        repo.checkout(useCashback, cashbackAmount)
            .shareIn(viewModelScope, SharingStarted.Lazily, 0)

}

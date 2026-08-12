package uz.alphazet.domain.cart

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import uz.alphazet.data.models.cart.CartData

/**
 * The last cart the server stated, shared by every screen that shows one.
 *
 * `CartVM` is a Koin factory, so each screen holds its own — the cart tab, the order screen, the
 * shop menu and the host all have separate instances. Without somewhere common to put the answer,
 * a screen could only learn about an edit made in front of it by asking for the cart again.
 *
 * Server truth only. Optimistic edits stay local to the screen making them, so two screens never
 * fight over a half-applied change.
 */
class CartStore {

    private val emitter = MutableStateFlow<CartData?>(null)
    val cartFlow: StateFlow<CartData?> get() = emitter

    fun publish(cart: CartData?) {
        emitter.value = cart
    }

    /** For the cart being emptied, which answers with nothing to publish. */
    fun clear() {
        emitter.value = null
    }
}

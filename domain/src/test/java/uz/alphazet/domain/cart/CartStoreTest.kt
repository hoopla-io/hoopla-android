package uz.alphazet.domain.cart

import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uz.alphazet.data.models.cart.CartData
import uz.alphazet.data.models.cart.CartItemData

@OptIn(ExperimentalCoroutinesApi::class)
class CartStoreTest {

    private val store = CartStore()

    private fun cart(id: Int, quantity: Int) = CartData(
        id = id,
        shopId = 7,
        partnerId = null,
        status = null,
        promoCode = null,
        comment = null,
        items = listOf(
            CartItemData(
                id = 1,
                drinkId = 42,
                name = "Green Tea",
                quantity = quantity,
                unitPrice = 5500.0,
                lineTotal = 5500.0 * quantity,
                modifiers = null
            )
        ),
        subtotal = 5500.0 * quantity,
        promoDiscount = null,
        total = 5500.0 * quantity
    )

    @Test
    fun `starts empty so nothing renders a cart before one has been fetched`() {
        assertNull(store.cartFlow.value)
    }

    @Test
    fun `publish replaces what observers see`() = runTest {
        store.cartFlow.test {
            assertNull(awaitItem())

            store.publish(cart(id = 1, quantity = 2))
            assertEquals(2, awaitItem()?.items?.first()?.quantity)

            store.publish(cart(id = 1, quantity = 5))
            assertEquals(5, awaitItem()?.items?.first()?.quantity)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clear empties the cart, for the response that carries none`() {
        store.publish(cart(id = 1, quantity = 2))
        store.clear()

        assertNull(store.cartFlow.value)
    }

    @Test
    fun `publishing the same cart twice does not emit again`() = runTest {
        val same = cart(id = 1, quantity = 2)

        store.cartFlow.test {
            assertNull(awaitItem())

            store.publish(same)
            assertEquals(same, awaitItem())

            // StateFlow conflates equal values, so a repeated response cannot cause a re-render.
            store.publish(same.copy())
            expectNoEvents()

            cancelAndIgnoreRemainingEvents()
        }
    }
}

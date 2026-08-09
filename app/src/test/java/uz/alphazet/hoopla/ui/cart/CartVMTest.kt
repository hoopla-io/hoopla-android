package uz.alphazet.hoopla.ui.cart

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uz.alphazet.data.UIResource
import uz.alphazet.data.models.DrinkItemData
import uz.alphazet.data.models.ShopCategoryData
import uz.alphazet.data.models.ShopData
import uz.alphazet.data.models.ShopDrinksData
import uz.alphazet.data.models.UserData
import uz.alphazet.data.models.cart.CartCountData
import uz.alphazet.data.models.cart.CartData
import uz.alphazet.data.models.cart.CartItemData
import uz.alphazet.data.models.order.CheckOutInfo
import uz.alphazet.domain.network.ConflictException
import uz.alphazet.domain.repositories.CartRepo
import uz.alphazet.domain.repositories.ProfileRepo
import uz.alphazet.domain.repositories.ShopRepo
import uz.alphazet.hoopla.rules.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class CartVMTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val cartRepo: CartRepo = mockk()
    private val profileRepo: ProfileRepo = mockk()
    private val shopRepo: ShopRepo = mockk()

    private val vm by lazy { CartVM(cartRepo, profileRepo, shopRepo) }

    @Before
    fun setUp() {
        // loadShopContext always asks for both; tests that care override their half.
        coEvery { shopRepo.getShopDetail(any()) } returns flowOf(UIResource.Success(null))
        coEvery { shopRepo.getShopDrinks(any()) } returns flowOf(UIResource.Success(null))
    }

    // --- getCart (StateFlow-backed screen state) --------------------------

    @Test
    fun cartFlow_starts_as_loading() = runTest {
        vm.cartFlow.test {
            assertTrue(awaitItem() is UIResource.Loading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getCart_populates_cartFlow_with_the_server_cart() = runTest {
        val cart = sampleCart()
        coEvery { cartRepo.getCart() } returns UIResource.Success(cart)

        vm.getCart()

        vm.cartFlow.test {
            val first = awaitItem()
            val result = if (first is UIResource.Loading) awaitItem() else first
            assertTrue(result is UIResource.Success)
            assertEquals(cart, (result as UIResource.Success).data)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getCart_surfaces_errors_on_cartFlow() = runTest {
        coEvery { cartRepo.getCart() } returns UIResource.Error(ConflictException("nope", 409))

        vm.getCart()

        vm.cartFlow.test {
            val first = awaitItem()
            val result = if (first is UIResource.Loading) awaitItem() else first
            assertTrue(result is UIResource.Error)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getUser_populates_userDataFlow() = runTest {
        val user = UserData("Ali", null, 1000.0, 100.0, "UZS", null, null, 1, null, 0)
        coEvery { profileRepo.getMe() } returns UIResource.Success(user)

        vm.getUser()

        vm.userDataFlow.test {
            val first = awaitItem()
            val result = if (first is UIResource.Loading) awaitItem() else first
            assertTrue(result is UIResource.Success)
            assertEquals(1000.0, (result as UIResource.Success).data?.balance)
            cancelAndIgnoreRemainingEvents()
        }
    }

    /** Unlike OrderVM, nothing is fetched until the screen asks — there is no init block. */
    @Test
    fun constructing_the_vm_does_not_hit_the_repositories() = runTest {
        CartVM(cartRepo, profileRepo, shopRepo)

        coVerify(exactly = 0) { cartRepo.getCart() }
        coVerify(exactly = 0) { profileRepo.getMe() }
        coVerify(exactly = 0) { shopRepo.getShopDrinks(any()) }
    }

    // --- drink images (resolved from the shop menu) -----------------------

    @Test
    fun loadShopContext_maps_drinkId_to_picture() = runTest {
        coEvery { shopRepo.getShopDrinks(3) } returns flowOf(UIResource.Success(shopDrinks()))

        vm.loadShopContext(3)

        assertEquals(
            mapOf(5 to "https://img/latte.png", 7 to "https://img/raf.png"),
            vm.drinkImagesFlow.value
        )
    }

    /** Drinks with no picture must not become null entries the adapter has to guard. */
    @Test
    fun loadShopContext_skips_drinks_without_a_picture() = runTest {
        coEvery { shopRepo.getShopDrinks(3) } returns flowOf(
            UIResource.Success(
                ShopDrinksData(
                    listOf(
                        ShopCategoryData(
                            1, "Coffee",
                            listOf(
                                DrinkItemData(5, "Latte", null, 25_000.0),
                                DrinkItemData(6, "Blank", "  ", 25_000.0)
                            )
                        )
                    )
                )
            )
        )

        vm.loadShopContext(3)

        assertTrue(vm.drinkImagesFlow.value.isEmpty())
    }

    /** A cart without pictures is still usable, so a menu failure must stay silent. */
    @Test
    fun loadShopContext_leaves_the_map_empty_when_the_menu_fails() = runTest {
        coEvery { shopRepo.getShopDrinks(any()) } returns
            flowOf(UIResource.Error(ConflictException("nope", 409)))

        vm.loadShopContext(3)

        assertTrue(vm.drinkImagesFlow.value.isEmpty())
    }

    @Test
    fun loadShopContext_ignores_an_unknown_shop() = runTest {
        vm.loadShopContext(-1)

        coVerify(exactly = 0) { shopRepo.getShopDrinks(any()) }
    }

    /** Re-entering the screen must not re-fetch a menu that is already resolved. */
    @Test
    fun loadShopContext_only_fetches_once() = runTest {
        coEvery { shopRepo.getShopDrinks(3) } returns flowOf(UIResource.Success(shopDrinks()))

        vm.loadShopContext(3)
        vm.loadShopContext(3)

        coVerify(exactly = 1) { shopRepo.getShopDrinks(3) }
        coVerify(exactly = 1) { shopRepo.getShopDetail(3) }
    }

    /** The cart tab has no shop up front, so the name has to come from the shop endpoint. */
    @Test
    fun loadShopContext_resolves_the_shop_name() = runTest {
        coEvery { shopRepo.getShopDetail(3) } returns
            flowOf(UIResource.Success(shopData("Hoopla Central")))

        vm.loadShopContext(3)

        assertEquals("Hoopla Central", vm.shopNameFlow.value)
    }

    @Test
    fun loadShopContext_ignores_a_blank_shop_name() = runTest {
        coEvery { shopRepo.getShopDetail(3) } returns flowOf(UIResource.Success(shopData("  ")))

        vm.loadShopContext(3)

        assertNull(vm.shopNameFlow.value)
    }

    /** A cart still works without knowing the cafe's name, so a failure stays silent. */
    @Test
    fun loadShopContext_leaves_the_name_null_when_the_shop_fails() = runTest {
        coEvery { shopRepo.getShopDetail(any()) } returns
            flowOf(UIResource.Error(ConflictException("nope", 409)))

        vm.loadShopContext(3)

        assertNull(vm.shopNameFlow.value)
    }

    /** The tab calls this again once the cart names a different shop. */
    @Test
    fun loadShopContext_refetches_for_a_different_shop() = runTest {
        vm.loadShopContext(3)
        vm.loadShopContext(4)

        coVerify(exactly = 1) { shopRepo.getShopDetail(3) }
        coVerify(exactly = 1) { shopRepo.getShopDetail(4) }
    }

    // --- one-shot actions -------------------------------------------------

    @Test
    fun getCartCount_returns_the_count() = runTest {
        coEvery { cartRepo.getCartCount() } returns flowOf(UIResource.Success(CartCountData(4)))

        vm.getCartCount().test {
            val result = awaitItem()
            assertTrue(result is UIResource.Success)
            assertEquals(4, (result as UIResource.Success).data?.count)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun addItem_forwards_every_argument_to_the_repo() = runTest {
        coEvery { cartRepo.addItem(3, 5, 2, any()) } returns flowOf(UIResource.Success(sampleCart()))

        vm.addItem(3, 5, 2, arrayListOf()).test {
            assertTrue(awaitItem() is UIResource.Success)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { cartRepo.addItem(3, 5, 2, any()) }
    }

    /** The cross-shop conflict has to reach the screen intact for the retry prompt to work. */
    @Test
    fun addItem_propagates_a_conflict_error() = runTest {
        coEvery { cartRepo.addItem(any(), any(), any(), any()) } returns
            flowOf(UIResource.Error(ConflictException("another shop", 409)))

        vm.addItem(4, 5, 1, arrayListOf()).test {
            val result = awaitItem()
            assertTrue(result is UIResource.Error)
            assertTrue((result as UIResource.Error).throwable is ConflictException)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun updateItemQuantity_forwards_zero_to_drop_a_line() = runTest {
        coEvery { cartRepo.updateItemQuantity(11, 0) } returns
            flowOf(UIResource.Success(sampleCart(items = emptyList())))

        vm.updateItemQuantity(11, 0).test {
            val result = awaitItem()
            assertTrue(result is UIResource.Success)
            assertEquals(emptyList<CartItemData>(), (result as UIResource.Success).data?.items)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { cartRepo.updateItemQuantity(11, 0) }
    }

    @Test
    fun removeItem_forwards_the_item_id() = runTest {
        coEvery { cartRepo.removeItem(11) } returns flowOf(UIResource.Success(sampleCart()))

        vm.removeItem(11).test {
            assertTrue(awaitItem() is UIResource.Success)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { cartRepo.removeItem(11) }
    }

    @Test
    fun clearCart_calls_the_repo() = runTest {
        coEvery { cartRepo.clearCart() } returns flowOf(UIResource.Success(null))

        vm.clearCart().test {
            assertTrue(awaitItem() is UIResource.Success)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { cartRepo.clearCart() }
    }

    @Test
    fun applyPromo_returns_the_repriced_cart() = runTest {
        val discounted = sampleCart().copy(promoCode = "SUMMER25", promoDiscount = 5_000.0)
        coEvery { cartRepo.applyPromo("SUMMER25") } returns flowOf(UIResource.Success(discounted))

        vm.applyPromo("SUMMER25").test {
            val result = awaitItem()
            assertTrue(result is UIResource.Success)
            assertEquals("SUMMER25", (result as UIResource.Success).data?.promoCode)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun removePromo_calls_the_repo() = runTest {
        coEvery { cartRepo.removePromo() } returns flowOf(UIResource.Success(sampleCart()))

        vm.removePromo().test {
            assertTrue(awaitItem() is UIResource.Success)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { cartRepo.removePromo() }
    }

    @Test
    fun setComment_forwards_the_note() = runTest {
        coEvery { cartRepo.setComment("no sugar") } returns
            flowOf(UIResource.Success(sampleCart().copy(comment = "no sugar")))

        vm.setComment("no sugar").test {
            val result = awaitItem()
            assertEquals("no sugar", (result as UIResource.Success).data?.comment)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { cartRepo.setComment("no sugar") }
    }

    /** Used from onPause, where no collector survives to observe the result. */
    @Test
    fun setCommentInBackground_sends_the_note_without_a_collector() = runTest {
        coEvery { cartRepo.setComment("no sugar") } returns
            flowOf(UIResource.Success(sampleCart().copy(comment = "no sugar")))

        vm.setCommentInBackground("no sugar")

        coVerify(exactly = 1) { cartRepo.setComment("no sugar") }
    }

    @Test
    fun setCommentInBackground_swallows_failures() = runTest {
        coEvery { cartRepo.setComment(any()) } returns
            flowOf(UIResource.Error(ConflictException("nope", 409)))

        // Must not throw: nothing is listening and the screen is already going away.
        vm.setCommentInBackground("no sugar")

        coVerify(exactly = 1) { cartRepo.setComment("no sugar") }
    }

    @Test
    fun checkout_forwards_the_cashback_choice_and_returns_checkout_info() = runTest {
        val info = CheckOutInfo(55_000, "https://pay/abc", null, null, 42, null)
        coEvery { cartRepo.checkout(true, 2_000.0) } returns flowOf(UIResource.Success(info))

        vm.checkout(true, 2_000.0).test {
            val result = awaitItem()
            assertTrue(result is UIResource.Success)
            assertEquals(42, (result as UIResource.Success).data?.order_id)
            cancelAndIgnoreRemainingEvents()
        }

        coVerify { cartRepo.checkout(true, 2_000.0) }
    }

    // --- helpers ---

    private fun sampleCart(items: List<CartItemData> = listOf(sampleItem())) = CartData(
        id = 1,
        shopId = 3,
        partnerId = 9,
        status = "active",
        promoCode = null,
        comment = null,
        items = items,
        subtotal = 55_000.0,
        promoDiscount = 0.0,
        total = 55_000.0
    )

    private fun shopData(name: String?) = ShopData(
        id = 3, partnerId = 9, name = name, pictureUrl = null, canAcceptOrders = true,
        location = null, phoneNumbers = null, workingHours = null, pictures = null,
        drinks = null, urls = null
    )

    private fun shopDrinks() = ShopDrinksData(
        listOf(
            ShopCategoryData(
                1, "Coffee",
                listOf(
                    DrinkItemData(5, "Latte", "https://img/latte.png", 25_000.0),
                    DrinkItemData(6, "No picture", null, 22_000.0)
                )
            ),
            ShopCategoryData(
                2, "Specials",
                listOf(DrinkItemData(7, "Raf", "https://img/raf.png", 30_000.0))
            )
        )
    )

    private fun sampleItem() = CartItemData(
        id = 11,
        drinkId = 5,
        name = "Cappuccino",
        quantity = 2,
        unitPrice = 25_000.0,
        lineTotal = 50_000.0,
        modifiers = emptyList()
    )

}

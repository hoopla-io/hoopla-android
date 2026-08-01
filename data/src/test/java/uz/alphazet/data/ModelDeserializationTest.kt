package uz.alphazet.data

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import uz.alphazet.data.models.AccessTokenData
import uz.alphazet.data.models.ConfirmSMSData
import uz.alphazet.data.models.LocationData
import uz.alphazet.data.models.NotificationItemData
import uz.alphazet.data.models.ShopItemData
import uz.alphazet.data.models.UserData
import uz.alphazet.data.models.order.CheckOutInfo
import uz.alphazet.data.models.order.OrderInfo
import uz.alphazet.data.models.order.OrderInfoData
import uz.alphazet.data.models.order.OrderItemData
import uz.alphazet.data.models.order.PaymentRequiredExceptionData

/**
 * Spot-checks that the DTOs the app relies on the most parse the JSON shapes
 * the backend actually sends. We only cover models with non-trivial
 * structure — nested objects, @SerializedName mappings, or BaseItem impls —
 * since testing every field of every flat DTO would be low-signal.
 */
class ModelDeserializationTest {

    private val gson = Gson()

    @Test
    fun user_data_maps_cashback_balance_from_snake_case() {
        val json = """
            {
                "name": "Ali",
                "phoneNumber": "998901234567",
                "balance": 1200.5,
                "cashback_balance": 300.0,
                "currency": "UZS",
                "gender": "M",
                "dateOfBirth": "1990-01-01",
                "userId": 42,
                "subscription": {
                    "id": 1,
                    "name": "Gold",
                    "endDate": "2030-01-01",
                    "endDateUnix": 1893456000
                },
                "unreadNotifications": 3
            }
        """.trimIndent()

        val user = gson.fromJson(json, UserData::class.java)

        assertEquals("Ali", user.name)
        assertEquals("998901234567", user.phoneNumber)
        assertEquals(1200.5, user.balance!!, 0.0)
        // @SerializedName("cashback_balance") → cashbackBalance
        assertEquals(300.0, user.cashbackBalance!!, 0.0)
        assertEquals(42, user.userId)
        assertEquals(3, user.unreadNotifications)
        val sub = user.subscription
        assertNotNull(sub)
        assertEquals("Gold", sub!!.name)
        assertEquals(1_893_456_000L, sub.endDateUnix)
    }

    @Test
    fun user_data_tolerates_missing_optional_fields() {
        val json = """{ "name": "Ali" }"""

        val user = gson.fromJson(json, UserData::class.java)

        assertEquals("Ali", user.name)
        assertNull(user.phoneNumber)
        assertNull(user.balance)
        assertNull(user.cashbackBalance)
        assertNull(user.subscription)
    }

    @Test
    fun access_token_data_deserializes_all_fields() {
        val json = """
            {
                "accessToken": "a.b.c",
                "refreshToken": "r.s.t",
                "expireAt": 1893456000
            }
        """.trimIndent()

        val token = gson.fromJson(json, AccessTokenData::class.java)

        assertEquals("a.b.c", token.accessToken)
        assertEquals("r.s.t", token.refreshToken)
        assertEquals(1_893_456_000L, token.expireAt)
    }

    @Test
    fun confirm_sms_data_deserializes_nested_jwt() {
        val json = """
            {
                "userId": 42,
                "isNewUser": true,
                "phoneNumber": "998901234567",
                "jwt": {
                    "accessToken": "a.b.c",
                    "refreshToken": "r.s.t",
                    "expireAt": 1893456000
                }
            }
        """.trimIndent()

        val result = gson.fromJson(json, ConfirmSMSData::class.java)

        assertEquals(42L, result.userId)
        assertTrue(result.isNewUser)
        assertEquals("998901234567", result.phoneNumber)
        assertEquals("a.b.c", result.jwt.accessToken)
        assertEquals(1_893_456_000L, result.jwt.expireAt)
    }

    @Test
    fun shop_item_data_deserializes_nested_location() {
        val json = """
            {
                "shopId": 7,
                "name": "Hoopla Central",
                "pictureUrl": "https://img/1.png",
                "distance": 0.42,
                "location": { "lat": 41.31, "lng": 69.24 }
            }
        """.trimIndent()

        val shop = gson.fromJson(json, ShopItemData::class.java)

        assertEquals(7, shop.shopId)
        assertEquals("Hoopla Central", shop.name)
        assertEquals(0.42, shop.distance!!, 0.0)
        val loc: LocationData? = shop.location
        assertNotNull(loc)
        assertEquals(41.31, loc!!.lat, 0.0)
        assertEquals(69.24, loc.lng, 0.0)
    }

    @Test
    fun shop_item_data_unique_id_derives_from_shop_id() {
        val shop = ShopItemData(
            shopId = 9,
            name = "x",
            pictureUrl = null,
            distance = null,
            location = null
        )

        assertEquals("9", shop.uniqueId)
    }

    @Test
    fun shop_item_data_unique_id_null_shop_id_falls_back_to_string_null() {
        val shop = ShopItemData(
            shopId = null,
            name = null,
            pictureUrl = null,
            distance = null,
            location = null
        )

        // Locks in current behavior: Int?.toString() on null returns "null"
        assertEquals("null", shop.uniqueId)
    }

    @Test
    fun notification_item_data_exposes_nested_files() {
        val json = """
            {
                "createdAt": "2026-01-01T12:00:00Z",
                "files": { "imageUrl": "https://img/n.png" },
                "isNew": false,
                "notificationDescription": "desc",
                "notificationId": 55,
                "notificationTitle": "title",
                "url": "app://n/55"
            }
        """.trimIndent()

        val notif = gson.fromJson(json, NotificationItemData::class.java)

        assertEquals(55, notif.notificationId)
        assertEquals("https://img/n.png", notif.files?.imageUrl)
        assertFalse(notif.isNew!!)
    }

    @Test
    fun notification_item_data_unique_id_works_via_constructor() {
        val direct = NotificationItemData(
            createdAt = null,
            files = null,
            isNew = null,
            notificationDescription = null,
            notificationId = 55,
            notificationTitle = null,
            url = null
        )

        assertEquals("55", direct.uniqueId)
    }

    @Test
    fun notification_item_data_unique_id_is_null_when_parsed_from_gson() {
        // Latent gotcha: NotificationItemData defines
        //   override val uniqueId: String = notificationId.toString()
        // as a constructor-time initializer. Gson bypasses the constructor
        // (sun.misc.Unsafe.allocateInstance), so the initializer never runs
        // and uniqueId ends up as null even though the type says non-null.
        // ShopItemData avoids this by using `get() = shopId.toString()`.
        // Locking this in as a regression test — switching the declaration
        // to a `get()` getter would flip this assertion.
        val json = """{ "notificationId": 55 }"""

        val notif = gson.fromJson(json, NotificationItemData::class.java)

        @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
        val idOrNull: String? = notif.uniqueId as String?
        assertEquals(null, idOrNull)
    }

    @Test
    fun payment_required_exception_data_maps_snake_case_fields() {
        val json = """
            {
                "amount": 5000,
                "checkout_url": "https://pay/co",
                "deeplink": "app://pay",
                "expires_at": "2030-01-01",
                "order_id": 88,
                "short_link": "http://s/x"
            }
        """.trimIndent()

        val data = gson.fromJson(json, PaymentRequiredExceptionData::class.java)

        assertEquals(5000, data.amount)
        assertEquals("https://pay/co", data.checkoutUrl)
        assertEquals("app://pay", data.deeplink)
        assertEquals("2030-01-01", data.expiresAt)
        assertEquals(88, data.orderId)
        assertEquals("http://s/x", data.shortLink)
    }

    // The scheduled pickup instant arrives under two different names depending on the
    // endpoint — snake_case from checkout, camelCase from the order list and detail — so
    // each mapping is pinned separately.

    @Test
    fun checkout_responses_map_pickup_at_from_snake_case() {
        val checkout = gson.fromJson(
            """{"order_id": 4, "pickup_at": "2026-07-30T17:30:00+05:00"}""",
            CheckOutInfo::class.java
        )
        assertEquals("2026-07-30T17:30:00+05:00", checkout.pickup_at)

        val created = gson.fromJson(
            """{"id": 4, "pickup_at": "2026-07-30T17:30:00+05:00"}""",
            OrderInfoData::class.java
        )
        assertEquals("2026-07-30T17:30:00+05:00", created.pickupAt)
    }

    @Test
    fun order_list_and_detail_map_pickup_at_from_camel_case() {
        val item = gson.fromJson(
            """{"id": 7, "pickupAt": "2026-07-30T17:30:00+05:00"}""",
            OrderItemData::class.java
        )
        assertEquals("2026-07-30T17:30:00+05:00", item.pickupAt)

        val info = gson.fromJson(
            """{"id": 7, "pickupAt": "2026-07-30T17:30:00+05:00"}""",
            OrderInfo::class.java
        )
        assertEquals("2026-07-30T17:30:00+05:00", info.pickupAt)
    }

    @Test
    fun an_asap_order_leaves_pickup_at_null() {
        // Every order placed before scheduled pickup existed looks like this.
        assertNull(gson.fromJson("""{"id": 7}""", OrderItemData::class.java).pickupAt)
        assertNull(gson.fromJson("""{"id": 7}""", OrderInfo::class.java).pickupAt)
        assertNull(gson.fromJson("""{"id": 7, "pickupAt": null}""", OrderItemData::class.java).pickupAt)
    }
}
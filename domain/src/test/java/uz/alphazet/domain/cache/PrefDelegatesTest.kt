package uz.alphazet.domain.cache

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PrefDelegatesTest {

    private lateinit var prefs: SharedPreferences

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        prefs = context.getSharedPreferences("prefs-test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
    }

    // --- string -----------------------------------------------------------

    @Test
    fun string_returns_default_before_write() {
        val holder = StringHolder(prefs)

        assertEquals("default-name", holder.name)
    }

    @Test
    fun string_round_trips() {
        val holder = StringHolder(prefs)

        holder.name = "Alisher"

        assertEquals("Alisher", holder.name)
        assertEquals("Alisher", prefs.getString("name", null))
    }

    // --- stringNullable ---------------------------------------------------

    @Test
    fun stringNullable_defaults_to_null() {
        val holder = NullableStringHolder(prefs)

        assertNull(holder.nickname)
    }

    @Test
    fun stringNullable_round_trips_null() {
        val holder = NullableStringHolder(prefs)
        holder.nickname = "ali"
        assertEquals("ali", holder.nickname)

        holder.nickname = null

        assertNull(holder.nickname)
    }

    // --- int / long / float / boolean -------------------------------------

    @Test
    fun int_default_and_round_trip() {
        val holder = PrimitiveHolder(prefs)

        assertEquals(0, holder.age)

        holder.age = 27
        assertEquals(27, holder.age)
    }

    @Test
    fun long_default_and_round_trip() {
        val holder = PrimitiveHolder(prefs)

        assertEquals(0L, holder.updatedAt)

        holder.updatedAt = 1_700_000_000L
        assertEquals(1_700_000_000L, holder.updatedAt)
    }

    @Test
    fun float_default_and_round_trip() {
        val holder = PrimitiveHolder(prefs)

        assertEquals(0f, holder.score, 0f)

        holder.score = 4.5f
        assertEquals(4.5f, holder.score, 0f)
    }

    @Test
    fun boolean_default_and_round_trip() {
        val holder = PrimitiveHolder(prefs)

        assertEquals(false, holder.enabled)

        holder.enabled = true
        assertEquals(true, holder.enabled)
    }

    // --- double (stored as String internally) ------------------------------

    @Test
    fun double_default_and_round_trip() {
        val holder = PrimitiveHolder(prefs)

        assertEquals(0.0, holder.balance, 0.0)

        holder.balance = 12.75
        assertEquals(12.75, holder.balance, 0.0)
    }

    @Test
    fun double_is_stored_as_string_under_the_hood() {
        val holder = PrimitiveHolder(prefs)

        holder.balance = 3.14

        assertEquals("3.14", prefs.getString("balance", null))
    }

    // --- any<T> ------------------------------------------------------------
    // The any<T> delegate writes with Gson.toJson(value), which works, but
    // reads via `TypeToken<T>()` from a non-reified factory function — the
    // type parameter erases to TypeVariable, so Gson cannot rehydrate into a
    // concrete class. The delegate is currently unused in production
    // (AppCacheImpl only uses string/long/boolean). We only lock in the
    // "absent key returns null" branch here; a round-trip test would require
    // fixing the delegate to take `inline <reified T>` or an explicit Class.

    @Test
    fun any_returns_null_when_absent() {
        val holder = ObjectHolder(prefs)

        assertNull(holder.user)
    }

    // --- custom key lambda ------------------------------------------------

    @Test
    fun custom_key_lambda_is_honored() {
        val holder = CustomKeyHolder(prefs)

        holder.token = "tok-123"

        assertEquals("tok-123", prefs.getString("custom_token", null))
        assertTrue(prefs.getString("token", null) == null)
    }

    // --- test holder classes ----------------------------------------------

    data class UserStub(val id: Int, val name: String)

    private class StringHolder(prefs: SharedPreferences) {
        var name: String by prefs.string(defaultValue = "default-name")
    }

    private class NullableStringHolder(prefs: SharedPreferences) {
        var nickname: String? by prefs.stringNullable()
    }

    private class PrimitiveHolder(prefs: SharedPreferences) {
        var age: Int by prefs.int()
        var updatedAt: Long by prefs.long()
        var score: Float by prefs.float()
        var balance: Double by prefs.double()
        var enabled: Boolean by prefs.boolean()
    }

    private class ObjectHolder(prefs: SharedPreferences) {
        var user: UserStub? by prefs.any()
    }

    private class CustomKeyHolder(prefs: SharedPreferences) {
        var token: String by prefs.string(
            defaultValue = "",
            key = { "custom_token" }
        )
    }
}
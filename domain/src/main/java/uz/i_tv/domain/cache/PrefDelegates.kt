package uz.i_tv.domain.cache

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun SharedPreferences.string(
    defaultValue: String = "",
    key: (KProperty<*>) -> String = KProperty<*>::name
): ReadWriteProperty<Any, String> = object : ReadWriteProperty<Any, String> {
    override fun getValue(
        thisRef: Any,
        property: KProperty<*>
    ): String = getString(key(property), defaultValue) ?: defaultValue

    override fun setValue(
        thisRef: Any,
        property: KProperty<*>,
        value: String
    ): Unit = edit().putString(key(property), value).apply()
}

fun SharedPreferences.stringNullable(
    defaultValue: String? = null,
    key: (KProperty<*>) -> String = KProperty<*>::name
): ReadWriteProperty<Any, String?> = object : ReadWriteProperty<Any, String?> {
    override fun getValue(
        thisRef: Any,
        property: KProperty<*>
    ) = getString(key(property), defaultValue)

    override fun setValue(
        thisRef: Any,
        property: KProperty<*>,
        value: String?
    ) = edit().putString(key(property), value).apply()
}

fun <T> SharedPreferences.any(
    key: (KProperty<*>) -> String = KProperty<*>::name
): ReadWriteProperty<Any, T?> = object : ReadWriteProperty<Any, T?> {

    override fun getValue(thisRef: Any, property: KProperty<*>): T? {
        val json = getString(key(property), null)
        if (json.isNullOrEmpty()) return null
        return Gson().fromJson(json, object : TypeToken<T>() {}.type)
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: T?): Unit =
        edit().putString(key(property), Gson().toJson(value)).apply()

}

fun SharedPreferences.int(
    defaultValue: Int = 0,
    key: (KProperty<*>) -> String = KProperty<*>::name
): ReadWriteProperty<Any, Int> = object : ReadWriteProperty<Any, Int> {
    override fun getValue(
        thisRef: Any,
        property: KProperty<*>
    ) = getInt(key(property), defaultValue)

    override fun setValue(
        thisRef: Any,
        property: KProperty<*>,
        value: Int
    ): Unit = edit().putInt(key(property), value).apply()
}

fun SharedPreferences.long(
    defaultValue: Long = 0L,
    key: (KProperty<*>) -> String = KProperty<*>::name
): ReadWriteProperty<Any, Long> = object : ReadWriteProperty<Any, Long> {
    override fun getValue(
        thisRef: Any,
        property: KProperty<*>
    ) = getLong(key(property), defaultValue)

    override fun setValue(
        thisRef: Any,
        property: KProperty<*>,
        value: Long
    ): Unit = edit().putLong(key(property), value).apply()
}

fun SharedPreferences.float(
    defaultValue: Float = 0f,
    key: (KProperty<*>) -> String = KProperty<*>::name
): ReadWriteProperty<Any, Float> = object : ReadWriteProperty<Any, Float> {
    override fun getValue(
        thisRef: Any,
        property: KProperty<*>
    ) = getFloat(key(property), defaultValue)

    override fun setValue(
        thisRef: Any,
        property: KProperty<*>,
        value: Float
    ): Unit = edit().putFloat(key(property), value).apply()
}


fun SharedPreferences.double(
    defaultValue: Double = 0.0,
    key: (KProperty<*>) -> String = KProperty<*>::name
): ReadWriteProperty<Any, Double> = object : ReadWriteProperty<Any, Double> {
    override fun getValue(
        thisRef: Any,
        property: KProperty<*>
    ): Double = getString(key(property), defaultValue.toString())?.toDouble() ?: defaultValue

    override fun setValue(
        thisRef: Any,
        property: KProperty<*>,
        value: Double
    ): Unit = edit().putString(key(property), value.toString()).apply()
}

fun SharedPreferences.boolean(
    defaultValue: Boolean = false,
    key: (KProperty<*>) -> String = KProperty<*>::name
): ReadWriteProperty<Any, Boolean> = object : ReadWriteProperty<Any, Boolean> {
    override fun getValue(
        thisRef: Any,
        property: KProperty<*>
    ): Boolean = getBoolean(key(property), defaultValue)

    override fun setValue(
        thisRef: Any,
        property: KProperty<*>,
        value: Boolean
    ): Unit = edit().putBoolean(key(property), value).apply()
}
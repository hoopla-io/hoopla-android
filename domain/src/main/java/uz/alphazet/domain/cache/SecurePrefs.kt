package uz.alphazet.domain.cache

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Builds the app's [SharedPreferences] backed by [EncryptedSharedPreferences] so
 * that auth tokens and other cached values are encrypted at rest (AES-256), keyed
 * by a non-exportable Android Keystore master key.
 *
 * On first run it migrates any values previously stored in the legacy plaintext
 * prefs ([create]'s `legacyName`) into the encrypted store and clears the old
 * file. If the Keystore is unavailable or the store is corrupted (rare — e.g. the
 * user removed their lock screen on some OEMs) it resets once and, only as a last
 * resort, falls back to plaintext so the app never crash-loops on launch.
 *
 * Note: `androidx.security:security-crypto` is deprecated by Google but remains
 * the lowest-risk way to encrypt SharedPreferences; revisit if a stable
 * replacement lands.
 */
object SecurePrefs {

    private const val TAG = "SecurePrefs"
    private const val SECURE_NAME = "app_cache_secure"

    fun create(context: Context, legacyName: String): SharedPreferences {
        val prefs = buildEncrypted(context)
            ?: context.getSharedPreferences(SECURE_NAME, Context.MODE_PRIVATE)
        migrateFromLegacy(context, legacyName, prefs)
        return prefs
    }

    private fun buildEncrypted(context: Context): SharedPreferences? {
        return try {
            createEncrypted(context)
        } catch (e: Exception) {
            // Master key / prefs file corrupted — wipe and retry once.
            Log.e(TAG, "Encrypted prefs init failed, resetting", e)
            runCatching { context.deleteSharedPreferences(SECURE_NAME) }
            try {
                createEncrypted(context)
            } catch (e2: Exception) {
                Log.e(TAG, "Encrypted prefs unavailable, using plaintext fallback", e2)
                null
            }
        }
    }

    private fun createEncrypted(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            SECURE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun migrateFromLegacy(
        context: Context,
        legacyName: String,
        target: SharedPreferences
    ) {
        if (legacyName == SECURE_NAME) return
        val legacy = context.getSharedPreferences(legacyName, Context.MODE_PRIVATE)
        val all = legacy.all
        if (all.isEmpty()) return

        val editor = target.edit()
        for ((key, value) in all) {
            when (value) {
                is String -> editor.putString(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Set<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    editor.putStringSet(key, value as Set<String>)
                }
            }
        }
        editor.apply()
        legacy.edit().clear().apply()
        Log.i(TAG, "Migrated ${all.size} legacy pref entries into encrypted store")
    }
}

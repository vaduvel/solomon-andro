package ro.solomon.app.security

import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import ro.solomon.app.di.ServiceLocator

/**
 * Encrypted key/value storage for secrets (API keys, private keys).
 * Backed by Jetpack Security's EncryptedSharedPreferences (AES-256).
 *
 * Secrets must never be persisted in plain DataStore / SharedPreferences.
 */
object SecureKeyStore {
    private const val FILE_NAME = "solomon_secure_prefs"

    @Volatile
    private var cached: SharedPreferences? = null

    private fun prefs(): SharedPreferences {
        return cached ?: synchronized(this) {
            cached ?: build().also { cached = it }
        }
    }

    private fun build(): SharedPreferences {
        val ctx = ServiceLocator.appContext
        val masterKey = MasterKey.Builder(ctx)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            ctx,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getString(key: String): String = prefs().getString(key, "").orEmpty()

    fun putString(key: String, value: String) {
        prefs().edit().putString(key, value).apply()
    }

    fun remove(key: String) {
        prefs().edit().remove(key).apply()
    }
}

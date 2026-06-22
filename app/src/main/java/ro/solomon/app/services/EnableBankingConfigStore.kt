package ro.solomon.app.services

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import ro.solomon.app.di.ServiceLocator
import ro.solomon.app.di.preferencesStore
import ro.solomon.app.security.SecureKeyStore
import ro.solomon.core.enablebanking.EnableBankingConfig

object EnableBankingConfigStore {
    private val KEY_APP_ID = stringPreferencesKey("enable_banking.app_id")
    // Legacy plaintext key kept only for one-time migration.
    private val LEGACY_PRIVATE_KEY = stringPreferencesKey("enable_banking.private_key")
    // The private key lives in EncryptedSharedPreferences.
    private const val SECURE_PRIVATE_KEY = "enable_banking.private_key"

    fun appIdFlow(): Flow<String> =
        ServiceLocator.appContext.preferencesStore.data.map { it[KEY_APP_ID].orEmpty() }
    fun privateKeyFlow(): Flow<String> = flow { emit(privateKey()) }

    suspend fun appId(): String =
        ServiceLocator.appContext.preferencesStore.data.first()[KEY_APP_ID].orEmpty()

    suspend fun privateKey(): String {
        migrateLegacyKeyIfNeeded()
        return SecureKeyStore.getString(SECURE_PRIVATE_KEY)
    }

    suspend fun save(appId: String, privateKey: String) {
        ServiceLocator.appContext.preferencesStore.edit { it[KEY_APP_ID] = appId }
        SecureKeyStore.putString(SECURE_PRIVATE_KEY, privateKey)
        sync()
    }

    suspend fun sync() {
        EnableBankingConfig.applicationID = runCatching { appId() }.getOrNull()?.takeIf { it.isNotBlank() }
        EnableBankingConfig.privateKeyPEM = runCatching { privateKey() }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private suspend fun migrateLegacyKeyIfNeeded() {
        val legacy = ServiceLocator.appContext.preferencesStore.data.first()[LEGACY_PRIVATE_KEY]
        if (!legacy.isNullOrBlank()) {
            SecureKeyStore.putString(SECURE_PRIVATE_KEY, legacy)
            ServiceLocator.appContext.preferencesStore.edit { it.remove(LEGACY_PRIVATE_KEY) }
        }
    }
}

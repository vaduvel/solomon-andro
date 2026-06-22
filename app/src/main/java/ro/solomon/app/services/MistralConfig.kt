package ro.solomon.app.services

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import ro.solomon.app.di.ServiceLocator
import ro.solomon.app.di.preferencesStore
import ro.solomon.app.security.SecureKeyStore

object MistralConfig {
    // Non-secret prefs stay in DataStore.
    private val KEY_ENABLED = booleanPreferencesKey("mistral.enabled")
    private val KEY_MODEL = stringPreferencesKey("mistral.model")
    // Legacy plaintext key, kept only to migrate existing installs out of DataStore.
    private val LEGACY_API_KEY = stringPreferencesKey("mistral.apiKey")
    // The secret lives in EncryptedSharedPreferences.
    private const val SECURE_API_KEY = "mistral.apiKey"
    private val DEFAULT_MODEL = "mistral-small-latest"

    fun apiKeyFlow(): Flow<String> = flow { emit(apiKey()) }

    fun enabledFlow(): Flow<Boolean> =
        ServiceLocator.appContext.preferencesStore.data.map { it[KEY_ENABLED] == true }

    fun modelFlow(): Flow<String> =
        ServiceLocator.appContext.preferencesStore.data.map { it[KEY_MODEL] ?: DEFAULT_MODEL }

    suspend fun apiKey(): String {
        migrateLegacyKeyIfNeeded()
        return SecureKeyStore.getString(SECURE_API_KEY)
    }

    suspend fun enabled(): Boolean =
        ServiceLocator.appContext.preferencesStore.data.first()[KEY_ENABLED] == true

    suspend fun model(): String =
        ServiceLocator.appContext.preferencesStore.data.first()[KEY_MODEL] ?: DEFAULT_MODEL

    suspend fun setApiKey(key: String) {
        SecureKeyStore.putString(SECURE_API_KEY, key)
    }

    suspend fun setEnabled(v: Boolean) {
        ServiceLocator.appContext.preferencesStore.edit { it[KEY_ENABLED] = v }
    }

    suspend fun setModel(m: String) {
        ServiceLocator.appContext.preferencesStore.edit { it[KEY_MODEL] = m }
    }

    private suspend fun migrateLegacyKeyIfNeeded() {
        val legacy = ServiceLocator.appContext.preferencesStore.data.first()[LEGACY_API_KEY]
        if (!legacy.isNullOrBlank()) {
            SecureKeyStore.putString(SECURE_API_KEY, legacy)
            ServiceLocator.appContext.preferencesStore.edit { it.remove(LEGACY_API_KEY) }
        }
    }
}

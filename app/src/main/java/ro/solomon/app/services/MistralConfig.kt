package ro.solomon.app.services

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ro.solomon.app.di.ServiceLocator
import ro.solomon.app.di.preferencesStore

object MistralConfig {
    private val KEY_API_KEY = stringPreferencesKey("mistral.apiKey")
    private val KEY_ENABLED = booleanPreferencesKey("mistral.enabled")
    private val KEY_MODEL = stringPreferencesKey("mistral.model")
    private val DEFAULT_MODEL = "mistral-small-latest"

    fun apiKeyFlow(): Flow<String> =
        ServiceLocator.appContext.preferencesStore.data.map { it[KEY_API_KEY].orEmpty() }

    fun enabledFlow(): Flow<Boolean> =
        ServiceLocator.appContext.preferencesStore.data.map { it[KEY_ENABLED] == true }

    fun modelFlow(): Flow<String> =
        ServiceLocator.appContext.preferencesStore.data.map { it[KEY_MODEL] ?: DEFAULT_MODEL }

    suspend fun apiKey(): String =
        ServiceLocator.appContext.preferencesStore.data.first()[KEY_API_KEY].orEmpty()

    suspend fun enabled(): Boolean =
        ServiceLocator.appContext.preferencesStore.data.first()[KEY_ENABLED] == true

    suspend fun model(): String =
        ServiceLocator.appContext.preferencesStore.data.first()[KEY_MODEL] ?: DEFAULT_MODEL

    suspend fun setApiKey(key: String) {
        ServiceLocator.appContext.preferencesStore.edit { it[KEY_API_KEY] = key }
    }

    suspend fun setEnabled(v: Boolean) {
        ServiceLocator.appContext.preferencesStore.edit { it[KEY_ENABLED] = v }
    }

    suspend fun setModel(m: String) {
        ServiceLocator.appContext.preferencesStore.edit { it[KEY_MODEL] = m }
    }
}

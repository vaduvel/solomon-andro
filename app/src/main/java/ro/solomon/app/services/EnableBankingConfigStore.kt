package ro.solomon.app.services

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ro.solomon.app.di.ServiceLocator
import ro.solomon.app.di.preferencesStore
import ro.solomon.core.enablebanking.EnableBankingConfig

object EnableBankingConfigStore {
    private val KEY_APP_ID = stringPreferencesKey("enable_banking.app_id")
    private val KEY_PRIVATE_KEY = stringPreferencesKey("enable_banking.private_key")

    fun appIdFlow(): Flow<String> =
        ServiceLocator.appContext.preferencesStore.data.map { it[KEY_APP_ID].orEmpty() }
    fun privateKeyFlow(): Flow<String> =
        ServiceLocator.appContext.preferencesStore.data.map { it[KEY_PRIVATE_KEY].orEmpty() }

    suspend fun appId(): String =
        ServiceLocator.appContext.preferencesStore.data.first()[KEY_APP_ID].orEmpty()
    suspend fun privateKey(): String =
        ServiceLocator.appContext.preferencesStore.data.first()[KEY_PRIVATE_KEY].orEmpty()

    suspend fun save(appId: String, privateKey: String) {
        ServiceLocator.appContext.preferencesStore.edit {
            it[KEY_APP_ID] = appId
            it[KEY_PRIVATE_KEY] = privateKey
        }
        sync()
    }

    suspend fun sync() {
        EnableBankingConfig.applicationID = runCatching {
            ServiceLocator.appContext.preferencesStore.data.first()[KEY_APP_ID]
        }.getOrNull()?.takeIf { it.isNotBlank() }
        EnableBankingConfig.privateKeyPEM = runCatching {
            ServiceLocator.appContext.preferencesStore.data.first()[KEY_PRIVATE_KEY]
        }.getOrNull()?.takeIf { it.isNotBlank() }
    }
}

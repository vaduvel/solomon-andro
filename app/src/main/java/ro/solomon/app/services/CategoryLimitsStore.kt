package ro.solomon.app.services

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ro.solomon.app.di.ServiceLocator
import ro.solomon.app.di.preferencesStore
import ro.solomon.core.domain.TransactionCategory

object CategoryLimitsStore {

    private fun limitKey(cat: TransactionCategory) =
        intPreferencesKey("categoryLimit.${cat.name}")

    private suspend fun readAll(): Map<TransactionCategory, Int> {
        return try {
            val prefs = ServiceLocator.appContext.preferencesStore.data.first()
            TransactionCategory.values().associateWith { cat ->
                prefs[limitKey(cat)] ?: 0
            }.filter { it.value > 0 }
        } catch (_: Throwable) { emptyMap() }
    }

    suspend fun limits(): Map<TransactionCategory, Int> = readAll()

    suspend fun limitFor(cat: TransactionCategory): Int? =
        readAll()[cat]?.takeIf { it > 0 }

    suspend fun setLimit(cat: TransactionCategory, ron: Int) {
        ServiceLocator.appContext.preferencesStore.edit {
            if (ron <= 0) it.remove(limitKey(cat)) else it[limitKey(cat)] = ron
        }
    }

    suspend fun remove(cat: TransactionCategory) {
        ServiceLocator.appContext.preferencesStore.edit { it.remove(limitKey(cat)) }
    }

    suspend fun usedFor(
        cat: TransactionCategory,
        spent: Int
    ): Double {
        val limit = limitFor(cat) ?: return 0.0
        if (limit == 0) return 0.0
        return (spent.toDouble() / limit.toDouble()).coerceAtLeast(0.0)
    }

    fun isNearLimit(used: Double): Boolean = used >= 0.80
    fun isOverLimit(used: Double): Boolean = used >= 1.00
}

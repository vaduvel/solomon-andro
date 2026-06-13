package ro.solomon.app.services

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.first
import ro.solomon.app.di.ServiceLocator
import ro.solomon.app.di.preferencesStore

object MomentCooldownManager {

    enum class CooldownType(val seconds: Long) {
        SpiralAlert(0L),
        CanIAfford(0L),
        UpcomingObligation(12L * 3600L),
        Payday(24L * 3600L),
        PatternAlert(72L * 3600L),
        SubscriptionAudit(7L * 24L * 3600L),
        WeeklySummary(7L * 24L * 3600L),
        WowMoment(-1L);
    }

    private fun keyFor(type: CooldownType) = longPreferencesKey("momentCooldown.${type.name}")
    private val KEY_WOW = booleanPreferencesKey("momentCooldown.wowShown")

    suspend fun isOnCooldown(type: CooldownType, nowEpoch: Long): Boolean {
        val cd = type.seconds
        if (cd < 0) {
            return ServiceLocator.appContext.preferencesStore.data.first()[KEY_WOW] == true
        }
        if (cd == 0L) return false
        val last = ServiceLocator.appContext.preferencesStore.data.first()[keyFor(type)] ?: return false
        return (nowEpoch - last) < cd
    }

    suspend fun recordShown(type: CooldownType, nowEpoch: Long) {
        val ctx = ServiceLocator.appContext
        if (type.seconds < 0L) {
            ctx.preferencesStore.edit { it[KEY_WOW] = true }
        } else {
            ctx.preferencesStore.edit { it[keyFor(type)] = nowEpoch }
        }
    }

    suspend fun resetAll() {
        ServiceLocator.appContext.preferencesStore.edit {
            it.clear()
        }
    }
}

package ro.solomon.app.services

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ro.solomon.app.di.ServiceLocator
import ro.solomon.app.di.preferencesStore

object LastMissionStore {
    private val KEY_ACTIVE = stringPreferencesKey("mission_active_json")
    private val KEY_PENDING = stringPreferencesKey("mission_pending_json")

    suspend fun saveActive(ctx: Context, mission: SolomonMission?) {
        ctx.preferencesStore.edit {
            if (mission == null) it.remove(KEY_ACTIVE) else it[KEY_ACTIVE] = ServiceLocator.json.encodeToString(mission)
        }
    }

    suspend fun readActive(): SolomonMission? {
        return try {
            val v = ServiceLocator.appContext.preferencesStore.data.first()[KEY_ACTIVE] ?: return null
            ServiceLocator.json.decodeFromString<SolomonMission>(v)
        } catch (_: Throwable) { null }
    }

    suspend fun savePending(ctx: Context, mission: SolomonMission?) {
        ctx.preferencesStore.edit {
            if (mission == null) it.remove(KEY_PENDING) else it[KEY_PENDING] = ServiceLocator.json.encodeToString(mission)
        }
    }

    suspend fun readPending(): SolomonMission? {
        return try {
            val v = ServiceLocator.appContext.preferencesStore.data.first()[KEY_PENDING] ?: return null
            ServiceLocator.json.decodeFromString<SolomonMission>(v)
        } catch (_: Throwable) { null }
    }
}

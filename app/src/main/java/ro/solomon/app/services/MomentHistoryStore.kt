package ro.solomon.app.services

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import ro.solomon.app.di.preferencesStore

@Serializable
data class MomentEntry(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val generatedAt: Long,
    val dismissed: Boolean = false
)

object MomentHistoryStore {
    private val KEY_LIST = stringPreferencesKey("moment_history_v1")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }
    private val listSerializer = ListSerializer(MomentEntry.serializer())
    private const val MAX_KEEP = 100

    suspend fun append(ctx: Context, type: String, title: String, body: String, nowEpochSeconds: Long) {
        val entry = MomentEntry(
            id = "m_" + nowEpochSeconds.toString() + "_" + (0..0xFFFF).random().toString(16),
            type = type,
            title = title,
            body = body,
            generatedAt = nowEpochSeconds
        )
        ctx.preferencesStore.edit { prefs ->
            val raw = prefs[KEY_LIST]
            val current: List<MomentEntry> = if (raw != null) {
                runCatching { json.decodeFromString(listSerializer, raw) }.getOrNull().orEmpty()
            } else emptyList()
            val next = (listOf(entry) + current).take(MAX_KEEP)
            prefs[KEY_LIST] = json.encodeToString(listSerializer, next)
        }
    }

    suspend fun list(ctx: Context): List<MomentEntry> {
        val raw = ctx.preferencesStore.data.first()[KEY_LIST] ?: return emptyList()
        return runCatching { json.decodeFromString(listSerializer, raw) }.getOrNull().orEmpty()
    }

    suspend fun dismiss(ctx: Context, id: String) {
        ctx.preferencesStore.edit { prefs ->
            val raw = prefs[KEY_LIST] ?: return@edit
            val current = runCatching { json.decodeFromString(listSerializer, raw) }.getOrNull().orEmpty()
            val updated = current.map { if (it.id == id) it.copy(dismissed = true) else it }
            prefs[KEY_LIST] = json.encodeToString(listSerializer, updated)
        }
    }

    suspend fun clearAll(ctx: Context) {
        ctx.preferencesStore.edit { it.remove(KEY_LIST) }
    }
}

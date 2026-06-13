package ro.solomon.app.services

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ro.solomon.app.di.preferencesStore

object LastMomentStore {
    private val KEY_TEXT = stringPreferencesKey("last_moment_text")
    private val KEY_TYPE = stringPreferencesKey("last_moment_type")
    private val KEY_TS = stringPreferencesKey("last_moment_ts")

    suspend fun save(ctx: Context, text: String, type: String) {
        ctx.preferencesStore.edit {
            it[KEY_TEXT] = text
            it[KEY_TYPE] = type
            it[KEY_TS] = System.currentTimeMillis().toString()
        }
    }

    suspend fun read(ctx: Context): Triple<String?, String?, Long> {
        val prefs = ctx.preferencesStore.data.first()
        val t = prefs[KEY_TEXT]
        val ty = prefs[KEY_TYPE]
        val ts = prefs[KEY_TS]?.toLongOrNull() ?: 0L
        return Triple(t, ty, ts)
    }
}

package ro.solomon.app.services

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import ro.solomon.app.di.preferencesStore

object LastForecastStore {
    private val KEY_TIP = stringPreferencesKey("last_forecast_tip")
    private val KEY_RISK = stringPreferencesKey("last_forecast_risk")

    suspend fun save(ctx: Context, tip: String, risk: String) {
        ctx.preferencesStore.edit {
            it[KEY_TIP] = tip
            it[KEY_RISK] = risk
        }
    }

    suspend fun read(ctx: Context): Pair<String?, String?> {
        val prefs = ctx.preferencesStore.data.first()
        return prefs[KEY_TIP] to prefs[KEY_RISK]
    }
}

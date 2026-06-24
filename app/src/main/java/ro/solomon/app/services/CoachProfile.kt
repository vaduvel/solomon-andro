package ro.solomon.app.services

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import ro.solomon.app.di.preferencesStore

/**
 * Profilul de coaching al userului - memoria reala a coach-ului.
 *
 * Inlocuieste memoria veche de UN SINGUR string (SolomonCoachMemory) cu un profil structurat:
 * money script, vulnerabilitatea principala, obiectivul curent, ultimul angajament luat si un
 * feedback loop (de cate ori a actionat vs a ignorat nudge-urile). Feedback loop-ul e ceea ce da
 * coach-ului "experienta": invata ce functioneaza pentru tipul asta de om.
 */
data class CoachProfile(
    val moneyScript: MoneyScript? = null,
    val vulnerability: SolomonCoachVulnerability? = null,
    val primaryGoalName: String? = null,
    val lastCommitment: String? = null,
    val nudgesActedOn: Int = 0,
    val nudgesIgnored: Int = 0,
) {
    /** Rata de actiune - semnal pentru cat de tare sa impinga coach-ul. */
    val engagementRatio: Double
        get() {
            val total = nudgesActedOn + nudgesIgnored
            return if (total == 0) 0.0 else nudgesActedOn.toDouble() / total
        }

    /** Avem destul istoric cat sa ne increatem in semnalul de engagement. */
    val hasEnoughHistory: Boolean
        get() = (nudgesActedOn + nudgesIgnored) >= 5
}

object CoachProfileStore {
    private val scriptKey = stringPreferencesKey("solomon.coach.moneyScript")
    private val vulnKey = stringPreferencesKey("solomon.coach.vulnerability")
    private val goalKey = stringPreferencesKey("solomon.coach.primaryGoal")
    private val commitmentKey = stringPreferencesKey("solomon.coach.lastCommitment")
    private val actedKey = intPreferencesKey("solomon.coach.nudgesActed")
    private val ignoredKey = intPreferencesKey("solomon.coach.nudgesIgnored")

    fun load(ctx: Context): CoachProfile = runBlocking {
        val prefs = ctx.preferencesStore.data.first()
        CoachProfile(
            moneyScript = MoneyScript.from(prefs[scriptKey]),
            vulnerability = SolomonCoachVulnerability.from(prefs[vulnKey]),
            primaryGoalName = prefs[goalKey],
            lastCommitment = prefs[commitmentKey],
            nudgesActedOn = prefs[actedKey] ?: 0,
            nudgesIgnored = prefs[ignoredKey] ?: 0,
        )
    }

    fun setMoneyScript(ctx: Context, script: MoneyScript) = runBlocking {
        ctx.preferencesStore.edit { it[scriptKey] = script.raw }
    }

    fun setVulnerability(ctx: Context, v: SolomonCoachVulnerability) = runBlocking {
        ctx.preferencesStore.edit { it[vulnKey] = v.raw }
    }

    fun setPrimaryGoal(ctx: Context, goalName: String?) = runBlocking {
        ctx.preferencesStore.edit {
            if (goalName.isNullOrBlank()) it.remove(goalKey) else it[goalKey] = goalName
        }
    }

    fun setLastCommitment(ctx: Context, commitment: String?) = runBlocking {
        ctx.preferencesStore.edit {
            if (commitment.isNullOrBlank()) it.remove(commitmentKey) else it[commitmentKey] = commitment
        }
    }

    /** Feedback loop: userul a dat curs unui nudge/angajament. */
    fun recordActedOn(ctx: Context) = runBlocking {
        ctx.preferencesStore.edit { it[actedKey] = (it[actedKey] ?: 0) + 1 }
    }

    /** Feedback loop: userul a ignorat/respins un nudge. */
    fun recordIgnored(ctx: Context) = runBlocking {
        ctx.preferencesStore.edit { it[ignoredKey] = (it[ignoredKey] ?: 0) + 1 }
    }
}

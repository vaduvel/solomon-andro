package ro.solomon.app.services

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import ro.solomon.app.di.preferencesStore

/**
 * Un angajament concret luat de user (ex: o misiune acceptat\u0103), cu momentul lu\u0103rii
 * \u0219i dac\u0103 a fost respectat. honored == null \u00eenseamn\u0103 \u00eenc\u0103 nerezolvat.
 */
data class CoachCommitment(
    val text: String,
    val takenAtMillis: Long,
    val honored: Boolean? = null
)

/**
 * Profilul de coaching al userului - memoria reala a coach-ului.
 *
 * Inlocuieste memoria veche de UN SINGUR string (SolomonCoachMemory) cu un profil structurat:
 * money script, vulnerabilitatea principala, obiectivul curent, ultimul angajament luat si un
 * feedback loop (de cate ori a actionat vs a ignorat nudge-urile). Feedback loop-ul e ceea ce da
 * coach-ului \"experienta\": invata ce functioneaza pentru tipul asta de om.
 */
data class CoachProfile(
    val moneyScript: MoneyScript? = null,
    val vulnerability: SolomonCoachVulnerability? = null,
    val primaryGoalName: String? = null,
    val lastCommitment: String? = null,
    val nudgesActedOn: Int = 0,
    val nudgesIgnored: Int = 0,
    val commitmentHistory: List<CoachCommitment> = emptyList(),
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

    /** Angajamentele deja rezolvate (respectate sau nu). */
    val resolvedCommitments: List<CoachCommitment>
        get() = commitmentHistory.filter { it.honored != null }

    /** C\u00E2te angajamente a luat userul \u00een total (istoric). */
    val commitmentCount: Int
        get() = commitmentHistory.size

    /** Rata de respectare a angajamentelor rezolvate (0..1). 0 c\u00E2nd nu exist\u0103 \u00eenc\u0103 niciunul rezolvat. */
    val commitmentRespectRate: Double
        get() {
            val resolved = resolvedCommitments
            return if (resolved.isEmpty()) 0.0 else resolved.count { it.honored == true }.toDouble() / resolved.size
        }
}

object CoachProfileStore {
    private val scriptKey = stringPreferencesKey("solomon.coach.moneyScript")
    private val vulnKey = stringPreferencesKey("solomon.coach.vulnerability")
    private val goalKey = stringPreferencesKey("solomon.coach.primaryGoal")
    private val commitmentKey = stringPreferencesKey("solomon.coach.lastCommitment")
    private val historyKey = stringPreferencesKey("solomon.coach.commitmentHistory")
    private val actedKey = intPreferencesKey("solomon.coach.nudgesActed")
    private val ignoredKey = intPreferencesKey("solomon.coach.nudgesIgnored")

    private const val MAX_HISTORY = 30

    fun load(ctx: Context): CoachProfile = runBlocking {
        val prefs = ctx.preferencesStore.data.first()
        CoachProfile(
            moneyScript = MoneyScript.from(prefs[scriptKey]),
            vulnerability = SolomonCoachVulnerability.from(prefs[vulnKey]),
            primaryGoalName = prefs[goalKey],
            lastCommitment = prefs[commitmentKey],
            nudgesActedOn = prefs[actedKey] ?: 0,
            nudgesIgnored = prefs[ignoredKey] ?: 0,
            commitmentHistory = decodeHistory(prefs[historyKey]),
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

    /**
     * Seteaz\u0103 ultimul angajament. P\u0103streaz\u0103 API-ul vechi, dar acum orice angajament
     * non-gol e \u0219i \u00eenregistrat \u00een istoric (vezi [addCommitment]).
     */
    fun setLastCommitment(ctx: Context, commitment: String?) {
        if (commitment.isNullOrBlank()) {
            runBlocking { ctx.preferencesStore.edit { it.remove(commitmentKey) } }
        } else {
            addCommitment(ctx, commitment)
        }
    }

    /** \u00CEnregistreaz\u0103 un angajament nou \u00een istoric (nerezolvat) \u0219i \u00eel marcheaz\u0103 ca ultimul. */
    fun addCommitment(ctx: Context, text: String) = runBlocking {
        if (text.isBlank()) return@runBlocking
        ctx.preferencesStore.edit { prefs ->
            val current = decodeHistory(prefs[historyKey]).toMutableList()
            current.add(
                CoachCommitment(
                    text = text,
                    takenAtMillis = System.currentTimeMillis(),
                    honored = null
                )
            )
            val capped = if (current.size > MAX_HISTORY) current.takeLast(MAX_HISTORY) else current
            prefs[historyKey] = encodeHistory(capped)
            prefs[commitmentKey] = text
        }
    }

    /**
     * Rezolv\u0103 cel mai recent angajament nerezolvat (sau ultimul, dac\u0103 toate sunt deja
     * rezolvate): respectat sau nu. Alimenteaz\u0103 [CoachProfile.commitmentRespectRate].
     */
    fun resolveLastCommitment(ctx: Context, honored: Boolean) = runBlocking {
        ctx.preferencesStore.edit { prefs ->
            val current = decodeHistory(prefs[historyKey]).toMutableList()
            if (current.isEmpty()) return@edit
            val idx = current.indexOfLast { it.honored == null }.let { if (it >= 0) it else current.lastIndex }
            current[idx] = current[idx].copy(honored = honored)
            prefs[historyKey] = encodeHistory(current)
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

    private fun encodeHistory(list: List<CoachCommitment>): String {
        val arr = JSONArray()
        for (c in list) {
            val o = JSONObject()
            o.put("t", c.text)
            o.put("at", c.takenAtMillis)
            if (c.honored == null) o.put("h", JSONObject.NULL) else o.put("h", c.honored)
            arr.put(o)
        }
        return arr.toString()
    }

    private fun decodeHistory(raw: String?): List<CoachCommitment> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                CoachCommitment(
                    text = o.optString("t", ""),
                    takenAtMillis = o.optLong("at", 0L),
                    honored = if (o.isNull("h")) null else o.optBoolean("h")
                )
            }
        } catch (e: Throwable) {
            emptyList()
        }
    }
}

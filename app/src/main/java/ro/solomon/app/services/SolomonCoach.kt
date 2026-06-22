package ro.solomon.app.services

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import ro.solomon.app.di.preferencesStore
import ro.solomon.core.domain.TransactionCategory

enum class SolomonCoachVulnerability(val raw: String) {
    SMALL_RECURRING("small_recurring"),
    HEAVY_OBLIGATIONS("heavy_obligations"),
    IRREGULAR_INCOME("irregular_income"),
    GOALS_WITHOUT_CONTRIBUTION("goals_without_contribution"),
    CASHFLOW_PRESSURE("cashflow_pressure");

    val title: String
        get() = when (this) {
            SMALL_RECURRING -> "cheltuieli mici repetate"
            HEAVY_OBLIGATIONS -> "obligații prea mari"
            IRREGULAR_INCOME -> "venit neregulat sau neclar"
            GOALS_WITHOUT_CONTRIBUTION -> "obiective fără contribuție"
            CASHFLOW_PRESSURE -> "cashflow tensionat"
        }

    val lesson: String
        get() = when (this) {
            SMALL_RECURRING -> "Vulnerabilitatea nu este achiziția mare, ci frecvența mică. 25 RON repetați des creează presiune fără să pară o decizie importantă."
            HEAVY_OBLIGATIONS -> "Obligațiile fixe îți mănâncă marja personală înainte să înceapă luna. Când ele trec de zona confortabilă, libertatea zilnică scade."
            IRREGULAR_INCOME -> "Venitul neregulat cere reguli mai simple, nu mai mult optimism. Ai nevoie de tampon, praguri și decizii luate pe media prudentă."
            GOALS_WITHOUT_CONTRIBUTION -> "Un obiectiv fără contribuție lunară rămâne o dorință. Ritmul mic, repetat, bate intenția mare amânată."
            CASHFLOW_PRESSURE -> "Cashflow-ul tensionat nu înseamnă neapărat venit mic. Înseamnă că banii intră și ies într-un ritm care lasă prea puțin spațiu de respirație."
        }

    val action: String
        get() = when (this) {
            SMALL_RECURRING -> "Azi pune o limită de 70 RON pentru cumpărături mici și notează manual orice tranzacție sub 50 RON."
            HEAVY_OBLIGATIONS -> "Azi listează primele 3 obligații fixe și marchează una pe care o poți renegocia, reduce sau anula luna asta."
            IRREGULAR_INCOME -> "Azi setează regula de bază: toate deciziile pornesc de la venitul prudent, nu de la luna cea mai bună."
            GOALS_WITHOUT_CONTRIBUTION -> "Azi transferă o sumă mică spre obiectiv sau setează un reminder lunar pentru contribuția minimă."
            CASHFLOW_PRESSURE -> "Azi amână 24 de ore orice cumpărătură neesențială și protejează marja personală rămasă."
        }

    val contextualTip: String
        get() = when (this) {
            SMALL_RECURRING -> "Cheltuielile mici sunt cele mai greu de simțit și cele mai ușor de repetat. Claritatea apare când le pui o limită zilnică, nu când promiți vag că vei fi atent."
            HEAVY_OBLIGATIONS -> "Obligațiile fixe trebuie privite ca presiune lunară înainte de orice buget de dorințe. Marja personală începe după ele, nu înainte."
            IRREGULAR_INCOME -> "Venitul neregulat se administrează cu un venit prudent de referință. Lunile bune construiesc tamponul; lunile slabe îl folosesc."
            GOALS_WITHOUT_CONTRIBUTION -> "Obiectivele au nevoie de ritm, nu doar de intenție. O contribuție mică recurentă creează mai mult progres decât o decizie mare amânată."
            CASHFLOW_PRESSURE -> "Cashflow-ul sănătos înseamnă spațiu de respirație între bani intrați și bani ieșiți. Când spațiul se îngustează, deciziile mici devin importante."
        }

    companion object {
        fun from(raw: String?): SolomonCoachVulnerability? {
            if (raw == null) return null
            val normalized = raw.lowercase()
                .replace("ă", "a").replace("â", "a").replace("î", "i")
                .replace("ș", "s").replace("ț", "t").replace(" ", "_")
            return entries.firstOrNull { v ->
                normalized == v.raw || normalized.contains(v.raw) ||
                normalized.contains(v.title.replace(" ", "_"))
            }
        }
    }
}

object SolomonCoachMemory {
    private val vulnerabilityKey = stringPreferencesKey("solomon.coach.vulnerability")

    fun vulnerability(ctx: Context): SolomonCoachVulnerability? {
        val raw = runBlocking { ctx.preferencesStore.data.first()[vulnerabilityKey] }
        return SolomonCoachVulnerability.from(raw)
    }

    val label: String get() = "încă neclară"

    fun save(ctx: Context, vulnerability: SolomonCoachVulnerability) {
        runBlocking {
            ctx.preferencesStore.edit { it[vulnerabilityKey] = vulnerability.raw }
        }
    }
}

object TrueCostComparator {
    private val references = listOf(
        200 to "o cină bună la restaurant",
        500 to "un abonament Netflix anual",
        900 to "un city-break de weekend în Europa",
        1500 to "un curs online premium cu certificare",
        2000 to "un city-break de 4 zile la Viena",
        3000 to "o vacanță de o săptămână în Grecia",
        5000 to "un iPhone 16 nou",
        6000 to "o lună și jumătate de chirie în Cluj",
        8000 to "un zbor dus-întors la New York cu hotel",
        10000 to "aproape un salariu mediu net în RO",
        15000 to "un avans parțial la o mașină",
        20000 to "o mașină second-hand de calitate"
    )

    fun compare(annualRON: Int, category: TransactionCategory): String? {
        if (annualRON < 200) return null
        val best = references.minByOrNull { kotlin.math.abs(it.first - annualRON) } ?: return null
        val prefix = when (category) {
            TransactionCategory.food_delivery -> "Livrările tale costă cât "
            TransactionCategory.food_dining -> "Mesele la restaurant costă cât "
            TransactionCategory.entertainment -> "Distracția costă cât "
            TransactionCategory.shopping_online, TransactionCategory.shopping_offline -> "Cumpărăturile costă cât "
            else -> "Echivalent anual: "
        }
        return "$prefix${best.second} pe an"
    }
}

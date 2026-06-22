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
            HEAVY_OBLIGATIONS -> "obliga\u021Bii prea mari"
            IRREGULAR_INCOME -> "venit neregulat sau neclar"
            GOALS_WITHOUT_CONTRIBUTION -> "obiective f\u0103r\u0103 contribu\u021Bie"
            CASHFLOW_PRESSURE -> "cashflow tensionat"
        }

    val lesson: String
        get() = when (this) {
            SMALL_RECURRING -> "Vulnerabilitatea nu este achizi\u021Bia mare, ci frecven\u021Ba mic\u0103. 25 RON repeta\u021Bi des creeaz\u0103 presiune f\u0103r\u0103 s\u0103 par\u0103 o decizie important\u0103."
            HEAVY_OBLIGATIONS -> "Obliga\u021Biile fixe \u00EE\u021Bi m\u00E2n\u00E2nc\u0103 marja personal\u0103 \u00EEnainte s\u0103 \u00EEnceap\u0103 luna. C\u00E2nd ele trec de zona confortabil\u0103, libertatea zilnic\u0103 scade."
            IRREGULAR_INCOME -> "Venitul neregulat cere reguli mai simple, nu mai mult optimism. Ai nevoie de tampon, praguri \u0219i decizii luate pe media prudent\u0103."
            GOALS_WITHOUT_CONTRIBUTION -> "Un obiectiv f\u0103r\u0103 contribu\u021Bie lunar\u0103 r\u0103m\u00E2ne o dorin\u021B\u0103. Ritmul mic, repetat, bate inten\u021Bia mare am\u00E2nat\u0103."
            CASHFLOW_PRESSURE -> "Cashflow-ul tensionat nu \u00EEnseamn\u0103 neap\u0103rat venit mic. \u00CEnseamn\u0103 c\u0103 banii intr\u0103 \u0219i ies \u00EEntr-un ritm care las\u0103 prea pu\u021Bin spa\u021Biu de respira\u021Bie."
        }

    val action: String
        get() = when (this) {
            SMALL_RECURRING -> "Azi pune o limit\u0103 de 70 RON pentru cump\u0103r\u0103turi mici \u0219i noteaz\u0103 manual orice tranzac\u021Bie sub 50 RON."
            HEAVY_OBLIGATIONS -> "Azi listeaz\u0103 primele 3 obliga\u021Bii fixe \u0219i marcheaz\u0103 una pe care o po\u021Bi renegocia, reduce sau anula luna asta."
            IRREGULAR_INCOME -> "Azi seteaz\u0103 regula de baz\u0103: toate deciziile pornesc de la venitul prudent, nu de la luna cea mai bun\u0103."
            GOALS_WITHOUT_CONTRIBUTION -> "Azi transfer\u0103 o sum\u0103 mic\u0103 spre obiectiv sau seteaz\u0103 un reminder lunar pentru contribu\u021Bia minim\u0103."
            CASHFLOW_PRESSURE -> "Azi am\u00E2n\u0103 24 de ore orice cump\u0103r\u0103tur\u0103 neesen\u021Bial\u0103 \u0219i protejeaz\u0103 marja personal\u0103 r\u0103mas\u0103."
        }

    val contextualTip: String
        get() = when (this) {
            SMALL_RECURRING -> "Cheltuielile mici sunt cele mai greu de sim\u021Bit \u0219i cele mai u\u0219or de repetat. Claritatea apare c\u00E2nd le pui o limit\u0103 zilnic\u0103, nu c\u00E2nd promi\u021Bi vag c\u0103 vei fi atent."
            HEAVY_OBLIGATIONS -> "Obliga\u021Biile fixe trebuie privite ca presiune lunar\u0103 \u00EEnainte de orice buget de dorin\u021Be. Marja personal\u0103 \u00EEncepe dup\u0103 ele, nu \u00EEnainte."
            IRREGULAR_INCOME -> "Venitul neregulat se administreaz\u0103 cu un venit prudent de referin\u021B\u0103. Lunile bune construiesc tamponul; lunile slabe \u00EE folosesc."
            GOALS_WITHOUT_CONTRIBUTION -> "Obiectivele au nevoie de ritm, nu doar de inten\u021Bie. O contribu\u021Bie mic\u0103 recurent\u0103 creeaz\u0103 mai mult progres dec\u00E2t o decizie mare am\u00E2nat\u0103."
            CASHFLOW_PRESSURE -> "Cashflow-ul s\u0103n\u0103tos \u00EEnseamn\u0103 spa\u021Biu de respira\u021Bie \u00EEntre bani intra\u021Bi \u0219i bani ie\u0219i\u021Bi. C\u00E2nd spa\u021Biul se \u00EEngusteaz\u0103, deciziile mici devin importante."
        }

    companion object {
        fun from(raw: String?): SolomonCoachVulnerability? {
            if (raw == null) return null
            val normalized = raw.lowercase()
                .replace("\u0103", "a").replace("\u00E2", "a").replace("\u00EE", "i")
                .replace("\u0219", "s").replace("\u021B", "t").replace(" ", "_")
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

    val label: String get() = "\u00EEnc\u0103 neclar\u0103"

    fun save(ctx: Context, vulnerability: SolomonCoachVulnerability) {
        runBlocking {
            ctx.preferencesStore.edit { it[vulnerabilityKey] = vulnerability.raw }
        }
    }
}

object TrueCostComparator {
    private val references = listOf(
        200 to "o cin\u0103 bun\u0103 la restaurant",
        500 to "un abonament Netflix anual",
        900 to "un city-break de weekend \u00EEn Europa",
        1500 to "un curs online premium cu certificare",
        2000 to "un city-break de 4 zile la Viena",
        3000 to "o vacan\u021B\u0103 de o s\u0103pt\u0103m\u00E2n\u0103 \u00EEn Grecia",
        5000 to "un iPhone 16 nou",
        6000 to "o lun\u0103 \u0219i jum\u0103tate de chirie \u00EEn Cluj",
        8000 to "un zbor dus-\u00EEntors la New York cu hotel",
        10000 to "aproape un salariu mediu net \u00EEn RO",
        15000 to "un avans par\u021Bial la o ma\u0219in\u0103",
        20000 to "o ma\u0219in\u0103 second-hand de calitate"
    )

    fun compare(annualRON: Int, category: TransactionCategory): String? {
        if (annualRON < 200) return null
        val best = references.minByOrNull { kotlin.math.abs(it.first - annualRON) } ?: return null
        val prefix = when (category) {
            TransactionCategory.food_delivery -> "Livr\u0103rile tale cost\u0103 c\u00E2t "
            TransactionCategory.food_dining -> "Mesele la restaurant cost\u0103 c\u00E2t "
            TransactionCategory.entertainment -> "Distrac\u021Bia cost\u0103 c\u00E2t "
            TransactionCategory.shopping_online, TransactionCategory.shopping_offline -> "Cump\u0103r\u0103turile cost\u0103 c\u00E2t "
            else -> "Echivalent anual: "
        }
        return "$prefix${best.second} pe an"
    }
}

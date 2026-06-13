package ro.solomon.core.registries

import kotlinx.serialization.Serializable

@Serializable
enum class WebSourceCategory { official, comparator, news, education }

@Serializable
enum class WebTrustLevel : Comparable<WebTrustLevel> {
    low, medium, high
}

@Serializable
data class WebCachePolicy(val seconds: Int) {
    companion object {
        val oneHour = WebCachePolicy(60 * 60)
        val sixHours = WebCachePolicy(6 * 60 * 60)
        val twentyFourH = WebCachePolicy(24 * 60 * 60)
        val sevenDays = WebCachePolicy(7 * 24 * 60 * 60)
    }
}

@Serializable
data class WhitelistedDomain(
    val host: String,
    val displayName: String,
    val category: WebSourceCategory,
    val trustLevel: WebTrustLevel,
    val defaultCachePolicy: WebCachePolicy,
    val topicTags: List<String> = emptyList()
) {
    val id: String get() = host
}

object WebSearchWhitelist {

    val official: List<WhitelistedDomain> = listOf(
        WhitelistedDomain("bnr.ro", "Banca Națională a României", WebSourceCategory.official, WebTrustLevel.high, WebCachePolicy.sixHours, listOf("curs_valutar", "dobanzi_referinta")),
        WhitelistedDomain("anaf.gov.ro", "ANAF", WebSourceCategory.official, WebTrustLevel.high, WebCachePolicy.twentyFourH, listOf("impozite", "deduceri", "e_factura")),
        WhitelistedDomain("asf.ro", "ASF", WebSourceCategory.official, WebTrustLevel.high, WebCachePolicy.oneHour, listOf("scam_alerts", "investitii")),
        WhitelistedDomain("anpc.ro", "ANPC", WebSourceCategory.official, WebTrustLevel.high, WebCachePolicy.oneHour, listOf("scam_alerts", "drepturi_consumator")),
        WhitelistedDomain("ms.ro", "Ministerul Sănătății", WebSourceCategory.official, WebTrustLevel.high, WebCachePolicy.twentyFourH, listOf("deduceri_sanatate")),
        WhitelistedDomain("ec.europa.eu", "EURES", WebSourceCategory.official, WebTrustLevel.high, WebCachePolicy.twentyFourH, listOf("munca_ue")),
        WhitelistedDomain("csalb.ro", "CSALB", WebSourceCategory.official, WebTrustLevel.high, WebCachePolicy.twentyFourH, listOf("mediere_bancara")),
    )

    val comparators: List<WhitelistedDomain> = listOf(
        WhitelistedDomain("conso.ro", "Conso", WebSourceCategory.comparator, WebTrustLevel.medium, WebCachePolicy.twentyFourH, listOf("depozite", "credite", "carduri")),
        WhitelistedDomain("finzoom.ro", "FinZoom", WebSourceCategory.comparator, WebTrustLevel.medium, WebCachePolicy.twentyFourH, listOf("credite")),
        WhitelistedDomain("creditede.ro", "CreditedeRomânia", WebSourceCategory.comparator, WebTrustLevel.medium, WebCachePolicy.twentyFourH, listOf("credite")),
        WhitelistedDomain("cumparcasa.ro", "CumpărCasa", WebSourceCategory.comparator, WebTrustLevel.medium, WebCachePolicy.twentyFourH, listOf("imobiliar")),
        WhitelistedDomain("ratemyrate.ro", "Rate My Rate", WebSourceCategory.comparator, WebTrustLevel.medium, WebCachePolicy.twentyFourH, listOf("depozite")),
    )

    val news: List<WhitelistedDomain> = listOf(
        WhitelistedDomain("zf.ro", "Ziarul Financiar", WebSourceCategory.news, WebTrustLevel.medium, WebCachePolicy.sevenDays, listOf("stiri_economie")),
        WhitelistedDomain("profit.ro", "Profit.ro", WebSourceCategory.news, WebTrustLevel.medium, WebCachePolicy.sevenDays, listOf("stiri_economie")),
        WhitelistedDomain("economica.net", "Economica.net", WebSourceCategory.news, WebTrustLevel.medium, WebCachePolicy.sevenDays, listOf("stiri_economie")),
        WhitelistedDomain("bursa.ro", "Bursa", WebSourceCategory.news, WebTrustLevel.medium, WebCachePolicy.sevenDays, listOf("stiri_economie", "burse")),
        WhitelistedDomain("hotnews.ro", "HotNews Economie", WebSourceCategory.news, WebTrustLevel.medium, WebCachePolicy.sevenDays, listOf("stiri_economie")),
    )

    val education: List<WhitelistedDomain> = listOf(
        WhitelistedDomain("iancuguda.ro", "Iancu Guda", WebSourceCategory.education, WebTrustLevel.medium, WebCachePolicy.sevenDays, listOf("educatie")),
        WhitelistedDomain("moneymag.ro", "Money.ro", WebSourceCategory.education, WebTrustLevel.medium, WebCachePolicy.sevenDays, listOf("educatie")),
        WhitelistedDomain("finantepersonale.ro", "Finanțe Personale", WebSourceCategory.education, WebTrustLevel.medium, WebCachePolicy.sevenDays, listOf("educatie")),
        WhitelistedDomain("educatiefinanciara.ro", "Educație Financiară", WebSourceCategory.education, WebTrustLevel.medium, WebCachePolicy.sevenDays, listOf("educatie")),
    )

    val all: List<WhitelistedDomain> = official + comparators + news + education

    fun entryForHost(host: String): WhitelistedDomain? {
        val normalized = host.lowercase()
        return all.firstOrNull { it.host == normalized }
    }
}

package ro.solomon.web

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ro.solomon.core.registries.WebCachePolicy
import java.net.URL

@Serializable
enum class WebQueryType {
    @SerialName("currency_rate") currencyRate,
    @SerialName("interest_rate") interestRate,
    @SerialName("scam_alert") scamAlert,
    @SerialName("general_finance") generalFinance,
    @SerialName("price_comparison") priceComparison;

    val cacheTTLSeconds: Long
        get() = when (this) {
            currencyRate -> 6L * 3600
            scamAlert -> 3600
            priceComparison -> 6L * 3600
            interestRate, generalFinance -> 24L * 3600
        }
}

data class WebSearchQuery(
    val text: String,
    val queryType: WebQueryType,
    val locale: String = "ro-ro"
) {
    val cacheKey: String
        get() = "${queryType.name}:${text.lowercase().trim()}"

    val ddgURL: URL?
        get() = runCatching {
            val base = "https://api.duckduckgo.com/"
            val q = text.replace(" ", "+")
            URL("$base?q=$q&format=json&no_html=1&skip_disambig=1&kl=$locale")
        }.getOrNull()
}

data class WebSearchResult(
    val query: String,
    val queryType: WebQueryType,
    val answer: String? = null,
    val abstractText: String? = null,
    val sourceURL: String? = null,
    val relatedTopics: List<String> = emptyList(),
    val fetchedAtEpochSeconds: Long = System.currentTimeMillis() / 1000L,
    val isFromCache: Boolean = false
) {
    val hasContent: Boolean
        get() = !answer.isNullOrEmpty() || !abstractText.isNullOrEmpty()
}

@Serializable
data class DDGResponse(
    val answer: String? = null,
    @SerialName("AbstractText") val abstractText: String = "",
    @SerialName("AbstractURL") val abstractURL: String = "",
    @SerialName("RelatedTopics") val relatedTopics: List<DDGRelatedTopic> = emptyList()
)

@Serializable
data class DDGRelatedTopic(
    val text: String? = null,
    @SerialName("FirstURL") val firstURL: String? = null
)

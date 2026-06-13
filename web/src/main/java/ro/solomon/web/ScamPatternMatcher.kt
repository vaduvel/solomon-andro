package ro.solomon.web

import ro.solomon.core.registries.ScamPatterns
import ro.solomon.core.registries.ScamPattern

data class ScamMatchResult(
    val pattern: ScamPattern
) {
    val riskScore: Double
        get() = when (pattern.severity) {
            ro.solomon.core.registries.ScamSeverity.suspicious -> 0.4
            ro.solomon.core.registries.ScamSeverity.likely_scam -> 0.7
            ro.solomon.core.registries.ScamSeverity.definite_scam -> 1.0
        }
    val shouldAlert: Boolean get() = riskScore >= 0.7
}

class ScamPatternMatcher {

    fun match(text: String): ScamMatchResult? {
        val pattern = ScamPatterns.matchIn(text.normalizeForScam()) ?: return null
        return ScamMatchResult(pattern = pattern)
    }

    fun allMatches(text: String): List<ScamMatchResult> {
        val normalized = text.normalizeForScam()
        return ScamPatterns.all
            .filter { p -> p.keywords.any { normalized.contains(it.normalizeForScam()) } }
            .map { ScamMatchResult(pattern = it) }
            .sortedByDescending { it.riskScore }
    }

    fun matchEmail(subject: String, body: String): ScamMatchResult? =
        match("$subject $body")

    fun matchURL(urlString: String): ScamMatchResult? {
        val parsed = runCatching { java.net.URL(urlString) }.getOrNull() ?: return null
        val text = listOfNotNull(parsed.host, parsed.path, parsed.query).joinToString(" ")
        return match(text)
    }

    fun patternsFor(category: ro.solomon.core.registries.ScamCategory): List<ScamPattern> =
        ScamPatterns.patternsIn(category)

    fun hasAnyRisk(text: String): Boolean = match(text) != null

    fun isDefiniteScam(text: String): Boolean =
        match(text)?.pattern?.severity == ro.solomon.core.registries.ScamSeverity.definite_scam
}

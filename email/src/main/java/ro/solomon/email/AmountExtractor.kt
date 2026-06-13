package ro.solomon.email

import java.text.Normalizer

class AmountExtractor {

    private val amountRegex = Regex(
        """(?<![,.\d])(\d{1,3}(?:[.,]\d{3})*(?:[.,]\d{1,2})?|\d+(?:[.,]\d{1,2})?)\s*(?:RON|lei|EUR|€)""",
        RegexOption.IGNORE_CASE
    )

    private val labeledAmountRegex = Regex(
        """(total|suma|de plata|de plată|platit|plătit|valoare|cost|comanda|comandă|achitat|rambursare|transfer)[:\s]+(\d{1,3}(?:[.,]\d{3})*(?:[.,]\d{1,2})?|\d+(?:[.,]\d{1,2})?)\s*(?:RON|lei|EUR|€)""",
        RegexOption.IGNORE_CASE
    )

    fun extractAll(text: String): List<ExtractedAmount> {
        val results = amountRegex.findAll(text)
            .mapNotNull { match -> parseMatch(match.value) }
            .toList()
        val seen = mutableSetOf<Int>()
        return results
            .sortedByDescending { it.value }
            .filter { seen.add(it.value) }
    }

    fun extractPrimary(text: String): ExtractedAmount? = extractAll(text).firstOrNull()

    fun extractTransactionAmount(text: String): ExtractedAmount? {
        labeledAmountRegex.find(text)?.let { match ->
            val groups = match.groupValues
            if (groups.size >= 3) {
                val numberStr = groups[2]
                parseNumberString(numberStr, fullMatch = match.value)?.let { return it }
            }
        }
        return extractPrimary(text)
    }

    private fun parseMatch(fullMatch: String): ExtractedAmount? {
        val trimmed = fullMatch.trim()
        val upper = trimmed.uppercase()
        val currency = if (upper.contains("EUR") || trimmed.contains("€")) {
            AmountCurrency.EUR
        } else {
            AmountCurrency.RON
        }
        val numberPart = trimmed
            .replace(Regex("""RON""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""lei""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""EUR""", RegexOption.IGNORE_CASE), "")
            .replace("€", "")
            .trim()
        return parseNumberString(numberPart, currency, fullMatch)
    }

    private fun parseNumberString(
        raw: String,
        currency: AmountCurrency = AmountCurrency.RON,
        fullMatch: String = ""
    ): ExtractedAmount? {
        val cleaned = raw.trim()
        if (cleaned.isEmpty()) return null
        val value = parseRomanianNumber(cleaned)
        if (value <= 0) return null
        return ExtractedAmount(
            value = value,
            currency = currency,
            rawString = if (fullMatch.isEmpty()) raw else fullMatch
        )
    }

    internal fun parseRomanianNumber(s: String): Int {
        val hasDot = s.contains('.')
        val hasComma = s.contains(',')

        val normalized = when {
            hasDot && hasComma -> {
                val lastDot = s.lastIndexOf('.')
                val lastComma = s.lastIndexOf(',')
                if (lastComma > lastDot) {
                    s.replace(".", "").replace(",", ".")
                } else {
                    s.replace(",", "")
                }
            }
            hasComma -> {
                val parts = s.split(",")
                if (parts.size == 2 && parts[1].length == 3) {
                    s.replace(",", "")
                } else {
                    s.replace(",", ".")
                }
            }
            hasDot -> {
                val parts = s.split(".")
                if (parts.size == 2 && parts[1].length == 3) {
                    s.replace(".", "")
                } else s
            }
            else -> s
        }

        return normalized.toDoubleOrNull()?.let { it.toInt() } ?: 0
    }
}

internal fun String.stripDiacritics(): String {
    val nfd = Normalizer.normalize(this, Normalizer.Form.NFD)
    return nfd.replace(Regex("""\p{InCombiningDiacriticalMarks}+"""), "")
}

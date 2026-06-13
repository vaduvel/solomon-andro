package ro.solomon.email

import ro.solomon.core.domain.FlowDirection
import ro.solomon.core.domain.Money
import ro.solomon.core.domain.Transaction
import ro.solomon.core.domain.TransactionCategory
import ro.solomon.core.domain.TransactionSource
import ro.solomon.core.domain.deterministicUUID

data class ParsedSmsPayment(
    val amount: Int,
    val direction: FlowDirection,
    val merchant: String?,
    val sender: String,
    val raw: String,
    val receivedAtEpochSeconds: Long = System.currentTimeMillis() / 1000
) {
    fun toTransaction(): Transaction {
        val dedupeKey = "sms|$sender|$raw|${receivedAtEpochSeconds / 60L}"
        return Transaction(
            id = deterministicUUID(dedupeKey).toString(),
            date = receivedAtEpochSeconds,
            amount = Money(amount),
            direction = direction,
            category = TransactionCategory.unknown,
            merchant = merchant,
            description = raw.take(120),
            source = TransactionSource.sms_parsed,
            categorizationConfidence = 0.90
        )
    }
}

object SmsPaymentParser {

    private val bankSenders = setOf(
        "BTPay", "BT Pay", "BCR", "ING", "INGBank", "INGB", "BRD", "Raiffeisen", "RAIFFEISEN",
        "UniCredit", "CEC", "Alpha", "Revolut", "Wise", "PayPal", "PayU", "NETOPIA", "eMAG",
        "OLX", "Glovo", "Bolt", "Uber", "Wolt", "Tazz", "Foodpanda", "Starbucks", "Decathlon"
    )

    private val patterns: List<Regex> = listOf(
        Regex(
            """(?:ati\s+efectuat|ai\s+efectuat|tranzactie|tranzacție|plata\s+efectuata|plată\s+efectuată)[:\s]+(\d+(?:[.,]\d{1,2})?)\s*(?:RON|LEI|EUR|€)?""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """(?:cumparare|achizitie|achiziție|retragere|plata|plată)[:\s]+(\d+(?:[.,]\d{1,2})?)\s*(?:RON|LEI|EUR|€)?""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """(?:suma\s+de|val[oa]rea\s+de)[:\s]+(\d+(?:[.,]\d{1,2})?)\s*(?:RON|LEI|EUR|€)?""",
            RegexOption.IGNORE_CASE
        ),
        Regex(
            """(\d+(?:[.,]\d{1,2})?)\s*(?:RON|LEI|EUR|€)\s+(?:la|la comerciantul|la magazinul|la furnizorul|la ATM|ATM)""",
            RegexOption.IGNORE_CASE
        )
    )

    private val incomingPatterns: List<Regex> = listOf(
        Regex("""(?:ati\s+primit|ai\s+primit|incasat|încasat|creditare|salariu|transfer\s+primit)[:\s]+(\d+(?:[.,]\d{1,2})?)\s*(?:RON|LEI|EUR|€)?""", RegexOption.IGNORE_CASE),
        Regex("""(\d+(?:[.,]\d{1,2})?)\s*(?:RON|LEI|EUR|€)\s+(?:incasat|încasat|primit|creditat)""", RegexOption.IGNORE_CASE)
    )

    private val merchantPatterns: List<Regex> = listOf(
        Regex("""(?:la|la comerciantul|la magazinul|la furnizorul)\s+([A-ZĂÂÎȘȚ][\w\s\-&]{2,40})""", RegexOption.IGNORE_CASE),
        Regex("""(?:de la|din partea)\s+([A-ZĂÂÎȘȚ][\w\s\-&]{2,40})""", RegexOption.IGNORE_CASE),
        Regex("""comerciant[:\s]+([A-ZĂÂÎȘȚ][\w\s\-&]{2,40})""", RegexOption.IGNORE_CASE),
        Regex("""merchant[:\s]+([\w\s\-&]{2,40})""", RegexOption.IGNORE_CASE)
    )

    private val incomingKeywords = listOf("primit", "încasat", "incasat", "salariu", "creditare", "creditat", "depus", "incasare", "încasare", "intrare", "received")
    private val outgoingKeywords = listOf("plata", "plată", "platit", "plătit", "achitat", "achizitie", "achiziție", "retragere", "cumparare", "cumpărare", "tranzactie", "tranzacție", "debit", "pos", "card", "efectuat", "efectuata", "efectuată")

    fun isBankSms(sender: String): Boolean {
        val s = sender.uppercase().trim()
        if (s.isBlank()) return false
        return bankSenders.any { s.contains(it.uppercase()) } || s.startsWith("INFO") || s.startsWith("SMS") || s.startsWith("NOTIF")
    }

    fun parse(sender: String, body: String, nowEpochSeconds: Long = System.currentTimeMillis() / 1000): ParsedSmsPayment? {
        if (body.isBlank()) return null
        val incoming = incomingPatterns.firstNotNullOfOrNull { it.find(body)?.groupValues?.get(1)?.toAmount() }
        if (incoming != null) {
            val merchant = extractMerchant(body)
            return ParsedSmsPayment(incoming, FlowDirection.incoming, merchant, sender, body, nowEpochSeconds)
        }
        val outgoing = patterns.firstNotNullOfOrNull { it.find(body)?.groupValues?.get(1)?.toAmount() }
        if (outgoing != null) {
            val merchant = extractMerchant(body)
            return ParsedSmsPayment(outgoing, FlowDirection.outgoing, merchant, sender, body, nowEpochSeconds)
        }
        val dir = detectDirection(body)
        val amount = fallbackAmount(body) ?: return null
        val merchant = extractMerchant(body)
        return ParsedSmsPayment(amount, dir, merchant, sender, body, nowEpochSeconds)
    }

    private fun fallbackAmount(text: String): Int? {
        val m = Regex("""(\d+(?:[.,]\d{1,2})?)\s*(?:RON|LEI|EUR|€)""", RegexOption.IGNORE_CASE).find(text) ?: return null
        return m.groupValues[1].toAmount()
    }

    private fun detectDirection(text: String): FlowDirection {
        val low = text.lowercase()
        return when {
            incomingKeywords.any { low.contains(it) } -> FlowDirection.incoming
            outgoingKeywords.any { low.contains(it) } -> FlowDirection.outgoing
            else -> FlowDirection.outgoing
        }
    }

    private fun extractMerchant(text: String): String? {
        for (p in merchantPatterns) {
            val m = p.find(text) ?: continue
            val s = m.groupValues[1].trim().split(Regex("""[.,;!?\n\r]""")).first().trim()
            if (s.length in 2..40) return s
        }
        return null
    }

    private fun String.toAmount(): Int? {
        val normalized = this.replace(".", "").replace(",", ".").trim()
        val v = normalized.toDoubleOrNull() ?: return null
        return v.toInt()
    }
}

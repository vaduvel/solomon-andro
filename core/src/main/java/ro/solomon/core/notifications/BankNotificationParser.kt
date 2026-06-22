package ro.solomon.core.notifications

import ro.solomon.core.domain.Bank
import ro.solomon.core.domain.FlowDirection
import ro.solomon.core.domain.Money
import ro.solomon.core.domain.Transaction
import ro.solomon.core.domain.TransactionCategory
import ro.solomon.core.domain.TransactionSource
import ro.solomon.core.domain.deterministicUUID
import ro.solomon.core.registries.IFNDatabase
import ro.solomon.core.registries.IFNRecord
import java.math.BigDecimal
import java.util.regex.Pattern

object BankNotificationParser {

    val knownAppPackages: Set<String> = setOf(
        "com.google.android.apps.walletnfcrel",
        "ro.btrl.bt24",
        "ro.ing.ingro",
        "ro.bcr.george",
        "ro.raiffeisen.raiffeisenbank",
        "com.revolut.revolut",
        "ro.brd.brdgo",
        "ro.cec.cecbank",
        "ro.alphabank.alphabank",
        "ro.garanti.garantibank",
        "ro.libra.librabank",
        "ro.unicredit.unicreditbank",
        "ro.saltedge.saltedge",
    )

    enum class DecimalFormatHint { auto, european, us }

    private val amountRegex = Regex(
        """(\d{1,3}(?:[.,]\d{3})*(?:[.,]\d{1,2})?|\d+(?:[.,]\d{1,2})?)\s*(RON|EUR|USD|GBP|CHF|HUF)""",
        RegexOption.IGNORE_CASE
    )

    private val dateSuffixes: List<Regex> = listOf(
        Regex("""\s+RO-\d+.*$""", RegexOption.IGNORE_CASE),
        Regex("""\s+(din|în|in)\s+contul.*$""", RegexOption.IGNORE_CASE),
        Regex("""\s+sold[:\s].*$""", RegexOption.IGNORE_CASE),
        Regex("""\s+\d{2}[./]\d{2}[./]\d{4}.*$"""),
        Regex("""\s+\d{2}[-]\d{2}[-]\d{4}.*$"""),
        Regex("""\s+\d{2}[./]\d{2}[./]\d{2}.*$"""),
        Regex("""\s+\d{2}:\d{2}.*$"""),
        Regex("""\s+la ora.*$""", RegexOption.IGNORE_CASE),
    )

    private val refPattern = Regex("""\s+[A-Z0-9]{8,}$""")

    private val bankKeywords = listOf(
        "plată", "plata", "platit", "plătit", "plătit",
        "tranzacție", "tranzactie", "tranzactia", "debitare", "credit",
        "card", "transfer", "retragere", "retras",
        "ai plătit", "ai platit", "ai efectuat", "ai autorizat",
        "payment", "purchase",
        "homebank", "ing romania"
    )

    private val incomingKeywords = listOf(
        "salariu", "salary", "transfer primit", "primit",
        "alimentare cont", "depunere", "virament primit",
        "credit", "reîncărcare", "cashback", "refund",
        "rambursare"
    )

    private val outgoingKeywords = listOf(
        "plată", "platit", "plătit", "debitare", "retragere",
        "transfer trimis", "payment", "purchase", "cumparat"
    )

    private val bankAppLabels = listOf(
        "ing romania", "homebank", "bt pay", "banca transilvania",
        "raiffeisen", "bcr", "cec bank", "revolut", "alpha bank", "garanti"
    )

    fun decimalHintFor(bank: Bank): DecimalFormatHint = when (bank) {
        Bank.Revolut -> DecimalFormatHint.us
        else -> DecimalFormatHint.european
    }

    fun parse(
        raw: String,
        dateEpochSeconds: Long = System.currentTimeMillis() / 1000L,
        decimalHint: DecimalFormatHint = DecimalFormatHint.auto
    ): Transaction? {
        val normalized = raw
            .replace("\u00A0", " ")
            .trim()

        val extracted = extractAmountAndMerchant(normalized, decimalHint) ?: return null

        val merchantClean = extracted.merchant.orEmpty()
        val ifnRecord = IFNDatabase.all.firstOrNull {
            merchantClean.lowercase().contains(it.name.lowercase())
        }
        val category: TransactionCategory = if (ifnRecord != null) {
            TransactionCategory.loans_ifn
        } else {
            MerchantCategoryMatcher.categoryFor(merchantClean)
        }
        val direction = determineDirection(normalized)

        if (extracted.currency != "RON") return null

        val moneyAmount = Money.fromRON(extracted.amount.toDouble())

        val bucketEpoch = dateEpochSeconds / 60L
        val dedupeKey = "notif|$normalized|$bucketEpoch"
        val id = deterministicUUID(dedupeKey).toString()

        return Transaction(
            id = id,
            date = dateEpochSeconds,
            amount = moneyAmount,
            direction = direction,
            category = category,
            merchant = extracted.merchant?.let { cleanMerchant(it) },
            description = "[${extracted.currency}] ${normalized.take(180)}",
            source = TransactionSource.notification_parsed,
            categorizationConfidence = if (category == TransactionCategory.unknown) 0.4 else 0.75
        )
    }

    fun looksLikeBankNotification(raw: String): Boolean {
        val lower = raw.lowercase()
        val hasMoney = amountRegex.containsMatchIn(raw)
        val hasKeyword = bankKeywords.any { lower.contains(it) }
        return hasMoney && hasKeyword
    }

    fun detectIFNRecord(text: String): IFNRecord? {
        val lower = text.lowercase()
        return IFNDatabase.all.firstOrNull { lower.contains(it.name.lowercase()) }
    }

    data class Extracted(
        val amount: BigDecimal,
        val currency: String,
        val merchant: String?
    )

    internal fun extractAmountAndMerchant(
        text: String,
        decimalHint: DecimalFormatHint = DecimalFormatHint.auto
    ): Extracted? {
        val match = amountRegex.find(text) ?: return null
        val (amountStr, currencyStr) = match.destructured
        val currency = currencyStr.uppercase()
        val amount = parseDecimal(amountStr, decimalHint) ?: return null

        val afterMatch = text.substring(match.range.last + 1).trim()
        val merchant = extractMerchant(afterMatch, text)
        return Extracted(amount, currency, merchant)
    }

    internal fun extractMerchant(afterAmount: String, fullText: String): String? {
        val prefixes = listOf(
            "la ", "lui ", "at ", "la: ", "- ", "@ ",
            "catre ", "către ", "beneficiar: "
        )

        var source = afterAmount
        for (prefix in prefixes) {
            if (source.lowercase().startsWith(prefix)) {
                source = source.drop(prefix.length).trim()
                break
            }
        }

        var result = source
        for (dp in dateSuffixes) {
            result = result.replace(dp, "").trim()
        }
        result = result.replace(refPattern, "").trim()

        if (result.isNotEmpty()) return result

        return extractMerchantBeforeAmount(fullText)
    }

    private fun extractMerchantBeforeAmount(text: String): String? {
        val match = amountRegex.find(text) ?: return null
        val prefix = text.substring(0, match.range.first)
        val cleanup = """[\s\n:•-]+""".toRegex()
        val candidates = prefix
            .split('\n')
            .map { it.replace(cleanup, " ").trim() }
            .filter { it.isNotEmpty() }
            .filter { !isBankAppLabel(it) }
        return candidates.lastOrNull()
    }

    private fun isBankAppLabel(value: String): Boolean {
        val lower = value.lowercase()
        return bankAppLabels.any { lower == it || lower.startsWith("$it:") }
    }

    internal fun determineDirection(text: String): FlowDirection {
        val lower = text.lowercase()
        val hasIncoming = incomingKeywords.any { lower.contains(it) }
        val hasOutgoing = outgoingKeywords.any { lower.contains(it) }

        if (lower.contains("retragere") || lower.contains("atm")) return FlowDirection.outgoing
        if (hasIncoming && !hasOutgoing) return FlowDirection.incoming
        return FlowDirection.outgoing
    }

    internal fun parseDecimal(s: String, hint: DecimalFormatHint = DecimalFormatHint.auto): BigDecimal? {
        val cleaned = s.trim()

        val normalized = when (hint) {
            DecimalFormatHint.european -> cleaned.replace(".", "").replace(",", ".")
            DecimalFormatHint.us -> cleaned.replace(",", "")
            DecimalFormatHint.auto -> {
                val lastComma = cleaned.lastIndexOf(',')
                val lastDot = cleaned.lastIndexOf('.')
                when {
                    lastComma == -1 && lastDot == -1 -> cleaned
                    lastDot == -1 -> cleaned.replace(",", ".")
                    lastComma == -1 -> cleaned
                    lastComma > lastDot -> cleaned.replace(".", "").replace(",", ".")
                    else -> cleaned.replace(",", "")
                }
            }
        }

        return runCatching { BigDecimal(normalized) }.getOrNull()
    }

    internal fun cleanMerchant(raw: String): String {
        val trimmed = raw.trim()
        return if (trimmed == trimmed.uppercase() && trimmed.length > 3) {
            trimmed.lowercase().split(" ").joinToString(" ") {
                it.replaceFirstChar { c -> c.uppercase() }
            }
        } else {
            trimmed
        }
    }
}

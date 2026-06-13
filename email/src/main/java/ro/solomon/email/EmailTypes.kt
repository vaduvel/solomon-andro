package ro.solomon.email

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ro.solomon.core.domain.FlowDirection
import ro.solomon.core.domain.Money
import ro.solomon.core.domain.Transaction
import ro.solomon.core.domain.TransactionCategory
import ro.solomon.core.domain.TransactionSource
import ro.solomon.core.registries.EmailSender
import ro.solomon.core.registries.SenderMatchConfidence

data class EmailMessage(
    val from: String,
    val subject: String,
    val bodyText: String,
    val dateEpochSeconds: Long = System.currentTimeMillis() / 1000L
) {
    val senderDomain: String
        get() = from.substringAfter("@", "").lowercase()
}

@Serializable
enum class AmountCurrency { RON, EUR }

data class ExtractedAmount(
    val value: Int,
    val currency: AmountCurrency,
    val rawString: String
) {
    val moneyRON: Money?
        get() = if (currency == AmountCurrency.RON) Money(value) else null
}

@Serializable
enum class ConfidenceSource {
    @SerialName("sender_exact") senderExactMatch,
    @SerialName("sender_domain") senderDomainMatch,
    @SerialName("keyword_only") keywordMatch,
    @SerialName("no_match") noMatch
}

data class ParsedEmailTransaction(
    val from: String,
    val subject: String,
    val dateEpochSeconds: Long,
    val amount: ExtractedAmount?,
    val merchant: String?,
    val suggestedCategory: TransactionCategory,
    val direction: FlowDirection,
    val confidence: Double,
    val confidenceSource: ConfidenceSource
) {
    val isAutoImportReady: Boolean get() = confidence >= 0.80
    val requiresManualReview: Boolean get() = confidence < 0.50 || amount == null

    fun toTransaction(): Transaction? {
        val amt = amount ?: return null
        val ron = amt.moneyRON ?: return null
        return Transaction(
            id = "",
            date = dateEpochSeconds,
            amount = ron,
            direction = direction,
            category = suggestedCategory,
            merchant = merchant,
            source = TransactionSource.email_parsed,
            categorizationConfidence = confidence
        )
    }
}

data class SenderMatchResult(
    val sender: EmailSender,
    val confidence: SenderMatchConfidence
) {
    val confidenceScore: Double
        get() = when (confidence) {
            SenderMatchConfidence.exact -> 0.90
            SenderMatchConfidence.domain -> 0.70
            SenderMatchConfidence.keyword -> 0.35
        }
}

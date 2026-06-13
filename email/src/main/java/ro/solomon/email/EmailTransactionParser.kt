package ro.solomon.email

import ro.solomon.core.domain.FlowDirection
import ro.solomon.core.domain.TransactionCategory
import ro.solomon.core.registries.EmailSenderCategory

class EmailTransactionParser(
    private val senderMapper: SenderMapper = SenderMapper(),
    private val subjectClassifier: SubjectClassifier = SubjectClassifier(),
    private val amountExtractor: AmountExtractor = AmountExtractor()
) {

    fun parse(email: EmailMessage): ParsedEmailTransaction? {
        val senderResult = senderMapper.map(email.from)
        val isRelevantSubject = subjectClassifier.isFinanciallyRelevant(email.subject)
        val isRelevantBody = bodyContainsAmountPattern(email.bodyText)

        if (senderResult == null && !isRelevantSubject && !isRelevantBody) return null

        val amount = amountExtractor.extractTransactionAmount(email.bodyText)
            ?: amountExtractor.extractPrimary(email.subject)

        val merchant = senderResult?.sender?.displayName
        val category = resolveCategory(senderResult, email.subject)
        val direction = resolveDirection(senderResult, email.subject, category)

        val (confidence, confidenceSource) = computeConfidence(
            senderResult, isRelevantSubject, isRelevantBody, amount != null
        )

        return ParsedEmailTransaction(
            from = email.from,
            subject = email.subject,
            dateEpochSeconds = email.dateEpochSeconds,
            amount = amount,
            merchant = merchant,
            suggestedCategory = category,
            direction = direction,
            confidence = confidence,
            confidenceSource = confidenceSource
        )
    }

    private fun bodyContainsAmountPattern(body: String): Boolean =
        quickAmountPattern.containsMatchIn(body)

    private val quickAmountPattern = Regex("""\d+[,.]?\d*\s*(RON|lei|EUR|€)""", RegexOption.IGNORE_CASE)

    private fun resolveCategory(
        senderResult: SenderMatchResult?,
        subject: String
    ): TransactionCategory {
        senderResult?.sender?.let { sr ->
            if (sr.defaultTransactionCategory != TransactionCategory.unknown) {
                return sr.defaultTransactionCategory
            }
        }
        subjectClassifier.suggestCategory(subject)?.let { return it }
        return TransactionCategory.unknown
    }

    private fun resolveDirection(
        senderResult: SenderMatchResult?,
        subject: String,
        category: TransactionCategory
    ): FlowDirection {
        if (senderResult?.sender?.category == EmailSenderCategory.ifn) {
            val s = subject.lowercase()
            if (s.contains("credit") || s.contains("aprobare") || s.contains("virat")) {
                return FlowDirection.incoming
            }
        }
        subjectClassifier.inferDirection(subject)?.let { return it }
        if (category == TransactionCategory.bnpl ||
            category == TransactionCategory.loans_ifn ||
            category == TransactionCategory.loans_bank
        ) return FlowDirection.outgoing
        return FlowDirection.outgoing
    }

    private fun computeConfidence(
        senderResult: SenderMatchResult?,
        isRelevantSubject: Boolean,
        isRelevantBody: Boolean,
        hasAmount: Boolean
    ): Pair<Double, ConfidenceSource> {
        val base: Double
        val source: ConfidenceSource

        if (senderResult != null) {
            base = senderResult.confidenceScore
            source = if (senderResult.confidence == ro.solomon.core.registries.SenderMatchConfidence.exact) {
                ConfidenceSource.senderExactMatch
            } else {
                ConfidenceSource.senderDomainMatch
            }
        } else if (isRelevantSubject || isRelevantBody) {
            base = 0.35
            source = ConfidenceSource.keywordMatch
        } else {
            return 0.0 to ConfidenceSource.noMatch
        }

        var final = base
        if (isRelevantSubject) final = (final + 0.05).coerceAtMost(1.0)
        if (isRelevantBody) final = (final + 0.03).coerceAtMost(1.0)
        if (!hasAmount) final = (final - 0.20).coerceAtLeast(0.0)

        return final to source
    }
}

package ro.solomon.core.domain

import kotlinx.serialization.Serializable

@Serializable
enum class FlowDirection {
    incoming, outgoing
}

@Serializable
enum class TransactionSource {
    email_parsed, csv_import, manual_entry, derived_from_obligation,
    notification_parsed, apple_pay_shortcut, bank_connection, sms_parsed, share_intent_parsed
}

@Serializable
data class Transaction(
    val id: String,
    val date: Long,
    val amount: Money,
    val direction: FlowDirection,
    val category: TransactionCategory,
    val merchant: String? = null,
    val description: String? = null,
    val source: TransactionSource,
    val categorizationConfidence: Double = 1.0
) {
    val signedAmount: Money get() = when (direction) {
        FlowDirection.incoming -> amount
        FlowDirection.outgoing -> -amount
    }

    val isIncoming: Boolean get() = direction == FlowDirection.incoming
    val isOutgoing: Boolean get() = direction == FlowDirection.outgoing
}

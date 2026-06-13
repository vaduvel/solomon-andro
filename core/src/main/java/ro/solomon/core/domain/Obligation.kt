package ro.solomon.core.domain

import kotlinx.serialization.Serializable

@Serializable
enum class ObligationKind {
    rent_mortgage, utility, subscription, loan_bank, loan_ifn, bnpl, insurance, other;

    val displayNameRO: String get() = when (this) {
        rent_mortgage -> "Chirie / rată"
        utility -> "Utilitate"
        subscription -> "Abonament"
        loan_bank -> "Credit bancar"
        loan_ifn -> "Credit IFN"
        bnpl -> "BNPL"
        insurance -> "Asigurare"
        other -> "Alte plăți"
    }
}

@Serializable
enum class ObligationConfidence {
    declared, detected, estimated
}

@Serializable
data class Obligation(
    val id: String,
    val name: String,
    val amount: Money,
    val dayOfMonth: Int,
    val kind: ObligationKind,
    val confidence: ObligationConfidence,
    val since: Long? = null,
    val nextDueDate: Long? = null
) {
    val isDebt: Boolean get() = when (kind) {
        ObligationKind.loan_bank, ObligationKind.loan_ifn, ObligationKind.bnpl -> true
        else -> false
    }

    val isEssential: Boolean get() = when (kind) {
        ObligationKind.rent_mortgage, ObligationKind.utility, ObligationKind.insurance -> true
        else -> false
    }
}

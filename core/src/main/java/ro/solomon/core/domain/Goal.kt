package ro.solomon.core.domain

import kotlinx.serialization.Serializable

@Serializable
enum class GoalKind {
    vacation, car, house, emergency_fund, debt_payoff, custom;

    val displayNameRO: String get() = when (this) {
        vacation -> "Vacanță"
        car -> "Mașină"
        house -> "Casă"
        emergency_fund -> "Fond de urgență"
        debt_payoff -> "Achitare datorii"
        custom -> "Obiectiv propriu"
    }
}

@Serializable
enum class GoalFeasibility {
    easy, on_track, challenging_but_possible, unrealistic;

    val displayNameRO: String get() = when (this) {
        easy -> "ușor de atins"
        on_track -> "pe drumul cel bun"
        challenging_but_possible -> "provocator dar posibil"
        unrealistic -> "nerealist la ritmul curent"
    }
}

@Serializable
data class Goal(
    val id: String,
    val kind: GoalKind,
    val destination: String? = null,
    val amountTarget: Money,
    val amountSaved: Money,
    val deadline: Long
) {
    val progressFraction: Double
        get() = if (amountTarget.amount > 0)
            amountSaved.amount.toDouble() / amountTarget.amount else 0.0

    val amountRemaining: Money
        get() {
            val remaining = amountTarget - amountSaved
            return if (remaining.isNegative) Money(0) else remaining
        }

    val isReached: Boolean get() = amountSaved >= amountTarget
}

package ro.solomon.core.domain

import kotlinx.serialization.Serializable

/**
 * Whether a [Focus] target is achievable in the chosen timeframe given the
 * money left after protecting Necesar and the active Goals' monthly contributions.
 */
@Serializable
enum class FocusFeasibility {
    realist, strans, nerealist;

    val displayNameRO: String
        get() = when (this) {
            realist -> "realist"
            strans -> "strâns"
            nerealist -> "nerealist"
        }
}

/**
 * A short-term prioritization the user activates *on top of* budgeting and
 * long-term [Goal]s. Activating a Focus never pauses or removes Goals — their
 * monthly contributions keep running; the Focus works with what remains (mostly
 * the Moft pool). All amounts are in **bani** (see [Money]).
 */
@Serializable
data class Focus(
    val id: String,
    val type: FocusType,
    val title: String,
    /** Target amount for goal-like focuses (emergency_fund, early_repayment, event). */
    val targetAmount: Money = Money.zero,
    val savedAmount: Money = Money.zero,
    /** Optional hard deadline (epoch millis) for [FocusType.event]. */
    val deadline: Long? = null,
    /** For [FocusType.moft_detox]: how much of Moft to cut, 0..100. */
    val detoxPercent: Int? = null,
    /** Monthly contribution Solomon plans toward this focus (bani). */
    val plannedMonthlyContribution: Money = Money.zero,
    val isPrimary: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: Long
) {
    val amountRemaining: Money
        get() {
            val remaining = targetAmount - savedAmount
            return if (remaining.isNegative) Money.zero else remaining
        }

    val progressFraction: Double
        get() = if (type.hasMonetaryTarget && targetAmount.amount > 0)
            (savedAmount.amount.toDouble() / targetAmount.amount).coerceIn(0.0, 1.0)
        else 0.0

    val isReached: Boolean
        get() = type.hasMonetaryTarget && savedAmount >= targetAmount
}

package ro.solomon.analytics

import ro.solomon.core.domain.Money

data class SafeToSpendBudget(
    val currentBalance: Money,
    val obligationsRemaining: Money,
    val availableAfterObligations: Money,
    val bufferRecommended: Money,
    val availableAfterBuffer: Money,
    val daysUntilNextPayday: Int,
    val availablePerDay: Money,
    val availablePerDayAfterBuffer: Money,
    val velocityRONPerDay: Money,
    val daysUntilCritical: Int? = null,
    val monthlyIncomeReference: Money? = null
) {
    val isTight: Boolean get() {
        // Thresholds in bani. Floor of 20 lei (2_000 bani), or 1% of monthly income.
        val threshold = if (monthlyIncomeReference != null && monthlyIncomeReference.amount > 0)
            maxOf(2_000, monthlyIncomeReference.amount / 100)
        else 3_000
        if (availablePerDay.amount <= threshold) return true
        if (daysUntilCritical != null && daysUntilCritical <= 5) return true
        return false
    }

    fun verdictFor(amount: Money): Verdict {
        val projectedAvailable = availableAfterObligations - amount
        if (projectedAvailable.isNegative) return Verdict.No(CanIAffordVerdictReason.would_break_obligation)
        val projectedPerDay = if (daysUntilNextPayday > 0)
            Money(projectedAvailable.amount / daysUntilNextPayday) else projectedAvailable
        // 50 lei/day = 5_000 bani.
        if (projectedPerDay.amount < 5_000) return Verdict.YesWithCaution(CanIAffordVerdictReason.tight_but_workable, projectedPerDay)
        return Verdict.Yes(projectedPerDay)
    }
}

sealed class Verdict {
    data class Yes(val projectedPerDay: Money) : Verdict()
    data class YesWithCaution(val reason: CanIAffordVerdictReason, val projectedPerDay: Money) : Verdict()
    data class No(val reason: CanIAffordVerdictReason) : Verdict()

    val isAffordable: Boolean get() = this is Yes || this is YesWithCaution

    val asContextVerdict: CanIAffordVerdict get() = when (this) {
        is Yes -> CanIAffordVerdict.yes
        is YesWithCaution -> CanIAffordVerdict.yes_with_caution
        is No -> CanIAffordVerdict.no
    }
}

enum class CanIAffordVerdict { yes, yes_with_caution, no }

enum class CanIAffordVerdictReason { would_break_obligation, tight_but_workable }

class SafeToSpendCalculator {

    fun calculate(
        currentBalance: Money,
        obligationsRemaining: Money,
        daysUntilNextPayday: Int,
        velocityRONPerDay: Money = Money(0),
        monthlyIncomeReference: Money? = null
    ): SafeToSpendBudget {
        val safeDays = maxOf(daysUntilNextPayday, 1)
        val afterObligations = currentBalance - obligationsRemaining

        val bufferRaw = afterObligations.amount / 10
        // Minimum buffer of 50 lei = 5_000 bani.
        val buffer = Money(maxOf(bufferRaw, if (afterObligations.isPositive) 5_000 else 0))
        val afterBuffer = afterObligations - buffer

        val perDay = Money(maxOf(0, afterObligations.amount) / safeDays)
        val perDayAfterBuffer = Money(maxOf(0, afterBuffer.amount) / safeDays)

        val daysUntilCritical: Int? = when {
            velocityRONPerDay.amount > 0 && afterObligations.isPositive -> {
                val est = afterObligations.amount / velocityRONPerDay.amount
                if (est < daysUntilNextPayday) est else null
            }
            afterObligations.isNegative -> 0
            else -> null
        }

        return SafeToSpendBudget(
            currentBalance = currentBalance,
            obligationsRemaining = obligationsRemaining,
            availableAfterObligations = afterObligations,
            bufferRecommended = buffer,
            availableAfterBuffer = afterBuffer,
            daysUntilNextPayday = daysUntilNextPayday,
            availablePerDay = perDay,
            availablePerDayAfterBuffer = perDayAfterBuffer,
            velocityRONPerDay = velocityRONPerDay,
            daysUntilCritical = daysUntilCritical,
            monthlyIncomeReference = monthlyIncomeReference
        )
    }
}

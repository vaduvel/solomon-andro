package ro.solomon.analytics

import ro.solomon.core.domain.*
import ro.solomon.core.format.RomanianDateFormatter
import java.util.Calendar
import kotlin.math.ceil

data class GoalScenario(
    val id: String,
    val description: String,
    val monthlyContribution: Money,
    val monthsToReach: Int? = null,
    val willReach: Boolean
)

data class GoalProgressReport(
    val goal: Goal,
    val monthsRemaining: Int,
    val monthsRemainingAtCurrentPace: Int? = null,
    val monthlyRequired: Money,
    val monthlyCurrentSavingPace: Money,
    val feasibility: GoalFeasibility,
    val currentPaceWillReach: Boolean,
    val shortfallPerMonth: Money? = null,
    val scenarios: List<GoalScenario>
) {
    val progressFraction: Double get() = goal.progressFraction
    val amountRemaining: Money get() = goal.amountRemaining
}

class GoalProgress {

    fun evaluate(
        goal: Goal,
        monthlyCurrentSavingPace: Money,
        referenceDate: Long = System.currentTimeMillis(),
        calendar: Calendar = RomanianDateFormatter.gregorianROCalendar()
    ): GoalProgressReport {
        val cal = calendar.clone() as Calendar; cal.timeInMillis = referenceDate
        val deadlineCal = calendar.clone() as Calendar; deadlineCal.timeInMillis = goal.deadline
        val monthsDiff = (deadlineCal.get(Calendar.YEAR) - cal.get(Calendar.YEAR)) * 12 +
                (deadlineCal.get(Calendar.MONTH) - cal.get(Calendar.MONTH))
        val monthsRemaining = maxOf(0, monthsDiff)

        val amountRemaining = goal.amountRemaining
        val monthlyRequired = if (monthsRemaining > 0)
            Money(ceil(amountRemaining.amount.toDouble() / monthsRemaining.toDouble()).toInt())
        else amountRemaining

        val willReach = goal.isReached || (monthlyCurrentSavingPace.isPositive && monthlyCurrentSavingPace >= monthlyRequired)

        val monthsAtCurrentPace = when {
            goal.isReached -> 0
            !monthlyCurrentSavingPace.isPositive -> null
            else -> ceil(amountRemaining.amount.toDouble() / monthlyCurrentSavingPace.amount.toDouble()).toInt()
        }

        val shortfall = when {
            willReach -> null
            else -> {
                val diff = monthlyRequired - monthlyCurrentSavingPace
                if (diff.isPositive) diff else null
            }
        }

        val feasibility = classifyFeasibility(monthlyCurrentSavingPace, monthlyRequired)

        val scenarios = listOf(
            scenario("current_pace", "ritmul actual", monthlyCurrentSavingPace, amountRemaining, monthsRemaining),
            scenario("required_pace", "ritmul necesar pentru deadline", monthlyRequired, amountRemaining, monthsRemaining),
            scenario("boost_50", "ritm boost cu 50% peste actual",
                Money((monthlyCurrentSavingPace.amount.toDouble() * 1.5).toInt()), amountRemaining, monthsRemaining)
        )

        return GoalProgressReport(
            goal = goal, monthsRemaining = monthsRemaining,
            monthsRemainingAtCurrentPace = monthsAtCurrentPace,
            monthlyRequired = monthlyRequired,
            monthlyCurrentSavingPace = monthlyCurrentSavingPace,
            feasibility = feasibility, currentPaceWillReach = willReach,
            shortfallPerMonth = shortfall, scenarios = scenarios
        )
    }

    private fun scenario(id: String, description: String, contribution: Money,
                         amountRemaining: Money, deadline: Int): GoalScenario {
        val months = if (contribution.isPositive)
            ceil(amountRemaining.amount.toDouble() / contribution.amount.toDouble()).toInt() else null
        val willReach = months != null && months <= deadline
        return GoalScenario(id = id, description = description, monthlyContribution = contribution,
            monthsToReach = months, willReach = willReach)
    }

    companion object {
        fun classifyFeasibility(monthlyCurrentSavingPace: Money, monthlyRequired: Money): GoalFeasibility {
            if (monthlyRequired.isZero) return GoalFeasibility.easy
            if (!monthlyCurrentSavingPace.isPositive) return GoalFeasibility.unrealistic
            val ratio = monthlyCurrentSavingPace.amount.toDouble() / monthlyRequired.amount.toDouble()
            return when {
                ratio >= 1.20 -> GoalFeasibility.easy
                ratio >= 0.95 -> GoalFeasibility.on_track
                ratio >= 0.50 -> GoalFeasibility.challenging_but_possible
                else -> GoalFeasibility.unrealistic
            }
        }
    }
}

package ro.solomon.analytics

import ro.solomon.core.domain.Goal
import ro.solomon.core.format.RomanianDateFormatter
import java.util.Calendar
import kotlin.math.ceil

class GoalBudgetContribution {

    fun monthlyRequired(
        goal: Goal,
        referenceDate: Long = System.currentTimeMillis(),
        calendar: Calendar = RomanianDateFormatter.gregorianROCalendar()
    ): Int {
        if (goal.isReached) return 0
        val amountRemaining = maxOf(0, goal.amountTarget.amount - goal.amountSaved.amount)
        if (amountRemaining <= 0) return 0

        val cal = calendar.clone() as Calendar; cal.timeInMillis = referenceDate
        val deadlineCal = calendar.clone() as Calendar; deadlineCal.timeInMillis = goal.deadline
        val monthsDiff = (deadlineCal.get(Calendar.YEAR) - cal.get(Calendar.YEAR)) * 12 +
                (deadlineCal.get(Calendar.MONTH) - cal.get(Calendar.MONTH))
        val monthsRemaining = maxOf(1, monthsDiff)

        return ceil(amountRemaining.toDouble() / monthsRemaining.toDouble()).toInt()
    }

    fun totalMonthlySaving(
        goals: List<Goal>,
        referenceDate: Long = System.currentTimeMillis(),
        calendar: Calendar = RomanianDateFormatter.gregorianROCalendar()
    ): Int {
        return goals.sumOf { monthlyRequired(it, referenceDate, calendar) }
    }
}

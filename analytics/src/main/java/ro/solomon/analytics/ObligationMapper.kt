package ro.solomon.analytics

import ro.solomon.core.domain.*

data class ObligationMap(
    val allObligations: List<Obligation>,
    val monthlyTotalFixed: Money,
    val obligationsToIncomeRatio: Double,
    val calendarByDay: Map<Int, List<Obligation>>,
    val detectedSilent: List<Obligation>,
    val debtMonthlyTotal: Money
) {
    fun obligationsBetweenDays(start: Int, end: Int): List<Obligation> {
        return allObligations.filter { it.dayOfMonth in start..end }
    }

    fun obligationsRemainingFrom(day: Int, untilDay: Int = 31): List<Obligation> {
        return allObligations
            .filter { it.dayOfMonth >= day && it.dayOfMonth <= untilDay }
            .sortedBy { it.dayOfMonth }
    }
}

class ObligationMapper {

    fun map(
        declared: List<Obligation>,
        detected: List<Obligation> = emptyList(),
        monthlyIncomeAvg: Money
    ): ObligationMap {
        val declaredNames = declared.map { it.name.lowercase() }.toSet()
        val dedupedDetected = detected.filter { !declaredNames.contains(it.name.lowercase()) }
        val all = declared + dedupedDetected

        val total = all.fold(Money(0)) { acc, o -> acc + o.amount }
        val ratio = if (monthlyIncomeAvg.amount > 0)
            total.amount.toDouble() / monthlyIncomeAvg.amount.toDouble()
        else 0.0

        val calendar = mutableMapOf<Int, MutableList<Obligation>>()
        for (obligation in all) {
            calendar.getOrPut(obligation.dayOfMonth) { mutableListOf() }.add(obligation)
        }
        for (day in calendar.keys) {
            calendar[day]?.sortByDescending { it.amount.amount }
        }

        val debtTotal = all.filter { it.isDebt }.fold(Money(0)) { acc, o -> acc + o.amount }

        return ObligationMap(
            allObligations = all,
            monthlyTotalFixed = total,
            obligationsToIncomeRatio = ratio,
            calendarByDay = calendar,
            detectedSilent = dedupedDetected,
            debtMonthlyTotal = debtTotal
        )
    }
}

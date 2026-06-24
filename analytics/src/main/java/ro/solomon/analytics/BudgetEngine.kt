package ro.solomon.analytics

import ro.solomon.core.domain.FlowDirection
import ro.solomon.core.domain.Money
import ro.solomon.core.domain.Transaction
import ro.solomon.core.domain.TransactionCategory
import ro.solomon.core.format.RomanianDateFormatter
import java.util.Calendar

enum class BudgetStatusLevel(val rank: Int) {
    on_track(0),
    warning(1),
    projected_over(2),
    over(3)
}

data class CategoryBudgetStatus(
    val category: TransactionCategory,
    val limit: Money,
    val spent: Money,
    val remaining: Money,
    val pctUsed: Double,
    val projectedSpend: Money,
    val projectedPctUsed: Double,
    val level: BudgetStatusLevel
) {
    val isAtRisk: Boolean
        get() = level == BudgetStatusLevel.over || level == BudgetStatusLevel.projected_over
}

data class BudgetReport(
    val cycleStart: Long,
    val cycleEnd: Long,
    val daysElapsed: Int,
    val daysTotal: Int,
    val daysRemaining: Int,
    val totalBudget: Money,
    val totalSpent: Money,
    val totalProjected: Money,
    val statuses: List<CategoryBudgetStatus>
) {
    val hasAnyBudget: Boolean get() = statuses.isNotEmpty()
    val overCount: Int get() = statuses.count { it.level == BudgetStatusLevel.over }
    val warningCount: Int get() = statuses.count { it.level == BudgetStatusLevel.warning }
    val projectedOverCount: Int get() = statuses.count { it.level == BudgetStatusLevel.projected_over }
    val atRisk: List<CategoryBudgetStatus>
        get() = statuses.filter { it.isAtRisk }
            .sortedWith(
                compareByDescending<CategoryBudgetStatus> { it.level.rank }
                    .thenByDescending { it.projectedPctUsed }
            )
}

/**
 * Deterministic budget tracker.
 *
 * Budgets are kept per category in whole RON (lei) by CategoryLimitsStore, while
 * Transaction amounts are in bani (1 RON = 100 bani). This engine converts
 * explicitly (RON * 100) so the comparison is exact and never off by 100x.
 *
 * The budget period is payday-to-payday (cycleStart = most recent payday,
 * cycleEnd = next payday), matching how people actually think about a month.
 */
class BudgetEngine {

    private val dayMs = 86_400_000L

    fun analyze(
        transactions: List<Transaction>,
        budgetsRON: Map<TransactionCategory, Int>,
        paydayDayOfMonth: Int,
        referenceDate: Long = System.currentTimeMillis(),
        calendar: Calendar = RomanianDateFormatter.gregorianROCalendar()
    ): BudgetReport {
        val (cycleStart, cycleEnd) = currentCycle(paydayDayOfMonth, referenceDate, calendar)
        val daysTotal = (((cycleEnd - cycleStart) / dayMs).toInt()).coerceAtLeast(1)
        val daysElapsed = ((((referenceDate - cycleStart) / dayMs).toInt()) + 1).coerceIn(1, daysTotal)
        val daysRemaining = (daysTotal - daysElapsed).coerceAtLeast(0)

        val spentByCat = mutableMapOf<TransactionCategory, Int>()
        for (tx in transactions) {
            if (tx.direction == FlowDirection.outgoing && tx.date >= cycleStart && tx.date <= referenceDate) {
                spentByCat[tx.category] = (spentByCat[tx.category] ?: 0) + tx.amount.amount
            }
        }

        val statuses = budgetsRON.entries
            .filter { it.value > 0 }
            .map { (cat, ron) ->
                val limitBani = ron * 100
                val spentBani = spentByCat[cat] ?: 0
                val pctUsed = if (limitBani > 0) spentBani.toDouble() / limitBani.toDouble() else 0.0
                val projectedBani =
                    if (daysElapsed > 0) (spentBani.toLong() * daysTotal / daysElapsed).toInt() else spentBani
                val projectedPct = if (limitBani > 0) projectedBani.toDouble() / limitBani.toDouble() else 0.0
                val level = when {
                    pctUsed >= 1.0 -> BudgetStatusLevel.over
                    pctUsed >= 0.80 -> BudgetStatusLevel.warning
                    projectedPct >= 1.0 -> BudgetStatusLevel.projected_over
                    else -> BudgetStatusLevel.on_track
                }
                CategoryBudgetStatus(
                    category = cat,
                    limit = Money(limitBani),
                    spent = Money(spentBani),
                    remaining = Money(limitBani - spentBani),
                    pctUsed = pctUsed,
                    projectedSpend = Money(projectedBani),
                    projectedPctUsed = projectedPct,
                    level = level
                )
            }
            .sortedByDescending { it.projectedPctUsed }

        return BudgetReport(
            cycleStart = cycleStart,
            cycleEnd = cycleEnd,
            daysElapsed = daysElapsed,
            daysTotal = daysTotal,
            daysRemaining = daysRemaining,
            totalBudget = Money(statuses.sumOf { it.limit.amount }),
            totalSpent = Money(statuses.sumOf { it.spent.amount }),
            totalProjected = Money(statuses.sumOf { it.projectedSpend.amount }),
            statuses = statuses
        )
    }

    /**
     * Suggested budget per category in whole RON, derived from real spending
     * history and normalised to one payday cycle. Ready to store via CategoryLimitsStore.
     */
    fun suggestBudgetsRON(
        transactions: List<Transaction>,
        cycleDays: Int = 30,
        lookbackDays: Int = 90,
        referenceDate: Long = System.currentTimeMillis()
    ): Map<TransactionCategory, Int> {
        val windowStart = referenceDate - lookbackDays * dayMs
        val totals = mutableMapOf<TransactionCategory, Int>()
        for (tx in transactions) {
            if (tx.direction == FlowDirection.outgoing && tx.date in windowStart..referenceDate) {
                totals[tx.category] = (totals[tx.category] ?: 0) + tx.amount.amount
            }
        }
        if (totals.isEmpty()) return emptyMap()
        return totals.mapNotNull { (cat, baniTotal) ->
            val perCycleBani = baniTotal.toLong() * cycleDays / lookbackDays
            val ron = Math.round(perCycleBani / 100.0).toInt()
            if (ron <= 0) null else cat to roundToNiceRON(ron)
        }.toMap()
    }

    private fun roundToNiceRON(ron: Int): Int = when {
        ron < 100 -> ((ron + 9) / 10) * 10
        ron < 1000 -> ((ron + 24) / 25) * 25
        else -> ((ron + 49) / 50) * 50
    }

    private fun currentCycle(
        paydayDayOfMonth: Int,
        referenceDate: Long,
        calendar: Calendar
    ): Pair<Long, Long> {
        val payday = paydayDayOfMonth.coerceIn(1, 28)
        val refCal = calendar.clone() as Calendar
        refCal.timeInMillis = referenceDate
        val today = refCal.get(Calendar.DAY_OF_MONTH)

        val startCal = calendar.clone() as Calendar
        startCal.timeInMillis = referenceDate
        startCal.set(Calendar.DAY_OF_MONTH, payday)
        startOfDay(startCal)

        val endCal = calendar.clone() as Calendar
        endCal.timeInMillis = referenceDate
        endCal.set(Calendar.DAY_OF_MONTH, payday)
        startOfDay(endCal)

        if (today >= payday) {
            endCal.add(Calendar.MONTH, 1)
        } else {
            startCal.add(Calendar.MONTH, -1)
        }
        return startCal.timeInMillis to endCal.timeInMillis
    }

    private fun startOfDay(c: Calendar) {
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
    }
}

package ro.solomon.analytics

import kotlinx.serialization.Serializable
import ro.solomon.core.domain.*
import ro.solomon.core.format.RomanianDateFormatter
import ro.solomon.core.moments.BalanceTrend
import kotlin.math.floor

@Serializable
data class MonthKey(
    val year: Int,
    val month: Int
) : Comparable<MonthKey> {
    init {
        require(month in 1..12) { "Lună invalidă: $month" }
    }

    val monthNameRO: String get() = RomanianDateFormatter.monthName(month)

    fun monthsAgo(n: Int): MonthKey {
        val totalMonths = year * 12 + (month - 1) - n
        val resolvedYear = (floor(totalMonths.toDouble() / 12.0)).toInt()
        val resolvedMonth = ((totalMonths % 12) + 12) % 12 + 1
        return MonthKey(resolvedYear, resolvedMonth)
    }

    override fun compareTo(other: MonthKey): Int {
        val y = year.compareTo(other.year)
        return if (y != 0) y else month.compareTo(other.month)
    }
}

@Serializable
data class MonthlyAmount(
    val amount: Money,
    val key: MonthKey
) {
    val monthNameRO: String get() = key.monthNameRO
}

@Serializable
enum class BreakEvenStatus {
    well_above_break_even, above_break_even, at_break_even, below_break_even, well_below_break_even
}

@Serializable
data class CashFlowAnalysis(
    val windowDays: Int,
    val analyzedMonths: Int,
    val monthlyIncomeAvg: Money,
    val monthlyIncomeLowest: MonthlyAmount? = null,
    val monthlyIncomeHighest: MonthlyAmount? = null,
    val monthlySpendingAvg: Money,
    val spendingByCategory: Map<TransactionCategory, Money>,
    val monthlyBalanceTrend: BalanceTrend,
    val velocityRONPerDay: Money,
    val breakEvenStatus: BreakEvenStatus,
    val monthlySavingsAvg: Money,
    val incomeConsumptionRatio: Double
) {
    fun topSpendingCategories(n: Int = 5): List<Pair<TransactionCategory, Money>> {
        return spendingByCategory.entries
            .sortedByDescending { it.value.amount }
            .take(n)
            .map { it.key to it.value }
    }
}

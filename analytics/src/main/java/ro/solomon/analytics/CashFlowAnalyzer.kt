package ro.solomon.analytics

import ro.solomon.core.domain.*
import ro.solomon.core.format.RomanianDateFormatter
import ro.solomon.core.moments.BalanceTrend
import java.util.Calendar

class CashFlowAnalyzer {

    data class MonthlyBucket(
        val key: MonthKey,
        val income: Money,
        val spending: Money
    ) {
        val balance: Money get() = income - spending
    }

    fun analyze(
        transactions: List<Transaction>,
        windowDays: Int = 180,
        referenceDate: Long = System.currentTimeMillis(),
        calendar: Calendar = RomanianDateFormatter.gregorianROCalendar()
    ): CashFlowAnalysis {
        val cal = calendar.clone() as Calendar
        cal.timeInMillis = referenceDate
        cal.add(Calendar.DAY_OF_YEAR, -windowDays)
        val windowStart = cal.timeInMillis

        val inWindow = transactions.filter { it.date >= windowStart && it.date <= referenceDate }
        if (inWindow.isEmpty()) return empty(windowDays)

        val monthlyBuckets = groupByMonth(inWindow, calendar)
        if (monthlyBuckets.isEmpty()) return empty(windowDays)

        val analyzedMonths = monthlyBuckets.size
        val incomeAmounts = monthlyBuckets.map { it.income }
        val spendingAmounts = monthlyBuckets.map { it.spending }
        val monthlyIncomeAvg = averageMoney(incomeAmounts)
        val monthlySpendingAvg = averageMoney(spendingAmounts)

        val lowestIncomeBucket = monthlyBuckets.minByOrNull { it.income }
        val highestIncomeBucket = monthlyBuckets.maxByOrNull { it.income }

        val spendingByCategory = aggregateByCategory(inWindow)

        val monthlySavingsAvg = monthlyIncomeAvg - monthlySpendingAvg
        val incomeConsumptionRatio = if (monthlyIncomeAvg.amount > 0)
            monthlySpendingAvg.amount.toDouble() / monthlyIncomeAvg.amount.toDouble()
        else 0.0

        val totalSpendingInWindow = spendingAmounts.sumOf { it.amount }
        val velocityRONPerDay = if (windowDays > 0) Money(totalSpendingInWindow / windowDays) else Money(0)
        val breakEvenStatus = classifyBreakEven(monthlyIncomeAvg, monthlySpendingAvg)
        val monthlyBalanceTrend = classifyBalanceTrend(monthlyBuckets)

        return CashFlowAnalysis(
            windowDays = windowDays,
            analyzedMonths = analyzedMonths,
            monthlyIncomeAvg = monthlyIncomeAvg,
            monthlyIncomeLowest = lowestIncomeBucket?.let { MonthlyAmount(it.income, it.key) },
            monthlyIncomeHighest = highestIncomeBucket?.let { MonthlyAmount(it.income, it.key) },
            monthlySpendingAvg = monthlySpendingAvg,
            spendingByCategory = spendingByCategory,
            monthlyBalanceTrend = monthlyBalanceTrend,
            velocityRONPerDay = velocityRONPerDay,
            breakEvenStatus = breakEvenStatus,
            monthlySavingsAvg = monthlySavingsAvg,
            incomeConsumptionRatio = incomeConsumptionRatio
        )
    }

    fun groupByMonth(transactions: List<Transaction>, calendar: Calendar): List<MonthlyBucket> {
        val income = mutableMapOf<MonthKey, Int>()
        val spending = mutableMapOf<MonthKey, Int>()

        for (tx in transactions) {
            val c = calendar.clone() as Calendar
            c.timeInMillis = tx.date
            val key = MonthKey(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1)
            when (tx.direction) {
                FlowDirection.incoming -> income[key] = (income[key] ?: 0) + tx.amount.amount
                FlowDirection.outgoing -> spending[key] = (spending[key] ?: 0) + tx.amount.amount
            }
        }

        val allKeys = income.keys + spending.keys
        return allKeys.distinct().sorted().map { key ->
            MonthlyBucket(
                key = key,
                income = Money(income[key] ?: 0),
                spending = Money(spending[key] ?: 0)
            )
        }
    }

    fun aggregateByCategory(transactions: List<Transaction>): Map<TransactionCategory, Money> {
        val totals = mutableMapOf<TransactionCategory, Int>()
        for (tx in transactions) {
            if (tx.direction == FlowDirection.outgoing) {
                totals[tx.category] = (totals[tx.category] ?: 0) + tx.amount.amount
            }
        }
        return totals.mapValues { Money(it.value) }
    }

    fun averageMoney(values: List<Money>): Money {
        if (values.isEmpty()) return Money(0)
        val total = values.sumOf { it.amount }
        return Money(total / values.size)
    }

    fun classifyBreakEven(income: Money, spending: Money): BreakEvenStatus {
        if (income.amount <= 0) {
            return if (spending.isZero) BreakEvenStatus.at_break_even else BreakEvenStatus.well_below_break_even
        }
        val net = income.amount - spending.amount
        val ratio = net.toDouble() / income.amount.toDouble()
        return when {
            ratio >= 0.15 -> BreakEvenStatus.well_above_break_even
            ratio >= 0.05 -> BreakEvenStatus.above_break_even
            ratio >= -0.05 -> BreakEvenStatus.at_break_even
            ratio >= -0.15 -> BreakEvenStatus.below_break_even
            else -> BreakEvenStatus.well_below_break_even
        }
    }

    fun classifyBalanceTrend(buckets: List<MonthlyBucket>): BalanceTrend {
        val recent = buckets.takeLast(3)
        if (recent.isEmpty()) return BalanceTrend.breaking_even
        val balances = recent.map { it.balance.amount }
        val allPositive = balances.all { it > 0 }
        val allNegative = balances.all { it < 0 }
        val avgBalance = if (balances.isNotEmpty()) balances.sum() / balances.size else 0
        val avgIncome = recent.map { it.income.amount }.let { if (it.isNotEmpty()) it.sum() / it.size else 0 }

        if (avgIncome == 0) return if (allNegative) BalanceTrend.negative else BalanceTrend.breaking_even

        val savingsRatio = avgBalance.toDouble() / avgIncome.toDouble()

        return when {
            allPositive && savingsRatio >= 0.20 -> BalanceTrend.healthy
            allPositive -> BalanceTrend.barely_breakeven
            allNegative && savingsRatio <= -0.10 -> BalanceTrend.negative
            allNegative -> BalanceTrend.sliding_negative
            savingsRatio > 0 -> BalanceTrend.barely_breakeven
            else -> BalanceTrend.sliding_negative
        }
    }

    companion object {
        fun empty(windowDays: Int) = CashFlowAnalysis(
            windowDays = windowDays, analyzedMonths = 0,
            monthlyIncomeAvg = Money(0), monthlyIncomeLowest = null, monthlyIncomeHighest = null,
            monthlySpendingAvg = Money(0), spendingByCategory = emptyMap(),
            monthlyBalanceTrend = BalanceTrend.breaking_even,
            velocityRONPerDay = Money(0), breakEvenStatus = BreakEvenStatus.at_break_even,
            monthlySavingsAvg = Money(0), incomeConsumptionRatio = 0.0
        )
    }
}

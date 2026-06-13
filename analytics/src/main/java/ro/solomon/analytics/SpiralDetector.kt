package ro.solomon.analytics

import ro.solomon.core.domain.*
import ro.solomon.core.format.RomanianDateFormatter
import ro.solomon.core.moments.SpiralFactor
import ro.solomon.core.moments.SpiralFactorKind
import ro.solomon.core.moments.SpiralSeverity
import java.util.Calendar

data class SpiralReport(
    val score: Int,
    val severity: SpiralSeverity,
    val factors: List<SpiralFactor>,
    val monthlyBalanceHistory: List<Int>,
    val requiresIntervention: Boolean,
    val csalbRelevant: Boolean
)

class SpiralDetector {

    fun detect(
        transactions: List<Transaction>,
        obligations: List<Obligation>,
        monthlyIncomeAvg: Money,
        monthlySpendingAvg: Money,
        monthlyBalanceHistory: List<Money>,
        referenceDate: Long = System.currentTimeMillis(),
        calendar: Calendar = RomanianDateFormatter.gregorianROCalendar()
    ): SpiralReport {
        val factors = mutableListOf<SpiralFactor>()

        balanceDecliningFactor(monthlyBalanceHistory)?.let { factors.add(it) }
        cardCreditIncreasingFactor(transactions, calendar)?.let { factors.add(it) }
        ifnActiveFactor(transactions, referenceDate, calendar)?.let { factors.add(it) }
        bnplStackingFactor(obligations)?.let { factors.add(it) }
        obligationsExceedIncomeFactor(obligations, monthlyIncomeAvg, monthlySpendingAvg)?.let { factors.add(it) }

        val weightedScore = factors.fold(0) { acc, factor ->
            acc + when (factor.factor) {
                SpiralFactorKind.obligations_exceed_income -> 2
                SpiralFactorKind.ifn_active -> 1
                else -> 1
            }
        }
        val score = minOf(weightedScore, 4)
        val severity = severityForScore(score)

        return SpiralReport(
            score = score,
            severity = severity,
            factors = factors,
            monthlyBalanceHistory = monthlyBalanceHistory.map { it.amount },
            requiresIntervention = score >= 2,
            csalbRelevant = factors.any { it.factor == SpiralFactorKind.ifn_active } || score >= 3
        )
    }

    private fun balanceDecliningFactor(history: List<Money>): SpiralFactor? {
        if (history.size < 3) return null
        val amounts = history.takeLast(4).map { it.amount }
        if (amounts.size < 3) return null
        var declining = true
        for (i in 1 until amounts.size) {
            if (amounts[i] >= amounts[i - 1]) { declining = false; break }
        }
        if (!declining) return null
        return SpiralFactor(
            factor = SpiralFactorKind.balance_declining,
            evidence = "balanță finală scade ${amounts.size} luni la rând",
            values = amounts
        )
    }

    private fun cardCreditIncreasingFactor(transactions: List<Transaction>, calendar: Calendar): SpiralFactor? {
        val monthly = mutableMapOf<MonthKey, Int>()
        for (tx in transactions) {
            if (tx.direction == FlowDirection.outgoing && tx.category == TransactionCategory.loans_bank) {
                val cal = calendar.clone() as Calendar; cal.timeInMillis = tx.date
                val key = MonthKey(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
                monthly[key] = (monthly[key] ?: 0) + tx.amount.amount
            }
        }
        if (monthly.size < 3) return null
        val sorted = monthly.entries.sortedBy { it.key }
        val amounts = sorted.map { it.value }
        var increasing = true
        for (i in 1 until amounts.size) {
            if (amounts[i] < amounts[i - 1]) { increasing = false; break }
        }
        if (!increasing || (amounts.lastOrNull() ?: 0) <= 0) return null
        val avgIncrease = (amounts.last() - amounts.first()).toDouble() / (amounts.size - 1)
        return SpiralFactor(
            factor = SpiralFactorKind.card_credit_increasing,
            evidence = "cheltuieli pe credit cresc lunar consecutiv ${amounts.size} luni",
            monthlyIncreaseAvg = Money(avgIncrease.toInt())
        )
    }

    private fun ifnActiveFactor(transactions: List<Transaction>, referenceDate: Long, calendar: Calendar): SpiralFactor? {
        val cal = calendar.clone() as Calendar; cal.timeInMillis = referenceDate; cal.add(Calendar.DAY_OF_YEAR, -30)
        val cutoff = cal.timeInMillis
        val recentIFN = transactions.filter {
            it.direction == FlowDirection.incoming && it.category == TransactionCategory.loans_ifn && it.date >= cutoff
        }
        val largest = recentIFN.maxByOrNull { it.amount } ?: return null
        return SpiralFactor(
            factor = SpiralFactorKind.ifn_active,
            evidence = "IFN incoming detectat în ultimele 30 zile",
            amount = largest.amount
        )
    }

    private fun bnplStackingFactor(obligations: List<Obligation>): SpiralFactor? {
        val bnpls = obligations.filter { it.kind == ObligationKind.bnpl }
        if (bnpls.size < 2) return null
        val total = bnpls.fold(Money(0)) { acc, o -> acc + o.amount }
        val names = bnpls.joinToString(", ") { it.name }
        return SpiralFactor(
            factor = SpiralFactorKind.bnpl_stacking,
            evidence = "${bnpls.size} BNPL active concomitent: $names",
            amount = total
        )
    }

    private fun obligationsExceedIncomeFactor(
        obligations: List<Obligation>, monthlyIncome: Money, monthlySpending: Money
    ): SpiralFactor? {
        val obligationsTotal = obligations.fold(Money(0)) { acc, o -> acc + o.amount }
        val combinedOutflow = obligationsTotal + monthlySpending
        val gap = combinedOutflow - monthlyIncome
        if (!gap.isPositive || monthlyIncome.amount <= 0) return null
        return SpiralFactor(
            factor = SpiralFactorKind.obligations_exceed_income,
            evidence = "obligații + cheltuieli medii depășesc venitul cu ${gap.amount} RON/lună",
            monthlyGap = gap
        )
    }

    private fun severityForScore(score: Int): SpiralSeverity = when (score) {
        0 -> SpiralSeverity.none; 1 -> SpiralSeverity.low; 2 -> SpiralSeverity.medium
        3 -> SpiralSeverity.high; else -> SpiralSeverity.critical
    }
}

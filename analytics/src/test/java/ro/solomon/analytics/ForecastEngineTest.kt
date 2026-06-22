package ro.solomon.analytics

import org.junit.Assert.assertEquals
import org.junit.Test
import ro.solomon.core.domain.Money
import ro.solomon.core.moments.BalanceTrend

class ForecastEngineTest {

    private fun cashFlow(incomeLei: Int, spendingLei: Int) = CashFlowAnalysis(
        windowDays = 180,
        analyzedMonths = 3,
        monthlyIncomeAvg = Money.fromLei(incomeLei),
        monthlyIncomeLowest = null,
        monthlyIncomeHighest = null,
        monthlySpendingAvg = Money.fromLei(spendingLei),
        spendingByCategory = emptyMap(),
        monthlyBalanceTrend = BalanceTrend.breaking_even,
        velocityRONPerDay = Money.fromLei(spendingLei / 30),
        breakEvenStatus = BreakEvenStatus.at_break_even,
        monthlySavingsAvg = Money.fromLei(incomeLei - spendingLei),
        incomeConsumptionRatio = if (incomeLei > 0) spendingLei.toDouble() / incomeLei else 0.0
    )

    @Test
    fun netDailyIsComputedFromMonthlyNetWithoutDoubleTruncation() {
        val cf = cashFlow(incomeLei = 3000, spendingLei = 4000)
        val result = ForecastEngine().analyze(
            cashFlow = cf,
            transactions = emptyList(),
            nowEpoch = 0L,
            currentBalance = Money.fromLei(200)
        )
        val netDaily = (Money.fromLei(3000).amount - Money.fromLei(4000).amount) / 30
        val expected7 = Money.fromLei(200).amount + netDaily * 7
        assertEquals(expected7, result.projectedBalanceIn7Days.amount)
    }

    @Test
    fun negativeShortTermProjectionIsHighRisk() {
        val cf = cashFlow(incomeLei = 1000, spendingLei = 5000)
        val result = ForecastEngine().analyze(cf, emptyList(), 0L, currentBalance = Money.fromLei(100))
        assertEquals(ForecastResult.RiskLevel.high, result.riskLevel)
    }

    @Test
    fun healthySurplusIsLowRisk() {
        val cf = cashFlow(incomeLei = 6000, spendingLei = 3000)
        val result = ForecastEngine().analyze(cf, emptyList(), 0L, currentBalance = Money.fromLei(2000))
        assertEquals(ForecastResult.RiskLevel.low, result.riskLevel)
    }
}

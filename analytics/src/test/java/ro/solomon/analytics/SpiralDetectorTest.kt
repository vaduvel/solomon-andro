package ro.solomon.analytics

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ro.solomon.core.domain.Money
import ro.solomon.core.moments.SpiralFactorKind

class SpiralDetectorTest {

    private val detector = SpiralDetector()

    private fun report(historyLei: List<Int>) = detector.detect(
        transactions = emptyList(),
        obligations = emptyList(),
        monthlyIncomeAvg = Money(0),
        monthlySpendingAvg = Money(0),
        monthlyBalanceHistory = historyLei.map { Money.fromLei(it) }
    )

    @Test
    fun decliningTrendWithSingleReboundIsDetected() {
        // Overall down but with one up-step that a strictly-monotonic check rejected.
        val r = report(listOf(2000, 1500, 1700, 800))
        assertTrue(r.factors.any { it.factor == SpiralFactorKind.balance_declining })
    }

    @Test
    fun stableOrGrowingBalanceIsNotFlagged() {
        val r = report(listOf(2000, 2100, 2050, 2080))
        assertFalse(r.factors.any { it.factor == SpiralFactorKind.balance_declining })
    }
}

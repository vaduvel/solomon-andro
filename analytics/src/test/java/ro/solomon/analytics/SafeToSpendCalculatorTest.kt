package ro.solomon.analytics

import org.junit.Assert.assertTrue
import org.junit.Test
import ro.solomon.core.domain.Money

class SafeToSpendCalculatorTest {

    private val calc = SafeToSpendCalculator()

    @Test
    fun verdictNoWhenItWouldBreakObligations() {
        val budget = calc.calculate(
            currentBalance = Money.fromLei(1000),
            obligationsRemaining = Money.fromLei(900),
            daysUntilNextPayday = 10
        )
        assertTrue(budget.verdictFor(Money.fromLei(200)) is Verdict.No)
    }

    @Test
    fun verdictCautionWhenPerDayUnderFiftyLei() {
        val budget = calc.calculate(
            currentBalance = Money.fromLei(2000),
            obligationsRemaining = Money.fromLei(1500),
            daysUntilNextPayday = 20
        )
        assertTrue(budget.verdictFor(Money.fromLei(0)) is Verdict.YesWithCaution)
    }

    @Test
    fun verdictYesWhenComfortable() {
        val budget = calc.calculate(
            currentBalance = Money.fromLei(10000),
            obligationsRemaining = Money.fromLei(1000),
            daysUntilNextPayday = 10
        )
        assertTrue(budget.verdictFor(Money.fromLei(500)) is Verdict.Yes)
    }

    @Test
    fun tightWhenPerDayBelowThreshold() {
        val budget = calc.calculate(
            currentBalance = Money.fromLei(1100),
            obligationsRemaining = Money.fromLei(1000),
            daysUntilNextPayday = 20,
            monthlyIncomeReference = Money.fromLei(3000)
        )
        assertTrue(budget.isTight)
    }
}

package ro.solomon.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MoneyTest {

    @Test
    fun fromRonPreservesBaniPrecision() {
        assertEquals(3999, Money.fromRON(39.99).amount)
        assertEquals(1050, Money.fromRON(10.50).amount)
    }

    @Test
    fun summingFractionalAmountsIsExact() {
        val total = Money.fromRON(10.50) + Money.fromRON(0.50)
        assertEquals(1100, total.amount)
        assertEquals(11.0, total.lei, 0.0001)
    }

    @Test
    fun fromLeiConvertsToBani() {
        assertEquals(150000, Money.fromLei(1500).amount)
        assertEquals(1500, Money.fromLei(15.0).amount)
    }

    @Test
    fun fromMinorPreservesBani() {
        assertEquals(2550, Money.fromMinor(2550).amount)
    }

    @Test
    fun predicatesAndArithmetic() {
        assertTrue(Money.fromLei(5).isPositive)
        assertTrue((-Money.fromLei(5)).isNegative)
        assertTrue(Money.zero.isZero)
        assertEquals(300, (Money(100) * 3).amount)
    }
}

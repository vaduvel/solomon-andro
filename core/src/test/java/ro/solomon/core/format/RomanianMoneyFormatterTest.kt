package ro.solomon.core.format

import org.junit.Assert.assertEquals
import org.junit.Test
import ro.solomon.core.domain.Money

class RomanianMoneyFormatterTest {

    @Test
    fun rendersBaniAsLeiWithTwoDecimals() {
        assertEquals("39,99", RomanianMoneyFormatter.leiWithDecimals(3999))
        assertEquals("0,05", RomanianMoneyFormatter.leiWithDecimals(5))
    }

    @Test
    fun groupsThousands() {
        assertEquals("1.234,56", RomanianMoneyFormatter.leiWithDecimals(123456))
    }

    @Test
    fun shortStyleAppendsRon() {
        assertEquals(
            "1.234,56 RON",
            RomanianMoneyFormatter.format(Money(123456), RomanianMoneyFormatter.Style.short)
        )
    }

    @Test
    fun negativeIsSigned() {
        assertEquals("-12,34", RomanianMoneyFormatter.leiWithDecimals(-1234))
    }
}

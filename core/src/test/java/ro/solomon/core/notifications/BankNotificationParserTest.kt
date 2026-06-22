package ro.solomon.core.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ro.solomon.core.domain.FlowDirection

class BankNotificationParserTest {

    @Test
    fun parsesRonAmountWithBaniPrecision() {
        val tx = BankNotificationParser.parse("Ai plătit 39,99 RON la Kaufland")
        assertNotNull(tx)
        assertEquals(3999, tx!!.amount.amount)
        assertEquals(FlowDirection.outgoing, tx.direction)
    }

    @Test
    fun ignoresNonRonCurrency() {
        val tx = BankNotificationParser.parse("Ai plătit 39,99 EUR la Kaufland")
        assertTrue(tx == null)
    }

    @Test
    fun detectsIncomingSalary() {
        val tx = BankNotificationParser.parse("Salariu primit 5.000,00 RON")
        assertNotNull(tx)
        assertEquals(FlowDirection.incoming, tx!!.direction)
        assertEquals(500000, tx.amount.amount)
    }
}

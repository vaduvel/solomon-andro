package ro.solomon.core.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * Regression tests for the date-unit fix: Transaction.date must be canonical
 * epoch MILLISECONDS so Calendar-based analytics bucket months correctly.
 */
class BankNotificationDateUnitTest {

    @Test
    fun parseUsesEpochMillisForDate() {
        val millis = 1_700_000_000_000L // 2023-11-14, clearly millisecond-scale
        val tx = BankNotificationParser.parse("Ai plătit 39,99 RON la Kaufland", dateEpochMillis = millis)
        assertNotNull(tx)
        assertEquals(millis, tx!!.date)
    }

    @Test
    fun defaultDateIsMillisScaleNotSeconds() {
        val before = System.currentTimeMillis()
        val tx = BankNotificationParser.parse("Ai plătit 10,00 RON la Lidl")
        val after = System.currentTimeMillis()
        assertNotNull(tx)
        // A seconds value would be ~1e9; a millis value is ~1e12 and within now bounds.
        assertTrue(tx!!.date in before..after)
    }

    @Test
    fun dateLandsInExpectedMonthWithCalendar() {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.set(2024, Calendar.MARCH, 15, 12, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val millis = cal.timeInMillis
        val tx = BankNotificationParser.parse("Ai plătit 25,00 RON la eMAG", dateEpochMillis = millis)
        assertNotNull(tx)
        val check = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        check.timeInMillis = tx!!.date
        assertEquals(2024, check.get(Calendar.YEAR))
        assertEquals(Calendar.MARCH, check.get(Calendar.MONTH))
    }
}

package ro.solomon.analytics

import org.junit.Assert.assertTrue
import org.junit.Test
import ro.solomon.core.domain.FlowDirection
import ro.solomon.core.domain.Money
import ro.solomon.core.domain.Transaction
import ro.solomon.core.domain.TransactionCategory
import ro.solomon.core.domain.TransactionSource

class RecurringDetectionEngineTest {

    private val engine = RecurringDetectionEngine()
    private val dayMillis = 24L * 60 * 60 * 1000

    private fun tx(id: String, daysAgo: Long, amountLei: Int, now: Long) = Transaction(
        id = id,
        date = now - daysAgo * dayMillis,
        amount = Money.fromLei(amountLei),
        direction = FlowDirection.outgoing,
        category = TransactionCategory.subscriptions,
        merchant = "Netflix",
        source = TransactionSource.notification_parsed
    )

    @Test
    fun detectsMonthlySubscription() {
        val now = 1_700_000_000_000L
        val txns = listOf(
            tx("a", 90, 39, now),
            tx("b", 60, 39, now),
            tx("c", 30, 39, now),
            tx("d", 0, 39, now)
        )
        val report = engine.detect(txns, windowDays = 200, referenceDate = now)
        val netflix = report.patterns.firstOrNull { it.merchant == "Netflix" }
        assertTrue(netflix != null && netflix.frequency == RecurringFrequency.monthly)
    }
}

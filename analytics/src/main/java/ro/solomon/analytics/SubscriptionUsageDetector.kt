package ro.solomon.analytics

import ro.solomon.core.domain.Transaction
import ro.solomon.core.domain.FlowDirection
import ro.solomon.core.domain.Subscription
import java.util.Calendar
import java.util.concurrent.TimeUnit

class SubscriptionUsageDetector {

    fun enrichWithUsage(
        subscriptions: List<Subscription>,
        transactions: List<Transaction>,
        referenceDate: Long = System.currentTimeMillis()
    ): List<Subscription> {
        return subscriptions.map { sub ->
            val daysAgo = computeDaysAgoFromTransactions(sub, transactions, referenceDate)
            sub.copy(lastUsedDaysAgo = daysAgo)
        }
    }

    fun computeDaysAgoFromTransactions(
        subscription: Subscription,
        transactions: List<Transaction>,
        referenceDate: Long
    ): Int? {
        val subNameLow = subscription.name.lowercase()
        val expectedAmount = subscription.amountMonthly.amount
        val lowerBound = (expectedAmount.toDouble() * 0.8).toInt()
        val upperBound = (expectedAmount.toDouble() * 1.2).toInt()

        val matchingTxs = transactions.filter { tx ->
            if (tx.direction != FlowDirection.outgoing) return@filter false
            val merchant = tx.merchant?.lowercase() ?: return@filter false
            if (!merchant.contains(subNameLow) && !subNameLow.contains(merchant)) return@filter false
            tx.amount.amount in lowerBound..upperBound
        }

        val lastMatch = matchingTxs.maxByOrNull { it.date } ?: return subscription.lastUsedDaysAgo

        val diffMs = referenceDate - lastMatch.date
        val days = TimeUnit.MILLISECONDS.toDays(diffMs).toInt()
        return maxOf(0, days)
    }
}

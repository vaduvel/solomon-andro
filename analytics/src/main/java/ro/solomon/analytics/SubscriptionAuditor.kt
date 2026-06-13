package ro.solomon.analytics

import ro.solomon.core.domain.Subscription
import ro.solomon.core.domain.Money

data class SubscriptionAuditReport(
    val ghostSubscriptions: List<Subscription>,
    val activeSubscriptions: List<Subscription>,
    val monthlyRecoverable: Money,
    val annualRecoverable: Money,
    val monthlyKeptTotal: Money,
    val annualKeptTotal: Money
) {
    val ghostCount: Int get() = ghostSubscriptions.size
}

class SubscriptionAuditor {

    fun audit(subscriptions: List<Subscription>): SubscriptionAuditReport {
        val ghosts = subscriptions.filter { it.isGhost }
        val active = subscriptions.filter { !it.isGhost }

        val monthlyRecoverable = ghosts.fold(Money(0)) { acc, s -> acc + s.amountMonthly }
        val annualRecoverable = ghosts.fold(Money(0)) { acc, s -> acc + s.amountAnnual }
        val monthlyKept = active.fold(Money(0)) { acc, s -> acc + s.amountMonthly }
        val annualKept = active.fold(Money(0)) { acc, s -> acc + s.amountAnnual }

        val sortedGhosts = ghosts.sortedWith(compareByDescending<Subscription> { it.amountMonthly }
            .thenByDescending { it.lastUsedDaysAgo ?: 0 })

        return SubscriptionAuditReport(
            ghostSubscriptions = sortedGhosts,
            activeSubscriptions = active,
            monthlyRecoverable = monthlyRecoverable,
            annualRecoverable = annualRecoverable,
            monthlyKeptTotal = monthlyKept,
            annualKeptTotal = annualKept
        )
    }
}

package ro.solomon.core.moments

import kotlinx.serialization.Serializable
import ro.solomon.core.domain.CancellationDifficulty
import ro.solomon.core.domain.Money

@Serializable
data class GhostSubscriptionDetail(
    val name: String,
    val amountMonthly: Money,
    val amountAnnual: Money,
    val lastUsedDaysAgo: Int,
    val cancellationDifficulty: CancellationDifficulty,
    val cancellationUrl: String? = null,
    val cancellationStepsSummary: String? = null,
    val cancellationWarning: String? = null,
    val alternativeSuggestion: String? = null
)

@Serializable
data class SubscriptionAuditTotals(
    val monthlyRecoverable: Money,
    val annualRecoverable: Money,
    val contextComparison: String
)

@Serializable
data class ActiveSubscriptionsKept(
    val count: Int,
    val monthlyTotal: Money,
    val examples: List<String>
)

@Serializable
data class SubscriptionAuditContext(
    val momentType: MomentType,
    val user: MomentUser,
    val auditPeriodDays: Int = 30,
    val ghostSubscriptions: List<GhostSubscriptionDetail>,
    val totals: SubscriptionAuditTotals,
    val activeSubscriptionsKept: ActiveSubscriptionsKept
) {
    companion object {
        fun create(
            user: MomentUser,
            auditPeriodDays: Int = 30,
            ghostSubscriptions: List<GhostSubscriptionDetail>,
            totals: SubscriptionAuditTotals,
            activeSubscriptionsKept: ActiveSubscriptionsKept
        ) = SubscriptionAuditContext(
            momentType = MomentType.subscriptionAudit,
            user = user,
            auditPeriodDays = auditPeriodDays,
            ghostSubscriptions = ghostSubscriptions,
            totals = totals,
            activeSubscriptionsKept = activeSubscriptionsKept
        )
    }
}

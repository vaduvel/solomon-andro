package ro.solomon.core.moments

import kotlinx.serialization.Serializable

@Serializable
data class MomentCandidates(
    val wowMoment: WowMomentContext? = null,
    val canIAfford: CanIAffordContext? = null,
    val payday: PaydayContext? = null,
    val upcomingObligation: UpcomingObligationContext? = null,
    val patternAlert: PatternAlertContext? = null,
    val subscriptionAudit: SubscriptionAuditContext? = null,
    val spiralAlert: SpiralAlertContext? = null,
    val weeklySummary: WeeklySummaryContext? = null
) {
    val hasAnyCandidate: Boolean
        get() = wowMoment != null || canIAfford != null || payday != null ||
            upcomingObligation != null || patternAlert != null ||
            subscriptionAudit != null || spiralAlert != null || weeklySummary != null
}

package ro.solomon.core.moments

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MomentType {
    @SerialName("wow_moment") wowMoment,
    @SerialName("can_i_afford") canIAfford,
    @SerialName("payday") payday,
    @SerialName("upcoming_obligation") upcomingObligation,
    @SerialName("pattern_alert") patternAlert,
    @SerialName("subscription_audit") subscriptionAudit,
    @SerialName("spiral_alert") spiralAlert,
    @SerialName("weekly_summary") weeklySummary;

    val maxWords: Int get() = when (this) {
        wowMoment -> 280
        canIAfford -> 60
        payday -> 100
        upcomingObligation -> 60
        patternAlert -> 110
        subscriptionAudit -> 140
        spiralAlert -> 200
        weeklySummary -> 90
    }
}

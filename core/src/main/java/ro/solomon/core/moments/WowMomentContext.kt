package ro.solomon.core.moments

import kotlinx.serialization.Serializable

@Serializable
data class WowMomentContext(
    val momentType: MomentType,
    val user: MomentUser,
    val analysisPeriodDays: Int = 180,
    val income: WowIncome,
    val spending: WowSpending,
    val outliers: List<OutlierItem>,
    val patterns: List<PatternItem>,
    val obligations: ObligationsBlock,
    val ghostSubscriptions: GhostSubscriptionsBlock,
    val positives: List<PositiveItem>,
    val goal: GoalBlock,
    val spiralRisk: SpiralBlock,
    val nextActionSuggested: NextActionSuggestion
) {
    companion object {
        fun create(
            user: MomentUser,
            analysisPeriodDays: Int = 180,
            income: WowIncome,
            spending: WowSpending,
            outliers: List<OutlierItem>,
            patterns: List<PatternItem>,
            obligations: ObligationsBlock,
            ghostSubscriptions: GhostSubscriptionsBlock,
            positives: List<PositiveItem>,
            goal: GoalBlock,
            spiralRisk: SpiralBlock,
            nextActionSuggested: NextActionSuggestion
        ) = WowMomentContext(
            momentType = MomentType.wowMoment,
            user = user,
            analysisPeriodDays = analysisPeriodDays,
            income = income,
            spending = spending,
            outliers = outliers,
            patterns = patterns,
            obligations = obligations,
            ghostSubscriptions = ghostSubscriptions,
            positives = positives,
            goal = goal,
            spiralRisk = spiralRisk,
            nextActionSuggested = nextActionSuggested
        )
    }
}

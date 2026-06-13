package ro.solomon.core.moments

import kotlinx.serialization.Serializable
import ro.solomon.core.domain.*

@Serializable
data class MomentUser(
    val name: String,
    val addressing: Addressing,
    val ageRange: AgeRange? = null
)

@Serializable
enum class IncomeStability {
    stable, slightly_variable, variable, unstable
}

@Serializable
data class LowestMonth(
    val amount: Money,
    val month: String
)

@Serializable
data class WowIncome(
    val monthlyAvg: Money,
    val stability: IncomeStability,
    val lowestMonth: LowestMonth,
    val extraIncomeDetected: Boolean,
    val extraIncomeAvg: Money? = null
)

@Serializable
enum class BalanceTrend {
    healthy, barely_breakeven, breaking_even, sliding_negative, negative
}

@Serializable
data class WowSpending(
    val monthlyAvg: Money,
    val incomeConsumptionRatio: Double,
    val monthlyBalanceTrend: BalanceTrend,
    val cardCreditUsed: Boolean,
    val overdraftUsedCount180d: Int
)

@Serializable
enum class OutlierType {
    single_large_purchase, category_concentration, unusual_frequency, unusual_merchant
}

@Serializable
data class OutlierItem(
    val rank: Int,
    val type: OutlierType,
    val category: TransactionCategory,
    val merchant: String? = null,
    val amount: Money? = null,
    val date: Long? = null,
    val amountTotal180d: Money? = null,
    val amountMonthlyAvg: Money? = null,
    val contextPhrase: String,
    val contextComparison: String
)

@Serializable
enum class PatternType {
    temporal_clustering, weekend_spike, frequency_spike, category_drift, merchant_loyalty
}

@Serializable
data class PatternItem(
    val type: PatternType,
    val category: TransactionCategory? = null,
    val description: String,
    val interpretation: String? = null,
    val averageWeekendSpend: Money? = null,
    val averageWeekdaySpend: Money? = null,
    val ratio: Double? = null
)

@Serializable
data class ObligationSummaryItem(
    val name: String,
    val amount: Money,
    val dayOfMonth: Int
)

@Serializable
data class ObligationsBlock(
    val monthlyTotalFixed: Money,
    val items: List<ObligationSummaryItem>,
    val obligationsToIncomeRatio: Double
)

@Serializable
data class GhostSubscriptionItem(
    val name: String,
    val amount: Money,
    val lastUsedDaysAgo: Int,
    val confidence: GhostConfidence
)

@Serializable
data class GhostSubscriptionsBlock(
    val count: Int,
    val monthlyTotal: Money,
    val annualTotal: Money,
    val items: List<GhostSubscriptionItem>
)

@Serializable
enum class PositiveType {
    no_ifn, no_late_payments, rent_to_income_healthy, savings_consistent,
    low_subscriptions, income_growth
}

@Serializable
data class PositiveItem(
    val type: PositiveType,
    val description: String,
    val rarityContext: String? = null,
    val durationMonths: Int? = null,
    val ratio: Double? = null
)

@Serializable
data class GoalBlock(
    val declared: Boolean,
    val type: GoalKind? = null,
    val destination: String? = null,
    val amountTarget: Money? = null,
    val amountSaved: Money? = null,
    val deadline: Long? = null,
    val monthsRemaining: Int? = null,
    val monthlyRequired: Money? = null,
    val feasibility: GoalFeasibility? = null,
    val currentPaceWillReach: Boolean? = null,
    val shortfallPerMonth: Money? = null
)

@Serializable
enum class SpiralSeverity {
    none, low, medium, high, critical;

    private val rank: Int get() = when (this) {
        none -> 0; low -> 1; medium -> 2; high -> 3; critical -> 4
    }

    companion object : Comparator<SpiralSeverity> {
        override fun compare(a: SpiralSeverity, b: SpiralSeverity) = a.rank.compareTo(b.rank)
    }
}

@Serializable
enum class SpiralFactorKind {
    balance_declining, card_credit_increasing, ifn_active,
    obligations_exceed_income, bnpl_stacking, overdraft_frequent
}

@Serializable
data class SpiralFactor(
    val factor: SpiralFactorKind,
    val evidence: String,
    val values: List<Int>? = null,
    val monthlyIncreaseAvg: Money? = null,
    val amount: Money? = null,
    val estimatedTotalRepayment: Money? = null,
    val monthlyGap: Money? = null
)

@Serializable
data class SpiralBlock(
    val score: Int,
    val severity: SpiralSeverity,
    val factors: List<SpiralFactor>
)

@Serializable
enum class NextActionType {
    cancel_ghost_subscriptions, reduce_category_spending, start_emergency_fund,
    refinance_debt, talk_to_csalb, no_action_needed
}

@Serializable
data class NextActionSuggestion(
    val type: NextActionType,
    val rationale: String,
    val monthlySaving: Money? = null,
    val annualSaving: Money? = null,
    val goalProgressImpact: String? = null
)

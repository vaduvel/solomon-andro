package ro.solomon.core.moments

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ro.solomon.core.domain.Money
import ro.solomon.core.domain.TransactionCategory

@Serializable
enum class PaydayReserveStatus {
    @SerialName("rezervat") rezervat,
    @SerialName("estimat") estimat
}

@Serializable
data class PaydayObligationReserve(
    val name: String,
    val amount: Money,
    val status: PaydayReserveStatus
)

@Serializable
data class PaydaySubscriptionReserve(
    val name: String,
    val amount: Money
)

@Serializable
data class PaydaySavingsAuto(
    val enabled: Boolean,
    val amount: Money? = null,
    val destination: String? = null
)

@Serializable
data class PaydaySalary(
    val amountReceived: Money,
    val receivedDate: String,
    val source: String,
    val isHigherThanAverage: Boolean,
    val isLowerThanAverage: Boolean
)

@Serializable
data class PaydayAllocation(
    val obligationsReserved: List<PaydayObligationReserve>,
    val subscriptionsReserved: List<PaydaySubscriptionReserve>,
    val obligationsTotal: Money,
    val subscriptionsTotal: Money,
    val savingsAuto: PaydaySavingsAuto,
    val availableToSpend: Money,
    val daysUntilNextPayday: Int,
    val availablePerDay: Money
)

@Serializable
enum class ComparisonDirection {
    @SerialName("better") better,
    @SerialName("worse") worse,
    @SerialName("same") same
}

@Serializable
data class PaydayComparisons(
    val vsLastMonthAvailable: Money,
    val vsLastMonthDiff: Money,
    val vsLastMonthDirection: ComparisonDirection
)

@Serializable
enum class BudgetBasis {
    @SerialName("average") average,
    @SerialName("reduced_target") reducedTarget,
    @SerialName("10_percent") tenPercent,
    @SerialName("last_month") lastMonth
}

@Serializable
data class CategoryBudgetSuggestion(
    val category: TransactionCategory,
    val amount: Money,
    val basedOn: BudgetBasis
)

@Serializable
enum class PaydayWarningType {
    @SerialName("upcoming_event") upcomingEvent,
    @SerialName("low_available") lowAvailable,
    @SerialName("obligations_too_high") obligationsTooHigh,
    @SerialName("savings_rate_low") savingsRateLow
}

@Serializable
data class PaydayWarning(
    val type: PaydayWarningType,
    val description: String,
    val impact: String? = null
)

@Serializable
data class PaydayContext(
    val momentType: MomentType,
    val user: MomentUser,
    val salary: PaydaySalary,
    val autoAllocation: PaydayAllocation,
    val comparisons: PaydayComparisons,
    val categoryBudgetsSuggested: List<CategoryBudgetSuggestion>,
    val warnings: List<PaydayWarning> = emptyList()
) {
    companion object {
        fun create(
            user: MomentUser,
            salary: PaydaySalary,
            autoAllocation: PaydayAllocation,
            comparisons: PaydayComparisons,
            categoryBudgetsSuggested: List<CategoryBudgetSuggestion>,
            warnings: List<PaydayWarning> = emptyList()
        ) = PaydayContext(
            momentType = MomentType.payday,
            user = user,
            salary = salary,
            autoAllocation = autoAllocation,
            comparisons = comparisons,
            categoryBudgetsSuggested = categoryBudgetsSuggested,
            warnings = warnings
        )
    }
}

package ro.solomon.core.moments

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ro.solomon.core.domain.Money
import ro.solomon.core.domain.TransactionCategory

@Serializable
enum class BudgetHealthLevel {
    @SerialName("on_track") onTrack,
    @SerialName("warning") warning,
    @SerialName("projected_over") projectedOver,
    @SerialName("over") over
}

@Serializable
data class BudgetCategoryStatus(
    val category: TransactionCategory,
    val limit: Money,
    val spent: Money,
    val remaining: Money,
    val pctUsed: Int,
    val projectedSpend: Money,
    val projectedPctUsed: Int,
    val health: BudgetHealthLevel
)

@Serializable
data class BudgetAlertContext(
    val momentType: MomentType,
    val user: MomentUser,
    val daysElapsed: Int,
    val daysTotal: Int,
    val daysRemaining: Int,
    val totalBudget: Money,
    val totalSpent: Money,
    val totalProjected: Money,
    val focus: BudgetCategoryStatus,
    val otherAtRisk: List<BudgetCategoryStatus> = emptyList()
) {
    companion object {
        fun create(
            user: MomentUser,
            daysElapsed: Int,
            daysTotal: Int,
            daysRemaining: Int,
            totalBudget: Money,
            totalSpent: Money,
            totalProjected: Money,
            focus: BudgetCategoryStatus,
            otherAtRisk: List<BudgetCategoryStatus> = emptyList()
        ): BudgetAlertContext = BudgetAlertContext(
            momentType = MomentType.budgetAlert,
            user = user,
            daysElapsed = daysElapsed,
            daysTotal = daysTotal,
            daysRemaining = daysRemaining,
            totalBudget = totalBudget,
            totalSpent = totalSpent,
            totalProjected = totalProjected,
            focus = focus,
            otherAtRisk = otherAtRisk
        )
    }
}

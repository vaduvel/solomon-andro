package ro.solomon.core.moments

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ro.solomon.core.domain.Money
import ro.solomon.core.domain.TransactionCategory

@Serializable
enum class CanIAffordVerdict {
    @SerialName("yes") yes,
    @SerialName("yes_with_caution") yesWithCaution,
    @SerialName("no") no
}

@Serializable
enum class CanIAffordVerdictReason {
    @SerialName("comfortable_margin") comfortableMargin,
    @SerialName("tight_but_workable") tightButWorkable,
    @SerialName("would_create_overdraft") wouldCreateOverdraft,
    @SerialName("would_break_obligation") wouldBreakObligation,
    @SerialName("category_already_over") categoryAlreadyOver
}

@Serializable
enum class CanIAffordAlternative {
    @SerialName("wait_until_payday") waitUntilPayday,
    @SerialName("wait_2_days") waitDays2,
    @SerialName("wait_3_days") waitDays3,
    @SerialName("smaller_amount") smallerAmount,
    @SerialName("wait_2_days_until_after_enel") waitTwoDaysAfterEnel,
    @SerialName("skip_this_category_this_week") skipThisCategoryWeek,
    @SerialName("none") none
}

@Serializable
data class AffordObligationRef(
    val name: String,
    val amount: Money,
    val dueDate: String
)

@Serializable
data class CanIAffordQuery(
    val rawText: String,
    val amountRequested: Money,
    val categoryInferred: TransactionCategory,
    val merchantInferred: String? = null,
    val isRecurring: Boolean = false
)

@Serializable
data class CanIAffordContextBlock(
    val today: String,
    val daysUntilPayday: Int,
    val currentBalance: Money,
    val obligationsRemainingThisPeriod: List<AffordObligationRef>,
    val obligationsTotalRemaining: Money,
    val availableAfterObligations: Money,
    val availablePerDayAfter: Money,
    val availablePerDayAfterPurchase: Money
)

@Serializable
data class CanIAffordDecision(
    val verdict: CanIAffordVerdict,
    val verdictReason: CanIAffordVerdictReason,
    val mathVisible: String,
    val alternativeToSuggest: CanIAffordAlternative = CanIAffordAlternative.none
)

@Serializable
data class CanIAffordHistoryContext(
    val thisCategoryThisMonth: Money,
    val thisCategoryAvgMonthly: Money,
    val isAboveAverageToday: Boolean
)

@Serializable
data class CanIAffordContext(
    val momentType: MomentType,
    val user: MomentUser,
    val query: CanIAffordQuery,
    val context: CanIAffordContextBlock,
    val decision: CanIAffordDecision,
    val userHistoryContext: CanIAffordHistoryContext
) {
    companion object {
        fun create(
            user: MomentUser,
            query: CanIAffordQuery,
            context: CanIAffordContextBlock,
            decision: CanIAffordDecision,
            userHistoryContext: CanIAffordHistoryContext
        ) = CanIAffordContext(
            momentType = MomentType.canIAfford,
            user = user,
            query = query,
            context = context,
            decision = decision,
            userHistoryContext = userHistoryContext
        )
    }
}

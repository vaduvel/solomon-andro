package ro.solomon.core.moments

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ro.solomon.core.domain.Money
import ro.solomon.core.domain.TransactionCategory

@Serializable
data class WeekRange(
    val start: String,
    val end: String,
    val weekNumber: Int
)

@Serializable
enum class SpendingTrendDirection {
    @SerialName("below") below,
    @SerialName("slightly_below") slightlyBelow,
    @SerialName("on_average") onAverage,
    @SerialName("slightly_above") slightlyAbove,
    @SerialName("above") above
}

@Serializable
data class WeeklySpendingBlock(
    val total: Money,
    val vsWeeklyAvg: Money,
    val diffPct: Int,
    val direction: SpendingTrendDirection
)

@Serializable
enum class WeeklyHighlightType {
    @SerialName("biggest_expense") biggestExpense,
    @SerialName("budget_kept") budgetKept,
    @SerialName("no_ifn_no_bnpl_temptation") noIFNNoBNPLTemptation,
    @SerialName("small_win_noticed") smallWinNoticed,
    @SerialName("new_recurring_detected") newRecurringDetected,
    @SerialName("category_drift") categoryDrift
}

@Serializable
data class WeeklyHighlight(
    val type: WeeklyHighlightType,
    val category: TransactionCategory? = null,
    val amount: Money? = null,
    val context: String
)

@Serializable
data class UpcomingObligationRef(
    val name: String,
    val amount: Money,
    val day: String
)

@Serializable
data class CalendarEventRef(
    val name: String,
    val estimatedCost: Money,
    val date: String
)

@Serializable
data class NextWeekPreview(
    val obligationsDue: List<UpcomingObligationRef>,
    val eventsInCalendar: List<CalendarEventRef>
)

@Serializable
data class SmallWin(
    val exists: Boolean,
    val description: String? = null
)

@Serializable
data class WeeklySummaryContext(
    val momentType: MomentType,
    val user: MomentUser,
    val week: WeekRange,
    val spending: WeeklySpendingBlock,
    val highlights: List<WeeklyHighlight>,
    val nextWeekPreview: NextWeekPreview,
    val smallWin: SmallWin
) {
    companion object {
        fun create(
            user: MomentUser,
            week: WeekRange,
            spending: WeeklySpendingBlock,
            highlights: List<WeeklyHighlight>,
            nextWeekPreview: NextWeekPreview,
            smallWin: SmallWin
        ) = WeeklySummaryContext(
            momentType = MomentType.weeklySummary,
            user = user,
            week = week,
            spending = spending,
            highlights = highlights,
            nextWeekPreview = nextWeekPreview,
            smallWin = smallWin
        )
    }
}

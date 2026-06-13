package ro.solomon.core.moments

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ro.solomon.core.domain.Money

@Serializable
enum class EstimationConfidence {
    @SerialName("low") low,
    @SerialName("medium") medium,
    @SerialName("high") high
}

@Serializable
data class UpcomingObligationItem(
    val name: String,
    val amountEstimated: Money,
    val dueDate: String,
    val daysUntilDue: Int,
    val amountEstimationConfidence: EstimationConfidence,
    val basedOnHistory: String
)

@Serializable
data class UpcomingObligationCashContext(
    val currentBalance: Money,
    val afterPayment: Money,
    val daysUntilNextPayday: Int,
    val availablePerDayAfter: Money
)

@Serializable
enum class AssessmentTone {
    @SerialName("reassuring") reassuring,
    @SerialName("calm") calm,
    @SerialName("alert") alert,
    @SerialName("urgent") urgent
}

@Serializable
data class UpcomingObligationAssessment(
    val isAffordable: Boolean,
    val isTight: Boolean,
    val tone: AssessmentTone
)

@Serializable
data class WeekendWarning(
    val isWeekendComing: Boolean,
    val weekendAvgSpend: Money,
    val wouldCreateProblem: Boolean
)

@Serializable
data class UpcomingObligationContext(
    val momentType: MomentType,
    val user: MomentUser,
    val upcoming: UpcomingObligationItem,
    val context: UpcomingObligationCashContext,
    val assessment: UpcomingObligationAssessment,
    val weekendWarning: WeekendWarning
) {
    companion object {
        fun create(
            user: MomentUser,
            upcoming: UpcomingObligationItem,
            context: UpcomingObligationCashContext,
            assessment: UpcomingObligationAssessment,
            weekendWarning: WeekendWarning
        ) = UpcomingObligationContext(
            momentType = MomentType.upcomingObligation,
            user = user,
            upcoming = upcoming,
            context = context,
            assessment = assessment,
            weekendWarning = weekendWarning
        )
    }
}

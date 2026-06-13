package ro.solomon.core.moments

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ro.solomon.core.domain.Money
import ro.solomon.core.domain.TransactionCategory

@Serializable
data class TemporalConcentration(
    val isTemporal: Boolean,
    val pattern: String,
    val interpretation: String
)

@Serializable
data class PatternDetected(
    val category: TransactionCategory,
    val merchantDominant: String? = null,
    val type: PatternType,
    val description: String,
    val amountPeriod: Money,
    val amountProjectedMonthly: Money,
    val vsBudget: Money,
    val vsBudgetPct: Int,
    val temporalConcentration: TemporalConcentration
)

@Serializable
enum class PatternScenarioID {
    @SerialName("continue") continueAsIs,
    @SerialName("reduce_2_per_week") reduce2PerWeek,
    @SerialName("skip_one_week") skipOneWeek,
    @SerialName("skip_month") skipMonth,
    @SerialName("cap_category") capCategory
}

@Serializable
data class PatternScenario(
    val scenarioId: PatternScenarioID,
    val description: String,
    val monthEndOutcome: String,
    val goalImpact: String
)

@Serializable
enum class PatternToneCalibration {
    @SerialName("warm_no_judgment") warmNoJudgment,
    @SerialName("factual_blunt") factualBlunt,
    @SerialName("curious_reflective") curiousReflective
}

@Serializable
data class PatternAlertContext(
    val momentType: MomentType,
    val user: MomentUser,
    val patternDetected: PatternDetected,
    val scenarios: List<PatternScenario>,
    val toneCalibration: PatternToneCalibration = PatternToneCalibration.warmNoJudgment
) {
    companion object {
        fun create(
            user: MomentUser,
            patternDetected: PatternDetected,
            scenarios: List<PatternScenario>,
            toneCalibration: PatternToneCalibration = PatternToneCalibration.warmNoJudgment
        ) = PatternAlertContext(
            momentType = MomentType.patternAlert,
            user = user,
            patternDetected = patternDetected,
            scenarios = scenarios,
            toneCalibration = toneCalibration
        )
    }
}

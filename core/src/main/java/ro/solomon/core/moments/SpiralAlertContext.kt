package ro.solomon.core.moments

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ro.solomon.core.domain.Money

@Serializable
enum class RecoveryComplexity {
    @SerialName("easy") easy,
    @SerialName("medium") medium,
    @SerialName("hard") hard,
    @SerialName("behavioral") behavioral
}

@Serializable
enum class RecoveryTool {
    @SerialName("CSALB") csalb,
    @SerialName("bank_negotiate") bankNegotiate,
    @SerialName("ANPC") anpc,
    @SerialName("self_service") selfService
}

@Serializable
data class RecoveryStep(
    val action: String,
    val monthlySaving: Money? = null,
    val potentialSaving: String? = null,
    val complexity: RecoveryComplexity,
    val tool: RecoveryTool? = null
)

@Serializable
data class RecoveryPlan(
    val step1: RecoveryStep,
    val step2: RecoveryStep,
    val step3: RecoveryStep
)

@Serializable
data class SpiralAlertContext(
    val momentType: MomentType,
    val user: MomentUser,
    val spiralScore: Int,
    val severity: SpiralSeverity,
    val factorsDetected: List<SpiralFactor>,
    val narrativeSummary: String,
    val interventionNeeded: Boolean,
    val csalbRelevant: Boolean,
    val recoveryPlan: RecoveryPlan
) {
    init {
        require(spiralScore in 0..4) { "spiralScore must be 0..4, was $spiralScore" }
    }

    companion object {
        fun create(
            user: MomentUser,
            spiralScore: Int,
            severity: SpiralSeverity,
            factorsDetected: List<SpiralFactor>,
            narrativeSummary: String,
            interventionNeeded: Boolean,
            csalbRelevant: Boolean,
            recoveryPlan: RecoveryPlan
        ) = SpiralAlertContext(
            momentType = MomentType.spiralAlert,
            user = user,
            spiralScore = spiralScore,
            severity = severity,
            factorsDetected = factorsDetected,
            narrativeSummary = narrativeSummary,
            interventionNeeded = interventionNeeded,
            csalbRelevant = csalbRelevant,
            recoveryPlan = recoveryPlan
        )
    }
}

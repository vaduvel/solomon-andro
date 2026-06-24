package ro.solomon.moments

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import ro.solomon.core.moments.*
import ro.solomon.llm.LLMProvider

class MomentOrchestrator(
    private val json: Json
) {

    @Suppress("UNCHECKED_CAST")
    private fun encode(context: Any, serializer: KSerializer<*>): String =
        json.encodeToString(serializer as KSerializer<Any>, context)

    fun selectedType(candidates: MomentCandidates): MomentType? {
        val spiral = candidates.spiralAlert
        if (spiral != null && spiral.spiralScore >= 2) {
            return MomentType.spiralAlert
        }
        if (candidates.canIAfford != null) return MomentType.canIAfford
        val upcoming = candidates.upcomingObligation
        if (upcoming != null && upcoming.upcoming.daysUntilDue <= 3) {
            return MomentType.upcomingObligation
        }
        if (candidates.payday != null) return MomentType.payday
        if (candidates.patternAlert != null) return MomentType.patternAlert
        if (candidates.subscriptionAudit != null) return MomentType.subscriptionAudit
        if (candidates.weeklySummary != null) return MomentType.weeklySummary
        if (candidates.wowMoment != null) return MomentType.wowMoment
        return null
    }

    suspend fun generate(
        candidates: MomentCandidates,
        llm: LLMProvider
    ): MomentOutput {
        if (!candidates.hasAnyCandidate) throw OrchestratorError.NoCandidatesAvailable
        val type = selectedType(candidates) ?: throw OrchestratorError.NoCandidatesAvailable
        return try {
            buildAndDispatch(type, candidates, llm)
        } catch (e: OrchestratorError) {
            throw e
        } catch (e: Throwable) {
            throw OrchestratorError.BuildFailed(type, e)
        }
    }

    private suspend fun buildAndDispatch(
        type: MomentType,
        candidates: MomentCandidates,
        llm: LLMProvider
    ): MomentOutput {
        val builder = builderFor(type)
        val context = contextFor(type, candidates) ?: throw OrchestratorError.NoCandidatesAvailable
        val contextJSON = encodeContext(builder, context)
        val response = llm.generate(
            systemPrompt = builder.systemPrompt,
            userContext = contextJSON,
            maxWords = type.maxWords
        )
        return MomentOutput(
            momentType = type,
            contextJSON = contextJSON,
            promptSent = builder.systemPrompt,
            llmResponse = response
        )
    }

    private fun builderFor(type: MomentType): MomentBuilder<*> = when (type) {
        MomentType.spiralAlert -> SpiralAlertBuilder()
        MomentType.canIAfford -> CanIAffordBuilder()
        MomentType.upcomingObligation -> UpcomingObligationBuilder()
        MomentType.payday -> PaydayMagicBuilder()
        MomentType.patternAlert -> PatternAlertBuilder()
        MomentType.subscriptionAudit -> SubscriptionAuditBuilder()
        MomentType.weeklySummary -> WeeklySummaryBuilder()
        MomentType.wowMoment -> WowMomentBuilder()
        MomentType.budgetAlert -> BudgetAlertBuilder()
    }

    private fun encodeContext(builder: MomentBuilder<*>, context: Any): String = when (builder) {
        is SpiralAlertBuilder -> encode(context, SpiralAlertContext.serializer())
        is CanIAffordBuilder -> encode(context, CanIAffordContext.serializer())
        is UpcomingObligationBuilder -> encode(context, UpcomingObligationContext.serializer())
        is PaydayMagicBuilder -> encode(context, PaydayContext.serializer())
        is PatternAlertBuilder -> encode(context, PatternAlertContext.serializer())
        is SubscriptionAuditBuilder -> encode(context, SubscriptionAuditContext.serializer())
        is WeeklySummaryBuilder -> encode(context, WeeklySummaryContext.serializer())
        is WowMomentBuilder -> encode(context, WowMomentContext.serializer())
        is BudgetAlertBuilder -> encode(context, BudgetAlertContext.serializer())
        else -> throw IllegalStateException("Unknown builder: ${'$'}{builder::class}")
    }

    private fun contextFor(type: MomentType, candidates: MomentCandidates): Any? = when (type) {
        MomentType.spiralAlert -> candidates.spiralAlert
        MomentType.canIAfford -> candidates.canIAfford
        MomentType.upcomingObligation -> candidates.upcomingObligation
        MomentType.payday -> candidates.payday
        MomentType.patternAlert -> candidates.patternAlert
        MomentType.subscriptionAudit -> candidates.subscriptionAudit
        MomentType.weeklySummary -> candidates.weeklySummary
        MomentType.wowMoment -> candidates.wowMoment
        MomentType.budgetAlert -> null
    }
}

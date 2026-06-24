package ro.solomon.app.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ro.solomon.core.domain.TransactionCategory
import java.util.UUID

data class SolomonMission(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val category: TransactionCategory,
    val targetSavingsRON: Int,
    val durationDays: Int,
    val startEpochSeconds: Long,
    val linkedGoalName: String? = null,
    val isAccepted: Boolean = false,
    val isCompleted: Boolean = false,
    val completedEpochSeconds: Long? = null
) {
    val endEpochSeconds: Long get() = startEpochSeconds + durationDays * 86400L
    fun daysRemaining(nowEpoch: Long): Int =
        ((endEpochSeconds - nowEpoch) / 86400L).toInt().coerceAtLeast(0)
    fun progressFraction(nowEpoch: Long): Double {
        val total = durationDays.toDouble()
        val elapsed = (durationDays - daysRemaining(nowEpoch)).toDouble()
        return if (total > 0) (elapsed / total).coerceIn(0.0, 1.0) else 0.0
    }
}

class MissionEngine {

    private val _active = MutableStateFlow<SolomonMission?>(null)
    val active: StateFlow<SolomonMission?> = _active.asStateFlow()

    private val _pending = MutableStateFlow<SolomonMission?>(null)
    val pending: StateFlow<SolomonMission?> = _pending.asStateFlow()

    suspend fun loadAndRefresh(nowEpoch: Long) {
        val active = LastMissionStore.readActive()
        _active.value = active?.takeIf { it.endEpochSeconds > nowEpoch && !it.isCompleted }
        val pending = LastMissionStore.readPending()
        _pending.value = pending
    }

    fun generate(
        topCategory: TransactionCategory?,
        topCategoryAmountRON: Int,
        linkedGoalName: String?
    ): SolomonMission? {
        val cat = topCategory ?: return null
        val now = System.currentTimeMillis() / 1000
        val target = (topCategoryAmountRON * 0.20).toInt().coerceAtLeast(50)
        val title = when (cat) {
            TransactionCategory.food_delivery, TransactionCategory.food_dining ->
                "S\u0103pt\u0103m\u00E2na f\u0103r\u0103 liv\u0103ri"
            TransactionCategory.shopping_online, TransactionCategory.shopping_offline ->
                "Pauz\u0103 de shopping online"
            TransactionCategory.entertainment ->
                "Mai pu\u021bin entertainment, mai mult ${linkedGoalName ?: "obiectiv"}"
            TransactionCategory.subscriptions ->
                "Anuleaz\u0103 un abonament"
            TransactionCategory.transport ->
                "Mers pe jos / transport public"
            else -> "Mai pu\u021bin pe ${cat.displayNameRO}"
        }
        val description = "\u00CEncearc\u0103 s\u0103 reduci ${cat.displayNameRO} cu 20% \u00een urm\u0103toarele 14 zile. " +
            "Dac\u0103 reu\u0219e\u0219ti, pune ${target} RON deoparte pentru ${linkedGoalName ?: "obiectivul t\u0103u"}."
        val mission = SolomonMission(
            title = title,
            description = description,
            category = cat,
            targetSavingsRON = target,
            durationDays = 14,
            startEpochSeconds = now,
            linkedGoalName = linkedGoalName
        )
        return mission
    }

    fun offer(mission: SolomonMission) {
        _pending.value = mission
    }

    suspend fun accept(ctx: android.content.Context) {
        val p = _pending.value ?: return
        _active.value = p.copy(isAccepted = true)
        _pending.value = null
        LastMissionStore.saveActive(ctx, _active.value!!)
        LastMissionStore.savePending(ctx, null)
        // Feedback loop: accepting a mission is a concrete commitment the coach learns from.
        CoachProfileStore.recordActedOn(ctx)
        CoachProfileStore.setLastCommitment(ctx, p.title)
    }

    suspend fun dismiss(ctx: android.content.Context) {
        _pending.value = null
        LastMissionStore.savePending(ctx, null)
        // Feedback loop: dismissing a suggested mission is an ignored nudge.
        CoachProfileStore.recordIgnored(ctx)
    }

    suspend fun complete(ctx: android.content.Context) {
        val a = _active.value ?: return
        val done = a.copy(isCompleted = true, completedEpochSeconds = System.currentTimeMillis() / 1000)
        _active.value = done
        LastMissionStore.saveActive(ctx, done)
    }
}

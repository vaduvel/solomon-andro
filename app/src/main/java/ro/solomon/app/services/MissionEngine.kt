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
                "Săptămâna fără livrări"
            TransactionCategory.shopping_online, TransactionCategory.shopping_offline ->
                "Pauză de shopping online"
            TransactionCategory.entertainment ->
                "Mai puțin entertainment, mai mult ${linkedGoalName ?: "obiectiv"}"
            TransactionCategory.subscriptions ->
                "Anulează un abonament"
            TransactionCategory.transport ->
                "Mers pe jos / transport public"
            else -> "Mai puțin pe ${cat.displayNameRO}"
        }
        val description = "Încearcă să reduci ${cat.displayNameRO} cu 20% în următoarele 14 zile. " +
            "Dacă reușești, pune ${target} RON deoparte pentru ${linkedGoalName ?: "obiectivul tău"}."
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
    }

    suspend fun dismiss(ctx: android.content.Context) {
        _pending.value = null
        LastMissionStore.savePending(ctx, null)
    }

    suspend fun complete(ctx: android.content.Context) {
        val a = _active.value ?: return
        val done = a.copy(isCompleted = true, completedEpochSeconds = System.currentTimeMillis() / 1000)
        _active.value = done
        LastMissionStore.saveActive(ctx, done)
    }
}

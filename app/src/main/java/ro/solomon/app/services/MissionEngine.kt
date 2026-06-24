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

    /**
     * Builds a localized, behavior-triggered mission for the user's heaviest
     * spending category. Missions are short, concrete and Romanian-first.
     */
    fun generate(
        topCategory: TransactionCategory?,
        topCategoryAmountRON: Int,
        linkedGoalName: String?
    ): SolomonMission? {
        val cat = topCategory ?: return null
        val now = System.currentTimeMillis() / 1000
        val goal = linkedGoalName ?: "obiectivul tău"

        val spec = specFor(cat)
        val target = (topCategoryAmountRON * spec.savingsFraction).toInt()
            .coerceAtLeast(spec.minSavingsRON)
        val title = spec.title
            .replace("{goal}", goal)
            .replace("{category}", cat.displayNameRO)
        val description = spec.descriptionTemplate
            .replace("{target}", target.toString())
            .replace("{goal}", goal)
            .replace("{category}", cat.displayNameRO)

        return SolomonMission(
            title = title,
            description = description,
            category = cat,
            targetSavingsRON = target,
            durationDays = spec.durationDays,
            startEpochSeconds = now,
            linkedGoalName = linkedGoalName
        )
    }

    private data class MissionSpec(
        val title: String,
        val durationDays: Int,
        val savingsFraction: Double,
        val minSavingsRON: Int,
        val descriptionTemplate: String
    )

    private fun specFor(cat: TransactionCategory): MissionSpec = when (cat) {
        TransactionCategory.food_delivery -> MissionSpec(
            title = "3 zile fără livări",
            durationDays = 3,
            savingsFraction = 0.30,
            minSavingsRON = 40,
            descriptionTemplate = "Gata cu livările 3 zile la rând — gătești sau mănânci ce ai deja acasă. " +
                "Dacă reziști, pui {target} RON deoparte pentru {goal}."
        )
        TransactionCategory.food_dining -> MissionSpec(
            title = "O săptămână cu prânzul de acasă",
            durationDays = 7,
            savingsFraction = 0.25,
            minSavingsRON = 60,
            descriptionTemplate = "Ia-ți prânzul de acasă în loc de restaurant timp de 7 zile. " +
                "Economia estimată: {target} RON pentru {goal}."
        )
        TransactionCategory.shopping_online, TransactionCategory.shopping_offline -> MissionSpec(
            title = "Pauză de shopping 7 zile",
            durationDays = 7,
            savingsFraction = 0.25,
            minSavingsRON = 80,
            descriptionTemplate = "Niciun cumpărat impulsiv 7 zile. Pune produsul pe wishlist și revino peste o săptămână. " +
                "Țintă: {target} RON puși deoparte pentru {goal}."
        )
        TransactionCategory.entertainment -> MissionSpec(
            title = "Mai puțin entertainment, mai mult {goal}",
            durationDays = 14,
            savingsFraction = 0.20,
            minSavingsRON = 50,
            descriptionTemplate = "Redu cheltuielile de entertainment cu 20% în 14 zile și mută {target} RON spre {goal}."
        )
        TransactionCategory.subscriptions -> MissionSpec(
            title = "Negociază sau anulează un abonament",
            durationDays = 14,
            savingsFraction = 0.50,
            minSavingsRON = 20,
            descriptionTemplate = "Alege un abonament pe care nu-l mai folosești și anulează-l (sau negociază un preț mai mic). " +
                "Eliberezi ~{target} RON/lună pentru {goal}."
        )
        TransactionCategory.transport -> MissionSpec(
            title = "Săptămâna cu transport public",
            durationDays = 7,
            savingsFraction = 0.20,
            minSavingsRON = 30,
            descriptionTemplate = "Lasă mașina/taxiul și mergi cu transportul public sau pe jos 7 zile. " +
                "Estimat: {target} RON pentru {goal}."
        )
        else -> MissionSpec(
            title = "Mai puțin pe {category}",
            durationDays = 14,
            savingsFraction = 0.20,
            minSavingsRON = 50,
            descriptionTemplate = "Încearcă să reduci {category} cu 20% în următoarele 14 zile. " +
                "Dacă reușești, pune {target} RON deoparte pentru {goal}."
        )
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

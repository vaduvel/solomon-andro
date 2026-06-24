package ro.solomon.app.services

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ro.solomon.core.domain.TransactionCategory
import java.util.UUID

/** Tipul de misiune - nu doar reducere pe o categorie, ci \u0219i economisire, datorie sau obicei. */
enum class MissionKind { CATEGORY_REDUCTION, SAVINGS, DEBT_PAYDOWN, HABIT }

data class SolomonMission(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val category: TransactionCategory,
    val targetSavingsRON: Int,
    val durationDays: Int,
    val startEpochSeconds: Long,
    val linkedGoalName: String? = null,
    val kind: MissionKind = MissionKind.CATEGORY_REDUCTION,
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
        val goal = linkedGoalName ?: "obiectivul t\u0103u"

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
            linkedGoalName = linkedGoalName,
            kind = MissionKind.CATEGORY_REDUCTION
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

    /** Misiune de economisire: ritm mic, repetat, spre un obiectiv concret. */
    fun generateSavings(nowEpoch: Long, goalName: String?, monthlyContributionRON: Int): SolomonMission {
        val target = monthlyContributionRON.coerceAtLeast(50)
        val goal = goalName ?: "obiectivul t\u0103u"
        return SolomonMission(
            title = "Pune deoparte pentru $goal",
            description = "\u00CEn urm\u0103toarele 14 zile transfer\u0103 ${target} RON spre $goal. " +
                "Ritm mic, repetat \u2014 a\u0219a cre\u0219te un obiectiv.",
            category = TransactionCategory.savings,
            targetSavingsRON = target,
            durationDays = 14,
            startEpochSeconds = nowEpoch,
            linkedGoalName = goalName,
            kind = MissionKind.SAVINGS
        )
    }

    /** Misiune de datorie: o plat\u0103 \u00een plus + nicio datorie nou\u0103. */
    fun generateDebtPaydown(nowEpoch: Long, monthlyDebtRON: Int): SolomonMission {
        val extra = (monthlyDebtRON * 0.10).toInt().coerceAtLeast(50)
        return SolomonMission(
            title = "Un pas \u00eenainte la datorii",
            description = "Pl\u0103te\u0219te ${extra} RON \u00een plus la o datorie \u00een urm\u0103toarele 14 zile " +
                "\u0219i evit\u0103 s\u0103 iei una nou\u0103. Fiecare plat\u0103 \u00een plus taie din dob\u00e2nd\u0103.",
            category = TransactionCategory.loans_bank,
            targetSavingsRON = extra,
            durationDays = 14,
            startEpochSeconds = nowEpoch,
            kind = MissionKind.DEBT_PAYDOWN
        )
    }

    /** Misiune de obicei: con\u0219tientizare prin notarea fiec\u0103rei cheltuieli. */
    fun generateHabit(nowEpoch: Long): SolomonMission {
        return SolomonMission(
            title = "7 zile de claritate",
            description = "Noteaz\u0103 fiecare cheltuial\u0103 timp de 7 zile. Nu schimba nimic \u2014 doar observ\u0103. " +
                "Con\u0219tientizarea e primul pas care schimb\u0103 comportamentul.",
            category = TransactionCategory.unknown,
            targetSavingsRON = 100,
            durationDays = 7,
            startEpochSeconds = nowEpoch,
            kind = MissionKind.HABIT
        )
    }

    /**
     * Alege tipul de misiune potrivit situa\u021biei reale a userului, nu doar reducere pe top categorie:
     *  - obiectiv f\u0103r\u0103 contribu\u021bie -> economisire;
     *  - obliga\u021bii grele -> datorie;
     *  - cheltuieli mici repetate -> obicei;
     *  - altfel -> reducere pe categoria dominant\u0103 (sau obicei dac\u0103 nu exist\u0103 categorie).
     */
    fun generateAny(
        nowEpoch: Long,
        topCategory: TransactionCategory?,
        topCategoryAmountRON: Int,
        linkedGoalName: String?,
        monthlyDebtPaymentRON: Int,
        hasGoalWithoutContribution: Boolean,
        vulnerability: SolomonCoachVulnerability?
    ): SolomonMission? {
        return when {
            (vulnerability == SolomonCoachVulnerability.GOALS_WITHOUT_CONTRIBUTION || hasGoalWithoutContribution) && linkedGoalName != null ->
                generateSavings(nowEpoch, linkedGoalName, (topCategoryAmountRON * 0.15).toInt())
            vulnerability == SolomonCoachVulnerability.HEAVY_OBLIGATIONS && monthlyDebtPaymentRON > 0 ->
                generateDebtPaydown(nowEpoch, monthlyDebtPaymentRON)
            vulnerability == SolomonCoachVulnerability.SMALL_RECURRING ->
                generateHabit(nowEpoch)
            topCategory != null ->
                generate(topCategory, topCategoryAmountRON, linkedGoalName)
            else -> generateHabit(nowEpoch)
        }
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
        CoachProfileStore.addCommitment(ctx, p.title)
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
        // Commitment resolved: completing the mission means the user honored it.
        CoachProfileStore.resolveLastCommitment(ctx, honored = true)
    }
}

package ro.solomon.app.services

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.first
import ro.solomon.app.di.ServiceLocator
import ro.solomon.app.di.preferencesStore
import ro.solomon.core.domain.Transaction
import ro.solomon.core.moments.MomentType
import ro.solomon.moments.MomentEngine

/**
 * Event-driven reactivity.
 *
 * Solomon's proactive engine normally runs once a day (DailyMomentWorker). That
 * means a user could spiral for almost a full day before Solomon reacts. This
 * evaluator closes that gap: the instant a new transaction is ingested — from a
 * bank notification, a shared SMS/receipt, or an open-banking sync — it runs the
 * SAME moment pipeline immediately and pushes a notification if (and only if) an
 * urgent moment is warranted.
 *
 * To keep this calm and Play-safe:
 *  - only OUTGOING transactions trigger evaluation (incoming/payday is handled by
 *    the daily worker, no need to react in-the-moment);
 *  - only urgent, reactive moment types may fire here (spiral / pattern alerts) —
 *    payday / weekly-summary / wow style moments stay on their daily cadence;
 *  - a global debounce coalesces the burst of transactions that arrives during a
 *    bank sync into a single evaluation;
 *  - a per-type cooldown prevents repeated alerts of the same kind.
 */
object ReactiveMomentEvaluator {

    /** Only these moment types are allowed to fire reactively (in-the-moment). */
    private val urgentReactiveTypes = setOf(MomentType.spiralAlert, MomentType.patternAlert)

    /** Coalesce the burst of transactions that arrives together during a bank sync. */
    private const val GLOBAL_DEBOUNCE_SECONDS = 90L
    private val lastEvalKey = longPreferencesKey("reactiveMoment.lastEval")

    /** Per-type reactive cooldown so we never spam the same alert. */
    private fun reactiveCooldownSeconds(type: MomentType): Long = when (type) {
        MomentType.spiralAlert -> 6L * 60L * 60L   // 6h
        MomentType.patternAlert -> 24L * 60L * 60L // 24h
        else -> Long.MAX_VALUE
    }

    private fun lastPushKey(type: MomentType) =
        longPreferencesKey("reactiveMoment.lastPush.${type.serialName()}")

    /**
     * Called right after a freshly ingested transaction has been persisted.
     * Safe to call from any ingestion path; never throws.
     */
    suspend fun onTransactionIngested(ctx: Context, tx: Transaction) {
        // Only money going OUT can put the user at risk in-the-moment.
        if (!tx.isOutgoing) return

        runCatching {
            val now = System.currentTimeMillis() / 1000L

            // Global debounce: skip if we evaluated very recently (coalesces sync bursts).
            val prefs = ctx.preferencesStore.data.first()
            val lastEval = prefs[lastEvalKey] ?: 0L
            if (now - lastEval < GLOBAL_DEBOUNCE_SECONDS) return@runCatching
            ctx.preferencesStore.edit { it[lastEvalKey] = now }

            // Build the exact same snapshot the daily worker uses.
            val txns = ServiceLocator.txnRepo.fetchAll()
            val obligs = ServiceLocator.obligationRepo.fetchAll()
            val subs = ServiceLocator.subRepo.fetchAll()
            val goals = ServiceLocator.goalRepo.fetchAll()
            val profile = ServiceLocator.userRepo.fetchProfile()
            val snap = MomentEngine.Snapshot(
                userProfile = profile,
                transactions = txns,
                obligations = obligs,
                subscriptions = subs,
                goals = goals,
                referenceDateEpochSeconds = now
            )

            // Cheap pre-check (no LLM): is the currently selected moment an urgent one?
            val candidateType = ServiceLocator.momentEngine.selectedType(snap) ?: return@runCatching
            if (candidateType !in urgentReactiveTypes) return@runCatching

            // Per-type cooldown.
            val lastPush = prefs[lastPushKey(candidateType)] ?: 0L
            if (now - lastPush < reactiveCooldownSeconds(candidateType)) return@runCatching

            // Generate the full moment (may hit the LLM) and re-verify before notifying.
            val moment = ServiceLocator.momentEngine.generateBestMoment(snap) ?: return@runCatching
            if (moment.momentType !in urgentReactiveTypes) return@runCatching

            MomentNotifier.notifyMoment(ctx, moment.momentType, moment.llmResponse)
            LastMomentStore.save(ctx, moment.llmResponse, moment.momentType.serialName())
            MomentHistoryStore.append(
                ctx = ctx,
                type = moment.momentType.serialName(),
                title = moment.momentType.toTitle(),
                body = moment.llmResponse,
                nowEpochSeconds = now
            )
            MomentCooldownManager.recordShown(
                type = moment.momentType.toCooldownType(),
                nowEpoch = now
            )
            ctx.preferencesStore.edit { it[lastPushKey(moment.momentType)] = now }
        }
    }

    private fun MomentType.toCooldownType(): MomentCooldownManager.CooldownType = when (this) {
        MomentType.spiralAlert -> MomentCooldownManager.CooldownType.SpiralAlert
        MomentType.canIAfford -> MomentCooldownManager.CooldownType.CanIAfford
        MomentType.upcomingObligation -> MomentCooldownManager.CooldownType.UpcomingObligation
        MomentType.payday -> MomentCooldownManager.CooldownType.Payday
        MomentType.patternAlert -> MomentCooldownManager.CooldownType.PatternAlert
        MomentType.subscriptionAudit -> MomentCooldownManager.CooldownType.SubscriptionAudit
        MomentType.weeklySummary -> MomentCooldownManager.CooldownType.WeeklySummary
        MomentType.wowMoment -> MomentCooldownManager.CooldownType.WowMoment
    }
}

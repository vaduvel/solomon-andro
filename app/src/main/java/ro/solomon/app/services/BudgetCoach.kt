package ro.solomon.app.services

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import ro.solomon.analytics.BudgetEngine
import ro.solomon.analytics.BudgetReport
import ro.solomon.analytics.BudgetStatusLevel
import ro.solomon.analytics.CategoryBudgetStatus
import ro.solomon.app.di.ServiceLocator
import ro.solomon.app.di.preferencesStore
import ro.solomon.core.domain.Addressing
import ro.solomon.core.domain.Transaction
import ro.solomon.core.domain.TransactionCategory
import ro.solomon.core.domain.UserProfile
import ro.solomon.core.moments.BudgetAlertContext
import ro.solomon.core.moments.BudgetCategoryStatus
import ro.solomon.core.moments.BudgetHealthLevel
import ro.solomon.core.moments.MomentType
import ro.solomon.core.moments.MomentUser
import ro.solomon.moments.BudgetAlertBuilder

/**
 * Budget coach — the part that makes Solomon's budgets different from a plain
 * Revolut-style budget bar.
 *
 * It runs the deterministic [BudgetEngine] over the user's real transactions and
 * their per-category budgets (CategoryLimitsStore, payday-to-payday cycle), and
 * when a category is over / projected to go over, it generates a dedicated
 * `budgetAlert` LLM moment with concrete, in-the-moment advice.
 *
 * Hybrid coach layer: the deterministic engine stays the source of truth for the
 * numbers, while every alert is now finished with a concrete implementation-intention
 * (if-then) plan for the at-risk category, phrased for the user's money script. That
 * is the anti "beep beep, stop spending" move — a real next action, not just an alarm.
 *
 * Two entry points:
 *  - [onOutgoing]: called the instant a new outgoing transaction is ingested
 *    (direct, real-time updates) — but only if that category actually has a budget;
 *  - [evaluateDaily]: a calm daily safety net from DailyMomentWorker.
 *
 * A single shared cooldown keeps it from ever spamming, regardless of path.
 * [report] and [suggestBudgetsRON] are pure helpers for the Budgets screen (no LLM, no notifications).
 */
object BudgetCoach {

    private val engine = BudgetEngine()
    private val lastPushKey = longPreferencesKey("budgetCoach.lastPush")
    private const val PUSH_COOLDOWN_SECONDS = 24L * 60L * 60L

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    /** Pure analysis for the UI — no LLM, no notifications. */
    suspend fun report(referenceDate: Long = System.currentTimeMillis()): BudgetReport {
        val txns = ServiceLocator.txnRepo.fetchAll()
        val profile = ServiceLocator.userRepo.fetchProfile()
        val budgets = CategoryLimitsStore.limits()
        return engine.analyze(
            transactions = txns,
            budgetsRON = budgets,
            paydayDayOfMonth = paydayDayOfMonth(profile),
            referenceDate = referenceDate
        )
    }

    /** Suggested budgets (whole RON) from real spending history, for first-time setup. */
    suspend fun suggestBudgetsRON(): Map<TransactionCategory, Int> {
        val txns = ServiceLocator.txnRepo.fetchAll()
        return engine.suggestBudgetsRON(transactions = txns)
    }

    /** Reactive entry: a transaction just landed. Only react if its category has a budget. */
    suspend fun onOutgoing(ctx: Context, tx: Transaction) {
        runCatching {
            if (!tx.isOutgoing) return@runCatching
            if (CategoryLimitsStore.limitFor(tx.category) == null) return@runCatching
            evaluateAndNotify(ctx)
        }
    }

    /** Daily entry from the worker. */
    suspend fun evaluateDaily(ctx: Context) {
        runCatching { evaluateAndNotify(ctx) }
    }

    private suspend fun evaluateAndNotify(ctx: Context) {
        val now = System.currentTimeMillis() / 1000L
        val prefs = ctx.preferencesStore.data.first()
        val lastPush = prefs[lastPushKey] ?: 0L
        if (now - lastPush < PUSH_COOLDOWN_SECONDS) return

        val report = report(now * 1000L)
        if (!report.hasAnyBudget) return
        val focus = report.atRisk.firstOrNull {
            it.level == BudgetStatusLevel.over ||
                (it.level == BudgetStatusLevel.projected_over && report.daysElapsed >= 7 && it.pctUsed >= 0.5)
        } ?: return

        val profile = ServiceLocator.userRepo.fetchProfile()
        val context = buildContext(report, focus, profile)
        val output = BudgetAlertBuilder().build(context, ServiceLocator.llm, json)
        if (output.llmResponse.isBlank()) return

        // Hybrid coach layer: finish the alert with a concrete if-then plan for the
        // at-risk category, phrased for the user's money script. The deterministic
        // engine owns the numbers; this owns the behavior-change nudge.
        val coachProfile = CoachProfileStore.load(ctx)
        val plan = ImplementationIntentionEngine.forCategory(focus.category, coachProfile.moneyScript)
        val body = buildString {
            append(output.llmResponse.trim())
            append("\n\nPlan concret: ")
            append(plan.asSentenceRo())
        }

        MomentNotifier.notifyMoment(ctx, MomentType.budgetAlert, body)
        LastMomentStore.save(ctx, body, MomentType.budgetAlert.serialName())
        MomentHistoryStore.append(
            ctx = ctx,
            type = MomentType.budgetAlert.serialName(),
            title = MomentType.budgetAlert.toTitle(),
            body = body,
            nowEpochSeconds = now
        )
        MomentCooldownManager.recordShown(
            type = MomentType.budgetAlert.toCooldownType(),
            nowEpoch = now
        )
        ctx.preferencesStore.edit { it[lastPushKey] = now }
    }

    private fun buildContext(
        report: BudgetReport,
        focus: CategoryBudgetStatus,
        profile: UserProfile?
    ): BudgetAlertContext {
        val user = MomentUser(
            name = profile?.demographics?.name ?: "prietene",
            addressing = profile?.demographics?.addressing ?: Addressing.tu,
            ageRange = profile?.demographics?.ageRange
        )
        val others = report.atRisk
            .filter { it.category != focus.category }
            .take(3)
            .map { mapStatus(it) }
        return BudgetAlertContext.create(
            user = user,
            daysElapsed = report.daysElapsed,
            daysTotal = report.daysTotal,
            daysRemaining = report.daysRemaining,
            totalBudget = report.totalBudget,
            totalSpent = report.totalSpent,
            totalProjected = report.totalProjected,
            focus = mapStatus(focus),
            otherAtRisk = others
        )
    }

    private fun mapStatus(s: CategoryBudgetStatus) = BudgetCategoryStatus(
        category = s.category,
        limit = s.limit,
        spent = s.spent,
        remaining = s.remaining,
        pctUsed = (s.pctUsed * 100).toInt(),
        projectedSpend = s.projectedSpend,
        projectedPctUsed = (s.projectedPctUsed * 100).toInt(),
        health = when (s.level) {
            BudgetStatusLevel.over -> BudgetHealthLevel.over
            BudgetStatusLevel.projected_over -> BudgetHealthLevel.projectedOver
            BudgetStatusLevel.warning -> BudgetHealthLevel.warning
            BudgetStatusLevel.on_track -> BudgetHealthLevel.onTrack
        }
    )

    private fun paydayDayOfMonth(profile: UserProfile?): Int {
        val p = profile ?: return 28
        return when (p.financials.salaryFrequency.type) {
            "monthly" -> p.financials.salaryFrequency.dayOfMonth.takeIf { it in 1..31 } ?: 28
            "bimonthly" -> p.financials.salaryFrequency.secondDay.takeIf { it in 1..31 } ?: 28
            else -> 28
        }
    }
}

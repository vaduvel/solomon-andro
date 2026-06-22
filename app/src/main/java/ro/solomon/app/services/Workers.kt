package ro.solomon.app.services

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ro.solomon.app.di.ServiceLocator
import ro.solomon.moments.MomentEngine
import ro.solomon.core.moments.MomentType
import ro.solomon.core.enablebanking.BankConnectionService

class DailyMomentWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        return runCatching {
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
                referenceDateEpochSeconds = System.currentTimeMillis() / 1000
            )
            val moment = ServiceLocator.momentEngine.generateBestMoment(snap)
            if (moment != null) {
                LastMomentStore.save(applicationContext, moment.llmResponse, moment.momentType.serialName())
                MomentHistoryStore.append(
                    ctx = applicationContext,
                    type = moment.momentType.serialName(),
                    title = moment.momentType.toTitle(),
                    body = moment.llmResponse,
                    nowEpochSeconds = System.currentTimeMillis() / 1000
                )
                MomentCooldownManager.recordShown(
                    type = moment.momentType.toCooldownType(),
                    nowEpoch = System.currentTimeMillis() / 1000
                )
                MomentNotifier.notifyMoment(applicationContext, moment.momentType, moment.llmResponse)
            }
        }.fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
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

fun MomentType.serialName(): String = when (this) {
    MomentType.spiralAlert -> "spiral_alert"
    MomentType.canIAfford -> "can_i_afford"
    MomentType.upcomingObligation -> "upcoming_obligation"
    MomentType.payday -> "payday"
    MomentType.patternAlert -> "pattern_alert"
    MomentType.subscriptionAudit -> "subscription_audit"
    MomentType.weeklySummary -> "weekly_summary"
    MomentType.wowMoment -> "wow_moment"
}

fun MomentType.toTitle(): String = when (this) {
    MomentType.spiralAlert -> "Risc de spirală detectat"
    MomentType.upcomingObligation -> "Obligație apropiată"
    MomentType.canIAfford -> "Îmi permit asta?"
    MomentType.payday -> "Salariu așteptat"
    MomentType.patternAlert -> "Pattern financiar"
    MomentType.subscriptionAudit -> "Audit abonamente"
    MomentType.weeklySummary -> "Sumar săptămânal"
    MomentType.wowMoment -> "Moment wow"
}

class HourlyIngestWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        return runCatching {
            // Real ingestion: pull the latest transactions from connected banks
            // (Enable Banking / Open Banking). Ingested transactions are persisted
            // via BankConnectionService.onTransactionIngested -> txnRepo.save, wired
            // in ServiceLocator. With no bank connected this is a safe no-op (0).
            BankConnectionService.syncAll()
        }.fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
    }
}

class ForecastRefreshWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        return runCatching {
            val txns = ServiceLocator.txnRepo.fetchAll()
            val cash = ServiceLocator.cashFlow.analyze(transactions = txns, referenceDate = System.currentTimeMillis())
            // nowEpoch is canonical epoch MILLIS (matches Transaction.date and CashFlow referenceDate).
            val result = ServiceLocator.forecastEngine.analyze(cashFlow = cash, transactions = txns, nowEpoch = System.currentTimeMillis())
            LastForecastStore.save(applicationContext, result.tip, result.riskLevel.name)
        }.fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
    }
}

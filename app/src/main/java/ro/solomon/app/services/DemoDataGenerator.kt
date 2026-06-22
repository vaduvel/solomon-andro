package ro.solomon.app.services

import ro.solomon.app.di.ServiceLocator
import ro.solomon.core.domain.*
import kotlin.random.Random

object DemoDataGenerator {

    suspend fun seedIfEmpty() {
        if (ServiceLocator.txnRepo.count() > 0) return
        // Transactions use canonical epoch MILLIS. Obligation/Goal dates below are
        // kept on the existing seconds basis (they are not used in calendar math).
        val nowMs = System.currentTimeMillis()
        val now = nowMs / 1000
        val txns = buildDemoTransactions(nowMs)
        txns.forEach { ServiceLocator.txnRepo.save(it) }

        val obligs = listOf(
            Obligation(
                id = "demo-chirie", name = "Chirie", amount = Money.fromLei(1500),
                dayOfMonth = 5, kind = ObligationKind.rent_mortgage,
                confidence = ObligationConfidence.declared, since = now, nextDueDate = now + 5L * 86400
            ),
            Obligation(
                id = "demo-internet", name = "Internet", amount = Money.fromLei(80),
                dayOfMonth = 15, kind = ObligationKind.utility,
                confidence = ObligationConfidence.declared, since = now, nextDueDate = now + 15L * 86400
            )
        )
        obligs.forEach { ServiceLocator.obligationRepo.save(it) }

        val goals = listOf(
            Goal(
                id = "demo-vacanta", kind = GoalKind.vacation, destination = "Vacanță vară",
                amountTarget = Money.fromLei(3000), amountSaved = Money.fromLei(450),
                deadline = now + 180L * 86400
            )
        )
        goals.forEach { ServiceLocator.goalRepo.save(it) }

        val subs = listOf(
            Subscription(
                id = "demo-netflix", name = "Netflix", amountMonthly = Money.fromLei(39),
                lastUsedDaysAgo = 14, cancellationDifficulty = CancellationDifficulty.medium,
                cancellationUrl = "https://netflix.com/cancel", cancellationStepsSummary = "Cont → Setări → Anulează",
                alternativeSuggestion = null, cancellationWarning = null
            ),
            Subscription(
                id = "demo-spotify", name = "Spotify", amountMonthly = Money.fromLei(29),
                lastUsedDaysAgo = 1, cancellationDifficulty = CancellationDifficulty.easy,
                cancellationUrl = null, cancellationStepsSummary = "App → Premium → Anulează",
                alternativeSuggestion = null, cancellationWarning = null
            )
        )
        subs.forEach { ServiceLocator.subRepo.save(it) }
    }

    private fun buildDemoTransactions(nowMs: Long): List<Transaction> {
        val merchants = listOf(
            "Kaufland" to TransactionCategory.food_grocery,
            "Lidl" to TransactionCategory.food_grocery,
            "Bolt" to TransactionCategory.transport,
            "eMAG" to TransactionCategory.shopping_online,
            "Netflix" to TransactionCategory.subscriptions,
            "Enel" to TransactionCategory.utilities,
            "Cinema City" to TransactionCategory.entertainment,
            "KFC" to TransactionCategory.food_dining
        )
        val rng = Random(42)
        val list = mutableListOf<Transaction>()
        for (day in 0..29) {
            val n = rng.nextInt(1, 4)
            repeat(n) {
                val (m, cat) = merchants.random(rng)
                val amount = Money.fromLei(rng.nextInt(15, 250))
                val isIncome = day % 14 == 0 && rng.nextDouble() < 0.5
                list.add(
                    Transaction(
                        id = "demo-${day}-${it}",
                        date = nowMs - day * 86_400_000L - rng.nextInt(0, 86_400_000),
                        amount = amount,
                        direction = if (isIncome) FlowDirection.incoming else FlowDirection.outgoing,
                        category = if (isIncome) TransactionCategory.unknown else cat,
                        merchant = if (isIncome) "Salariu" else m,
                        source = TransactionSource.csv_import,
                        categorizationConfidence = 0.95
                    )
                )
            }
        }
        return list
    }
}

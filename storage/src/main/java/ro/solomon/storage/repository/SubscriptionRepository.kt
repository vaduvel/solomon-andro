package ro.solomon.storage.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ro.solomon.core.domain.*
import ro.solomon.storage.dao.SubscriptionDao
import ro.solomon.storage.entity.SubscriptionEntity

class SubscriptionRepository(private val dao: SubscriptionDao) {

    fun observeAll(): Flow<List<Subscription>> = dao.observeAll().map { list -> list.mapNotNull { it.toDomain() } }

    suspend fun save(subscription: Subscription) {
        dao.upsert(subscription.toEntity())
    }

    suspend fun saveAll(subscriptions: List<Subscription>) {
        dao.upsertAll(subscriptions.map { it.toEntity() })
    }

    suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    suspend fun fetchAll(): List<Subscription> {
        return dao.fetchAll().mapNotNull { it.toDomain() }
    }

    suspend fun fetchGhosts(): List<Subscription> {
        return dao.fetchGhosts().mapNotNull { it.toDomain() }
    }

    suspend fun count(): Int = dao.count()
}

private fun Subscription.toEntity() = SubscriptionEntity(
    id = id,
    name = name,
    amountMonthlyRON = amountMonthly.amount.toLong(),
    lastUsedDaysAgo = lastUsedDaysAgo,
    cancellationDifficultyRaw = cancellationDifficulty.name,
    cancellationUrl = cancellationUrl,
    cancellationStepsSummary = cancellationStepsSummary,
    alternativeSuggestion = alternativeSuggestion,
    cancellationWarning = cancellationWarning
)

private fun SubscriptionEntity.toDomain(): Subscription? {
    val difficulty = try { CancellationDifficulty.valueOf(cancellationDifficultyRaw) } catch (_: Exception) { return null }

    return Subscription(
    id = id,
    name = name,
    amountMonthly = Money(amountMonthlyRON.toInt()),
    lastUsedDaysAgo = lastUsedDaysAgo,
    cancellationDifficulty = difficulty,
    cancellationUrl = cancellationUrl,
    cancellationStepsSummary = cancellationStepsSummary,
    alternativeSuggestion = alternativeSuggestion,
    cancellationWarning = cancellationWarning
    )
}

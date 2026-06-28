package ro.solomon.storage.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ro.solomon.core.domain.*
import ro.solomon.storage.dao.FocusDao
import ro.solomon.storage.entity.FocusEntity

class FocusRepository(private val dao: FocusDao) {

    fun observeAll(): Flow<List<Focus>> = dao.observeAll().map { list -> list.mapNotNull { it.toDomain() } }

    fun observeActive(): Flow<List<Focus>> = dao.observeActive().map { list -> list.mapNotNull { it.toDomain() } }

    suspend fun save(focus: Focus) {
        dao.upsert(focus.toEntity())
    }

    suspend fun saveAll(focuses: List<Focus>) {
        dao.upsertAll(focuses.map { it.toEntity() })
    }

    /** Make [id] the single primary focus (clears the flag on all others). */
    suspend fun setPrimary(id: String) {
        dao.setPrimary(id)
    }

    suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    suspend fun fetchAll(): List<Focus> = dao.fetchAll().mapNotNull { it.toDomain() }

    suspend fun fetchActive(): List<Focus> = dao.fetchActive().mapNotNull { it.toDomain() }

    suspend fun countActive(): Int = dao.countActive()
}

private fun Focus.toEntity() = FocusEntity(
    id = id,
    typeRaw = type.name,
    title = title,
    targetAmountRON = targetAmount.amount.toLong(),
    savedAmountRON = savedAmount.amount.toLong(),
    deadline = deadline,
    detoxPercent = detoxPercent,
    plannedMonthlyContributionRON = plannedMonthlyContribution.amount.toLong(),
    isPrimary = isPrimary,
    isActive = isActive,
    createdAt = createdAt
)

private fun FocusEntity.toDomain(): Focus? {
    val type = try { FocusType.valueOf(typeRaw) } catch (_: Exception) { return null }
    return Focus(
        id = id,
        type = type,
        title = title,
        targetAmount = Money(targetAmountRON.toInt()),
        savedAmount = Money(savedAmountRON.toInt()),
        deadline = deadline,
        detoxPercent = detoxPercent,
        plannedMonthlyContribution = Money(plannedMonthlyContributionRON.toInt()),
        isPrimary = isPrimary,
        isActive = isActive,
        createdAt = createdAt
    )
}

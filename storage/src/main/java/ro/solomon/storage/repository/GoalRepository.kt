package ro.solomon.storage.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ro.solomon.core.domain.*
import ro.solomon.storage.dao.GoalDao
import ro.solomon.storage.entity.GoalEntity

class GoalRepository(private val dao: GoalDao) {

    fun observeAll(): Flow<List<Goal>> = dao.observeAll().map { list -> list.mapNotNull { it.toDomain() } }

    suspend fun save(goal: Goal) {
        dao.upsert(goal.toEntity())
    }

    suspend fun saveAll(goals: List<Goal>) {
        dao.upsertAll(goals.map { it.toEntity() })
    }

    suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    suspend fun fetchAll(): List<Goal> {
        return dao.fetchAll().mapNotNull { it.toDomain() }
    }

    suspend fun fetchActive(): List<Goal> {
        return dao.fetchActive().mapNotNull { it.toDomain() }
    }

    suspend fun count(): Int = dao.count()
}

private fun Goal.toEntity() = GoalEntity(
    id = id,
    kindRaw = kind.name,
    destination = destination,
    amountTargetRON = amountTarget.amount.toLong(),
    amountSavedRON = amountSaved.amount.toLong(),
    deadline = deadline
)

private fun GoalEntity.toDomain(): Goal? {
    val kind = try { GoalKind.valueOf(kindRaw) } catch (_: Exception) { return null }
    if (amountTargetRON <= 0) return null

    return Goal(
        id = id,
        kind = kind,
        destination = destination,
        amountTarget = Money(amountTargetRON.toInt()),
        amountSaved = Money(amountSavedRON.toInt()),
        deadline = deadline
    )
}

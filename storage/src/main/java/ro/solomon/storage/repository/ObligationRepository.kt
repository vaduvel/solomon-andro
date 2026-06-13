package ro.solomon.storage.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ro.solomon.core.domain.*
import ro.solomon.storage.dao.ObligationDao
import ro.solomon.storage.entity.ObligationEntity

class ObligationRepository(private val dao: ObligationDao) {

    fun observeAll(): Flow<List<Obligation>> = dao.observeAll().map { list -> list.mapNotNull { it.toDomain() } }

    suspend fun save(obligation: Obligation) {
        dao.upsert(obligation.toEntity())
    }

    suspend fun saveAll(obligations: List<Obligation>) {
        dao.upsertAll(obligations.map { it.toEntity() })
    }

    suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    suspend fun fetchAll(): List<Obligation> {
        return dao.fetchAll().mapNotNull { it.toDomain() }
    }

    suspend fun fetchByKind(kind: ObligationKind): List<Obligation> {
        return dao.fetchByKind(kind.name).mapNotNull { it.toDomain() }
    }

    suspend fun fetchDebts(): List<Obligation> {
        val debtKinds = listOf("loan_bank", "loan_ifn", "bnpl")
        return dao.fetchDebts(debtKinds).mapNotNull { it.toDomain() }
    }

    suspend fun count(): Int = dao.count()
}

private fun Obligation.toEntity() = ObligationEntity(
    id = id,
    name = name,
    amountRON = amount.amount.toLong(),
    dayOfMonth = dayOfMonth,
    kindRaw = kind.name,
    confidenceRaw = confidence.name,
    since = since,
    nextDueDate = nextDueDate
)

private fun ObligationEntity.toDomain(): Obligation? {
    val kind = try { ObligationKind.valueOf(kindRaw) } catch (_: Exception) { return null }
    val confidence = try { ObligationConfidence.valueOf(confidenceRaw) } catch (_: Exception) { return null }
    if (dayOfMonth !in 1..31) return null

    return Obligation(
        id = id,
        name = name,
        amount = Money(amountRON.toInt()),
        dayOfMonth = dayOfMonth,
        kind = kind,
        confidence = confidence,
        since = since,
        nextDueDate = nextDueDate
    )
}

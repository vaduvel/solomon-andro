package ro.solomon.storage.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ro.solomon.core.domain.*
import ro.solomon.storage.dao.TransactionDao
import ro.solomon.storage.entity.TransactionEntity

class TransactionRepository(private val dao: TransactionDao) {

    fun observeAll(): Flow<List<Transaction>> = dao.observeAll().map { list -> list.mapNotNull { it.toDomain() } }

    suspend fun save(transaction: Transaction) {
        dao.upsert(transaction.toEntity())
    }

    suspend fun saveAll(transactions: List<Transaction>) {
        dao.upsertAll(transactions.map { it.toEntity() })
    }

    suspend fun delete(id: String) {
        dao.deleteById(id)
    }

    suspend fun fetchAll(): List<Transaction> = dao.fetchAll().mapNotNull { it.toDomain() }

    suspend fun fetchRange(startDate: Long, endDate: Long): List<Transaction> =
        dao.fetchRange(startDate, endDate).mapNotNull { it.toDomain() }

    suspend fun fetchByCategory(category: TransactionCategory): List<Transaction> =
        dao.fetchByCategory(category.name).mapNotNull { it.toDomain() }

    suspend fun fetchRecent(limit: Int): List<Transaction> =
        dao.fetchRecent(limit).mapNotNull { it.toDomain() }

    suspend fun count(): Int = dao.count()

    suspend fun deleteAll() = dao.deleteAll()
}

private fun Transaction.toEntity() = TransactionEntity(
    id = id,
    date = date,
    amountRON = amount.amount.toLong(),
    directionRaw = direction.name,
    categoryRaw = category.name,
    merchant = merchant,
    txDescription = description,
    sourceRaw = source.name,
    categorizationConfidence = categorizationConfidence
)

private fun TransactionEntity.toDomain(): Transaction? {
    val direction = try { FlowDirection.valueOf(directionRaw) } catch (_: Exception) { return null }
    val category = try { TransactionCategory.valueOf(categoryRaw) } catch (_: Exception) { return null }
    val source = try { TransactionSource.valueOf(sourceRaw) } catch (_: Exception) { return null }

    return Transaction(
        id = id,
        date = date,
        amount = Money(amountRON.toInt()),
        direction = direction,
        category = category,
        merchant = merchant,
        description = txDescription,
        source = source,
        categorizationConfidence = categorizationConfidence
    )
}

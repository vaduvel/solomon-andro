package ro.solomon.storage.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ro.solomon.storage.entity.TransactionEntity

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    suspend fun fetchAll(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    suspend fun fetchRange(startDate: Long, endDate: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE category_raw = :category ORDER BY date DESC")
    suspend fun fetchByCategory(category: String): List<TransactionEntity>

    @Query("SELECT * FROM transactions ORDER BY date DESC LIMIT :limit")
    suspend fun fetchRecent(limit: Int): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(transaction: TransactionEntity)

    @Upsert
    suspend fun upsertAll(transactions: List<TransactionEntity>)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}

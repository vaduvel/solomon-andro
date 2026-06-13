package ro.solomon.storage.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ro.solomon.storage.entity.ObligationEntity

@Dao
interface ObligationDao {
    @Query("SELECT * FROM obligations ORDER BY day_of_month ASC")
    fun observeAll(): Flow<List<ObligationEntity>>

    @Query("SELECT * FROM obligations ORDER BY day_of_month ASC")
    suspend fun fetchAll(): List<ObligationEntity>

    @Query("SELECT * FROM obligations WHERE kind_raw = :kind ORDER BY day_of_month ASC")
    suspend fun fetchByKind(kind: String): List<ObligationEntity>

    @Query("SELECT * FROM obligations WHERE kind_raw IN (:debtKinds) ORDER BY amount_ron DESC")
    suspend fun fetchDebts(debtKinds: List<String>): List<ObligationEntity>

    @Query("SELECT COUNT(*) FROM obligations")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(obligation: ObligationEntity)

    @Upsert
    suspend fun upsertAll(obligations: List<ObligationEntity>)

    @Query("DELETE FROM obligations WHERE id = :id")
    suspend fun deleteById(id: String)
}

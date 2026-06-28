package ro.solomon.storage.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ro.solomon.storage.entity.FocusEntity

@Dao
interface FocusDao {
    @Query("SELECT * FROM focuses ORDER BY is_primary DESC, created_at ASC")
    fun observeAll(): Flow<List<FocusEntity>>

    @Query("SELECT * FROM focuses WHERE is_active = 1 ORDER BY is_primary DESC, created_at ASC")
    fun observeActive(): Flow<List<FocusEntity>>

    @Query("SELECT * FROM focuses ORDER BY is_primary DESC, created_at ASC")
    suspend fun fetchAll(): List<FocusEntity>

    @Query("SELECT * FROM focuses WHERE is_active = 1 ORDER BY is_primary DESC, created_at ASC")
    suspend fun fetchActive(): List<FocusEntity>

    @Query("SELECT COUNT(*) FROM focuses WHERE is_active = 1")
    suspend fun countActive(): Int

    @Upsert
    suspend fun upsert(focus: FocusEntity)

    @Upsert
    suspend fun upsertAll(focuses: List<FocusEntity>)

    @Query("UPDATE focuses SET is_primary = 0")
    suspend fun clearPrimary()

    @Query("UPDATE focuses SET is_primary = 1 WHERE id = :id")
    suspend fun markPrimary(id: String)

    /** Atomically make a single focus the primary one. */
    @Transaction
    suspend fun setPrimary(id: String) {
        clearPrimary()
        markPrimary(id)
    }

    @Query("DELETE FROM focuses WHERE id = :id")
    suspend fun deleteById(id: String)
}

package ro.solomon.storage.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ro.solomon.storage.entity.GoalEntity

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY deadline ASC")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals ORDER BY deadline ASC")
    suspend fun fetchAll(): List<GoalEntity>

    @Query("SELECT * FROM goals WHERE amount_saved_ron < amount_target_ron ORDER BY deadline ASC")
    suspend fun fetchActive(): List<GoalEntity>

    @Query("SELECT COUNT(*) FROM goals")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(goal: GoalEntity)

    @Upsert
    suspend fun upsertAll(goals: List<GoalEntity>)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteById(id: String)
}

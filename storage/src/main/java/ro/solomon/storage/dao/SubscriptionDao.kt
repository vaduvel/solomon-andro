package ro.solomon.storage.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ro.solomon.storage.entity.SubscriptionEntity

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY amount_monthly_ron DESC")
    fun observeAll(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions ORDER BY amount_monthly_ron DESC")
    suspend fun fetchAll(): List<SubscriptionEntity>

    @Query("SELECT * FROM subscriptions WHERE last_used_days_ago IS NOT NULL AND last_used_days_ago > 30 ORDER BY amount_monthly_ron DESC")
    suspend fun fetchGhosts(): List<SubscriptionEntity>

    @Query("SELECT COUNT(*) FROM subscriptions")
    suspend fun count(): Int

    @Upsert
    suspend fun upsert(subscription: SubscriptionEntity)

    @Upsert
    suspend fun upsertAll(subscriptions: List<SubscriptionEntity>)

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun deleteById(id: String)
}

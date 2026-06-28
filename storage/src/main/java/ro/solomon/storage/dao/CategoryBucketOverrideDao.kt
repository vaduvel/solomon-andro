package ro.solomon.storage.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import ro.solomon.storage.entity.CategoryBucketOverrideEntity

@Dao
interface CategoryBucketOverrideDao {
    @Query("SELECT * FROM category_bucket_overrides")
    fun observeAll(): Flow<List<CategoryBucketOverrideEntity>>

    @Query("SELECT * FROM category_bucket_overrides")
    suspend fun fetchAll(): List<CategoryBucketOverrideEntity>

    @Upsert
    suspend fun upsert(override: CategoryBucketOverrideEntity)

    @Query("DELETE FROM category_bucket_overrides WHERE category_raw = :categoryRaw")
    suspend fun deleteByCategory(categoryRaw: String)

    @Query("DELETE FROM category_bucket_overrides")
    suspend fun clear()
}

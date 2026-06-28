package ro.solomon.storage.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ro.solomon.core.domain.SolBucket
import ro.solomon.core.domain.TransactionCategory
import ro.solomon.storage.dao.CategoryBucketOverrideDao
import ro.solomon.storage.entity.CategoryBucketOverrideEntity

/**
 * Stores user overrides of the category -> bucket mapping used by FocusEngine.
 * Returns a sparse map (only explicitly overridden categories); FocusEngine
 * falls back to SolBucket.defaultFor(category) for everything else.
 */
class CategoryBucketOverrideRepository(private val dao: CategoryBucketOverrideDao) {

    fun observeOverrides(): Flow<Map<TransactionCategory, SolBucket>> =
        dao.observeAll().map { list -> list.toDomainMap() }

    suspend fun fetchOverrides(): Map<TransactionCategory, SolBucket> =
        dao.fetchAll().toDomainMap()

    suspend fun setOverride(category: TransactionCategory, bucket: SolBucket) {
        dao.upsert(CategoryBucketOverrideEntity(category.name, bucket.name))
    }

    suspend fun clearOverride(category: TransactionCategory) {
        dao.deleteByCategory(category.name)
    }

    suspend fun clearAll() {
        dao.clear()
    }
}

private fun List<CategoryBucketOverrideEntity>.toDomainMap(): Map<TransactionCategory, SolBucket> =
    mapNotNull { row ->
        val category = try { TransactionCategory.valueOf(row.categoryRaw) } catch (_: Exception) { return@mapNotNull null }
        val bucket = try { SolBucket.valueOf(row.bucketRaw) } catch (_: Exception) { return@mapNotNull null }
        category to bucket
    }.toMap()

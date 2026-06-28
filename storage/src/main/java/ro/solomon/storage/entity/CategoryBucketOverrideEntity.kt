package ro.solomon.storage.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A user override that re-assigns a [ro.solomon.core.domain.TransactionCategory]
 * to a specific [ro.solomon.core.domain.SolBucket].
 *
 * Only categories the user has explicitly re-bucketed are stored here; every
 * other category falls back to SolBucket.defaultFor(category).
 */
@Entity(tableName = "category_bucket_overrides")
data class CategoryBucketOverrideEntity(
    @PrimaryKey @ColumnInfo(name = "category_raw") val categoryRaw: String,
    @ColumnInfo(name = "bucket_raw") val bucketRaw: String
)

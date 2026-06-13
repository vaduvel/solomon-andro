package ro.solomon.storage.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "amount_monthly_ron") val amountMonthlyRON: Long,
    @ColumnInfo(name = "last_used_days_ago") val lastUsedDaysAgo: Int? = null,
    @ColumnInfo(name = "cancellation_difficulty_raw") val cancellationDifficultyRaw: String = "medium",
    @ColumnInfo(name = "cancellation_url") val cancellationUrl: String? = null,
    @ColumnInfo(name = "cancellation_steps_summary") val cancellationStepsSummary: String? = null,
    @ColumnInfo(name = "alternative_suggestion") val alternativeSuggestion: String? = null,
    @ColumnInfo(name = "cancellation_warning") val cancellationWarning: String? = null
)

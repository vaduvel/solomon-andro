package ro.solomon.storage.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room persistence for a Solomon Focus.
 *
 * Monetary columns store BANI (minor units), consistent with the rest of the
 * schema (see SolomonDatabase v2 note). Domain <-> entity mapping lives in
 * FocusRepository.
 */
@Entity(tableName = "focuses")
data class FocusEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "type_raw") val typeRaw: String,
    val title: String,
    @ColumnInfo(name = "target_amount_ron") val targetAmountRON: Long = 0,
    @ColumnInfo(name = "saved_amount_ron") val savedAmountRON: Long = 0,
    val deadline: Long? = null,
    @ColumnInfo(name = "detox_percent") val detoxPercent: Int? = null,
    @ColumnInfo(name = "planned_monthly_contribution_ron") val plannedMonthlyContributionRON: Long = 0,
    @ColumnInfo(name = "is_primary") val isPrimary: Boolean = false,
    @ColumnInfo(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "created_at") val createdAt: Long
)

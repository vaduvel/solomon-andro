package ro.solomon.storage.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "kind_raw") val kindRaw: String,
    val destination: String? = null,
    @ColumnInfo(name = "amount_target_ron") val amountTargetRON: Long,
    @ColumnInfo(name = "amount_saved_ron") val amountSavedRON: Long = 0,
    val deadline: Long
)

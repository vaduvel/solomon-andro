package ro.solomon.storage.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "obligations")
data class ObligationEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "amount_ron") val amountRON: Long,
    @ColumnInfo(name = "day_of_month") val dayOfMonth: Int,
    @ColumnInfo(name = "kind_raw") val kindRaw: String,
    @ColumnInfo(name = "confidence_raw") val confidenceRaw: String,
    val since: Long? = null,
    @ColumnInfo(name = "next_due_date") val nextDueDate: Long? = null
)

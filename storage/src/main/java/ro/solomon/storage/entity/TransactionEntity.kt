package ro.solomon.storage.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey val id: String,
    val date: Long,
    @ColumnInfo(name = "amount_ron") val amountRON: Long,
    @ColumnInfo(name = "direction_raw") val directionRaw: String,
    @ColumnInfo(name = "category_raw") val categoryRaw: String,
    val merchant: String? = null,
    @ColumnInfo(name = "tx_description") val txDescription: String? = null,
    @ColumnInfo(name = "source_raw") val sourceRaw: String,
    @ColumnInfo(name = "categorization_confidence") val categorizationConfidence: Double = 1.0
)

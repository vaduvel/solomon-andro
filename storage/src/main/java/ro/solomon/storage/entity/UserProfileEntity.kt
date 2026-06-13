package ro.solomon.storage.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    @ColumnInfo(name = "addressing_raw") val addressingRaw: String = "tu",
    @ColumnInfo(name = "age_range_raw") val ageRangeRaw: String = "25-35",
    @ColumnInfo(name = "salary_range_raw") val salaryRangeRaw: String = "3-5k",
    @ColumnInfo(name = "salary_freq_type") val salaryFreqType: String = "variable",
    @ColumnInfo(name = "salary_freq_day1") val salaryFreqDay1: Int = 0,
    @ColumnInfo(name = "salary_freq_day2") val salaryFreqDay2: Int = 0,
    @ColumnInfo(name = "has_secondary_income") val hasSecondaryIncome: Boolean = false,
    @ColumnInfo(name = "secondary_income_ron") val secondaryIncomeRON: Long? = null,
    @ColumnInfo(name = "primary_bank_raw") val primaryBankRaw: String = "Other",
    @ColumnInfo(name = "email_access_granted") val emailAccessGranted: Boolean = false,
    @ColumnInfo(name = "notifications_granted") val notificationsGranted: Boolean = false,
    @ColumnInfo(name = "dataset_opt_in") val datasetOptIn: Boolean = false,
    @ColumnInfo(name = "onboarding_complete") val onboardingComplete: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

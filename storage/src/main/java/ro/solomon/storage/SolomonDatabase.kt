package ro.solomon.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import ro.solomon.storage.dao.*
import ro.solomon.storage.entity.*

@Database(
    entities = [
        TransactionEntity::class,
        ObligationEntity::class,
        SubscriptionEntity::class,
        GoalEntity::class,
        UserProfileEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SolomonDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun obligationDao(): ObligationDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun goalDao(): GoalDao
    abstract fun userProfileDao(): UserProfileDao

    companion object {
        @Volatile
        private var INSTANCE: SolomonDatabase? = null

        fun getInstance(context: Context): SolomonDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SolomonDatabase::class.java,
                    "solomon.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }

        fun createInMemory(context: Context): SolomonDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                SolomonDatabase::class.java
            ).build()
        }
    }
}

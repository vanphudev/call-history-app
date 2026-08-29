package com.antimobile.mcas.data.messaging.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        MessageDraftEntity::class,
        ConversationPreferenceEntity::class,
        SmsPartAttemptEntity::class,
        InboundFingerprintEntity::class,
        MmsTransferEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class MessagingDatabase : RoomDatabase() {
    abstract fun messagingDao(): MessagingDao

    companion object {
        const val FILE_NAME = "mcas-messaging-private.db"

        @Volatile private var instance: MessagingDatabase? = null

        fun get(context: Context): MessagingDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                MessagingDatabase::class.java,
                FILE_NAME,
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `mms_transfers` (
                        `providerId` INTEGER NOT NULL,
                        `transactionId` TEXT NOT NULL,
                        `threadId` INTEGER NOT NULL,
                        `address` TEXT NOT NULL,
                        `contentLocation` TEXT NOT NULL,
                        `subscriptionId` INTEGER,
                        `direction` TEXT NOT NULL,
                        `state` TEXT NOT NULL,
                        `tempFileName` TEXT,
                        `attemptCount` INTEGER NOT NULL,
                        `resultCode` INTEGER,
                        `httpStatus` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`providerId`)
                    )""".trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_mms_transfers_transactionId` ON `mms_transfers` (`transactionId`)")
            }
        }
    }
}

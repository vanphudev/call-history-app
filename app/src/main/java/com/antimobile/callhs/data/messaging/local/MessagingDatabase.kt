package com.antimobile.callhs.data.messaging.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MessageDraftEntity::class,
        ConversationPreferenceEntity::class,
        SmsPartAttemptEntity::class,
        InboundFingerprintEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class MessagingDatabase : RoomDatabase() {
    abstract fun messagingDao(): MessagingDao

    companion object {
        const val FILE_NAME = "callhs-messaging-private.db"

        @Volatile private var instance: MessagingDatabase? = null

        fun get(context: Context): MessagingDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                MessagingDatabase::class.java,
                FILE_NAME,
            ).build().also { instance = it }
        }
    }
}


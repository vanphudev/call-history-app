package com.antimobile.mcas.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.antimobile.mcas.data.blocking.CallBlockDao
import com.antimobile.mcas.data.blocking.CallBlockHistoryEntity
import com.antimobile.mcas.data.blocking.CallBlockNumberEntryEntity
import com.antimobile.mcas.data.blocking.CallBlockRuleEntity

/**
 * Room DB cục bộ cho toàn bộ dữ liệu do MCAS tự quản. Call Log và Danh bạ hệ thống vẫn chỉ được
 * đọc trực tiếp, không sao chép vào database này.
 *
 * Đây là schema phát triển đầu tiên của ứng dụng. Trước khi public, thay đổi cấu trúc được cập nhật
 * trực tiếp vào version 1 và dữ liệu cài thử được tạo mới; chưa duy trì migration lịch sử.
 */
@Database(
    entities = [
        CategoryEntity::class,
        CategoryMemberEntity::class,
        CallBlockRuleEntity::class,
        CallBlockNumberEntryEntity::class,
        CallBlockHistoryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun callBlockDao(): CallBlockDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mcas.db",
                )
                    .build()
                    .also { instance = it }
            }
    }
}

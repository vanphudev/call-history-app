package com.antimobile.callhs.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Room DB cục bộ (KHÔNG mã hoá) cho dữ liệu người dùng tự tạo — hiện chỉ có nhóm phân loại số điện thoại.
 * Nhật ký cuộc gọi & danh bạ VẪN đọc read-only từ hệ thống, không đưa vào DB này.
 *
 * version 1, exportSchema=true (schema xuất ra app/schemas → về sau đổi version phải viết Migration
 * thật, KHÔNG fallbackToDestructiveMigration để không mất dữ liệu người dùng khi cập nhật app).
 */
@Database(
    entities = [CategoryEntity::class, CategoryMemberEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "callhs.db",
                ).build().also { instance = it }
            }
    }
}

package com.antimobile.callhs.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Một nhóm phân loại số điện thoại. Tối đa 5 nhóm; 2 nhóm mặc định (Công việc / Yêu thích) có
 * [builtInKey] != null → KHÔNG xoá được, tên hiển thị lấy từ i18n theo [builtInKey] (không dịch tên
 * nhóm do người dùng tự đặt).
 *
 * [colorArgb] là màu người dùng chọn (dữ liệu người dùng, dùng cho cả sáng/tối), lưu dạng ARGB Long.
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val description: String = "",
    val iconKey: String,
    val colorArgb: Long,
    val builtInKey: String? = null,   // "work" | "favorite" | null (user)
    val sortOrder: Int = 0,
    val createdAt: Long = 0L,
)

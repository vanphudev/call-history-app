package com.antimobile.callhs.data.local

/**
 * UI model cho một nhóm phân loại (tách khỏi entity Room). [memberCount] = số điện thoại đang thuộc nhóm.
 * Nhóm mặc định ([builtInKey] != null) không xoá được; tên hiển thị lấy từ i18n theo builtInKey.
 */
data class Category(
    val id: Long,
    val name: String,
    val description: String,
    val iconKey: String,
    val colorArgb: Long,
    val builtInKey: String?,
    val memberCount: Int = 0,
) {
    val isBuiltIn: Boolean get() = builtInKey != null
}

/** Badge nhỏ vẽ ở góc dưới-phải avatar: icon + màu của nhóm mà một số điện thoại đang thuộc. */
data class CategoryBadge(
    val categoryId: Long,
    val iconKey: String,
    val colorArgb: Long,
)

/** Một số điện thoại trong nhóm + thời điểm được thêm. */
data class CategoryMember(
    val rawNumber: String,
    val phoneKey: String,
    val addedAt: Long,
)

/** Kết quả thêm số vào nhóm. */
enum class AddMemberResult { ADDED, ALREADY, FULL, INVALID }

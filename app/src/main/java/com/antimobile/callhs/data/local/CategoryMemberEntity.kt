package com.antimobile.callhs.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Một số điện thoại thuộc một nhóm ([categoryId]) kèm thời điểm thêm ([addedAt]).
 *
 * [rawNumber] = số như người dùng thêm (để hiển thị / mở lịch sử); [phoneKey] = 9 số cuối (khoá so
 * khớp, xem [com.antimobile.callhs.util.PhoneKey]). Unique index (categoryId, phoneKey) chống thêm
 * trùng; FK CASCADE → xoá nhóm sẽ xoá sạch thành viên.
 */
@Entity(
    tableName = "category_members",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        Index(value = ["categoryId", "phoneKey"], unique = true),
        Index(value = ["phoneKey"]),
        Index(value = ["categoryId"]),
    ]
)
data class CategoryMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val categoryId: Long,
    val rawNumber: String,
    val phoneKey: String,
    val addedAt: Long,
)

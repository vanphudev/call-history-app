package com.antimobile.mcas.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    // --- Categories ---
    // Nhóm mặc định (builtInKey != null) luôn đứng trước, rồi tới sortOrder, rồi id.
    @Query("SELECT * FROM categories ORDER BY (builtInKey IS NULL), sortOrder, id")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY (builtInKey IS NULL), sortOrder, id")
    suspend fun getCategories(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategory(id: Long): CategoryEntity?

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun categoryCount(): Int

    @Query("SELECT COUNT(*) FROM categories WHERE builtInKey = :key")
    suspend fun builtInCount(key: String): Int

    @Insert
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    /** Chỉ xoá nhóm do người dùng tạo (builtInKey IS NULL). @return số dòng bị xoá (0 nếu là nhóm mặc định). */
    @Query("DELETE FROM categories WHERE id = :id AND builtInKey IS NULL")
    suspend fun deleteUserCategory(id: Long): Int

    /** Xoá MỌI nhóm do người dùng tạo (thành viên tự CASCADE) — dùng cho khôi phục kiểu "Ghi đè toàn bộ". */
    @Query("DELETE FROM categories WHERE builtInKey IS NULL")
    suspend fun deleteAllUserCategories(): Int

    // --- Members ---
    @Query("SELECT * FROM category_members WHERE categoryId = :categoryId ORDER BY addedAt DESC, id DESC")
    fun observeMembers(categoryId: Long): Flow<List<CategoryMemberEntity>>

    @Query("SELECT * FROM category_members")
    fun observeAllMembers(): Flow<List<CategoryMemberEntity>>

    @Query("SELECT * FROM category_members")
    suspend fun getAllMembers(): List<CategoryMemberEntity>

    @Query("UPDATE category_members SET phoneKey = :phoneKey WHERE id = :id")
    suspend fun updateMemberKey(id: Long, phoneKey: String)

    @Query("DELETE FROM category_members WHERE id = :id")
    suspend fun deleteMemberById(id: Long)

    /** Xoá mọi thành viên của một nhóm (dùng khi "Ghi đè toàn bộ" cho nhóm mặc định — nhóm không bị xoá). */
    @Query("DELETE FROM category_members WHERE categoryId = :categoryId")
    suspend fun deleteMembersOf(categoryId: Long)

    @Query("SELECT COUNT(*) FROM category_members WHERE categoryId = :categoryId")
    suspend fun memberCount(categoryId: Long): Int

    @Query("SELECT COUNT(*) FROM category_members WHERE categoryId = :categoryId AND phoneKey = :phoneKey")
    suspend fun memberExists(categoryId: Long, phoneKey: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMember(member: CategoryMemberEntity): Long

    @Query("DELETE FROM category_members WHERE categoryId = :categoryId AND phoneKey = :phoneKey")
    suspend fun deleteMember(categoryId: Long, phoneKey: String)
}

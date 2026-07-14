package com.antimobile.callhs.data.local

import android.content.Context
import com.antimobile.callhs.util.PhoneKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Cửa ngõ DUY NHẤT tới dữ liệu nhóm phân loại. Đóng gói cap (tối đa [MAX_CATEGORIES] nhóm,
 * [MAX_MEMBERS] số/nhóm) và việc seed 2 nhóm mặc định. Mọi hàm ghi là suspend (gọi off-main).
 */
class CategoryRepository(context: Context) {

    private val dao = AppDatabase.get(context).categoryDao()

    // --- Reactive reads ---

    /** Danh sách nhóm kèm memberCount, đã sắp xếp (mặc định trước). */
    fun observeCategories(): Flow<List<Category>> =
        combine(dao.observeCategories(), dao.observeAllMembers()) { cats, members ->
            val counts = members.groupingBy { it.categoryId }.eachCount()
            cats.map { it.toModel(counts[it.id] ?: 0) }
        }

    /** Thành viên (số điện thoại) của một nhóm, mới thêm trước. */
    fun observeMembers(categoryId: Long): Flow<List<CategoryMember>> =
        dao.observeMembers(categoryId).map { list ->
            list.map { CategoryMember(it.rawNumber, it.phoneKey, it.addedAt) }
        }

    /** phoneKey -> các badge của nhóm mà số đó thuộc (cho avatar toàn app). */
    fun badgeFlow(): Flow<Map<String, List<CategoryBadge>>> =
        combine(dao.observeCategories(), dao.observeAllMembers()) { cats, members ->
            val byId = cats.associateBy { it.id }
            members.groupBy { it.phoneKey }.mapValues { (_, ms) ->
                ms.mapNotNull { m -> byId[m.categoryId]?.let { CategoryBadge(it.id, it.iconKey, it.colorArgb) } }
            }
        }

    /** categoryId -> tập phoneKey thành viên (cho bộ lọc CallListScreen). */
    fun membersByCategoryFlow(): Flow<Map<Long, Set<String>>> =
        dao.observeAllMembers().map { members ->
            members.groupBy { it.categoryId }.mapValues { (_, ms) -> ms.mapTo(HashSet()) { it.phoneKey } }
        }

    // --- One-shot reads ---

    suspend fun getCategory(id: Long): Category? =
        dao.getCategory(id)?.toModel(dao.memberCount(id))

    suspend fun categoryCount(): Int = dao.categoryCount()

    suspend fun memberCount(categoryId: Long): Int = dao.memberCount(categoryId)

    suspend fun isMember(categoryId: Long, number: String): Boolean =
        dao.memberExists(categoryId, PhoneKey.of(number)) > 0

    // --- Writes ---

    /** Seed 2 nhóm mặc định nếu chưa có. Idempotent — gọi 1 lần lúc app khởi động. */
    suspend fun ensureSeeded() {
        if (dao.builtInCount(BUILTIN_WORK) == 0) {
            dao.insertCategory(
                CategoryEntity(
                    name = "Công việc", iconKey = WORK_ICON, colorArgb = WORK_COLOR,
                    builtInKey = BUILTIN_WORK, sortOrder = 0, createdAt = System.currentTimeMillis(),
                )
            )
        }
        if (dao.builtInCount(BUILTIN_FAVORITE) == 0) {
            dao.insertCategory(
                CategoryEntity(
                    name = "Yêu thích", iconKey = FAVORITE_ICON, colorArgb = FAVORITE_COLOR,
                    builtInKey = BUILTIN_FAVORITE, sortOrder = 1, createdAt = System.currentTimeMillis(),
                )
            )
        }
    }

    /** @return id nhóm mới, hoặc null nếu đã đạt tối đa [MAX_CATEGORIES]. */
    suspend fun createCategory(name: String, description: String, iconKey: String, colorArgb: Long): Long? {
        if (dao.categoryCount() >= MAX_CATEGORIES) return null
        val maxSort = dao.getCategories().maxOfOrNull { it.sortOrder } ?: 1
        return dao.insertCategory(
            CategoryEntity(
                name = name.trim(), description = description.trim(), iconKey = iconKey,
                colorArgb = colorArgb, builtInKey = null, sortOrder = maxSort + 1,
                createdAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun updateCategory(id: Long, name: String, description: String, iconKey: String, colorArgb: Long) {
        val existing = dao.getCategory(id) ?: return
        dao.updateCategory(
            existing.copy(name = name.trim(), description = description.trim(), iconKey = iconKey, colorArgb = colorArgb)
        )
    }

    /** @return true nếu đã xoá (nhóm người dùng); false nếu là nhóm mặc định (không xoá). */
    suspend fun deleteCategory(id: Long): Boolean = dao.deleteUserCategory(id) > 0

    suspend fun addMember(categoryId: Long, rawNumber: String): AddMemberResult {
        val key = PhoneKey.of(rawNumber)
        if (key.isEmpty()) return AddMemberResult.INVALID
        if (dao.memberExists(categoryId, key) > 0) return AddMemberResult.ALREADY
        if (dao.memberCount(categoryId) >= MAX_MEMBERS) return AddMemberResult.FULL
        dao.insertMember(
            CategoryMemberEntity(
                categoryId = categoryId, rawNumber = rawNumber, phoneKey = key,
                addedAt = System.currentTimeMillis(),
            )
        )
        return AddMemberResult.ADDED
    }

    suspend fun removeMember(categoryId: Long, phoneKey: String) = dao.deleteMember(categoryId, phoneKey)

    private fun CategoryEntity.toModel(memberCount: Int) =
        Category(id, name, description, iconKey, colorArgb, builtInKey, memberCount)

    companion object {
        const val MAX_CATEGORIES = 5
        const val MAX_MEMBERS = 100

        const val BUILTIN_WORK = "work"
        const val BUILTIN_FAVORITE = "favorite"

        // Icon/màu mặc định cho 2 nhóm built-in.
        private const val WORK_ICON = "work"
        private const val FAVORITE_ICON = "favorite"
        private const val WORK_COLOR = 0xFF2E86E0L      // blue
        private const val FAVORITE_COLOR = 0xFFE5484DL  // red
    }
}

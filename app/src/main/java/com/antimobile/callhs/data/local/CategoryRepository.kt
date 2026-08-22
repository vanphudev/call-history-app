package com.antimobile.callhs.data.local

import android.content.Context
import androidx.room.withTransaction
import com.antimobile.callhs.data.backup.BackupCategory
import com.antimobile.callhs.data.backup.BackupMember
import com.antimobile.callhs.data.backup.MergeMode
import com.antimobile.callhs.data.backup.SectionResult
import com.antimobile.callhs.util.PhoneKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Cửa ngõ DUY NHẤT tới dữ liệu nhóm phân loại. Đóng gói cap (tối đa [MAX_CATEGORIES] nhóm,
 * [MAX_MEMBERS] số/nhóm) và việc seed 2 nhóm mặc định. Mọi hàm ghi là suspend (gọi off-main).
 */
class CategoryRepository(context: Context) {

    private val db = AppDatabase.get(context)
    private val dao = db.categoryDao()

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

    /**
     * Chuẩn hoá lại [CategoryMemberEntity.phoneKey] theo thuật toán [PhoneKey] hiện hành từ [rawNumber] đã lưu.
     * Cần vì [PhoneKey.of] đổi cách tính cho số CỐ ĐỊNH (NSN thay vì "9 số cuối"): thành viên là số cố định lưu
     * theo bản cũ sẽ không còn khớp badge/bộ lọc nếu không tính lại. Idempotent (chạy lại → không đổi gì). Với DI
     * ĐỘNG khoá không đổi nên đa số dòng bỏ qua. Trùng khoá trong cùng nhóm sau chuẩn hoá → bỏ bản dư (unique index).
     */
    suspend fun renormalizeMemberKeys() {
        for (m in dao.getAllMembers()) {
            val newKey = PhoneKey.of(m.rawNumber)
            if (newKey.isEmpty() || newKey == m.phoneKey) continue
            if (runCatching { dao.updateMemberKey(m.id, newKey) }.isFailure) dao.deleteMemberById(m.id)
        }
    }

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
        // Chuẩn hoá lại khoá thành viên theo thuật toán PhoneKey mới (chủ yếu cho số cố định) — idempotent.
        renormalizeMemberKeys()
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
        // Loại số ẩn danh/đặc biệt của nhật ký ("-1"/"-2"/"-3"): filter chữ số biến "-1"→"1" nên KHÔNG rỗng,
        // nếu để lọt sẽ badge/filter mọi cuộc private (và cả số thật có NSN "1"). Chặn giống lookupContact.
        val clean = rawNumber.trim()
        if (clean.isEmpty() || clean.startsWith("-")) return AddMemberResult.INVALID
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

    /** Gỡ số khỏi nhóm. Nhận số ở BẤT KỲ dạng nào ([number] thô hoặc khoá) — repo tự chuẩn hoá về khoá, nên
     *  caller không thể vô tình truyền số chưa chuẩn hoá làm xoá hụt (PhoneKey.of của một khoá = chính nó). */
    suspend fun removeMember(categoryId: Long, number: String) = dao.deleteMember(categoryId, PhoneKey.of(number))

    // --- Sao lưu & khôi phục ---

    /** Xuất TOÀN BỘ nhóm (kể cả mặc định) kèm thành viên cho bản sao lưu. */
    suspend fun exportForBackup(): List<BackupCategory> {
        val membersByCat = dao.getAllMembers().groupBy { it.categoryId }
        return dao.getCategories().map { c ->
            BackupCategory(
                name = c.name,
                description = c.description,
                iconKey = c.iconKey,
                colorArgb = c.colorArgb,
                builtInKey = c.builtInKey,
                sortOrder = c.sortOrder,
                createdAt = c.createdAt,
                members = (membersByCat[c.id] ?: emptyList()).map {
                    BackupMember(it.rawNumber, it.phoneKey, it.addedAt)
                },
            )
        }
    }

    /**
     * KHÔI PHỤC nhóm phân loại từ bản sao lưu theo [mode].
     *
     * Ghép nhóm: nhóm MẶC ĐỊNH khớp theo [CategoryEntity.builtInKey] (luôn tồn tại sau [ensureSeeded]);
     * nhóm NGƯỜI DÙNG khớp theo TÊN (cắt khoảng trắng, không phân biệt hoa/thường). Thành viên so trùng
     * theo [PhoneKey] trong cùng nhóm (unique index chống trùng).
     *
     *  - [MergeMode.REPLACE]: xoá sạch nhóm người dùng + dọn thành viên nhóm mặc định, rồi dựng lại từ bản sao lưu.
     *  - [MergeMode.ADD]: giữ nguyên cái đang có; chỉ thêm nhóm/thành viên CHƯA có.
     *  - [MergeMode.UPDATE]: như [ADD] nhưng còn cập nhật giao diện (icon/màu/mô tả) của nhóm khớp tên.
     *
     * Tôn trọng giới hạn [MAX_CATEGORIES] / [MAX_MEMBERS]; phần vượt bị bỏ và đánh dấu [SectionResult.truncated].
     * [added] = số nhóm mới + số thành viên mới; [updated] = số nhóm được cập nhật giao diện.
     *
     * Toàn bộ chạy trong MỘT giao dịch ([withTransaction]): với REPLACE (xoá sạch rồi dựng lại), nếu giữa chừng
     * lỗi hoặc coroutine bị huỷ (app bị kill), giao dịch được ROLL BACK → dữ liệu cũ còn nguyên, không mất trắng.
     */
    suspend fun restore(incoming: List<BackupCategory>, mode: MergeMode): SectionResult = db.withTransaction {
        ensureSeeded() // đảm bảo 2 nhóm mặc định tồn tại trước khi ghép

        var added = 0
        var updated = 0
        var skipped = 0
        var truncated = false

        if (mode == MergeMode.REPLACE) {
            dao.deleteAllUserCategories() // thành viên tự CASCADE
            dao.getCategories().filter { it.builtInKey != null }.forEach { dao.deleteMembersOf(it.id) }
        }

        // Bảng tra id hiện có, cập nhật dần khi tạo nhóm mới (để nhóm trùng tên trong bản sao lưu gộp vào một nhóm).
        val existing = dao.getCategories()
        val builtInIdByKey = existing.filter { it.builtInKey != null }.associate { it.builtInKey!! to it.id }
        val userIdByName = HashMap<String, Long>()
        existing.filter { it.builtInKey == null }.forEach { userIdByName[it.name.trim().lowercase()] = it.id }

        for (bc in incoming) {
            val targetId: Long? = if (bc.builtInKey != null) {
                val id = builtInIdByKey[bc.builtInKey]
                // Nhóm mặc định KHÔNG xoá được → với REPLACE/UPDATE, áp lại giao diện (icon/màu/mô tả) theo bản
                // sao lưu để "khôi phục về đúng trạng thái đã lưu". Tên nhóm mặc định lấy từ i18n nên bỏ qua.
                if (id != null && (mode == MergeMode.UPDATE || mode == MergeMode.REPLACE)) {
                    dao.getCategory(id)?.let {
                        dao.updateCategory(it.copy(description = bc.description, iconKey = bc.iconKey, colorArgb = bc.colorArgb))
                        updated++
                    }
                }
                id
            } else {
                val nameKey = bc.name.trim().lowercase()
                val existingId = userIdByName[nameKey]
                when {
                    existingId != null -> {
                        if (mode == MergeMode.UPDATE) {
                            dao.getCategory(existingId)?.let {
                                dao.updateCategory(it.copy(name = bc.name.trim(), description = bc.description, iconKey = bc.iconKey, colorArgb = bc.colorArgb))
                                updated++
                            }
                        }
                        existingId
                    }
                    dao.categoryCount() >= MAX_CATEGORIES -> {
                        truncated = true; null
                    }
                    else -> {
                        val maxSort = dao.getCategories().maxOfOrNull { it.sortOrder } ?: 1
                        val newId = dao.insertCategory(
                            CategoryEntity(
                                name = bc.name.trim(),
                                description = bc.description,
                                iconKey = bc.iconKey,
                                colorArgb = bc.colorArgb,
                                builtInKey = null,
                                sortOrder = maxSort + 1,
                                createdAt = if (bc.createdAt > 0) bc.createdAt else System.currentTimeMillis(),
                            )
                        )
                        userIdByName[nameKey] = newId
                        added++
                        newId
                    }
                }
            }

            if (targetId == null) {
                skipped += bc.members.size
                continue
            }

            for (m in bc.members) {
                // Tính khoá CHUẨN từ số gốc (không tin khoá đã lưu — bản sao lưu cũ có thể theo thuật toán khác);
                // nhờ vậy so trùng & chèn dùng đúng khoá hiện hành, renormalize phía dưới không phải xoá lại dòng
                // vừa chèn (tránh đếm "added" ảo).
                val key = PhoneKey.of(m.rawNumber).ifEmpty { m.phoneKey }
                if (key.isEmpty()) {
                    skipped++; continue
                }
                if (dao.memberExists(targetId, key) > 0) {
                    skipped++; continue
                }
                if (dao.memberCount(targetId) >= MAX_MEMBERS) {
                    truncated = true; skipped++; continue
                }
                dao.insertMember(
                    CategoryMemberEntity(
                        categoryId = targetId,
                        rawNumber = m.rawNumber,
                        phoneKey = key,
                        addedAt = if (m.addedAt > 0) m.addedAt else System.currentTimeMillis(),
                    )
                )
                added++
            }
        }

        // Chuẩn hoá lại khoá thành viên theo thuật toán PhoneKey hiện hành (idempotent) — khớp seed/ensure.
        renormalizeMemberKeys()
        SectionResult(added = added, updated = updated, skipped = skipped, truncated = truncated)
    }

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

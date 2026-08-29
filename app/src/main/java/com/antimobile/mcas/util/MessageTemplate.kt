package com.antimobile.mcas.util

import android.content.Context
import android.content.SharedPreferences
import com.antimobile.mcas.data.backup.MergeMode
import com.antimobile.mcas.data.backup.SectionResult
import org.json.JSONArray
import org.json.JSONObject

/** Một mẫu tin nhắn do người dùng soạn sẵn: tiêu đề + nội dung (có thể chứa pattern {…}). */
data class MessageTemplate(
    val id: Long,
    val title: String,
    val content: String
)

/**
 * Một mẫu do HỆ THỐNG phát hành (khác mẫu người dùng tự tạo). Nội dung được khai trong [Releases] —
 * mỗi mẫu thuộc về đúng một bản phát hành.
 *
 * [stableKey] là định danh VĨNH VIỄN, chỉ dùng lúc gieo/gộp để biết "máy này đã được chào mẫu đó chưa".
 * KHÔNG lưu vào [MessageTemplate] (giữ nguyên schema JSON 3 khoá).
 * **Đã phát hành thì TUYỆT ĐỐI không đổi [stableKey]** — đổi = coi như mẫu mới, sẽ gieo lại cho người đã có.
 */
data class SystemTemplate(
    val stableKey: String,
    val title: String,
    val content: String
)

/**
 * Lưu/đọc danh sách mẫu tin nhắn trong SharedPreferences dưới dạng JSON (giống lịch sử tìm kiếm),
 * KHÔNG cần cơ sở dữ liệu. Tối đa [MAX] mẫu do người dùng thêm; giữ thứ tự theo lúc thêm (mới nhất ở cuối).
 *
 * Mọi thao tác trả về danh sách MỚI sau khi ghi, để UI cập nhật state ngay.
 *
 * ### Bản cập nhật THÊM mẫu mới thế nào (bất biến bắt buộc)
 * Chỉ được **THÊM**: không bao giờ ghi đè, xoá, hay sắp xếp lại mẫu đang có của người dùng; mẫu người
 * dùng đã chủ động xoá thì **không mọc lại**. Cơ chế: mọi [SystemTemplate.stableKey] từng được chào đều
 * ghi vào [SEEDED_KEYS]; chỉ mẫu có key CHƯA từng chào mới được nối vào cuối với `id` mới.
 * Xem [mergeSystemTemplates] và [AppMigrations].
 */
object MessageTemplateStore {

    /** Số mẫu tối đa người dùng được tự thêm. */
    const val MAX = 10

    private const val PREFS = "message_templates"
    private const val KEY = "templates"

    /** Cờ đã GIEO bộ mẫu hệ thống lần đầu trên máy này. */
    private const val SEEDED = "seeded_defaults_v1"

    /** Tập [SystemTemplate.stableKey] ĐÃ TỪNG được chào cho máy này — chống gieo lại / chống mọc lại. */
    private const val SEEDED_KEYS = "seeded_system_keys"

    /**
     * Máy này đã từng gieo bộ mẫu hệ thống chưa. Chỉ ĐỌC cờ, KHÔNG kích hoạt gieo (khác [load]) —
     * [AppMigrations] dùng để nhận biết người dùng cũ.
     */
    fun hasSeeded(context: Context): Boolean = prefs(context).getBoolean(SEEDED, false)

    /**
     * Đánh dấu [templates] là "đã từng chào" mà KHÔNG thêm gì.
     *
     * Dùng cho người dùng CŨ ở lần đầu chạy [AppMigrations]: mẫu của các bản `<= BASELINE` họ đã có sẵn
     * (hoặc đã cố ý xoá), gộp lại sẽ nhân đôi / làm mẫu đã xoá mọc lại.
     *
     * Ghi ĐỒNG BỘ vì chạy trong migration, phải bền trước khi dấu trang phiên bản nhích lên.
     */
    fun markSystemTemplatesOffered(context: Context, templates: List<SystemTemplate>) {
        if (templates.isEmpty()) return
        val p = prefs(context)
        writeOfferedKeys(p, readOfferedKeys(p) + templates.map { it.stableKey }, durable = true)
    }

    /**
     * GỘP [incoming] (mẫu hệ thống của một bản phát hành) vào kho của người dùng — **CHỈ THÊM**.
     *
     * Bất biến:
     * - Mẫu đang có (kể cả mẫu mặc định người dùng đã sửa) **giữ nguyên tuyệt đối**: không đè nội dung,
     *   không đổi `id`, không đổi thứ tự.
     * - Mẫu người dùng đã xoá **không mọc lại**: key của nó đã nằm trong [SEEDED_KEYS] từ lúc chào.
     * - Mẫu mới nối vào CUỐI với `id = max(id) + 1` → không bao giờ trùng `id` mẫu người dùng.
     * - Máy chưa từng mở màn mẫu: [load] bên dưới sẽ gieo trọn danh mục và đánh dấu mọi key → `fresh`
     *   rỗng → không nhân đôi.
     *
     * Cố tình BỎ QUA [MAX] (giống đường gieo lần đầu): thà hiển thị 11–12 mẫu còn hơn âm thầm nuốt mất
     * mẫu mới. Đường [add] của người dùng vẫn chặn tại [MAX].
     */
    fun mergeSystemTemplates(context: Context, incoming: List<SystemTemplate>): List<MessageTemplate> {
        if (incoming.isEmpty()) return load(context)

        // Gọi load() TRƯỚC khi đọc offered: nếu máy chưa từng gieo, load() gieo trọn danh mục và ghi
        // toàn bộ key vào SEEDED_KEYS — nhờ vậy `fresh` rỗng và không thêm trùng.
        val current = load(context)
        val p = prefs(context)
        val offered = readOfferedKeys(p)
        val fresh = incoming.filterNot { it.stableKey in offered }
        if (fresh.isEmpty()) return current

        var nextId = (current.maxOfOrNull { it.id } ?: 0L) + 1L
        val updated = current + fresh.map { MessageTemplate(nextId++, it.title, it.content) }
        // Ghi NGUYÊN TỬ: danh sách mẫu và tập key "đã chào" phải cùng vào/cùng không. Nếu tách hai lệnh,
        // app chết ở giữa sẽ khiến bản này chạy lại và nhân đôi mẫu vừa thêm.
        //
        // commit() (ĐỒNG BỘ, xuống đĩa ngay) chứ KHÔNG apply(): [AppMigrations] ghi dấu trang phiên bản
        // bằng commit() ở một file prefs KHÁC ngay sau đây. Nếu phần việc này còn nằm trong hàng đợi
        // apply() mà tiến trình bị giết đột ngột (OOM/SIGKILL), dấu trang sẽ sống sót còn mẫu mới thì mất
        // → bản này không bao giờ chạy lại → NUỐT MẤT mẫu. commit() cũng đẩy luôn mọi apply() đang chờ
        // trên cùng file prefs (kể cả lần gieo trong load() ở trên).
        p.edit()
            .putString(KEY, encode(updated))
            .putStringSet(SEEDED_KEYS, offered + incoming.map { it.stableKey })
            .commit()
        return updated
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Đọc danh sách mẫu; rỗng nếu chưa có / JSON hỏng (không ném lỗi). Lần đầu sẽ GIEO danh mục hệ thống. */
    fun load(context: Context): List<MessageTemplate> {
        val p = prefs(context)
        val json = p.getString(KEY, null)
        // Gieo danh mục ĐÚNG MỘT LẦN, chỉ cho máy CHƯA từng có mẫu nào (người dùng cũ đã có mẫu thì bỏ qua,
        // không đè). Đánh dấu SEEDED ngay để lần sau không xét lại → xoá mẫu mặc định cũng không hiện lại.
        if (!p.getBoolean(SEEDED, false)) {
            if (json == null) {
                val catalog = Releases.allTemplates()
                val seeded = catalog.mapIndexed { i, t -> MessageTemplate(i + 1L, t.title, t.content) }
                // Cả danh mục coi như "đã chào" → merge của mọi bản về sau đều thành vô hại cho máy này.
                p.edit()
                    .putBoolean(SEEDED, true)
                    .putString(KEY, encode(seeded))
                    .putStringSet(SEEDED_KEYS, catalog.mapTo(mutableSetOf()) { it.stableKey })
                    .apply()
                return seeded
            }
            p.edit().putBoolean(SEEDED, true).apply()
        }
        if (json == null) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                MessageTemplate(o.getLong("id"), o.getString("title"), o.getString("content"))
            }
        }.getOrDefault(emptyList())
    }

    private fun encode(list: List<MessageTemplate>): String {
        val arr = JSONArray()
        list.forEach { t ->
            arr.put(JSONObject().put("id", t.id).put("title", t.title).put("content", t.content))
        }
        return arr.toString()
    }

    private fun save(context: Context, list: List<MessageTemplate>) {
        prefs(context).edit().putString(KEY, encode(list)).apply()
    }

    /** Sao chép phòng thủ: tập trả về từ SharedPreferences KHÔNG được phép sửa tại chỗ. */
    private fun readOfferedKeys(p: SharedPreferences): Set<String> =
        p.getStringSet(SEEDED_KEYS, emptySet())?.toSet().orEmpty()

    /** [durable] = ghi ĐỒNG BỘ; bắt buộc khi gọi từ migration, để dấu trang không nhích trước dữ liệu. */
    private fun writeOfferedKeys(p: SharedPreferences, keys: Set<String>, durable: Boolean = false) {
        val editor = p.edit().putStringSet(SEEDED_KEYS, keys)
        if (durable) editor.commit() else editor.apply()
    }

    /**
     * Thêm mẫu mới. Nếu đã đạt [MAX] thì GIỮ NGUYÊN (trả về danh sách hiện tại) — nơi gọi kiểm tra
     * kích thước trước để báo người dùng. Tiêu đề được cắt khoảng trắng thừa; nội dung giữ nguyên.
     */
    fun add(context: Context, title: String, content: String): List<MessageTemplate> {
        val current = load(context)
        if (current.size >= MAX) return current
        val nextId = (current.maxOfOrNull { it.id } ?: 0L) + 1L
        val updated = current + MessageTemplate(nextId, title.trim(), content)
        save(context, updated)
        return updated
    }

    /** Sửa mẫu theo [id] (nếu tồn tại). */
    fun update(context: Context, id: Long, title: String, content: String): List<MessageTemplate> {
        val updated = load(context).map {
            if (it.id == id) it.copy(title = title.trim(), content = content) else it
        }
        save(context, updated)
        return updated
    }

    /** Xoá mẫu theo [id]. */
    fun delete(context: Context, id: Long): List<MessageTemplate> {
        val updated = load(context).filterNot { it.id == id }
        save(context, updated)
        return updated
    }

    /**
     * KHÔI PHỤC mẫu tin nhắn từ một bản sao lưu theo [mode]. Khoá so trùng = TIÊU ĐỀ (đã cắt khoảng trắng,
     * không phân biệt hoa/thường) — coi tiêu đề là "danh tính" của mẫu.
     *  - [MergeMode.REPLACE]: danh sách trở thành ĐÚNG [incoming].
     *  - [MergeMode.ADD]: giữ mẫu hiện có, chỉ thêm mẫu có tiêu đề CHƯA tồn tại.
     *  - [MergeMode.UPDATE]: đụng trùng tiêu đề → ghi đè nội dung theo bản sao lưu; tiêu đề mới → thêm.
     *
     * Sau khi gộp, đánh lại `id` tuần tự và đặt cờ [SEEDED] = true để lần gộp mẫu hệ thống về sau KHÔNG
     * gieo lại / không đụng vào mẫu người dùng (giống đường [load] đã gieo). KHÔNG chặn tại [MAX] (thà giữ
     * đủ mẫu người dùng còn hơn nuốt mất — cùng tinh thần với [mergeSystemTemplates]).
     */
    fun restore(context: Context, incoming: List<MessageTemplate>, mode: MergeMode): SectionResult {
        val current = load(context)
        fun key(t: MessageTemplate) = t.title.trim().lowercase()

        val merged: List<MessageTemplate>
        val result: SectionResult
        when (mode) {
            MergeMode.REPLACE -> {
                merged = incoming
                result = SectionResult(added = incoming.size)
            }
            MergeMode.ADD -> {
                val have = current.mapTo(HashSet()) { key(it) }
                var added = 0
                var skipped = 0
                val extra = ArrayList<MessageTemplate>()
                for (t in incoming) {
                    if (key(t) in have) {
                        skipped++
                    } else {
                        have.add(key(t)); extra.add(t); added++
                    }
                }
                merged = current + extra
                result = SectionResult(added = added, skipped = skipped)
            }
            MergeMode.UPDATE -> {
                val byKey = LinkedHashMap<String, MessageTemplate>()
                current.forEach { byKey[key(it)] = it }
                var added = 0
                var updated = 0
                for (t in incoming) {
                    val k = key(t)
                    if (byKey.containsKey(k)) {
                        byKey[k] = byKey.getValue(k).copy(title = t.title.trim(), content = t.content)
                        updated++
                    } else {
                        byKey[k] = t; added++
                    }
                }
                merged = byKey.values.toList()
                result = SectionResult(added = added, updated = updated)
            }
        }

        val reIded = merged.mapIndexed { i, t -> t.copy(id = i + 1L, title = t.title.trim()) }
        prefs(context).edit()
            .putBoolean(SEEDED, true)
            .putString(KEY, encode(reIded))
            .apply()
        return result
    }
}

package com.antimobile.callhs.util

import android.content.Context
import android.content.SharedPreferences
import com.antimobile.callhs.data.backup.MergeMode
import com.antimobile.callhs.data.backup.SectionResult
import org.json.JSONArray
import org.json.JSONObject

/** Một kết quả quét mã QR đã lưu: nội dung GỐC đọc được + thời điểm quét (mốc mới nhất của nội dung này). */
data class QrScanEntry(val raw: String, val time: Long)

/**
 * Lưu/đọc LỊCH SỬ quét mã QR trong SharedPreferences dạng JSON (giống mẫu tin nhắn / lịch sử tìm kiếm),
 * KHÔNG cần cơ sở dữ liệu. Giữ tối đa [MAX] mục, MỚI NHẤT ở ĐẦU.
 *
 * Chỉ lưu CHUỖI GỐC + thời gian: loại mã (link / tel / văn bản…) được suy ra bằng [QrContent.parse] lúc hiển
 * thị, nên không cần lưu kèm. Mọi thao tác ghi trả về danh sách MỚI để UI cập nhật state ngay.
 */
object QrScanHistoryStore {

    /** Số mục lịch sử tối đa. */
    const val MAX = 15

    private const val PREFS = "qr_scan_history"
    private const val KEY = "entries"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Đọc lịch sử (mới → cũ); rỗng nếu chưa có / JSON hỏng (không ném lỗi). */
    fun load(context: Context): List<QrScanEntry> {
        val json = prefs(context).getString(KEY, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                QrScanEntry(o.getString("r"), o.getLong("t"))
            }
        }.getOrDefault(emptyList())
    }

    private fun save(context: Context, list: List<QrScanEntry>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("r", it.raw).put("t", it.time)) }
        prefs(context).edit().putString(KEY, arr.toString()).apply()
    }

    /**
     * Ghi một lần quét MỚI lên ĐẦU lịch sử. Nội dung TRÙNG (đúng từng ký tự) được gộp lại & cập nhật thời gian
     * (đưa lên đầu, không tạo dòng lặp). Cắt còn tối đa [MAX] mục (bỏ cũ nhất). Chuỗi rỗng/toàn khoảng trắng
     * bị bỏ qua. Trả về danh sách mới.
     */
    fun record(context: Context, raw: String, now: Long = System.currentTimeMillis()): List<QrScanEntry> {
        if (raw.isBlank()) return load(context)
        val deduped = load(context).filterNot { it.raw == raw }
        val updated = (listOf(QrScanEntry(raw, now)) + deduped).take(MAX)
        save(context, updated)
        return updated
    }

    /** Xoá một mục theo nội dung gốc (khoá duy nhất sau khi gộp trùng). */
    fun delete(context: Context, raw: String): List<QrScanEntry> {
        val updated = load(context).filterNot { it.raw == raw }
        save(context, updated)
        return updated
    }

    /** Xoá TOÀN BỘ lịch sử. */
    fun clear(context: Context): List<QrScanEntry> {
        prefs(context).edit().remove(KEY).apply()
        return emptyList()
    }

    /**
     * KHÔI PHỤC lịch sử quét QR từ bản sao lưu theo [mode]. Khoá so trùng = nội dung GỐC ([QrScanEntry.raw]).
     *  - [MergeMode.REPLACE]: lịch sử trở thành đúng [incoming] (khử trùng, giữ mốc mới hơn).
     *  - [MergeMode.ADD]: GIỮ mọi mục hiện có; chỉ thêm nội dung CHƯA có vào các chỗ trống còn lại.
     *  - [MergeMode.UPDATE]: như ADD, nhưng nội dung trùng → cập nhật lên MỐC THỜI GIAN mới hơn.
     *
     * Lịch sử là "vòng đệm" tối đa [MAX] mục mới nhất. Điểm mấu chốt: khi cắt còn [MAX], mục HIỆN CÓ được ưu
     * tiên GIỮ (xếp trước), phần mới chỉ điền vào chỗ còn trống — nên ADD/UPDATE KHÔNG bao giờ hất mục hiện có
     * ra để nhường cho mục từ bản sao lưu (đúng cam kết "giữ mục hiện có"). [SectionResult] đếm theo số THỰC SỰ
     * được lưu sau khi cắt; đụng cắt → [SectionResult.truncated] = true.
     */
    fun restore(context: Context, incoming: List<QrScanEntry>, mode: MergeMode): SectionResult {
        if (mode == MergeMode.REPLACE) {
            // Thay toàn bộ: khử trùng raw (giữ mốc mới nhất), sắp mới→cũ, cắt còn MAX.
            val byRaw = LinkedHashMap<String, QrScanEntry>()
            for (e in incoming) {
                val ex = byRaw[e.raw]
                if (ex == null || e.time > ex.time) byRaw[e.raw] = e
            }
            val capped = byRaw.values.sortedByDescending { it.time }.take(MAX)
            save(context, capped)
            return SectionResult(added = capped.size, truncated = byRaw.size > MAX)
        }

        // ADD / UPDATE: giữ mục hiện có; nội dung mới đưa vào danh sách "fresh".
        val kept = LinkedHashMap<String, QrScanEntry>()
        load(context).forEach { kept[it.raw] = it }
        val fresh = ArrayList<QrScanEntry>()
        val seenFresh = HashSet<String>()
        var updated = 0
        var skipped = 0
        for (e in incoming) {
            val ex = kept[e.raw]
            when {
                ex != null -> {
                    // Trùng với mục hiện có: UPDATE + mốc mới hơn → cập nhật; còn lại → bỏ qua (giữ nguyên).
                    if (mode == MergeMode.UPDATE && e.time > ex.time) {
                        kept[e.raw] = e; updated++
                    } else {
                        skipped++
                    }
                }
                e.raw in seenFresh -> skipped++ // trùng trong chính bản sao lưu
                else -> {
                    seenFresh.add(e.raw); fresh.add(e)
                }
            }
        }

        // Xếp mục hiện có trước (không bao giờ bị hất), rồi tới mục mới; cắt còn MAX.
        val keptSorted = kept.values.sortedByDescending { it.time }
        val freshSorted = fresh.sortedByDescending { it.time }
        val capped = (keptSorted + freshSorted).take(MAX)
        save(context, capped)
        // "added" = số mục MỚI thực sự lọt vào danh sách sau khi cắt.
        val added = capped.count { it.raw in seenFresh }
        val truncated = keptSorted.size + freshSorted.size > MAX
        return SectionResult(added = added, updated = updated, skipped = skipped, truncated = truncated)
    }
}

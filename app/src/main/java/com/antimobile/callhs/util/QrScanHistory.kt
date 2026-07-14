package com.antimobile.callhs.util

import android.content.Context
import android.content.SharedPreferences
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
}

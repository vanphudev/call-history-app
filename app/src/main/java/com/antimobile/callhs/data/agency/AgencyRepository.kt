package com.antimobile.callhs.data.agency

import android.content.Context
import com.antimobile.callhs.data.remote.RemoteFileCache
import com.antimobile.callhs.data.remote.RemoteOrigin

/** Nguồn gốc dữ liệu danh bạ (mạng/cache) — alias của [RemoteOrigin] dùng chung. */
typealias AgencyOrigin = RemoteOrigin

/** Kết quả nạp danh bạ: dữ liệu đã phân tích + nguồn gốc (mạng/cache). */
data class AgencyResult(val data: AgencyData, val origin: AgencyOrigin)

/**
 * Nạp danh bạ cơ quan cho một [AgencyDataset]: lấy XML qua [RemoteFileCache] (PURE REMOTE, cache 7 ngày +
 * mạng có điều kiện) rồi phân tích bằng [AgencyXmlParser]. Trả **`null`** khi không lấy được dữ liệu
 * (offline/máy chủ lỗi & chưa có cache) hoặc phân tích ra rỗng/hỏng → UI hiện lỗi/modal + nút "Thử lại".
 *
 * BLOCKING — gọi ở luồng nền (Dispatchers.IO).
 */
class AgencyRepository(context: Context) {

    private val cache = RemoteFileCache(context, subdir = SUBDIR, prefsName = PREFS_NAME)

    fun load(dataset: AgencyDataset, forceRefresh: Boolean = false): AgencyResult? {
        val fetched = cache.fetch(dataset.fileName, TTL_MS, forceRefresh) ?: return null
        val parsed = runCatching { AgencyXmlParser.parse(dataset, fetched.bytes.inputStream()) }.getOrNull()
        return if (parsed != null && parsed.agencies.isNotEmpty()) AgencyResult(parsed, fetched.origin) else null
    }

    companion object {
        /** 7 ngày (mili-giây). */
        const val TTL_MS = 7L * 24 * 60 * 60 * 1000

        private const val SUBDIR = "agency"
        private const val PREFS_NAME = "agency_cache"

        /**
         * XOÁ cache của CẢ 4 tập dữ liệu → lần mở màn danh bạ kế tiếp tải lại NGAY, không đợi hết [TTL_MS].
         *
         * KHÔNG gọi = giữ nguyên vòng đời 7 ngày như thường lệ. Gọi từ bậc tương ứng trong
         * [com.antimobile.callhs.util.AppMigrations] khi bản cập nhật đổi dữ liệu danh bạ.
         *
         * BLOCKING (đĩa).
         */
        fun clearCache(context: Context) {
            val cache = RemoteFileCache(context, subdir = SUBDIR, prefsName = PREFS_NAME)
            AgencyDataset.entries.forEach { cache.clear(it.fileName) }
        }
    }
}

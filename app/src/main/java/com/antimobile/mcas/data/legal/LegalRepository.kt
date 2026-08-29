package com.antimobile.mcas.data.legal

import android.content.Context
import com.antimobile.mcas.data.remote.RemoteFileCache

/**
 * Nạp nội dung pháp lý (privacy/terms) từ `legal.xml` trên GitHub qua [RemoteFileCache] — cache **30 NGÀY**
 * (nội dung ít đổi, không cần tải thường xuyên). Trả tài liệu theo [docId], hoặc `null` khi chưa lấy được
 * (offline & chưa có cache). BLOCKING — gọi ở luồng nền (Dispatchers.IO).
 */
class LegalRepository(context: Context) {

    private val cache = RemoteFileCache(context, subdir = SUBDIR, prefsName = PREFS_NAME)

    fun load(docId: String, forceRefresh: Boolean = false): LegalDocument? {
        val fetched = cache.fetch(FILE_NAME, TTL_MS, forceRefresh) ?: return null
        val docs = runCatching { LegalXmlParser.parse(fetched.bytes.inputStream()) }.getOrNull() ?: return null
        return docs.firstOrNull { it.id == docId }
    }

    companion object {
        const val FILE_NAME = "legal.xml"
        /** 30 ngày (mili-giây). */
        const val TTL_MS = 30L * 24 * 60 * 60 * 1000

        private const val SUBDIR = "legal"
        private const val PREFS_NAME = "legal_cache"

        /**
         * XOÁ cache `legal.xml` → lần mở màn Chính sách/Điều khoản kế tiếp tải lại NGAY, không đợi hết
         * [TTL_MS]. KHÔNG gọi = giữ nguyên vòng đời 30 ngày. Gọi từ bậc tương ứng trong
         * [com.antimobile.mcas.util.AppMigrations] khi bản cập nhật đổi nội dung pháp lý.
         *
         * BLOCKING (đĩa).
         */
        fun clearCache(context: Context) {
            RemoteFileCache(context, subdir = SUBDIR, prefsName = PREFS_NAME).clear(FILE_NAME)
        }
    }
}

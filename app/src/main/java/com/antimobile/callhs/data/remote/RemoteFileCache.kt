package com.antimobile.callhs.data.remote

import android.content.Context
import com.antimobile.callhs.util.AppLinks
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Nguồn gốc dữ liệu đang dùng. */
enum class RemoteOrigin { NETWORK, CACHE }

/**
 * Cache file `.xml` tải từ GitHub theo cơ chế THÔNG MINH, NHẸ máy chủ — DÙNG CHUNG cho danh bạ cơ quan
 * (TTL 7 ngày) và nội dung pháp lý (TTL 30 ngày). Mỗi nhóm cache có [subdir] + [prefsName] riêng.
 *
 *  1. Cache còn hạn (< `ttlMs`) → dùng cache tại máy, **KHÔNG gọi mạng**.
 *  2. Hết hạn / buộc làm mới → GET **CÓ ĐIỀU KIỆN** (`If-None-Match`/`If-Modified-Since`) qua
 *     [AppLinks.DATA_BASES] (GitHub Pages → raw): `200`→ghi **NGUYÊN TỬ** + đặt lại đồng hồ/ETag CHỈ khi
 *     ghi thành công; `304`→giữ cache + đặt lại đồng hồ CHỈ khi cache đọc được.
 *  3. Lỗi mạng / offline → cache cũ (kể cả quá hạn) nếu có; hết cách → **`null`**.
 *
 * MỌI thao tác BLOCKING (đĩa + mạng) — BẮT BUỘC gọi ở luồng nền (Dispatchers.IO).
 */
class RemoteFileCache(context: Context, subdir: String, prefsName: String) {

    private val appContext = context.applicationContext
    private val dir = File(appContext.filesDir, subdir).apply { if (!exists()) mkdirs() }
    private val prefs = appContext.getSharedPreferences(prefsName, Context.MODE_PRIVATE)

    data class Result(val bytes: ByteArray, val origin: RemoteOrigin)

    /** Trả `null` khi hoàn toàn không có dữ liệu (offline/máy chủ lỗi & chưa từng cache thành công). */
    fun fetch(fileName: String, ttlMs: Long, forceRefresh: Boolean): Result? {
        val cacheFile = File(dir, fileName)
        val fetchedAt = prefs.getLong(keyAt(fileName), 0L)
        val age = System.currentTimeMillis() - fetchedAt
        val cacheFresh = cacheFile.exists() && age in 0 until ttlMs

        // (1) Cache còn hạn → dùng luôn, không đụng mạng.
        if (cacheFresh && !forceRefresh) {
            readFile(cacheFile)?.let { return Result(it, RemoteOrigin.CACHE) }
        }

        // (2) Cần gọi mạng.
        val haveCache = cacheFile.exists()
        val net = runCatching { downloadConditional(fileName, haveCache) }.getOrNull()
        when (net) {
            is Net.Ok -> {
                if (writeAtomically(cacheFile, net.body)) {
                    prefs.edit()
                        .putLong(keyAt(fileName), System.currentTimeMillis())
                        .putString(keyEtag(fileName), net.etag)
                        .putString(keyMod(fileName), net.lastModified)
                        .apply()
                }
                return Result(net.body, RemoteOrigin.NETWORK)
            }
            Net.NotModified -> readFile(cacheFile)?.let {
                prefs.edit().putLong(keyAt(fileName), System.currentTimeMillis()).apply()
                return Result(it, RemoteOrigin.CACHE)
            }
            null -> { /* lỗi mạng — rơi xuống dự phòng */ }
        }

        // (3) Dự phòng: cache cũ nếu còn đọc được; hết cách → null.
        readFile(cacheFile)?.let { return Result(it, RemoteOrigin.CACHE) }
        return null
    }

    /**
     * XOÁ SẠCH cache của [fileName] → lần [fetch] kế tiếp BẮT BUỘC tải lại TOÀN BỘ từ mạng (HTTP 200),
     * bỏ qua `ttlMs`. Dùng khi bản cập nhật cần người dùng thấy dữ liệu mới NGAY, không đợi hết hạn.
     *
     * Phải xoá cả FILE lẫn 3 khoá metadata. Nếu chỉ xoá đồng hồ (`_at`) mà để file lại thì [fetch] vẫn
     * thấy `haveCache = true` → gửi kèm `If-None-Match`/`If-Modified-Since` → máy chủ trả `304` →
     * cache CŨ được giữ nguyên. Vì lý do đó `forceRefresh = true` cũng KHÔNG thay thế được hàm này.
     *
     * BLOCKING (đĩa). Dùng `commit()` vì chạy đúng một lần lúc khởi động, cần chắc chắn đã ghi.
     */
    fun clear(fileName: String) {
        File(dir, fileName).delete()
        File(dir, "$fileName.tmp").delete()
        prefs.edit()
            .remove(keyAt(fileName))
            .remove(keyEtag(fileName))
            .remove(keyMod(fileName))
            .commit()
    }

    private fun readFile(f: File): ByteArray? =
        if (f.exists() && f.length() > 0) runCatching { f.readBytes() }.getOrNull() else null

    /** Ghi NGUYÊN TỬ: file `.tmp` → đổi tên đè đích (cùng filesystem). Không để lại file đích cắt cụt. */
    private fun writeAtomically(dest: File, body: ByteArray): Boolean {
        val tmp = File(dest.parentFile ?: return false, dest.name + ".tmp")
        return try {
            tmp.writeBytes(body)
            var ok = tmp.renameTo(dest)
            if (!ok) {
                dest.delete()
                ok = tmp.renameTo(dest)
            }
            if (!ok) tmp.delete()
            ok
        } catch (_: Exception) {
            tmp.delete()
            false
        }
    }

    private sealed interface Net {
        data class Ok(val body: ByteArray, val etag: String?, val lastModified: String?) : Net
        data object NotModified : Net
    }

    private fun downloadConditional(fileName: String, haveCache: Boolean): Net {
        val etag = if (haveCache) prefs.getString(keyEtag(fileName), null) else null
        val lastMod = if (haveCache) prefs.getString(keyMod(fileName), null) else null
        var lastError: Exception? = null
        for (base in AppLinks.DATA_BASES) {
            try {
                return requestOne(base + fileName, etag, lastMod)
            } catch (e: Exception) {
                lastError = e
            }
        }
        throw lastError ?: IOException("Không tải được $fileName")
    }

    private fun requestOne(url: String, etag: String?, lastMod: String?): Net {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 15_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/xml, text/xml, */*")
            if (etag != null) setRequestProperty("If-None-Match", etag)
            if (lastMod != null) setRequestProperty("If-Modified-Since", lastMod)
        }
        try {
            return when (val code = conn.responseCode) {
                HttpURLConnection.HTTP_NOT_MODIFIED -> Net.NotModified
                in 200..299 -> {
                    val body = conn.inputStream.use { it.readBytes() }
                    if (body.isEmpty()) throw IOException("Phản hồi rỗng")
                    Net.Ok(body, conn.getHeaderField("ETag"), conn.getHeaderField("Last-Modified"))
                }
                else -> throw IOException("HTTP $code")
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun keyAt(f: String) = "${f}_at"
    private fun keyEtag(f: String) = "${f}_etag"
    private fun keyMod(f: String) = "${f}_lastmod"
}

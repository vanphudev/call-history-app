package com.antimobile.callhs.data.donate

import android.content.Context
import com.antimobile.callhs.util.Donate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Tải & CACHE danh sách app ngân hàng (deeplink) từ [Donate.DEEPLINK_LIST_URL].
 *
 * Thứ tự ưu tiên (giống cơ chế nhẹ máy chủ của app): cache còn hạn (< 7 ngày) → dùng luôn; hết hạn/thiếu →
 * gọi mạng, cache lại; lỗi mạng → cache cũ; hết cách → danh sách [BankApp.BUNDLED] gói sẵn. Danh sách trả
 * về đã SẮP theo lượt cài giảm dần (ngân hàng phổ biến lên trước). MỌI thao tác chạy ở [Dispatchers.IO].
 */
object BankAppRepository {

    private const val TTL_MS = 7L * 24 * 60 * 60 * 1000
    private const val SUBDIR = "donate"
    private const val FILE = "bank_apps.json"
    private const val PREFS = "donate_banks"
    private const val KEY_AT = "fetched_at"

    suspend fun load(context: Context): List<BankApp> = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val dir = File(app.filesDir, SUBDIR).apply { if (!exists()) mkdirs() }
        val file = File(dir, FILE)
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val age = System.currentTimeMillis() - prefs.getLong(KEY_AT, 0L)
        val fresh = file.exists() && age in 0 until TTL_MS

        if (fresh) {
            parse(readText(file))?.let { if (it.isNotEmpty()) return@withContext it }
        }

        val body = runCatching { fetch(Donate.DEEPLINK_LIST_URL) }.getOrNull()
        val parsed = if (body != null) parse(body) else null
        if (!parsed.isNullOrEmpty() && body != null) {
            runCatching {
                val tmp = File(dir, "$FILE.tmp")
                tmp.writeText(body)
                if (!tmp.renameTo(file)) { file.delete(); tmp.renameTo(file) }
                prefs.edit().putLong(KEY_AT, System.currentTimeMillis()).apply()
            }
            return@withContext parsed
        }

        // Dự phòng: cache cũ (kể cả quá hạn) rồi tới danh sách gói sẵn.
        parse(readText(file))?.let { if (it.isNotEmpty()) return@withContext it }
        BankApp.BUNDLED
    }

    private fun readText(file: File): String? =
        if (file.exists() && file.length() > 0) runCatching { file.readText() }.getOrNull() else null

    private fun parse(json: String?): List<BankApp>? {
        if (json.isNullOrBlank()) return null
        return runCatching {
            val arr = JSONObject(json).optJSONArray("apps") ?: return null
            val out = ArrayList<BankApp>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val appId = o.optString("appId")
                if (appId.isBlank()) continue
                val deeplink = o.optString("deeplink").ifBlank { Donate.bankAppBaseDeeplink(appId) }
                out.add(
                    BankApp(
                        appId = appId,
                        appName = o.optString("appName").ifBlank { appId },
                        bankName = o.optString("bankName"),
                        logoUrl = o.optString("appLogo"),
                        deeplink = deeplink,
                        monthlyInstall = o.optLong("monthlyInstall", 0L),
                        autofill = o.optInt("autofill", 0) == 1
                    )
                )
            }
            out.sortByDescending { it.monthlyInstall }
            out
        }.getOrNull()
    }

    private fun fetch(url: String): String? {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 15_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "CallHS-Android")
        }
        return try {
            if (conn.responseCode !in 200..299) null
            else conn.inputStream.use { it.readBytes() }.toString(Charsets.UTF_8).ifBlank { null }
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }
}

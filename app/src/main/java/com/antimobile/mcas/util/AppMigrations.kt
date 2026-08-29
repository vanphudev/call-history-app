package com.antimobile.mcas.util

import android.content.Context
import com.antimobile.mcas.data.agency.AgencyRepository
import com.antimobile.mcas.data.legal.LegalRepository

/**
 * ★ BỘ ÁP DỤNG BẢN PHÁT HÀNH ★
 *
 * Chạy ở mỗi lần mở app (`MainActivity.onCreate`), nhưng **mỗi bản phát hành chỉ được áp dụng ĐÚNG MỘT
 * LẦN** trong đời máy. Nó không quyết định gì cả — chỉ đọc [Releases] rồi thi hành.
 *
 * ### Cách hoạt động
 * - Máy lưu một "dấu trang" = `versionCode` của bản mà nó đã áp dụng xong.
 * - Mở app: lấy mọi entry trong `(dấu trang, versionCode đang chạy]` rồi thi hành lần lượt.
 * - Áp dụng xong → dấu trang nhích tới bản đang chạy → lần mở sau bỏ qua.
 * - **CÀI MỚI ⇒ không áp dụng bản nào** (chẳng có dữ liệu cũ để dọn) và không hiện modal nào.
 * - Người dùng **nhảy cóc** (3 → 6) được áp dụng đủ mọi entry ở giữa, đúng thứ tự.
 *
 * ### Muốn thêm việc cho bản mới thì sửa ở đâu
 * **KHÔNG sửa file này.** Thêm một entry [Release] vào [Releases.ALL] là xong — reset cache, mẫu tin
 * nhắn mới, thông báo… đều khai ở đó. File này chỉ là cỗ máy thi hành.
 *
 * Ngoại lệ duy nhất: `versionCode` / `versionName` vẫn phải bump trong `app/build.gradle.kts` vì Google
 * Play đọc từ đó.
 *
 * ### Ép chạy lại khi test
 * Xoá SharedPreferences `app_migrations` (và `whatsnew_prefs` nếu muốn thử modal), hoặc "Xoá dữ liệu".
 */
object AppMigrations {

    private const val PREFS = "app_migrations"

    /** `versionCode` của bản phát hành đã áp dụng xong trên máy này. */
    private const val KEY_APPLIED_VERSION = "applied_version_code"

    /** Chưa từng chạy bộ này trên máy (khác với "đã áp dụng tới bản 0"). */
    private const val NEVER_RUN = -1

    /**
     * Áp dụng mọi bản phát hành còn thiếu. BLOCKING (prefs + xoá vài file nhỏ) — gọi trong `onCreate`
     * trước `setContent` để khung hình đầu đã thấy dữ liệu/cờ đúng.
     */
    fun run(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = AppVersion.code(context)
        val applied = p.getInt(KEY_APPLIED_VERSION, NEVER_RUN)

        if (applied >= current) return // đã áp dụng tới bản đang chạy

        if (applied == NEVER_RUN) {
            if (!isReturningUser(context)) {
                // CÀI MỚI: không có dữ liệu cũ nào để dọn; mẫu hệ thống sẽ được gieo trọn bộ ở lần
                // MessageTemplateStore.load() đầu tiên. Cũng không làm phiền bằng tin của bản họ chưa dùng.
                WhatsNewStore.markAllSeen(context)
                p.edit().putInt(KEY_APPLIED_VERSION, current).commit()
                return
            }
            // NGƯỜI DÙNG CŨ, lần đầu chạy bộ này: mẫu của các bản <= mốc họ đã có sẵn (hoặc đã cố ý xoá)
            // → đánh dấu "đã chào" để không bao giờ gieo lại. Nếu bỏ bước này, mẫu họ đã xoá sẽ mọc lại.
            MessageTemplateStore.markSystemTemplatesOffered(
                context,
                Releases.templatesUpTo(Releases.BASELINE_VERSION_CODE)
            )
        }

        // Người dùng cũ chưa từng chạy bộ này ⇒ coi như đang đứng ở bản mốc.
        val from = if (applied == NEVER_RUN) Releases.BASELINE_VERSION_CODE else applied

        // Một bản lỗi (vd không xoá được file) sẽ DỪNG tại đó và được thử lại ở lần mở app sau, thay vì
        // bị đánh dấu là xong rồi bỏ qua vĩnh viễn. Mọi việc trong [apply] đều an toàn khi chạy lại.
        var lastOk = from
        var failed = false
        for (release in Releases.since(from, current)) {
            if (runCatching { apply(context, release) }.isFailure) {
                failed = true
                break
            }
            lastOk = release.versionCode
        }

        // Không lỗi ⇒ nhích thẳng tới bản đang chạy (kể cả khi bản đó không khai entry nào).
        val done = if (failed) lastOk else current
        if (done > applied) p.edit().putInt(KEY_APPLIED_VERSION, done).commit()
    }

    /**
     * Máy này đã dùng app trước đây chưa. Đồng ý điều khoản là cửa BẮT BUỘC để vào app nên nó là bằng
     * chứng chắc chắn nhất; cờ đã-gieo-mẫu là lưới đỡ cho trường hợp dữ liệu đồng ý bị xoá riêng lẻ.
     */
    private fun isReturningUser(context: Context): Boolean =
        ConsentStore.hasEverAccepted(context) || MessageTemplateStore.hasSeeded(context)

    /**
     * Thi hành đúng những gì [release] khai. Chỉ chạy trên máy CẬP NHẬT, không bao giờ khi cài mới.
     *
     * Phải AN TOÀN KHI CHẠY LẠI: gộp mẫu dựa trên tập key đã chào, xoá cache là xoá file — lặp lại đều vô hại.
     */
    private fun apply(context: Context, release: Release) {
        // Mẫu tin nhắn: CHỈ THÊM, không đè/xoá mẫu người dùng.
        // Bản không khai mẫu nào thì KHÔNG gọi — tránh kích hoạt gieo mẫu sớm cho máy chưa từng mở màn mẫu.
        if (release.newTemplates.isNotEmpty()) {
            MessageTemplateStore.mergeSystemTemplates(context, release.newTemplates)
        }

        // Reset cache từ xa: buộc tải lại NGAY ở lần mở màn kế tiếp, bỏ qua TTL.
        if (release.resetAgencyCache) AgencyRepository.clearCache(context)
        if (release.resetLegalCache) LegalRepository.clearCache(context)
    }
}

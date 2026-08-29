package com.antimobile.mcas.util

import android.content.Context

/**
 * Nhớ "đã thông báo tới bản nào" cho hai modal lúc vào app (chính sách / tính năng mới), để mỗi tin chỉ
 * hiện ĐÚNG MỘT LẦN.
 *
 * Không chứa nội dung nào — mọi thứ đọc từ [Releases]. Cả hai hàm `…ToShow` đều dựa trên cùng một phép
 * "những bản đã xảy ra kể từ lúc máy này được báo, cho tới bản đang chạy" ([Releases.since]), nên người
 * dùng **nhảy cóc** phiên bản vẫn nhận đủ, gộp lại thành một modal.
 *
 * Người dùng CÀI MỚI được [markAllSeen] ngay trong [AppMigrations] nên không bao giờ thấy tin của bản
 * họ chưa từng dùng.
 */
object WhatsNewStore {

    private const val PREFS = "whatsnew_prefs"
    private const val KEY_POLICY_SEEN = "policy_seen"
    private const val KEY_FEATURE_SEEN = "feature_seen"

    /**
     * Nội dung modal chính sách cần hiện, hoặc `null` nếu không có gì mới.
     *
     * Nhiều bản cùng đổi chính sách trong khoảng chưa xem ⇒ lấy nội dung của bản MỚI NHẤT (nó đã bao
     * hàm các thay đổi trước đó).
     */
    fun policyMessageToShow(context: Context): String? =
        releasesSince(context, KEY_POLICY_SEEN).lastOrNull { it.policyMessage != null }?.policyMessage

    fun shouldShowPolicy(context: Context): Boolean = policyMessageToShow(context) != null

    /** Gạch đầu dòng tính năng chưa xem, gộp mọi bản trong khoảng. Rỗng ⇒ không có gì để báo. */
    fun featuresToShow(context: Context): List<String> =
        releasesSince(context, KEY_FEATURE_SEEN).flatMap { it.features }

    fun shouldShowFeatures(context: Context): Boolean = featuresToShow(context).isNotEmpty()

    /** Ghi nhận đã báo tới ĐÚNG bản đang chạy — mọi tin của bản `<=` bản này sẽ không hiện lại. */
    fun markPolicySeen(context: Context) {
        prefs(context).edit().putInt(KEY_POLICY_SEEN, AppVersion.code(context)).apply()
    }

    fun markFeatureSeen(context: Context) {
        prefs(context).edit().putInt(KEY_FEATURE_SEEN, AppVersion.code(context)).apply()
    }

    /**
     * CÀI MỚI: coi như đã báo hết → không làm phiền người dùng mới bằng tin của bản cũ.
     *
     * Dùng `commit()` (ĐỒNG BỘ, xuống đĩa ngay) để BỀN trước khi [AppMigrations.run] ghi dấu trang phiên
     * bản. Nếu chỉ `apply()` mà tiến trình bị giết đột ngột, dấu trang sống sót còn cờ "đã xem" thì mất
     * → người dùng mới bị hiện modal của bản họ chưa từng dùng.
     */
    fun markAllSeen(context: Context) {
        val current = AppVersion.code(context)
        prefs(context).edit()
            .putInt(KEY_POLICY_SEEN, current)
            .putInt(KEY_FEATURE_SEEN, current)
            .commit()
    }

    private fun releasesSince(context: Context, seenKey: String): List<Release> = Releases.since(
        seenVersion = prefs(context).getInt(seenKey, 0),
        currentVersion = AppVersion.code(context)
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

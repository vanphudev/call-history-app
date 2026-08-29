package com.antimobile.mcas.i18n

import android.content.res.Resources

/** Các NGÔN NGỮ app thực sự hỗ trợ. [tag] dùng cho recognizer giọng nói / BCP-47 khi cần. */
enum class Lang(val tag: String) {
    VI("vi"),
    EN("en");

    /** Bảng chuỗi tương ứng với ngôn ngữ này. */
    val strings: AppStrings
        get() = when (this) {
            VI -> ViStrings
            EN -> EnStrings
        }
}

/**
 * LỰA CHỌN ngôn ngữ NGƯỜI DÙNG lưu (khác với [Lang] đã GIẢI ra để áp thực tế):
 * - [SYSTEM]: bám theo ngôn ngữ MÁY — tiếng Việt→VI, tiếng Anh→EN, ngôn ngữ khác→VI (mặc định của app).
 * - [VI] / [EN]: người dùng ÉP CỨNG, không đổi theo máy.
 */
enum class LangPref { SYSTEM, VI, EN }

/**
 * Ngôn ngữ MÁY quy về [Lang] app hỗ trợ: `vi`→VI, `en`→EN, còn lại→VI (mặc định).
 *
 * Đọc từ [Resources.getSystem] (cấu hình THIẾT BỊ) nên KHÔNG bị ảnh hưởng bởi mọi override cấu hình ở
 * tầng app (vd ghim fontScale trong [com.antimobile.mcas.MainActivity]).
 */
fun systemLang(): Lang {
    val locales = Resources.getSystem().configuration.locales
    val primary = if (!locales.isEmpty) locales[0] else return Lang.VI
    return when (primary.language) {
        "vi" -> Lang.VI
        "en" -> Lang.EN
        else -> Lang.VI
    }
}

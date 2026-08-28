package com.antimobile.callhs.i18n

import android.content.Context
import androidx.compose.runtime.mutableStateOf

/**
 * NGÔN NGỮ trong ứng dụng — do NGƯỜI DÙNG chọn ở Cài đặt, xác định NGAY khi mở app.
 *
 * Đồng bộ cách làm với [com.antimobile.callhs.util.FontScaleSettings]: lưu bằng SharedPreferences + giữ một
 * snapshot-state để đổi ngôn ngữ là CẢ app cập nhật NGAY (recompose), KHÔNG cần khởi động lại Activity.
 *
 * Mô hình: lưu [LangPref] (SYSTEM/VI/EN); [lang] là ngôn ngữ ĐÃ GIẢI để áp thực tế. Mặc định SYSTEM → bám
 * theo máy (vi→VI, en→EN, khác→VI). Gọi [init] MỘT LẦN thật sớm (trước setContent, xem MainActivity.onCreate)
 * để khung hình đầu đã đúng ngôn ngữ, không nhấp nháy.
 */
object LanguageSettings {

    // Dùng CHUNG file prefs "display_settings" với FontScaleSettings (đều là cài đặt hiển thị), khác KEY.
    private const val PREFS = "display_settings"
    private const val KEY_LANG_PREF = "language_pref"

    // Nguồn sự thật cho UI: đọc trong @Composable sẽ tự recompose khi [set] đổi giá trị.
    private val prefState = mutableStateOf(LangPref.SYSTEM)
    private val langState = mutableStateOf(Lang.VI)

    /** Lựa chọn NGƯỜI DÙNG đang lưu (để tô mục đang chọn ở màn picker). */
    val pref: LangPref get() = prefState.value

    /** Ngôn ngữ ĐÃ GIẢI đang áp. Đọc TRONG @Composable → tự recompose khi đổi. */
    val lang: Lang get() = langState.value

    /** Bảng chuỗi hiện hành. */
    val strings: AppStrings get() = langState.value.strings

    /** Ngôn ngữ MÁY hiện quy về [Lang] app hỗ trợ — cho phụ đề mục "Theo hệ thống" ở picker. */
    val systemResolved: Lang get() = systemLang()

    /** Đọc bảng chuỗi đã lưu mà không thay đổi Compose state; an toàn cho receiver/service chạy nền. */
    fun stringsFor(context: Context): AppStrings = resolve(readPref(context.applicationContext)).strings

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * Nạp lựa chọn đã lưu + GIẢI ra [lang]. Gọi MỘT LẦN trước setContent (xem MainActivity.onCreate).
     */
    fun init(context: Context) {
        val p = readPref(context)
        prefState.value = p
        langState.value = resolve(p)
    }

    /** Ghi lựa chọn mới: cập nhật state (app đổi NGAY) + lưu bền vào prefs. */
    fun set(context: Context, p: LangPref) {
        prefState.value = p
        langState.value = resolve(p)
        prefs(context).edit().putString(KEY_LANG_PREF, p.name).apply()
    }

    /**
     * GIẢI lại ngôn ngữ khi cấu hình MÁY đổi (chỉ khi đang ở chế độ SYSTEM). Gọi từ
     * MainActivity.onConfigurationChanged để đổi ngôn ngữ máy lúc đang mở app được phản ánh ngay.
     */
    fun onConfigChanged() {
        if (prefState.value == LangPref.SYSTEM) langState.value = resolve(LangPref.SYSTEM)
    }

    private fun resolve(p: LangPref): Lang = when (p) {
        LangPref.VI -> Lang.VI
        LangPref.EN -> Lang.EN
        LangPref.SYSTEM -> systemLang()
    }

    private fun readPref(context: Context): LangPref {
        val name = prefs(context).getString(KEY_LANG_PREF, null) ?: return LangPref.SYSTEM
        return runCatching { LangPref.valueOf(name) }.getOrDefault(LangPref.SYSTEM)
    }
}

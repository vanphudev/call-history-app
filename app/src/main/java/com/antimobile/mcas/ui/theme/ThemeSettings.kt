package com.antimobile.mcas.ui.theme

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.mutableStateOf

/**
 * CHẾ ĐỘ GIAO DIỆN (Sáng / Tối) trong ứng dụng — do NGƯỜI DÙNG chọn ở Cài đặt.
 *
 * Đồng bộ cách làm với [com.antimobile.mcas.i18n.LanguageSettings] và
 * [com.antimobile.mcas.util.FontScaleSettings]: lưu bằng SharedPreferences + giữ snapshot-state để đổi chế độ
 * là CẢ app tô lại NGAY (recompose), KHÔNG khởi động lại Activity.
 *
 * Mô hình: lưu [Pref] (SYSTEM/LIGHT/DARK). [dark] là kết quả ĐÃ GIẢI (true = tối) để áp thực tế:
 *  - LIGHT/DARK → cố định.
 *  - SYSTEM → bám theo chế độ tối của MÁY ([systemDarkState]): nạp lần đầu ở [init]; cập nhật khi máy đổi
 *    Sáng/Tối qua MainActivity.onConfigurationChanged/onResume (đọc cấu hình THẬT của thiết bị bằng [isSystemNight]
 *    rồi gọi [setSystemDark]).
 *
 * [colors] trả về [Palette] đang áp; mọi token màu trong [Color.kt] đọc qua đây nên đổi [dark] là đổi cả app.
 * Gọi [init] MỘT LẦN thật sớm (trước setContent, xem MainActivity.onCreate) để khung hình đầu đã đúng chế độ,
 * tránh nhấp nháy sáng→tối.
 */
object ThemeSettings {

    enum class Pref { SYSTEM, LIGHT, DARK }

    // Dùng CHUNG file prefs "display_settings" với Font/Language (đều là cài đặt hiển thị), khác KEY.
    private const val PREFS = "display_settings"
    private const val KEY_THEME_PREF = "theme_pref"

    // Nguồn sự thật cho UI: đọc trong @Composable sẽ tự recompose khi đổi.
    private val prefState = mutableStateOf(Pref.SYSTEM)
    private val systemDarkState = mutableStateOf(false)

    /** Lựa chọn NGƯỜI DÙNG đang lưu (để tô mục đang chọn ở màn picker). */
    val pref: Pref get() = prefState.value

    /** Đang ở chế độ TỐI hay không (đã giải). Đọc TRONG @Composable → tự recompose khi đổi. */
    val dark: Boolean
        get() = when (prefState.value) {
            Pref.LIGHT -> false
            Pref.DARK -> true
            Pref.SYSTEM -> systemDarkState.value
        }

    /** Palette đang áp. Mọi token màu (Color.kt) đọc trường của nó. */
    val colors: Palette get() = if (dark) DarkPalette else LightPalette

    /** Chế độ tối của MÁY hiện tại — cho phụ đề mục "Theo hệ thống" ở picker. */
    val systemDark: Boolean get() = systemDarkState.value

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * Nạp lựa chọn đã lưu + chế độ tối HỆ THỐNG hiện tại. Gọi MỘT LẦN trước setContent (xem MainActivity.onCreate).
     */
    fun init(context: Context) {
        prefState.value = readPref(context)
        systemDarkState.value = isSystemNight()
    }

    /** Ghi lựa chọn mới: cập nhật state (app đổi NGAY) + lưu bền vào prefs. */
    fun set(context: Context, p: Pref) {
        prefState.value = p
        prefs(context).edit().putString(KEY_THEME_PREF, p.name).apply()
    }

    /**
     * Cập nhật chế độ tối của MÁY (MainActivity gọi khi onConfigurationChanged/onResume, truyền [isSystemNight]),
     * để chế độ SYSTEM phản ánh ngay khi người dùng đổi Dark/Light của máy lúc đang mở app. Có guard idempotent.
     */
    fun setSystemDark(dark: Boolean) {
        if (systemDarkState.value != dark) systemDarkState.value = dark
    }

    /**
     * Máy có đang ở chế độ TỐI không.
     *
     * QUAN TRỌNG — đọc từ [Resources.getSystem] (cấu hình THẬT của thiết bị), KHÔNG từ context của Activity.
     * Lý do: [com.antimobile.mcas.MainActivity.attachBaseContext] ghim fontScale bằng cách SAO NGUYÊN
     * Configuration rồi tạo context override → toàn bộ trường (kể cả bit NIGHT của uiMode) bị ĐÓNG BĂNG ở giá
     * trị lúc khởi động. Nên đọc uiMode qua context/Activity (và cả Compose `isSystemInDarkTheme`, vốn lấy từ
     * config của Activity) sẽ KHÔNG đổi khi máy chuyển Sáng↔Tối lúc app đang mở. `Resources.getSystem()` phản
     * ánh cấu hình thiết bị hiện thời — đúng cách [com.antimobile.mcas.i18n.Lang] đã xử lý cho LOCALE.
     */
    fun isSystemNight(): Boolean =
        (Resources.getSystem().configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    private fun readPref(context: Context): Pref {
        val name = prefs(context).getString(KEY_THEME_PREF, null) ?: return Pref.SYSTEM
        return runCatching { Pref.valueOf(name) }.getOrDefault(Pref.SYSTEM)
    }
}

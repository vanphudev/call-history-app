package com.antimobile.callhs.util

import android.content.Context
import androidx.compose.runtime.mutableFloatStateOf
import com.antimobile.callhs.i18n.appStrings

/**
 * CỠ CHỮ trong ứng dụng — do NGƯỜI DÙNG chọn, ĐỘC LẬP hoàn toàn với "Cỡ chữ / Cỡ hiển thị" của hệ thống.
 *
 * Vì sao tự quản: thiết kế của app dựa trên thang chữ [com.antimobile.callhs.ui.theme.Typography] cố định
 * (heading đậm, letter-spacing siết). Nếu để hệ điều hành phóng chữ theo cài đặt của máy (nhiều máy đẩy tới
 * 150–200%) thì layout dễ VỠ. Nên app CHẶN cỡ chữ hệ thống (xem [com.antimobile.callhs.MainActivity]
 * attachBaseContext ghim fontScale = 1.0) và CHỈ áp HỆ SỐ do người dùng chọn ở đây (xem
 * [com.antimobile.callhs.ui.theme.CallHSTheme] override LocalDensity).
 *
 * Lưu bằng SharedPreferences (đồng bộ cách làm với [SmsSettings] / [MyNumberStore]); kèm một snapshot-state
 * để đổi cỡ chữ là CẢ app cập nhật NGAY, không cần khởi động lại màn hình.
 */
object FontScaleSettings {

    private const val PREFS = "display_settings"
    private const val KEY_FONT_SCALE = "font_scale"

    /** Hệ số MẶC ĐỊNH = 1.0 (đúng như thiết kế gốc, tương đương cỡ chữ hệ thống 100%). */
    const val DEFAULT = 1.0f

    /** Mức cỡ chữ (TÊN hiển thị đổi theo ngôn ngữ — resolve qua [label]). */
    enum class Tier { SMALL, DEFAULT, LARGE, XLARGE }

    /** Một mức cỡ chữ: [tier] để lấy nhãn ĐA NGÔN NGỮ, [scale] là hệ số nhân lên cỡ chữ gốc. */
    data class Option(val tier: Tier, val scale: Float)

    /**
     * Các mức cho người dùng chọn. Giới hạn 0.90–1.30 (± quanh mặc định) để KHÔNG vỡ giao diện —
     * đủ nhỏ/lớn cho nhu cầu thực tế mà layout vẫn an toàn. Muốn thêm mức chỉ cần bổ sung vào danh sách.
     */
    val OPTIONS = listOf(
        Option(Tier.SMALL, 0.90f),
        Option(Tier.DEFAULT, 1.00f),
        Option(Tier.LARGE, 1.15f),
        Option(Tier.XLARGE, 1.30f)
    )

    /** Nhãn hiển thị (ĐA NGÔN NGỮ) cho một mức cỡ chữ. Đọc TRONG @Composable → tự đổi khi chuyển ngôn ngữ. */
    fun label(tier: Tier): String = with(appStrings().fontSize) {
        when (tier) {
            Tier.SMALL -> small
            Tier.DEFAULT -> default
            Tier.LARGE -> large
            Tier.XLARGE -> xlarge
        }
    }

    private val MIN = OPTIONS.minOf { it.scale }
    private val MAX = OPTIONS.maxOf { it.scale }

    // Nguồn sự thật cho UI: đọc trong composable sẽ TỰ cập nhật khi [set] đổi giá trị.
    private val state = mutableFloatStateOf(DEFAULT)

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * Nạp hệ số đã lưu vào snapshot-state. Gọi MỘT LẦN thật sớm (trước setContent, xem MainActivity.onCreate)
     * để lần vẽ đầu đã đúng cỡ chữ, tránh nhấp nháy từ mặc định sang giá trị đã lưu.
     */
    fun init(context: Context) {
        state.floatValue = read(context)
    }

    /** Hệ số hiện tại. Đọc TRONG @Composable → tự cập nhật (recompose) khi người dùng đổi lựa chọn. */
    val scale: Float
        get() = state.floatValue

    /** Ghi lựa chọn mới: cập nhật state (app đổi NGAY) + lưu bền vào prefs. Giá trị được kẹp trong [MIN]..[MAX]. */
    fun set(context: Context, scale: Float) {
        val v = scale.coerceIn(MIN, MAX)
        state.floatValue = v
        prefs(context).edit().putFloat(KEY_FONT_SCALE, v).apply()
    }

    private fun read(context: Context): Float =
        prefs(context).getFloat(KEY_FONT_SCALE, DEFAULT).coerceIn(MIN, MAX)
}

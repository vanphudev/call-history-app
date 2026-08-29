package com.antimobile.mcas.util

import com.antimobile.mcas.i18n.appStrings
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Dữ liệu để thay các pattern động trong nội dung mẫu tin nhắn.
 * - [simNumbers]→ {phonesim1..N}: số theo từng khe SIM (index 0 = SIM 1); phần tử có thể rỗng.
 * - [qrText]    → {contextqr}: kết quả quét mã QR (rỗng nếu chưa quét).
 * - [nowMillis] → mốc thời gian điền {date}/{datetime}/{timedate}/{weekdate}.
 */
data class TemplateContext(
    val simNumbers: List<String>,
    val qrText: String,
    val nowMillis: Long
)

/**
 * Thay thế các pattern {…} trong nội dung mẫu. Quy tắc (theo yêu cầu):
 *  - Pattern HỢP LỆ → điền giá trị tương ứng (giá trị có thể rỗng, ví dụ số SIM máy không đọc được).
 *  - Pattern KHÔNG hợp lệ (không nằm trong danh sách) → GIỮ NGUYÊN, không báo lỗi.
 *
 * Hỗ trợ: {date} {datetime} {timedate} {weekdate} {phonesim1..N} {contextqr}.
 * Không phân biệt hoa/thường (VD {DATE} = {date}).
 * ({phone} không còn là chip gợi ý nhưng VẪN nhận diện được — coi như {phonesim1} — để mẫu cũ không hỏng.)
 */
object TemplateFill {

    private val zone: ZoneId = ZoneId.systemDefault()
    private val vi: Locale = Locale.forLanguageTag("vi-VN")
    private val dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy", vi)
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", vi)

    /**
     * Bắt các token dạng {abc123}. Chỉ chữ + số bên trong để không nuốt dấu ngoặc thường của người dùng.
     * LƯU Ý: PHẢI escape cả `\}` — bộ máy regex ICU của Android ném PatternSyntaxException với `}` trần
     * (dù JVM desktop chấp nhận, nên unit test không bắt được lỗi này).
     */
    private val tokenRegex = Regex("\\{[A-Za-z0-9]+\\}")

    fun fill(content: String, ctx: TemplateContext): String =
        tokenRegex.replace(content) { m -> resolve(m.value, ctx) ?: m.value }

    /** Trả giá trị thay thế cho một token, hoặc null nếu token không hợp lệ (→ giữ nguyên). */
    private fun resolve(token: String, ctx: TemplateContext): String? {
        val key = token.trim('{', '}').lowercase(Locale.ROOT)
        val zdt = Instant.ofEpochMilli(ctx.nowMillis).atZone(zone)
        return when {
            key == "date" -> zdt.format(dateFmt)
            key == "datetime" -> "${zdt.format(dateFmt)} ${zdt.format(timeFmt)}"
            key == "timedate" -> "${zdt.format(timeFmt)} ${zdt.format(dateFmt)}"
            key == "weekdate" -> "${weekdayLabel(zdt.dayOfWeek)}, ${zdt.format(dateFmt)}"
            key == "phone" -> slotNumber(1, ctx) // tương thích ngược: {phone} = số SIM 1
            key == "contextqr" -> ctx.qrText
            key.startsWith("phonesim") -> {
                val n = key.removePrefix("phonesim").toIntOrNull() ?: return null // {phonesimX} lạ → giữ nguyên
                if (n < 1) return null
                slotNumber(n, ctx)
            }
            else -> null
        }
    }

    /**
     * Số đã định dạng cho khe [n] (1 = SIM 1). Nếu khe đó KHÔNG có số (vd mẫu dùng {phonesim2} nhưng máy
     * chỉ còn 1 SIM, hoặc chưa nhập số cho SIM đó) → MẶC ĐỊNH lấy số SIM 1. Cả hai cùng rỗng → chuỗi rỗng.
     */
    private fun slotNumber(n: Int, ctx: TemplateContext): String {
        val direct = ctx.simNumbers.getOrNull(n - 1).orEmpty()
        val resolved = direct.ifBlank { ctx.simNumbers.getOrNull(0).orEmpty() }
        return prettyPhone(resolved)
    }

    private fun prettyPhone(number: String): String =
        if (number.isBlank()) "" else formatPhone(number)

    private fun weekdayLabel(dow: DayOfWeek): String = appStrings().datetime.weekdayShort(dow)

    /**
     * Danh sách pattern gợi ý (token → mô tả) để hiện & chèn nhanh trong màn tạo/sửa mẫu.
     * [simCount] = số SIM đang hoạt động: chỉ hiện {phonesim2} khi máy đang có TỪ 2 SIM; máy 1 SIM (hoặc
     * chưa xác định) chỉ hiện {phonesim1} (theo yêu cầu).
     */
    fun hints(simCount: Int): List<Pair<String, String>> = with(appStrings().templateEditor) {
        buildList {
            add("{date}" to hintDate)
            add("{datetime}" to hintDatetime)
            add("{timedate}" to hintTimedate)
            add("{weekdate}" to hintWeekdate)
            add("{phonesim1}" to hintPhonesim1)
            if (simCount >= 2) add("{phonesim2}" to hintPhonesim2)
            add("{contextqr}" to hintContextqr)
        }
    }
}

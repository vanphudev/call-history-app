package com.antimobile.mcas.util

/**
 * Chuẩn hoá số điện thoại Việt Nam về DẠNG QUỐC TẾ không dấu "+" (ví dụ 0987654321 -> 84987654321).
 *
 * - Bỏ mọi ký tự thừa: khoảng trắng, '-', '.', '(', ')', '+'…
 * - Bỏ mã truy cập quốc tế "00" ở đầu (vd 0084… -> 84…).
 * - Nội địa "0" + NSN -> đổi "0" đầu thành "84"; đã "84…" -> giữ nguyên.
 * - NSN trần 9 số (di động thiếu số 0) -> thêm "84".
 * - Số KHÁC (quốc tế không phải VN, hotline, mã ngắn…) -> GIỮ NGUYÊN, KHÔNG gắn "84" bừa (tránh tạo số VN ma).
 */
object PhoneNormalizer {

    fun toIntl(raw: String): String {
        var digits = raw.filter { it.isDigit() }
        if (digits.isEmpty()) return digits
        if (digits.length > 2 && digits.startsWith("00")) digits = digits.substring(2)
        return when {
            digits.startsWith("84") && digits.length >= 11 -> digits         // đã là quốc tế VN
            digits.startsWith("0") && digits.length >= 10 -> "84" + digits.drop(1) // nội địa 0 + NSN
            digits.length == 9 -> "84$digits"                                // NSN di động trần
            else -> digits                                                   // không rõ là số VN → để nguyên
        }
    }
}

package com.antimobile.callhs.util

/**
 * Chuẩn hoá số điện thoại Việt Nam về DẠNG QUỐC TẾ không dấu "+" (ví dụ 0987654321 -> 84987654321).
 *
 * - Bỏ mọi ký tự thừa: khoảng trắng, '-', '.', '(', ')', '+'…
 * - Bắt đầu bằng "0"  -> thay "0" đầu bằng "84".
 * - Đã bắt đầu bằng "84" -> giữ nguyên (đã là quốc tế).
 * - Còn lại (số VN thiếu số 0 đầu) -> thêm "84" phía trước.
 */
object PhoneNormalizer {

    fun toIntl(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        if (digits.isEmpty()) return digits
        return when {
            digits.startsWith("84") -> digits
            digits.startsWith("0") -> "84" + digits.drop(1)
            else -> "84$digits"
        }
    }
}

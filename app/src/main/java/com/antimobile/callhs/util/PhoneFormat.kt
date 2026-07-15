package com.antimobile.callhs.util

/** Định dạng số gọn cho dễ đọc (kiểu "0123 456 789"). Giữ nguyên nếu không phải số thuần. */
fun formatPhone(raw: String): String {
    if (raw.isBlank()) return raw
    if (raw.any { !it.isDigit() && it != '+' && it != ' ' }) return raw
    val plus = raw.startsWith("+")
    var digits = raw.filter { it.isDigit() }
    // Dạng quốc tế VN ("+84…"/"84…" = 84 + NSN) → đưa về nội địa "0…" để chia nhóm ĐÚNG (tránh "+8491 234 5678").
    val vnIntl = digits.startsWith("84") && (digits.length == 11 || digits.length == 12)
    if (vnIntl) digits = "0" + digits.substring(2)
    val grouped = when (digits.length) {
        10, 11 -> "${digits.substring(0, 4)} ${digits.substring(4, 7)} ${digits.substring(7)}"
        9 -> "${digits.substring(0, 3)} ${digits.substring(3, 6)} ${digits.substring(6)}"
        else -> return raw
    }
    // Đã quy về "0…" thì bỏ dấu "+"; số quốc tế KHÁC vẫn giữ "+".
    return if (plus && !vnIntl) "+$grouped" else grouped
}

package com.antimobile.callhs.util

/** Định dạng số gọn cho dễ đọc (kiểu "0123 456 789"). Giữ nguyên nếu không phải số thuần. */
fun formatPhone(raw: String): String {
    if (raw.isBlank()) return raw
    if (raw.any { !it.isDigit() && it != '+' && it != ' ' }) return raw
    val plus = raw.startsWith("+")
    val digits = raw.filter { it.isDigit() }
    val grouped = when (digits.length) {
        10 -> "${digits.substring(0, 4)} ${digits.substring(4, 7)} ${digits.substring(7)}"
        11 -> "${digits.substring(0, 4)} ${digits.substring(4, 7)} ${digits.substring(7)}"
        9 -> "${digits.substring(0, 3)} ${digits.substring(3, 6)} ${digits.substring(6)}"
        else -> return raw
    }
    return if (plus) "+$grouped" else grouped
}

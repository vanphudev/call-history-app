package com.antimobile.callhs.util

/**
 * Khoá so khớp số điện thoại: chỉ giữ chữ số, lấy 9 số cuối (để `+84…` khớp `0…`).
 *
 * PHẢI đồng nhất thuật toán với `CallLogRepository.phoneKey` (private companion) — nhóm phân loại
 * lưu số theo khoá này, badge & bộ lọc CallList so khớp số của nhật ký cũng theo khoá này, nên nếu
 * lệch thuật toán thì số lưu sẽ không match số trong log.
 */
object PhoneKey {
    fun of(raw: String): String {
        val digits = raw.filter(Char::isDigit)
        return if (digits.length >= 9) digits.takeLast(9) else digits
    }
}

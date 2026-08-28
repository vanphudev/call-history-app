package com.antimobile.callhs.data.messaging

/** Tách danh sách người nhận từ phần sau scheme của sms:/smsto: mà không phụ thuộc Android UI. */
object SmsRecipientParser {
    fun parse(rawSchemeSpecificPart: String): List<String> = rawSchemeSpecificPart
        .substringBefore('?')
        .split(',', ';')
        .map(String::trim)
        .filter(String::isNotEmpty)

    /** Cho phép số thường/quốc tế với ký tự định dạng; không nhầm tên có chứa một chữ số thành địa chỉ SMS. */
    fun isValidAddress(address: String): Boolean {
        val value = address.trim()
        if (value.none(Char::isDigit)) return false
        return value.all { char ->
            char.isDigit() || char.isWhitespace() || char in "+-()./"
        }
    }
}

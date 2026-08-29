package com.antimobile.mcas.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Định dạng tiền VND kiểu Việt Nam: "1.234 đ" (dấu chấm phân tách hàng nghìn). 0/âm → "0 đ". */
private val vndFormat = DecimalFormat(
    "#,###",
    DecimalFormatSymbols(Locale.forLanguageTag("vi-VN")).apply { groupingSeparator = '.' }
)

/** "12.345 đ" — số tiền đầy đủ. */
fun formatDong(amount: Long): String = if (amount <= 0L) "0 đ" else "${vndFormat.format(amount)} đ"

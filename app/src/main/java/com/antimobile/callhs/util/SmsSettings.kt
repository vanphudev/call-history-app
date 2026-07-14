package com.antimobile.callhs.util

import android.content.Context

/**
 * Tuỳ chọn liên quan tới việc GỬI SMS, lưu trong SharedPreferences (không cần cơ sở dữ liệu).
 *
 * [isRemoveDiacritics]: bật/tắt việc BỎ DẤU tiếng Việt + ký tự Unicode đặc biệt KHI chuyển nội dung sang ứng
 * dụng nhắn tin (để tin dùng bảng mã GSM‑7, tiết kiệm phí). MẶC ĐỊNH TẮT — người dùng tự bật. Nội dung lưu &
 * hiển thị trong ứng dụng KHÔNG bị ảnh hưởng (xem [SmsText] và [CallActions]).
 */
object SmsSettings {

    private const val PREFS = "sms_settings"
    private const val KEY_REMOVE_DIACRITICS = "remove_diacritics_on_send"

    fun isRemoveDiacritics(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_REMOVE_DIACRITICS, false)

    fun setRemoveDiacritics(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_REMOVE_DIACRITICS, enabled).apply()
    }
}

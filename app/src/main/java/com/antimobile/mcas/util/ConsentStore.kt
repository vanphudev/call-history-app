package com.antimobile.mcas.util

import android.content.Context

/**
 * Lưu trạng thái người dùng ĐÃ ĐỒNG Ý với Điều khoản sử dụng & Chính sách quyền riêng tư.
 *
 * Đây là bước cuối BẮT BUỘC sau các quyền nền tảng và quyền SMS (trên thiết bị hỗ trợ) — xem
 * [com.antimobile.mcas.ui.permissions.PermissionGate]. Chỉ hỏi MỘT lần; đã đồng ý thì các lần mở
 * sau bỏ qua màn này.
 *
 * Dùng số [CURRENT_VERSION]: khi điều khoản/chính sách thay đổi ĐÁNG KỂ, tăng số này lên để yêu cầu
 * người dùng đồng ý lại (đồng ý phiên bản cũ sẽ không còn tính là hợp lệ).
 */
object ConsentStore {
    private const val PREFS = "consent_prefs"
    private const val KEY_ACCEPTED_VERSION = "accepted_version"

    /** Tăng khi điều khoản/chính sách thay đổi đáng kể để hỏi lại. */
    const val CURRENT_VERSION = 1

    /** Đã đồng ý phiên bản điều khoản hiện tại (hoặc mới hơn) hay chưa. */
    fun isAccepted(context: Context): Boolean =
        prefs(context).getInt(KEY_ACCEPTED_VERSION, 0) >= CURRENT_VERSION

    /**
     * Đã từng đồng ý BẤT KỲ phiên bản điều khoản nào — kể cả bản cũ đã hết hiệu lực.
     *
     * Vì đồng ý điều khoản là cửa BẮT BUỘC để vào app, giá trị `true` là bằng chứng chắc chắn máy này
     * "đã dùng app trước đây". [AppMigrations] dùng nó để phân biệt CẬP NHẬT với CÀI MỚI.
     */
    fun hasEverAccepted(context: Context): Boolean =
        prefs(context).getInt(KEY_ACCEPTED_VERSION, 0) > 0

    /** Ghi nhận người dùng đồng ý với phiên bản điều khoản hiện tại. */
    fun accept(context: Context) {
        prefs(context).edit().putInt(KEY_ACCEPTED_VERSION, CURRENT_VERSION).apply()
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

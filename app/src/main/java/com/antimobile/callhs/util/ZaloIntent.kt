package com.antimobile.callhs.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Tạo Intent điều hướng tới Zalo qua APP LINK CÔNG KHAI `https://zalo.me/<số quốc tế>`.
 *
 * VÌ SAO dùng https://zalo.me/… thay vì `tel:`? `tel:` chỉ mở TRÌNH QUAY SỐ, không mở được trang hồ sơ
 * Zalo. Link `https://zalo.me/<số>` là App Link công khai của Zalo → khi có Zalo, hệ thống mở THẲNG
 * Zalo và Zalo tự hiện popup hồ sơ của số đó. Đây là cách cho tỉ lệ hiện profile CAO hơn nên ta dùng nó.
 *
 * QUAN TRỌNG — popup hồ sơ (avatar, tên tài khoản, số điện thoại, "Trò chuyện", "Gọi miễn phí",
 * "Kết bạn") là UI NỘI BỘ do ZALO tự render sau khi nhận Intent. Ứng dụng KHÔNG đọc được:
 *   - avatar
 *   - tên
 *   - uid
 *   - trạng thái kết bạn
 * Tất cả đều do Zalo hiển thị. Ở đây CHỈ dùng Android SDK công khai (ACTION_VIEW + https link +
 * setPackage). KHÔNG dùng intent riêng/API nội bộ của Zalo, không reflection/hidden API/accessibility.
 */
object ZaloIntent {

    /** Package công khai của ứng dụng Zalo — chỉ dùng để mở THẲNG Zalo (không hiện hộp chọn ứng dụng). */
    const val ZALO_PACKAGE = "com.zing.zalo"

    private fun zaloMeUri(phoneIntl: String): Uri = Uri.parse("https://zalo.me/$phoneIntl")

    /** Intent mở THẲNG Zalo tới trang số điện thoại (ép package = Zalo → bỏ qua chooser). */
    fun openInZalo(phoneIntl: String): Intent =
        Intent(Intent.ACTION_VIEW, zaloMeUri(phoneIntl)).apply {
            setPackage(ZALO_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    /** Dự phòng khi CHƯA cài Zalo: mở link zalo.me bằng trình duyệt (không ép package). */
    fun openInBrowser(phoneIntl: String): Intent =
        Intent(Intent.ACTION_VIEW, zaloMeUri(phoneIntl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    /** Có Zalo cài & xử lý được link không (cần khai báo `<queries>` gói com.zing.zalo trong manifest). */
    fun isAvailable(context: Context, phoneIntl: String): Boolean =
        openInZalo(phoneIntl).resolveActivity(context.packageManager) != null
}

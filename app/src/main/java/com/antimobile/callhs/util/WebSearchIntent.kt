package com.antimobile.callhs.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Tạo Intent mở trang KẾT QUẢ tìm kiếm Google cho một câu truy vấn.
 *
 * ƯU TIÊN mở bằng **Chrome** (trình duyệt của Google) qua `setPackage` để bỏ qua hộp chọn ứng dụng;
 * nếu máy KHÔNG có Chrome thì nơi gọi lùi về [openInBrowser] (trình duyệt mặc định).
 *
 * Chỉ dùng Android SDK công khai (ACTION_VIEW + link https). Cần khai báo `<queries>` gói
 * `com.android.chrome` + intent VIEW/https trong manifest để `resolveActivity` thấy được Chrome
 * trên Android 11+ (package visibility).
 */
object WebSearchIntent {

    /** Package của Chrome bản chính thức (stable). */
    const val CHROME_PACKAGE = "com.android.chrome"

    /** URL trang kết quả Google cho [query] (query được mã hoá an toàn). */
    fun searchUrl(query: String): Uri =
        Uri.parse("https://www.google.com/search?q=" + Uri.encode(query))

    /** Intent mở THẲNG Chrome tới trang kết quả Google (ép package = Chrome → bỏ qua chooser). */
    fun openInChrome(query: String): Intent =
        Intent(Intent.ACTION_VIEW, searchUrl(query)).apply {
            setPackage(CHROME_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    /** Dự phòng: mở trang kết quả bằng trình duyệt MẶC ĐỊNH (không ép package). */
    fun openInBrowser(query: String): Intent =
        Intent(Intent.ACTION_VIEW, searchUrl(query)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    /** Máy có Chrome cài & xử lý được link không (cần `<queries>` gói com.android.chrome). */
    fun isChromeAvailable(context: Context, query: String): Boolean =
        openInChrome(query).resolveActivity(context.packageManager) != null
}

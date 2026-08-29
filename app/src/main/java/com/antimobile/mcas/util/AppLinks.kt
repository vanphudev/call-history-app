package com.antimobile.mcas.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.antimobile.mcas.i18n.appStrings
import com.antimobile.mcas.ui.components.AppToastType

/**
 * NƠI DUY NHẤT chứa danh tính & liên kết của MCAS (đổi tài khoản/repo GitHub chỉ sửa ở đây).
 *
 * - Website chính thức + trang pháp lý: GitHub Pages `vanphudev.github.io/call-history-site`.
 * - Dữ liệu danh bạ cơ quan: tải theo thứ tự ưu tiên trong [DATA_BASES] — GitHub Pages (có CDN)
 *   trước, `raw.githubusercontent` dự phòng. App tự lưu cache + chỉ làm mới tối đa 7 ngày/lần
 *   (xem [com.antimobile.mcas.data.remote.RemoteFileCache]).
 */
object AppLinks {
    const val AUTHOR = "vanphudev"
    const val CONTACT_EMAIL = "vanphu.dev@gmail.com"

    const val SITE = "https://vanphudev.github.io/call-history-site/"
    const val PRIVACY_URL = "${SITE}privacy-policy/"
    const val TERMS_URL = "${SITE}terms/"

    /** Base URL của dữ liệu danh bạ, thử lần lượt cho tới khi thành công. */
    val DATA_BASES = listOf(
        "${SITE}data/",
        "https://raw.githubusercontent.com/vanphudev/call-history-site/main/data/"
    )

    /** Mở một URL bằng trình duyệt / ứng dụng phù hợp. Không crash nếu không có app xử lý. */
    fun openUrl(context: Context, url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure { CallActions.toast(context, appStrings().actions.linkOpenFailed, AppToastType.Error) }
    }

    /** Soạn email tới nhà phát triển (chỉ mở ứng dụng email, không tự gửi). */
    fun emailDeveloper(context: Context, subject: String = appStrings().actions.feedbackEmailSubject) {
        val uri = Uri.parse("mailto:$CONTACT_EMAIL?subject=" + Uri.encode(subject))
        runCatching {
            context.startActivity(Intent(Intent.ACTION_SENDTO, uri))
        }.onFailure { CallActions.toast(context, appStrings().common.emailOpenFailed, AppToastType.Error) }
    }
}

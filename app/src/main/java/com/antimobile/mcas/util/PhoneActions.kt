package com.antimobile.mcas.util

import android.content.Context
import android.util.Log
import com.antimobile.mcas.i18n.appStrings
import com.antimobile.mcas.ui.components.AppToastType

/**
 * Điều phối hành động theo SỐ ĐIỆN THOẠI — KHÔNG để logic trong UI.
 *
 * "Liên hệ qua Zalo": chuẩn hoá số ([PhoneNormalizer]) → mở THẲNG Zalo tới trang số đó
 * ([ZaloIntent], không hiện hộp chọn ứng dụng vì ta dùng trực tiếp Zalo). Chưa cài Zalo thì mở
 * zalo.me trên trình duyệt; lỗi thì báo toast, KHÔNG crash.
 *
 * "Tìm kiếm qua Google": dựng câu truy vấn SÂU nhiều định dạng ([PhoneSearchQuery]) → ưu tiên mở
 * bằng Chrome ([WebSearchIntent]); không có Chrome thì mở trình duyệt mặc định.
 */
object PhoneActions {

    private const val TAG = "PhoneActions"

    /**
     * Tìm SỐ ĐIỆN THOẠI trên web: dựng truy vấn SÂU (số ở nhiều định dạng: nội địa, `84…`, `+84…`)
     * rồi mở trang kết quả Google — ƯU TIÊN Chrome, sau đó là trình duyệt mặc định.
     */
    fun searchNumberOnWeb(context: Context, rawPhone: String) {
        val query = PhoneSearchQuery.buildGoogleQuery(rawPhone)
        Log.d(TAG, "Google search query: $rawPhone -> $query")
        if (query.isBlank()) {
            CallActions.toast(context, appStrings().actions.invalidPhoneSearch, AppToastType.Error)
            return
        }
        if (WebSearchIntent.isChromeAvailable(context, query)) {
            Log.d(TAG, "Open in Chrome")
            val opened = runCatching { context.startActivity(WebSearchIntent.openInChrome(query)) }.isSuccess
            if (!opened) openDefaultBrowser(context, query)
        } else {
            openDefaultBrowser(context, query)
        }
    }

    /** Dự phòng: mở trang kết quả Google bằng trình duyệt mặc định khi không mở được Chrome. */
    private fun openDefaultBrowser(context: Context, query: String) {
        Log.d(TAG, "Open in default browser")
        val opened = runCatching { context.startActivity(WebSearchIntent.openInBrowser(query)) }.isSuccess
        if (!opened) {
            CallActions.toast(context, appStrings().actions.browserOpenFailed, AppToastType.Error)
        }
    }

    fun openZalo(context: Context, rawPhone: String) {
        val intl = PhoneNormalizer.toIntl(rawPhone)
        Log.d(TAG, "Phone normalized: $rawPhone -> $intl")
        if (intl.isBlank()) {
            CallActions.toast(context, appStrings().actions.invalidPhone, AppToastType.Error)
            return
        }
        if (ZaloIntent.isAvailable(context, intl)) {
            Log.d(TAG, "Open Zalo")
            val opened = runCatching { context.startActivity(ZaloIntent.openInZalo(intl)) }.isSuccess
            if (!opened) openBrowserFallback(context, intl)
        } else {
            openBrowserFallback(context, intl)
        }
    }

    /** Dự phòng: mở zalo.me bằng trình duyệt khi không mở được app Zalo. */
    private fun openBrowserFallback(context: Context, intl: String) {
        Log.d(TAG, "Fallback")
        val opened = runCatching { context.startActivity(ZaloIntent.openInBrowser(intl)) }.isSuccess
        if (!opened) {
            CallActions.toast(context, appStrings().actions.zaloAndBrowserUnavailable, AppToastType.Error)
        }
    }
}

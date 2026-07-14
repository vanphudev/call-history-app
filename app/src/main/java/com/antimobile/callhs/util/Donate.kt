package com.antimobile.callhs.util

import android.net.Uri

/**
 * NGUỒN DUY NHẤT chứa thông tin ỦNG HỘ nhà phát triển (tài khoản nhận + cách dựng mã QR chuyển khoản).
 *
 * Đổi tài khoản chỉ sửa ở đây. Tinh thần: hoàn toàn TỰ NGUYỆN, minh bạch — mọi thông tin hiển thị rõ ràng.
 *
 * - Mã QR chuyển khoản: dùng ảnh QR có thương hiệu của VietQR (kèm logo ngân hàng + tên chủ TK + số tiền)
 *   qua [imageUrl]. Khi ngoại tuyến, [VietQrPayload] dựng chuỗi QR NAPAS/EMVCo ngay tại máy làm dự phòng.
 * - Danh sách app ngân hàng để mở nhanh: [DEEPLINK_LIST_URL] (xem [com.antimobile.callhs.data.donate]).
 */
object Donate {

    /** Mã ngân hàng NAPAS (BIN). 970416 = Ngân hàng TMCP Á Châu (ACB). */
    const val BANK_BIN = "970416"
    const val BANK_SHORT = "ACB"
    const val BANK_NAME_VI = "TMCP Á Châu (ACB)"

    /** Mã ngân hàng dùng trong deeplink VietQR (`ba=<số TK>@<mã NH>`). ACB → "acb". */
    private const val BANK_DEEPLINK_CODE = "acb"

    const val ACCOUNT_NO = "45578647"
    const val ACCOUNT_NAME = "NGUYEN VAN PHU"

    /** Mã mẫu (template) VietQR dùng trong đường dẫn ảnh. */
    const val QR_TEMPLATE = "XiL1886"

    /** Nội dung chuyển khoản mặc định (ASCII, không dấu — an toàn cho mọi app ngân hàng). */
    const val ADD_INFO = "UNG HO CALL HS"

    /** Các mức gợi ý (VND). 0 = "tuỳ tâm" (mã QR MỞ — người chuyển tự nhập số tiền). */
    val PRESET_AMOUNTS = listOf(2_000L, 5_000L, 8_000L, 10_000L, 15_000L, 20_000L)

    /** Trần số tiền cho ô "Số khác" (VND). Vượt mức này sẽ báo lỗi, không cho xác nhận. */
    const val MAX_AMOUNT = 1_000_000L

    private const val IMAGE_BASE = "https://api.vietqr.io/image/$BANK_BIN-$ACCOUNT_NO-$QR_TEMPLATE.jpg"

    /**
     * URL ảnh QR chuyển khoản có sẵn số tiền [amount] (VND, KHÔNG dấu chấm/phẩy). [amount] ≤ 0 → mã MỞ
     * (amount=0, người chuyển tự nhập số tiền khi quét). `accountName`/`addInfo` được mã hoá `%20` nhờ
     * [Uri.Builder] (đúng như đường dẫn mẫu VietQR yêu cầu).
     */
    fun imageUrl(amount: Long): String {
        val amt = if (amount > 0L) amount else 0L
        return Uri.parse(IMAGE_BASE).buildUpon()
            .appendQueryParameter("accountName", ACCOUNT_NAME)
            .appendQueryParameter("amount", amt.toString())
            .appendQueryParameter("addInfo", ADD_INFO)
            .build()
            .toString()
    }

    /** Nội dung chuỗi QR NAPAS/EMVCo dựng TẠI MÁY cho [amount] — dùng làm mã dự phòng khi ngoại tuyến. */
    fun offlinePayload(amount: Long): String =
        VietQrPayload.accountTransfer(BANK_BIN, ACCOUNT_NO, amount.coerceAtLeast(0L), ADD_INFO)

    /** API trả về danh sách app ngân hàng + deeplink (JSON). */
    const val DEEPLINK_LIST_URL = "https://api.vietqr.io/v2/android-app-deeplinks"

    /** Deeplink CƠ BẢN chỉ mở app ngân hàng theo `appId` (vd "icb", "bidv") — dùng làm giá trị lưu sẵn. */
    fun bankAppBaseDeeplink(appId: String) = "https://dl.vietqr.io/pay?app=$appId"

    /**
     * Deeplink mở app ngân hàng [appId] và ĐIỀN SẴN thông tin chuyển khoản (với ngân hàng có hỗ trợ
     * autofill — vd VietinBank, BIDV, OCB, ACB…). Tham số theo chuẩn VietQR:
     * `ba` = tài khoản nhận `<số TK>@<mã NH>`, `am` = số tiền (bỏ nếu mã mở), `tn` = nội dung, `bn` = tên
     * người nhận. `tn`/`bn` mã hoá `%20`. Ngân hàng chưa hỗ trợ autofill sẽ chỉ mở app (bỏ qua tham số).
     */
    fun bankAppDeeplink(appId: String, amount: Long): String {
        val sb = StringBuilder("https://dl.vietqr.io/pay?app=").append(appId)
        sb.append("&ba=").append(ACCOUNT_NO).append("@").append(BANK_DEEPLINK_CODE)
        if (amount > 0L) sb.append("&am=").append(amount)
        sb.append("&tn=").append(Uri.encode(ADD_INFO))
        sb.append("&bn=").append(Uri.encode(ACCOUNT_NAME))
        return sb.toString()
    }
}

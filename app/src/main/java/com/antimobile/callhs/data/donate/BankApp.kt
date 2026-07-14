package com.antimobile.callhs.data.donate

import com.antimobile.callhs.util.Donate

/**
 * Một app NGÂN HÀNG có thể mở nhanh bằng deeplink để người dùng quét mã QR ủng hộ. Lấy từ API
 * [Donate.DEEPLINK_LIST_URL] (xem [BankAppRepository]); [logoUrl] là ảnh trên mạng (tải qua
 * [com.antimobile.callhs.ui.components.RemoteImage]).
 */
data class BankApp(
    val appId: String,
    val appName: String,
    val bankName: String,
    val logoUrl: String,
    val deeplink: String,
    val monthlyInstall: Long,
    /** 1 = app điền sẵn thông tin chuyển khoản từ deeplink; 0 = chỉ mở app (phải tự quét/nhập). */
    val autofill: Boolean = false
) {
    companion object {
        /**
         * Danh sách DỰ PHÒNG (không logo) khi lần đầu mở app mà đang ngoại tuyến — vẫn mở được app ngân
         * hàng phổ biến. Khi có mạng, danh sách đầy đủ (kèm logo) tải từ API và được cache.
         */
        val BUNDLED: List<BankApp> = listOf(
            bundled("vcb", "Vietcombank", "Ngân hàng TMCP Ngoại thương Việt Nam"),
            bundled("icb", "VietinBank iPay", "Ngân hàng TMCP Công thương Việt Nam"),
            bundled("bidv", "BIDV SmartBanking", "Ngân hàng TMCP Đầu tư và Phát triển Việt Nam"),
            bundled("vba", "Agribank", "Ngân hàng NN&PTNT Việt Nam"),
            bundled("mb", "MB Bank", "Ngân hàng TMCP Quân đội"),
            bundled("tcb", "Techcombank", "Ngân hàng TMCP Kỹ thương Việt Nam"),
            bundled("acb", "ACB", "Ngân hàng TMCP Á Châu"),
            bundled("vpb", "VPBank NEO", "Ngân hàng TMCP Việt Nam Thịnh Vượng"),
            bundled("tpb", "TPBank", "Ngân hàng TMCP Tiên Phong"),
            bundled("stb", "Sacombank", "Ngân hàng TMCP Sài Gòn Thương Tín")
        )

        private fun bundled(id: String, name: String, bank: String) =
            BankApp(id, name, bank, "", Donate.bankAppBaseDeeplink(id), 0L)
    }
}

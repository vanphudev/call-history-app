package com.antimobile.mcas.util

/**
 * Dựng chuỗi nội dung mã QR CHUYỂN KHOẢN theo chuẩn **VietQR / NAPAS (EMVCo)** NGAY TẠI MÁY — dùng làm
 * mã DỰ PHÒNG khi không tải được ảnh QR có thương hiệu từ VietQR (ngoại tuyến). Quét bằng mọi app ngân
 * hàng Việt Nam đều ra đúng: ngân hàng + số tài khoản + số tiền + nội dung.
 *
 * Cấu trúc TLV (mỗi trường = ID 2 số + ĐỘ DÀI 2 số + GIÁ TRỊ), với chuyển khoản tới TÀI KHOẢN:
 * ```
 * 00 Payload Format Indicator = "01"
 * 01 Point of Initiation      = "12" (động, có số tiền) | "11" (tĩnh, mã mở)
 * 38 Merchant Account Info (VietQR)
 *    00 GUID       = "A000000727"
 *    01 Beneficiary
 *       00 Acquirer BIN     = <bin> (vd 970416 = ACB)
 *       01 Account number   = <accountNo>
 *    02 Service code = "QRIBFTTA"  (chuyển tới TÀI KHOẢN; QRIBFTTC là tới THẺ)
 * 53 Currency = "704" (VND)
 * 54 Amount   = <amount>   (BỎ TRỐNG khi mã mở)
 * 58 Country  = "VN"
 * 62 Additional Data
 *    08 Purpose/addInfo = <message>
 * 63 CRC (CRC-16/CCITT-FALSE) — tính trên toàn chuỗi ĐÃ gồm "6304".
 * ```
 * Lưu ý: trường 52 (Merchant Category Code) là TUỲ CHỌN và VietQR chuyển khoản cá nhân thường BỎ — ở đây
 * không thêm để bám sát các mã VietQR cá nhân thực tế. Giá trị đều ASCII nên độ dài TLV = số ký tự.
 */
object VietQrPayload {

    private const val GUID = "A000000727"
    private const val SERVICE_TO_ACCOUNT = "QRIBFTTA"
    private const val CURRENCY_VND = "704"
    private const val COUNTRY_VN = "VN"

    /**
     * Chuỗi QR chuyển khoản tới [accountNo] tại ngân hàng có BIN [bin]. [amount] > 0 → mã ĐỘNG cố định số
     * tiền; [amount] ≤ 0 → mã MỞ (không gắn số tiền). [message] là nội dung chuyển khoản (bỏ qua nếu rỗng).
     */
    fun accountTransfer(bin: String, accountNo: String, amount: Long, message: String): String {
        val beneficiary = tlv("00", bin) + tlv("01", accountNo)
        val merchant = tlv("00", GUID) + tlv("01", beneficiary) + tlv("02", SERVICE_TO_ACCOUNT)

        val sb = StringBuilder()
        sb.append(tlv("00", "01"))
        sb.append(tlv("01", if (amount > 0L) "12" else "11"))
        sb.append(tlv("38", merchant))
        sb.append(tlv("53", CURRENCY_VND))
        if (amount > 0L) sb.append(tlv("54", amount.toString()))
        sb.append(tlv("58", COUNTRY_VN))
        val msg = message.trim()
        if (msg.isNotEmpty()) sb.append(tlv("62", tlv("08", msg)))

        // CRC tính trên TOÀN chuỗi + "6304" (ID 63 + độ dài 04), rồi nối 4 ký tự HEX HOA vào cuối.
        val withCrcTag = sb.append("6304").toString()
        return withCrcTag + crc16CcittFalse(withCrcTag)
    }

    /** Một trường TLV: `ID` + độ dài 2 số + `value`. Độ dài tính theo SỐ KÝ TỰ (giá trị VietQR là ASCII). */
    private fun tlv(id: String, value: String): String {
        require(value.length <= 99) { "TLV value too long for field $id: ${value.length}" }
        return id + value.length.toString().padStart(2, '0') + value
    }

    /**
     * CRC-16/CCITT-FALSE: đa thức 0x1021, khởi tạo 0xFFFF, KHÔNG đảo bit vào/ra, xorout 0x0000. Trả về 4 ký
     * tự HEX in HOA. (Kiểm chứng: chuỗi "123456789" → "29B1".) Tính trên byte ASCII của [data].
     */
    fun crc16CcittFalse(data: String): String {
        var crc = 0xFFFF
        for (ch in data) {
            crc = crc xor ((ch.code and 0xFF) shl 8)
            repeat(8) {
                crc = if (crc and 0x8000 != 0) (crc shl 1) xor 0x1021 else crc shl 1
                crc = crc and 0xFFFF
            }
        }
        return (crc and 0xFFFF).toString(16).uppercase().padStart(4, '0')
    }
}

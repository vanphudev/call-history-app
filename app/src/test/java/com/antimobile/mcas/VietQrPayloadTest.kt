package com.antimobile.mcas

import com.antimobile.mcas.util.VietQrPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Kiểm thử thuần JVM cho bộ dựng chuỗi QR VietQR/NAPAS (EMVCo) ngoại tuyến ([VietQrPayload]).
 * Feature LIÊN QUAN TIỀN → kiểm kỹ CRC + cấu trúc TLV để mã quét ra ĐÚNG tài khoản.
 */
class VietQrPayloadTest {

    /** Vector chuẩn của CRC-16/CCITT-FALSE: "123456789" → 0x29B1. */
    @Test
    fun `crc16 dung vector chuan`() {
        assertEquals("29B1", VietQrPayload.crc16CcittFalse("123456789"))
    }

    /** Mã ĐỘNG (có số tiền): đủ các trường bắt buộc, POI=12, có trường 54, CRC hợp lệ. */
    @Test
    fun `chuyen khoan co so tien`() {
        val s = VietQrPayload.accountTransfer("970416", "45578647", 10_000L, "UNG HO MCAS")

        assertTrue("bắt đầu bằng 000201", s.startsWith("000201"))
        assertTrue("POI động 010212", s.contains("010212"))
        assertTrue("GUID VietQR", s.contains("0010A000000727"))
        assertTrue("BIN + số TK trong field 01", s.contains("0006970416010845578647"))
        assertTrue("service QRIBFTTA", s.contains("0208QRIBFTTA"))
        assertTrue("tiền tệ VND", s.contains("5303704"))
        assertTrue("số tiền 10000", s.contains("540510000"))
        assertTrue("quốc gia VN", s.contains("5802VN"))
        assertTrue("field 62 bọc addInfo (08 len11)", s.contains("62150811UNG HO MCAS"))
        assertTrue("có field CRC 6304", s.contains("6304"))

        assertCrcValid(s)
    }

    /** Mã MỞ (tuỳ tâm): POI=11, KHÔNG có trường 54. */
    @Test
    fun `chuyen khoan ma mo khong so tien`() {
        val s = VietQrPayload.accountTransfer("970416", "45578647", 0L, "UNG HO MCAS")

        assertTrue("POI tĩnh 010211", s.contains("010211"))
        assertFalse("không có trường số tiền 54", s.contains("5405"))
        assertCrcValid(s)
    }

    /** 4 ký tự cuối phải khớp CRC tính trên toàn chuỗi trước đó (đã gồm "6304"). */
    private fun assertCrcValid(payload: String) {
        assertTrue(payload.length > 8)
        val body = payload.dropLast(4)
        val crc = payload.takeLast(4)
        assertTrue("field CRC nằm cuối", body.endsWith("6304"))
        assertEquals("CRC khớp", VietQrPayload.crc16CcittFalse(body), crc)
    }
}

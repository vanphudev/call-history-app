package com.antimobile.mcas

import com.antimobile.mcas.util.SmsText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Kiểm thử thuần JVM cho việc chuẩn hoá nội dung SMS về GSM‑7 ([SmsText.toGsm7]).
 * Ký tự VÔ HÌNH (nbsp, zwsp) dựng từ code point để test xác định; glyph nhìn thấy được dùng trực tiếp.
 */
class SmsTextTest {

    @Test
    fun `bo dau tieng Viet co ban`() {
        assertEquals("Xin chao ban", SmsText.toGsm7("Xin chào bạn"))
    }

    @Test
    fun `quy D va d cho chu Dd`() {
        assertEquals("Duong Nguyen Hue, Quan 1", SmsText.toGsm7("Đường Nguyễn Huệ, Quận 1"))
    }

    @Test
    fun `giu dau cau ascii va chu so`() {
        assertEquals("Chao, ban! Khoe chu? Gia: 50%", SmsText.toGsm7("Chào, bạn! Khỏe chứ? Giá: 50%"))
    }

    @Test
    fun `giu xuong dong`() {
        assertEquals("Dong 1\nDong 2", SmsText.toGsm7("Dòng 1\nDòng 2"))
    }

    @Test
    fun `chuoi ascii thuan giu nguyen`() {
        val s = "Ma don: ABC-123 (x2) @ 50%"
        assertEquals(s, SmsText.toGsm7(s))
    }

    @Test
    fun `chuyen ky tu in an ve ascii`() {
        // “ ” (nháy kép cong) -> " ; – (gạch dài) -> - ; … (ellipsis) -> ...
        assertEquals("\"Chao\" - ban...", SmsText.toGsm7("“Chao” – ban…"))
    }

    @Test
    fun `dong tien va bullet`() {
        assertEquals("100.000d", SmsText.toGsm7("100.000₫"))  // ₫
        assertEquals("* Muc 1", SmsText.toGsm7("• Muc 1"))    // •
    }

    @Test
    fun `bo emoji va ky hieu la`() {
        // Emoji (cặp surrogate 😀) và ✅ bị bỏ; phần chữ giữ lại.
        assertEquals("Hi ", SmsText.toGsm7("Hi 😀"))
        assertEquals("Da nhan hang", SmsText.toGsm7("✅Da nhan hang"))
    }

    @Test
    fun `khoang trang unicode ve khoang trang thuong, zwsp bi bo`() {
        val nbsp = "a" + 0x00A0.toChar() + "b"   // no-break space
        val zwsp = "a" + 0x200B.toChar() + "b"   // zero-width space
        assertEquals("a b", SmsText.toGsm7(nbsp))
        assertEquals("ab", SmsText.toGsm7(zwsp))
    }

    @Test
    fun `tab chuyen thanh space vi tab khong thuoc GSM7`() {
        // Tab (\t) KHÔNG thuộc GSM‑7 → phải đổi thành space, nếu giữ sẽ ép cả tin sang UCS‑2.
        val out = SmsText.toGsm7("Ma DH:\t123")
        assertEquals("Ma DH: 123", out)
        assertTrue("Không được còn tab", '\t' !in out)
    }

    @Test
    fun `ket qua khong con ky tu ngoai GSM7 co ban`() {
        val input = "Chào\t✅ bạn – 100.000₫ 😀 “ok”…\nDòng 2"
        val out = SmsText.toGsm7(input)
        // Chỉ còn ASCII in được + \n \r (đều thuộc GSM‑7); không còn tab hay ký tự > 0x7F.
        assertTrue("Còn ký tự ngoài GSM‑7: $out", out.all { it.code in 0x20..0x7E || it == '\n' || it == '\r' })
    }
}

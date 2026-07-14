package com.antimobile.callhs

import com.antimobile.callhs.util.TemplateContext
import com.antimobile.callhs.util.TemplateFill
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Kiểm thử thuần JVM cho việc thay pattern trong mẫu tin nhắn.
 * Các pattern ngày/giờ kiểm theo ĐỊNH DẠNG (không phụ thuộc múi giờ máy chạy test).
 */
class TemplateFillTest {

    private val ctx = TemplateContext(
        simNumbers = listOf("0911222333", "0900111222"),
        qrText = "HELLO-QR",
        nowMillis = 1_751_700_600_000L
    )

    @Test
    fun `pattern lạ được giữ nguyên`() {
        assertEquals("Xin chào {foo} {bar}", TemplateFill.fill("Xin chào {foo} {bar}", ctx))
    }

    @Test
    fun `phone tương thích ngược dùng số SIM 1`() {
        assertEquals("Số: 0911 222 333", TemplateFill.fill("Số: {phone}", ctx))
    }

    @Test
    fun `phonesim ánh xạ theo khe`() {
        assertEquals("0911 222 333 / 0900 111 222", TemplateFill.fill("{phonesim1} / {phonesim2}", ctx))
    }

    @Test
    fun `phonesim2 lùi về SIM 1 khi chỉ có 1 SIM`() {
        val oneSim = ctx.copy(simNumbers = listOf("0911222333"))
        assertEquals("0911 222 333", TemplateFill.fill("{phonesim2}", oneSim))
    }

    @Test
    fun `phonesim ngoài phạm vi lùi về SIM 1`() {
        assertEquals("[0911 222 333]", TemplateFill.fill("[{phonesim3}]", ctx))
    }

    @Test
    fun `phonesim không phải số thì giữ nguyên`() {
        assertEquals("{phonesimX}", TemplateFill.fill("{phonesimX}", ctx))
    }

    @Test
    fun `contextqr được thay`() {
        assertEquals("Mã: HELLO-QR", TemplateFill.fill("Mã: {contextqr}", ctx))
    }

    @Test
    fun `không phân biệt hoa thường`() {
        assertEquals("Mã: HELLO-QR", TemplateFill.fill("Mã: {CONTEXTQR}", ctx))
    }

    @Test
    fun `date đúng định dạng`() {
        val out = TemplateFill.fill("{date}", ctx)
        assertTrue(out, Regex("""\d{2}/\d{2}/\d{4}""").matches(out))
    }

    @Test
    fun `datetime đúng định dạng`() {
        val out = TemplateFill.fill("{datetime}", ctx)
        assertTrue(out, Regex("""\d{2}/\d{2}/\d{4} \d{2}:\d{2}""").matches(out))
    }

    @Test
    fun `timedate đúng định dạng`() {
        val out = TemplateFill.fill("{timedate}", ctx)
        assertTrue(out, Regex("""\d{2}:\d{2} \d{2}/\d{2}/\d{4}""").matches(out))
    }

    @Test
    fun `weekdate bắt đầu bằng thứ`() {
        val out = TemplateFill.fill("{weekdate}", ctx)
        assertTrue(out, out.startsWith("Thứ ") || out.startsWith("Chủ nhật"))
    }

    @Test
    fun `không có số SIM nào cho ra chuỗi rỗng`() {
        val c = ctx.copy(simNumbers = emptyList())
        assertEquals("Số:", TemplateFill.fill("Số:{phonesim1}", c))
    }
}

package com.antimobile.mcas

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.antimobile.mcas.util.TemplateContext
import com.antimobile.mcas.util.TemplateFill
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Chạy TRÊN THIẾT BỊ (bộ máy regex ICU của Android) — bắt lỗi PatternSyntaxException mà unit test JVM
 * KHÔNG phát hiện được (JVM desktop chấp nhận `}` trần, ICU thì không). Chỉ cần chạm TemplateFill là
 * `<clinit>` biên dịch tokenRegex; pattern sai sẽ ném ngay tại đây.
 */
@RunWith(AndroidJUnit4::class)
class TemplateFillInstrumentedTest {

    @Test
    fun fill_substitutes_and_keeps_unknown_on_device() {
        val ctx = TemplateContext(
            simNumbers = listOf("0911222333"),
            qrText = "QR-DATA",
            nowMillis = 1_751_700_600_000L
        )
        assertEquals("Mã: QR-DATA, giữ {foo}", TemplateFill.fill("Mã: {contextqr}, giữ {foo}", ctx))
        assertEquals("Số: 0911 222 333", TemplateFill.fill("Số: {phonesim1}", ctx))
    }
}

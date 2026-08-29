package com.antimobile.mcas

import com.antimobile.mcas.util.PhoneSearchQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Kiểm thử LOGIC thuần (không phụ thuộc Android) cho dựng truy vấn tìm kiếm số điện thoại. */
class PhoneSearchQueryTest {

    // ---- variants: số VN dạng nội địa 0… ----

    @Test fun localMobile_hasAllFormats() {
        val v = PhoneSearchQuery.variants("0987654321")
        assertTrue("0987654321" in v)        // nội địa
        assertTrue("0987 654 321" in v)      // nội địa có nhóm
        assertTrue("+84987654321" in v)      // quốc tế +84
        assertTrue("+84 987 654 321" in v)   // quốc tế +84 có nhóm
        assertTrue("84987654321" in v)       // quốc tế không dấu +
    }

    @Test fun variants_haveNoDuplicates() {
        val v = PhoneSearchQuery.variants("0987654321")
        assertEquals(v.size, v.toSet().size)
    }

    // ---- variants: số đã ở dạng quốc tế 84… quy về cùng tập ----

    @Test fun intlInput_yieldsLocalVariantsToo() {
        val v = PhoneSearchQuery.variants("84987654321")
        assertTrue("0987654321" in v)
        assertTrue("+84987654321" in v)
        assertTrue("84987654321" in v)
    }

    @Test fun plusIntlInput_sameSet() {
        assertEquals(
            PhoneSearchQuery.variants("+84 987 654 321").toSet(),
            PhoneSearchQuery.variants("0987654321").toSet()
        )
    }

    // ---- số không phải VN chuẩn: chỉ giữ dạng gốc, không sinh biến thể 84/0 rác ----

    @Test fun nonVnNumber_noJunkPrefixes() {
        val v = PhoneSearchQuery.variants("19001234")
        assertTrue("19001234" in v)
        assertTrue(v.none { it.contains("84") })
        assertTrue(v.none { it.startsWith("0") })
    }

    // ---- buildGoogleQuery: OR các biến thể trong ngoặc kép ----

    @Test fun query_joinsVariantsWithQuotedOr() {
        val q = PhoneSearchQuery.buildGoogleQuery("0987654321")
        assertTrue(q.contains(" OR "))
        assertTrue(q.contains("\"0987654321\""))
        assertTrue(q.contains("\"+84 987 654 321\""))
    }

    // ---- số ẩn / marker Android → truy vấn rỗng ----

    @Test fun hiddenAndMarkerNumbers_yieldEmptyQuery() {
        assertEquals("", PhoneSearchQuery.buildGoogleQuery(""))
        assertEquals("", PhoneSearchQuery.buildGoogleQuery("   "))
        assertEquals("", PhoneSearchQuery.buildGoogleQuery("-1"))
        assertEquals("", PhoneSearchQuery.buildGoogleQuery("-2"))
        assertEquals("", PhoneSearchQuery.buildGoogleQuery("-3"))
    }
}

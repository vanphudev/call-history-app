package com.antimobile.callhs

import com.antimobile.callhs.data.contacts.Contact
import com.antimobile.callhs.data.contacts.ContactPhone
import com.antimobile.callhs.data.contacts.ContactPhoneType
import com.antimobile.callhs.data.contacts.ContactSearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Kiểm thử LOGIC tìm kiếm danh bạ (toàn văn tên/số/email/tổ chức) — thuần, không phụ thuộc Android. */
class ContactSearchTest {

    private fun contact(
        id: Long,
        name: String,
        numbers: List<String> = listOf("0987654321"),
        emails: List<String> = emptyList(),
        org: String? = null
    ) = Contact(
        id = id, lookupKey = null, displayName = name, photoUri = null,
        phones = numbers.map { ContactPhone(it, ContactPhoneType.MOBILE, null, null) },
        emails = emails, organization = org
    )

    private val list = listOf(
        contact(1, "Nguyễn Văn A", numbers = listOf("0987 654 321"), emails = listOf("vana@example.com")),
        contact(2, "Trần Thị B", numbers = listOf("+84 912 000 111"), org = "Giám đốc · Công ty ABC"),
        contact(3, "Alpha Corp", numbers = listOf("1900 1234"))
    )

    @Test fun emptyQuery_returnsAll() {
        assertEquals(3, ContactSearch.filter(list, "").size)
        assertEquals(3, ContactSearch.filter(list, "   ").size)
    }

    @Test fun matchesByName_caseInsensitive() {
        assertEquals(listOf(1L), ContactSearch.filter(list, "nguyễn").map { it.id })
        assertEquals(listOf(3L), ContactSearch.filter(list, "alpha").map { it.id })
    }

    @Test fun matchesByNumber_ignoringFormatting() {
        // Số lưu "0987 654 321" khớp truy vấn "654321" (chỉ so phần chữ số).
        assertTrue(ContactSearch.matches(list[0], "654321"))
        // Số quốc tế "+84 912 000 111" khớp "912000".
        assertTrue(ContactSearch.matches(list[1], "912000"))
    }

    @Test fun matchesByEmailAndOrganization() {
        assertEquals(listOf(1L), ContactSearch.filter(list, "example.com").map { it.id })
        assertEquals(listOf(2L), ContactSearch.filter(list, "công ty").map { it.id })
    }

    @Test fun noMatch_returnsEmpty() {
        assertFalse(ContactSearch.matches(list[0], "zzz"))
        assertEquals(0, ContactSearch.filter(list, "0000000").size)
    }
}

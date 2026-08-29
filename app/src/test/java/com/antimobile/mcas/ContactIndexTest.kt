package com.antimobile.mcas

import com.antimobile.mcas.data.contacts.Contact
import com.antimobile.mcas.data.contacts.ContactIndex
import com.antimobile.mcas.data.contacts.ContactPhone
import com.antimobile.mcas.data.contacts.ContactPhoneType
import com.antimobile.mcas.data.contacts.ContactRowItem
import org.junit.Assert.assertEquals
import org.junit.Test

/** Kiểm thử LOGIC sắp xếp A→Z + nhóm chữ cái (bỏ dấu tiếng Việt) — thuần, không phụ thuộc Android. */
class ContactIndexTest {

    private fun contact(id: Long, name: String) = Contact(
        id = id, lookupKey = null, displayName = name, photoUri = null,
        phones = listOf(ContactPhone("0900000000", ContactPhoneType.MOBILE, null, null)),
        emails = emptyList(), organization = null
    )

    @Test fun fold_removesVietnameseDiacriticsAndD() {
        assertEquals("An", ContactIndex.fold("Ân"))
        assertEquals("Dong", ContactIndex.fold("Đông"))
        assertEquals("uu", ContactIndex.fold("ưu"))
    }

    @Test fun sectionLetter_mapsToAZorHash() {
        assertEquals("A", ContactIndex.sectionLetter("Ánh"))
        assertEquals("D", ContactIndex.sectionLetter("Đông"))
        assertEquals("B", ContactIndex.sectionLetter("  Bình"))
        assertEquals("#", ContactIndex.sectionLetter("0912 345 678"))
        assertEquals("#", ContactIndex.sectionLetter(""))
    }

    @Test fun build_sortsGroupsAndIndexesWithHashLast() {
        val listing = ContactIndex.build(
            listOf(
                contact(1, "Bình"),
                contact(2, "An"),
                contact(3, "Đông"),
                contact(4, "0912"),
                contact(5, "Ánh")
            )
        )
        // Chữ cái theo thứ tự, "#" cuối.
        assertEquals(listOf("A", "B", "D", "#"), listing.letters)
        // Nhóm A gồm "An" rồi "Ánh" (đã bỏ dấu, cùng khoá thì theo tên gốc).
        val names = listing.rows.mapNotNull { (it as? ContactRowItem.Item)?.contact?.displayName }
        assertEquals(listOf("An", "Ánh", "Bình", "Đông", "0912"), names)
        // Header A ở dòng 0; nhóm "#" (Header) là dòng áp chót.
        assertEquals(0, listing.letterToRowIndex["A"])
        assertEquals("#", (listing.rows[listing.rows.size - 2] as ContactRowItem.Header).letter)
    }

    @Test fun build_leadingSymbolNames_groupContiguously_noDuplicateSections() {
        // Tên có ký hiệu dẫn đầu (số thấp khi so chuỗi) từng gây nhóm trùng → khoá LazyColumn trùng → crash.
        val listing = ContactIndex.build(
            listOf(
                contact(1, "!Bob"),
                contact(2, "-Zoe"),
                contact(3, "Alice"),
                contact(4, "Bella"),
                contact(5, "Zack")
            )
        )
        // Chữ cái DUY NHẤT, đúng thứ tự A→Z.
        assertEquals(listOf("A", "B", "Z"), listing.letters)
        // Không có Header trùng (khoá "h_<letter>" phải duy nhất cho LazyColumn).
        val headerLetters = listing.rows.filterIsInstance<ContactRowItem.Header>().map { it.letter }
        assertEquals(headerLetters, headerLetters.distinct())
        // "!Bob" nằm CÙNG nhóm B với "Bella" (liền nhau ngay sau Header B).
        val bIndex = listing.letterToRowIndex.getValue("B")
        val bNames = listing.rows.drop(bIndex + 1)
            .takeWhile { it is ContactRowItem.Item }
            .map { (it as ContactRowItem.Item).contact.displayName }
        assertEquals(setOf("Bella", "!Bob"), bNames.toSet())
    }

    @Test fun currentLetter_picksSectionAtOrAboveFirstVisible() {
        val listing = ContactIndex.build(listOf(contact(1, "An"), contact(2, "Bình"), contact(3, "Cường")))
        // rows: [H:A(0), An(1), H:B(2), Bình(3), H:C(4), Cường(5)]
        assertEquals("A", ContactIndex.currentLetter(1, listing))
        assertEquals("B", ContactIndex.currentLetter(3, listing))
        assertEquals("C", ContactIndex.currentLetter(5, listing))
    }
}

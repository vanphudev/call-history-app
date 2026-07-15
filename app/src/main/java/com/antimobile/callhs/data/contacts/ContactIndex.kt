package com.antimobile.callhs.data.contacts

import java.text.Normalizer

/** Một dòng trong danh sách danh bạ đã nhóm: tiêu đề CHỮ CÁI hoặc một liên hệ. */
sealed interface ContactRowItem {
    data class Header(val letter: String) : ContactRowItem
    data class Item(val contact: Contact) : ContactRowItem
}

/**
 * Danh bạ đã SẮP XẾP A→Z + nhóm theo chữ cái:
 * [rows] = danh sách phẳng (Header xen Item) để đưa vào LazyColumn;
 * [letters] = các chữ cái CÓ mặt (theo thứ tự) cho thanh mục lục;
 * [letterToRowIndex] = chữ cái → chỉ số dòng Header (để cuộn nhanh tới nhóm).
 */
data class ContactListing(
    val rows: List<ContactRowItem>,
    val letters: List<String>,
    val letterToRowIndex: Map<String, Int>
)

object ContactIndex {

    /**
     * Bỏ dấu tiếng Việt (NFD + xoá dấu tổ hợp) và quy Đ→D để lấy chữ cái A–Z. Lọc dấu bằng
     * [Character.getType] thay vì regex `\p{Mn}` — nhanh hơn (không biên dịch regex mỗi lần gọi, tránh gọi hàng
     * chục nghìn lần lúc sắp xếp danh bạ lớn) và an toàn hơn trên vài máy Android nhạy cảm cú pháp `{}` trong regex.
     */
    fun fold(text: String): String {
        val nfd = Normalizer.normalize(text, Normalizer.Form.NFD)
        val sb = StringBuilder(nfd.length)
        for (c in nfd) {
            when {
                Character.getType(c) == Character.NON_SPACING_MARK.toInt() -> {} // bỏ dấu tổ hợp
                c == 'Đ' -> sb.append('D')
                c == 'đ' -> sb.append('d')
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    /** Chữ cái mục lục của [name]: A–Z (đã bỏ dấu); ký tự đầu không phải chữ cái → "#". */
    fun sectionLetter(name: String): String {
        val first = name.trim().firstOrNull { it.isLetter() || it.isDigit() } ?: return "#"
        val folded = fold(first.toString()).uppercase().firstOrNull() ?: return "#"
        return if (folded in 'A'..'Z') folded.toString() else "#"
    }

    // Khoá sắp xếp phải KHỚP sectionLetter: bỏ ký tự đầu KHÔNG phải chữ/số trước khi bỏ dấu, để tên có
    // ký hiệu dẫn đầu ('!Bob', '(Work) Anna', '-Dad') vẫn nằm ĐÚNG nhóm chữ cái (không tách/trùng nhóm).
    private fun sortKey(name: String): String =
        fold(name.trim().dropWhile { !it.isLetter() && !it.isDigit() }).lowercase()

    /**
     * Sắp xếp A→Z (không phân biệt hoa/thường & dấu), nhóm theo chữ cái, chèn Header trước mỗi nhóm;
     * nhóm "#" (số/ký hiệu) luôn xuống CUỐI.
     */
    fun build(contacts: List<Contact>): ContactListing {
        if (contacts.isEmpty()) return ContactListing(emptyList(), emptyList(), emptyMap())
        // Tính khoá sắp xếp MỘT LẦN cho mỗi liên hệ (letter/sortKey/nameLower) rồi mới sort — tránh gọi
        // sectionLetter/sortKey (mỗi cái NFD-normalize) HAI LẦN cho MỖI so sánh (O(n log n) lần) trên UI thread.
        data class Keyed(val contact: Contact, val letter: String, val sortKey: String, val nameLower: String)
        val keyed = contacts.map { c ->
            val name = c.displayNameOrNumber
            Keyed(c, sectionLetter(name), sortKey(name), name.lowercase())
        }
        val sorted = keyed.sortedWith(
            compareBy<Keyed> { if (it.letter == "#") 1 else 0 }
                .thenBy { it.sortKey }
                .thenBy { it.nameLower }
        )
        // Gom theo chữ cái bằng getOrPut → mỗi chữ cái CHỉ MỘT nhóm (không Header/khoá LazyColumn trùng →
        // không crash), thứ tự chữ cái = thứ tự xuất hiện trong danh sách ĐÃ sắp xếp (A→Z, "#" cuối).
        val bySection = LinkedHashMap<String, MutableList<Contact>>()
        for (k in sorted) bySection.getOrPut(k.letter) { ArrayList() }.add(k.contact)

        val rows = ArrayList<ContactRowItem>(sorted.size + bySection.size)
        val letters = ArrayList<String>(bySection.size)
        val letterToRowIndex = LinkedHashMap<String, Int>()
        for ((letter, items) in bySection) {
            letterToRowIndex[letter] = rows.size
            letters.add(letter)
            rows.add(ContactRowItem.Header(letter))
            items.forEach { rows.add(ContactRowItem.Item(it)) }
        }
        return ContactListing(rows, letters, letterToRowIndex)
    }

    /**
     * Chữ cái của mục đang ở ĐẦU danh sách theo [firstVisibleRowIndex] (để tô sáng trên thanh mục lục).
     * Không phụ thuộc thứ tự duyệt map — lấy Header có chỉ số LỚN NHẤT mà vẫn <= vị trí đang xem.
     */
    fun currentLetter(firstVisibleRowIndex: Int, listing: ContactListing): String? =
        listing.letterToRowIndex.entries
            .filter { it.value <= firstVisibleRowIndex }
            .maxByOrNull { it.value }
            ?.key
}

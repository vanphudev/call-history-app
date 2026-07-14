package com.antimobile.callhs.data.contacts

import com.antimobile.callhs.i18n.appStrings

/** LOẠI số danh bạ (chuẩn hoá từ ContactsContract) — TÊN hiển thị đổi theo ngôn ngữ, resolve ở tầng UI. */
enum class ContactPhoneType { MOBILE, HOME, WORK, MAIN, WORK_MOBILE, FAX, PAGER, CUSTOM, OTHER }

/** Một SỐ của liên hệ: số + LOẠI (+ nhãn tuỳ biến khi [ContactPhoneType.CUSTOM]) + nhà mạng (suy từ đầu số). */
data class ContactPhone(
    val number: String,
    val type: ContactPhoneType,
    val customLabel: String?,
    val carrier: String?
)

/**
 * Một LIÊN HỆ trong danh bạ (chỉ đọc) — gộp mọi số/email/tổ chức của một contact hệ thống.
 * [phones] luôn có ít nhất 1 phần tử (chỉ nạp liên hệ CÓ số điện thoại).
 */
data class Contact(
    val id: Long,
    val lookupKey: String?,      // dựng lookup URI để mở trong app Danh bạ
    val displayName: String,
    val photoUri: String?,
    val phones: List<ContactPhone>,
    val emails: List<String>,
    val organization: String?    // "Chức danh · Công ty" nếu có
) {
    val primaryPhone: ContactPhone? get() = phones.firstOrNull()

    /** Tên hiển thị; rỗng tên → số đầu tiên → "Không tên". */
    val displayNameOrNumber: String
        get() = displayName.takeIf { it.isNotBlank() } ?: (primaryPhone?.number ?: appStrings().contacts.noName)
}

/**
 * Tìm kiếm danh bạ TOÀN VĂN — cùng logic với tìm kiếm nhật ký cuộc gọi:
 * khớp TÊN / TỔ CHỨC / EMAIL theo chuỗi con (không phân biệt hoa thường), hoặc SỐ theo chuỗi CHỮ SỐ.
 */
object ContactSearch {

    fun matches(contact: Contact, query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        if (contact.displayName.contains(q, ignoreCase = true)) return true
        if (contact.organization?.contains(q, ignoreCase = true) == true) return true
        if (contact.emails.any { it.contains(q, ignoreCase = true) }) return true
        val qDigits = q.filter { it.isDigit() }
        if (qDigits.isNotEmpty() &&
            contact.phones.any { it.number.filter { ch -> ch.isDigit() }.contains(qDigits) }
        ) return true
        return false
    }

    fun filter(contacts: List<Contact>, query: String): List<Contact> {
        val q = query.trim()
        if (q.isEmpty()) return contacts
        return contacts.filter { matches(it, q) }
    }
}

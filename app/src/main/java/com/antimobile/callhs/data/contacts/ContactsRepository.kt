package com.antimobile.callhs.data.contacts

import android.content.ContentResolver
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Organization
import android.provider.ContactsContract.CommonDataKinds.Phone
import com.antimobile.callhs.util.Carrier
import com.antimobile.callhs.util.PhoneKey

/**
 * Đọc DANH BẠ hệ thống (chỉ đọc, cần READ_CONTACTS). Gộp 3 truy vấn (số / email / tổ chức) thành danh
 * sách [Contact] đầy đủ. Chỉ lấy liên hệ CÓ số điện thoại (danh bạ cho app gọi điện). Số/email trùng
 * trong cùng một liên hệ được khử; nhà mạng suy từ đầu số qua [Carrier] (khớp cách hiện ở nhật ký).
 */
class ContactsRepository(private val resolver: ContentResolver) {

    fun loadAll(): List<Contact> {
        val builders = LinkedHashMap<Long, Builder>()
        queryPhones(builders)
        if (builders.isEmpty()) return emptyList()
        attachEmails(builders)
        attachOrganizations(builders)
        return dedupContacts(builders.values.map { it.build() })
    }

    /**
     * Gộp các liên hệ TRÙNG chưa được hệ thống hợp nhất (vd bản Google + bản SIM/Zalo của cùng người có
     * CONTACT_ID khác nhau) — nếu CÙNG tên (đã bỏ dấu) và CÙNG tập số (theo [PhoneKey]) thì chỉ giữ MỘT thẻ,
     * ưu tiên bản có lookupKey (mở/sửa được trong Danh bạ). Liên hệ KHÔNG tên không gộp (tránh gộp nhầm số lạ).
     */
    private fun dedupContacts(contacts: List<Contact>): List<Contact> {
        if (contacts.size < 2) return contacts
        val bySig = LinkedHashMap<String, Contact>()
        for (c in contacts) {
            val name = ContactIndex.fold(c.displayName).trim().lowercase()
            if (name.isEmpty()) { bySig["u:${c.id}"] = c; continue }
            val keys = c.phones.mapNotNull { PhoneKey.of(it.number).takeIf(String::isNotEmpty) }
                .toSortedSet().joinToString(",")
            val sig = "$name|$keys"
            val existing = bySig[sig]
            when {
                existing == null -> bySig[sig] = c
                existing.lookupKey == null && c.lookupKey != null -> bySig[sig] = c
            }
        }
        return bySig.values.toList()
    }

    private class Builder(
        val id: Long,
        val name: String,
        val photoUri: String?,
        val lookupKey: String?
    ) {
        val phones = ArrayList<ContactPhone>()
        private val seenNumbers = HashSet<String>()
        val emails = ArrayList<String>()
        private val seenEmails = HashSet<String>()
        var organization: String? = null

        fun addPhone(number: String, type: ContactPhoneType, customLabel: String?) {
            // Khử trùng theo KHOÁ CHUẨN [PhoneKey] (chứ không phải toàn bộ chữ số) → "+84912345678" và
            // "0912345678" của cùng một liên hệ CHỈ còn MỘT dòng. Số được sắp super-primary/primary lên trước
            // (xem queryPhones) nên bản GIỮ lại là số MẶC ĐỊNH; các dạng còn lại của cùng số bị gộp.
            val key = PhoneKey.of(number).ifEmpty { number.filter { it.isDigit() } }.ifEmpty { number }
            if (seenNumbers.add(key)) phones.add(ContactPhone(number, type, customLabel, Carrier.of(number)))
        }

        fun addEmail(address: String) {
            if (seenEmails.add(address.lowercase())) emails.add(address)
        }

        fun build() = Contact(id, lookupKey, name, photoUri, phones.toList(), emails.toList(), organization)
    }

    private fun queryPhones(out: LinkedHashMap<Long, Builder>) {
        val projection = arrayOf(
            Phone.CONTACT_ID,
            Phone.DISPLAY_NAME_PRIMARY,
            Phone.PHOTO_URI,
            Phone.LOOKUP_KEY,
            Phone.NUMBER,
            Phone.TYPE,
            Phone.LABEL,
            Phone.IS_SUPER_PRIMARY,
            Phone.IS_PRIMARY
        )
        resolver.query(
            Phone.CONTENT_URI,
            projection,
            null,
            null,
            // Sắp theo TÊN, rồi ĐƯA SỐ MẶC ĐỊNH (super-primary → primary) LÊN ĐẦU mỗi liên hệ để
            // Contact.primaryPhone = số người dùng đặt làm mặc định, không phải số có _id nhỏ nhất tuỳ tiện.
            "${Phone.DISPLAY_NAME_PRIMARY} COLLATE NOCASE ASC, ${Phone.IS_SUPER_PRIMARY} DESC, ${Phone.IS_PRIMARY} DESC"
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(Phone.CONTACT_ID)
            val nameCol = c.getColumnIndexOrThrow(Phone.DISPLAY_NAME_PRIMARY)
            val photoCol = c.getColumnIndexOrThrow(Phone.PHOTO_URI)
            val lookupCol = c.getColumnIndexOrThrow(Phone.LOOKUP_KEY)
            val numberCol = c.getColumnIndexOrThrow(Phone.NUMBER)
            val typeCol = c.getColumnIndexOrThrow(Phone.TYPE)
            val labelCol = c.getColumnIndexOrThrow(Phone.LABEL)
            while (c.moveToNext()) {
                val number = c.getString(numberCol)?.trim().orEmpty()
                if (number.isEmpty()) continue
                val id = c.getLong(idCol)
                val builder = out.getOrPut(id) {
                    Builder(
                        // GIỮ tên thật (kể cả rỗng) — để Contact.displayNameOrNumber tự fallback về số rồi "Không tên",
                        // tránh gán số làm tên khiến thẻ hiện SỐ hai lần (tiêu đề + phụ đề).
                        id = id,
                        name = c.getString(nameCol).orEmpty(),
                        photoUri = c.getString(photoCol),
                        lookupKey = c.getString(lookupCol)
                    )
                }
                builder.addPhone(number, phoneType(c.getInt(typeCol)), c.getString(labelCol)?.takeIf { it.isNotBlank() })
            }
        }
    }

    private fun attachEmails(out: LinkedHashMap<Long, Builder>) {
        resolver.query(
            Email.CONTENT_URI,
            arrayOf(Email.CONTACT_ID, Email.ADDRESS),
            null, null, null
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(Email.CONTACT_ID)
            val addrCol = c.getColumnIndexOrThrow(Email.ADDRESS)
            while (c.moveToNext()) {
                val addr = c.getString(addrCol)?.trim()?.takeIf { it.isNotEmpty() } ?: continue
                out[c.getLong(idCol)]?.addEmail(addr)
            }
        }
    }

    private fun attachOrganizations(out: LinkedHashMap<Long, Builder>) {
        resolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(Organization.CONTACT_ID, Organization.COMPANY, Organization.TITLE),
            "${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(Organization.CONTENT_ITEM_TYPE),
            // Đưa dòng tổ chức MẶC ĐỊNH (super-primary → primary) lên trước → mỗi liên hệ giữ đúng tổ chức chính,
            // không phải dòng tuỳ tiện của tài khoản đồng bộ cũ (builder giữ dòng ĐẦU gặp cho mỗi liên hệ).
            "${Organization.IS_SUPER_PRIMARY} DESC, ${Organization.IS_PRIMARY} DESC"
        )?.use { c ->
            val idCol = c.getColumnIndexOrThrow(Organization.CONTACT_ID)
            val companyCol = c.getColumnIndexOrThrow(Organization.COMPANY)
            val titleCol = c.getColumnIndexOrThrow(Organization.TITLE)
            while (c.moveToNext()) {
                val b = out[c.getLong(idCol)] ?: continue
                if (b.organization != null) continue
                val company = c.getString(companyCol)?.trim().orEmpty()
                val title = c.getString(titleCol)?.trim().orEmpty()
                val org = listOf(title, company).filter { it.isNotEmpty() }.joinToString(" · ")
                if (org.isNotEmpty()) b.organization = org
            }
        }
    }

    /** Chuẩn hoá loại số ContactsContract → [ContactPhoneType] (TÊN hiển thị resolve ở tầng UI theo ngôn ngữ). */
    private fun phoneType(type: Int): ContactPhoneType = when (type) {
        Phone.TYPE_MOBILE -> ContactPhoneType.MOBILE
        Phone.TYPE_HOME -> ContactPhoneType.HOME
        Phone.TYPE_WORK -> ContactPhoneType.WORK
        Phone.TYPE_MAIN -> ContactPhoneType.MAIN
        Phone.TYPE_WORK_MOBILE -> ContactPhoneType.WORK_MOBILE
        Phone.TYPE_FAX_WORK, Phone.TYPE_FAX_HOME -> ContactPhoneType.FAX
        Phone.TYPE_PAGER -> ContactPhoneType.PAGER
        Phone.TYPE_CUSTOM -> ContactPhoneType.CUSTOM
        Phone.TYPE_OTHER -> ContactPhoneType.OTHER
        else -> ContactPhoneType.MOBILE
    }
}

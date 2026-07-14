package com.antimobile.callhs.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import android.widget.Toast
import com.antimobile.callhs.i18n.appStrings

/**
 * Điều phối thao tác với DANH BẠ hệ thống — chỉ MỞ ứng dụng Danh bạ mặc định qua Intent
 * (không tự ghi liên hệ, không cần quyền WRITE_CONTACTS).
 *
 * - [addToContacts]: mở màn TẠO liên hệ mới với SỐ đã điền sẵn → người dùng tự nhập tên và lưu.
 * - [viewContact]: mở & XEM một liên hệ đã lưu (theo lookup URI) trong ứng dụng Danh bạ.
 *
 * Không dùng `resolveActivity` (tránh phụ thuộc `<queries>`); mọi `startActivity` bọc `runCatching`
 * nên máy thiếu ứng dụng Danh bạ chỉ báo toast, KHÔNG crash.
 */
object ContactActions {

    private const val TAG = "ContactActions"

    /** Mở màn tạo liên hệ mới của Danh bạ, điền sẵn [number]; tên do người dùng tự thêm. */
    fun addToContacts(context: Context, number: String) {
        val clean = number.trim()
        if (clean.isEmpty() || clean.startsWith("-")) {
            Toast.makeText(context, appStrings().actions.invalidPhone, Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_INSERT).apply {
            type = ContactsContract.Contacts.CONTENT_TYPE
            putExtra(ContactsContract.Intents.Insert.PHONE, clean)
        }
        Log.d(TAG, "Add to contacts: $clean")
        val opened = runCatching { context.startActivity(intent) }.isSuccess
        if (!opened) Toast.makeText(context, appStrings().actions.contactsAppOpenFailed, Toast.LENGTH_SHORT).show()
    }

    /**
     * Mở màn TẠO liên hệ mới điền sẵn NHIỀU trường (từ danh thiếp QR vCard/MECARD):
     * tên / điện thoại / email / tổ chức. Trường rỗng thì bỏ qua; không tự lưu — người dùng xác nhận trong Danh bạ.
     */
    fun addContact(context: Context, name: String?, phone: String?, email: String?, org: String?) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            type = ContactsContract.Contacts.CONTENT_TYPE
            if (!name.isNullOrBlank()) putExtra(ContactsContract.Intents.Insert.NAME, name.trim())
            if (!phone.isNullOrBlank()) putExtra(ContactsContract.Intents.Insert.PHONE, phone.trim())
            if (!email.isNullOrBlank()) putExtra(ContactsContract.Intents.Insert.EMAIL, email.trim())
            if (!org.isNullOrBlank()) putExtra(ContactsContract.Intents.Insert.COMPANY, org.trim())
        }
        Log.d(TAG, "Add contact from QR: name=$name phone=$phone")
        val opened = runCatching { context.startActivity(intent) }.isSuccess
        if (!opened) Toast.makeText(context, appStrings().actions.contactsAppOpenFailed, Toast.LENGTH_SHORT).show()
    }

    /** Mở & xem liên hệ đã lưu theo [lookupUri] (chuỗi URI lấy từ PhoneLookup) trong Danh bạ. */
    fun viewContact(context: Context, lookupUri: String) {
        val uri = runCatching { Uri.parse(lookupUri) }.getOrNull()
        if (uri == null) {
            Toast.makeText(context, appStrings().actions.contactOpenFailed, Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_VIEW, uri)
        Log.d(TAG, "View contact: $lookupUri")
        val opened = runCatching { context.startActivity(intent) }.isSuccess
        if (!opened) Toast.makeText(context, appStrings().actions.contactsAppOpenFailed, Toast.LENGTH_SHORT).show()
    }

    /** Mở màn TẠO liên hệ mới TRỐNG (không điền sẵn trường nào) của Danh bạ mặc định. */
    fun createContact(context: Context) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            type = ContactsContract.Contacts.CONTENT_TYPE
        }
        Log.d(TAG, "Create new contact")
        val opened = runCatching { context.startActivity(intent) }.isSuccess
        if (!opened) Toast.makeText(context, appStrings().actions.contactsAppOpenFailed, Toast.LENGTH_SHORT).show()
    }

    /** Mở màn SỬA liên hệ (ACTION_EDIT) theo [lookupUri] trong Danh bạ mặc định. */
    fun editContact(context: Context, lookupUri: String) {
        val uri = runCatching { Uri.parse(lookupUri) }.getOrNull()
        if (uri == null) {
            Toast.makeText(context, appStrings().actions.contactOpenFailed, Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(Intent.ACTION_EDIT).apply {
            setDataAndType(uri, ContactsContract.Contacts.CONTENT_ITEM_TYPE)
            // Lưu xong thì đóng màn sửa, trả về app (chuẩn của Danh bạ Google/AOSP).
            putExtra("finishActivityOnSaveCompleted", true)
        }
        Log.d(TAG, "Edit contact: $lookupUri")
        val opened = runCatching { context.startActivity(intent) }.isSuccess
        if (!opened) Toast.makeText(context, appStrings().actions.contactsAppOpenFailed, Toast.LENGTH_SHORT).show()
    }

    /**
     * "Xoá" liên hệ: app CHỈ ĐỌC (không tự xoá, không cần WRITE_CONTACTS) nên CHUYỂN sang Danh bạ mặc định.
     * Thử [Intent.ACTION_DELETE] trước (một số app hiện hộp xác nhận xoá ngay); app không nhận thì lùi về
     * [Intent.ACTION_VIEW] để mở liên hệ cho người dùng tự xoá trong menu của Danh bạ.
     */
    fun deleteContact(context: Context, lookupUri: String) {
        val uri = runCatching { Uri.parse(lookupUri) }.getOrNull()
        if (uri == null) {
            Toast.makeText(context, appStrings().actions.contactOpenFailed, Toast.LENGTH_SHORT).show()
            return
        }
        Log.d(TAG, "Delete contact (route to Contacts app): $lookupUri")
        val deleted = runCatching { context.startActivity(Intent(Intent.ACTION_DELETE, uri)) }.isSuccess
        if (deleted) return
        val viewed = runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri)) }.isSuccess
        if (!viewed) Toast.makeText(context, appStrings().actions.contactsAppOpenFailed, Toast.LENGTH_SHORT).show()
    }
}

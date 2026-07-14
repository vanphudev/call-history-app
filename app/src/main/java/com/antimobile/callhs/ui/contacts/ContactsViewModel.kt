package com.antimobile.callhs.ui.contacts

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antimobile.callhs.data.contacts.Contact
import com.antimobile.callhs.data.contacts.ContactsRepository
import com.antimobile.callhs.util.ContactsObserver
import com.antimobile.callhs.util.hasPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel màn DANH BẠ: nạp toàn bộ liên hệ (nền IO) khi có quyền READ_CONTACTS. Lọc tìm kiếm làm ở
 * Compose (dữ liệu đã nạp sẵn) nên gõ tới đâu lọc tới đó, không truy vấn lại máy.
 */
class ContactsViewModel(app: Application) : AndroidViewModel(app) {

    private val context: Context = app
    private val repo = ContactsRepository(app.contentResolver)

    var contacts by mutableStateOf<List<Contact>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var loaded by mutableStateOf(false)
        private set
    var hasContactsPermission by mutableStateOf(hasPermission(app, Manifest.permission.READ_CONTACTS))
        private set

    private var job: Job? = null

    /** Danh bạ máy đổi (sửa ở app Danh bạ, đồng bộ tài khoản…) → tự nạp lại NGẦM, không cần quay lại màn. */
    private val contactsObserver = ContactsObserver(app) { load() }

    fun load() {
        hasContactsPermission = hasPermission(context, Manifest.permission.READ_CONTACTS)
        if (!hasContactsPermission) return
        contactsObserver.ensureRegistered()
        job?.cancel()
        job = viewModelScope.launch {
            loading = true
            val data = withContext(Dispatchers.IO) {
                runCatching { repo.loadAll() }.getOrDefault(emptyList())
            }
            contacts = data
            loaded = true
            loading = false
        }
    }

    /**
     * Quay lại màn (mở lại app / vừa sửa liên hệ ở app Danh bạ hệ thống / vừa cấp quyền) → cập nhật quyền +
     * LUÔN nạp lại khi đã có quyền, để bắt thay đổi danh bạ xảy ra lúc app ở nền (khớp cách nhật ký cuộc gọi
     * tự nạp lại ở ON_START). Chưa có quyền thì thôi (tránh xoá trắng dữ liệu đã hiển thị).
     */
    fun refresh() {
        val nowGranted = hasPermission(context, Manifest.permission.READ_CONTACTS)
        hasContactsPermission = nowGranted
        if (nowGranted) load()
    }

    override fun onCleared() {
        contactsObserver.dispose()
    }
}

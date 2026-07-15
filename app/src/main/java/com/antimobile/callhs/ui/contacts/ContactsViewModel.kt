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
import com.antimobile.callhs.util.ContactsSignal
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
    // Lần nạp gần nhất THẤT BẠI (đọc danh bạ ném lỗi) → UI phân biệt "lỗi tải" với "danh bạ trống thật".
    var loadFailed by mutableStateOf(false)
        private set
    var hasContactsPermission by mutableStateOf(hasPermission(app, Manifest.permission.READ_CONTACTS))
        private set

    private var job: Job? = null

    /** Danh bạ máy đổi (sửa ở app Danh bạ, đồng bộ tài khoản…) → tự nạp lại NGẦM, không cần quay lại màn. */
    init { ContactsSignal.observe(viewModelScope) { load() } }

    fun load() {
        hasContactsPermission = hasPermission(context, Manifest.permission.READ_CONTACTS)
        if (!hasContactsPermission) return
        // Lần nạp ĐẦU đang chạy (chưa loaded) mà bị gọi lại (LaunchedEffect + ON_START cùng bắn lúc vào màn) →
        // đừng huỷ rồi quét lại danh bạ lần nữa. Lần vào lại (đã loaded) vẫn nạp lại bình thường để bắt thay đổi.
        if (job?.isActive == true && !loaded) return
        ContactsSignal.ensureRegistered(context)
        job?.cancel()
        job = viewModelScope.launch {
            loading = true
            val result = withContext(Dispatchers.IO) { runCatching { repo.loadAll() } }
            if (result.isSuccess) {
                contacts = result.getOrDefault(emptyList())
                loadFailed = false
                loaded = true
            } else {
                // Lỗi đọc danh bạ (quyền bị thu hồi giữa chừng / provider lỗi) → GIỮ dữ liệu đang hiện, bật cờ lỗi.
                loadFailed = true
            }
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
}

package com.antimobile.callhs.ui.calllist

import android.Manifest
import android.app.Application
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antimobile.callhs.data.model.CallEntry
import com.antimobile.callhs.data.repository.CallLogRepository
import com.antimobile.callhs.util.ContactsSignal
import com.antimobile.callhs.util.hasPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CallListViewModel(app: Application) : AndroidViewModel(app) {

    private val resolver = app.contentResolver
    private val repo = CallLogRepository(app)
    private val context: Context = app

    var entries by mutableStateOf<List<CallEntry>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    /** Đang làm mới do NGƯỜI DÙNG vuốt xuống (pull-to-refresh) — tách khỏi [loading] (nạp đầu / tự động). */
    var refreshing by mutableStateOf(false)
        private set
    var loaded by mutableStateOf(false)
        private set

    private var job: Job? = null
    private var observing = false

    /**
     * Theo dõi thay đổi nhật ký cuộc gọi (cuộc gọi mới, xoá ở app khác…) → TỰ nạp lại NGẦM ngay,
     * không cần thao tác tay. Nạp qua [load] nên KHÔNG hiện vòng xoay khi đã có sẵn dữ liệu.
     */
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = load()
    }

    /**
     * Danh bạ đổi (đổi tên/ảnh liên hệ, thêm/xoá số, đồng bộ tài khoản…) → nạp lại NGẦM để tên/ảnh trong
     * danh sách luôn mới, kể cả khi KHÔNG có cuộc gọi mới nào. Bổ trợ [observer] (chỉ bắt nhật ký cuộc gọi).
     */
    init { ContactsSignal.observe(viewModelScope) { load() } }

    /** Nạp danh sách (lần đầu / tự động khi nhật ký đổi) — hiện loader toàn màn chỉ khi CHƯA có dữ liệu. */
    fun load() = fetch(userInitiated = false)

    /** NGƯỜI DÙNG vuốt xuống làm mới — hiện vòng xoay pull-to-refresh thay cho loader toàn màn. */
    fun refresh() = fetch(userInitiated = true)

    /**
     * Nạp lại nhật ký. Bỏ qua khi chưa có quyền (tránh xoá trắng dữ liệu); huỷ lần nạp trước còn dở.
     * [userInitiated] = true → bật [refreshing] (pull-to-refresh); false → bật [loading].
     * Chỉ lần nạp CHẠY XONG (isActive) mới tắt cờ. Lần nạp bị HUỶ (do lần nạp mới hơn tiếp quản) KHÔNG
     * đụng cờ → tránh tắt nhầm vòng xoay của lần nạp mới đang chạy; lần nạp mới sẽ tự tắt khi xong.
     */
    private fun fetch(userInitiated: Boolean) {
        if (!hasPermission(context, Manifest.permission.READ_CALL_LOG)) return
        startObservingIfNeeded()
        job?.cancel()
        job = viewModelScope.launch {
            if (userInitiated) refreshing = true else loading = true
            try {
                val data = withContext(Dispatchers.IO) {
                    runCatching { repo.loadRecent() }.getOrDefault(emptyList())
                }
                entries = data
                loaded = true
            } finally {
                if (isActive) {
                    loading = false
                    refreshing = false
                }
            }
        }
    }

    /**
     * Đăng ký ContentObserver LƯỜI — CHỈ sau khi đã có quyền. registerContentObserver mở
     * CallLogProvider nên gọi lúc CHƯA có quyền sẽ ném SecurityException (từng làm app crash ngay
     * khi mở lần đầu trên máy mới cài, chưa cấp quyền). load() đã kiểm tra quyền trước khi gọi hàm này.
     */
    private fun startObservingIfNeeded() {
        if (!observing) {
            resolver.registerContentObserver(CallLog.Calls.CONTENT_URI, true, observer)
            observing = true
        }
        // Quyền Danh bạ là TUỲ CHỌN nên đăng ký riêng (lười): tự bám khi người dùng cấp quyền sau.
        ContactsSignal.ensureRegistered(context)
    }

    override fun onCleared() {
        if (observing) resolver.unregisterContentObserver(observer)
    }
}

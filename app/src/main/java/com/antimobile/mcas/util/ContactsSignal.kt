package com.antimobile.mcas.util

import android.Manifest
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Tín hiệu "DANH BẠ ĐỔI" cấp TIẾN TRÌNH — MỘT [ContentObserver] duy nhất cho cả app, thay cho việc mỗi ViewModel
 * tự đăng ký một cái. Trước đây 4–8 ViewModel sống song song, mỗi cái một observer + một debounce riêng: một đợt
 * đồng bộ tài khoản kích 4–8 lần [ContactPhotoSignal.invalidate] (dọn cache ảnh 4–8 lần) và 4–8 lượt nạp lại,
 * mỗi lượt quét TOÀN bảng Phone. Nay: một observer, gom nhóm (debounce) rồi bump [ContactPhotoSignal.generation]
 * MỘT lần; ViewModel lắng nghe generation đó ([observe]) để tự nạp lại → 1 lần dọn cache, và bảng Phone chỉ quét
 * 1 lần nhờ cache trong CallLogRepository.
 *
 * Đăng ký app-lifetime (không gỡ) như [com.antimobile.mcas.data.local.CategoryCatalog]; dùng applicationContext
 * nên không giữ Activity. [ContactPhotoSignal.generation] là snapshot-state nên composable đọc ảnh vẫn tự cập nhật.
 */
object ContactsSignal {

    private const val DEBOUNCE_MS = 450L
    private val handler = Handler(Looper.getMainLooper())
    private val fire = Runnable { ContactPhotoSignal.invalidate() }

    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) {
            handler.removeCallbacks(fire)      // gộp cả đợt onChange → chỉ báo lần cuối
            handler.postDelayed(fire, DEBOUNCE_MS)
        }
    }
    private var registered = false

    /**
     * Đăng ký MỘT lần khi đã có READ_CONTACTS (mở ContactsProvider lúc chưa cấp quyền có thể ném SecurityException).
     * An toàn khi gọi lại nhiều lần từ mọi ViewModel; tự bám khi quyền được cấp về sau.
     */
    fun ensureRegistered(context: Context) {
        if (registered) return
        if (!hasPermission(context, Manifest.permission.READ_CONTACTS)) return
        context.applicationContext.contentResolver
            .registerContentObserver(ContactsContract.AUTHORITY_URI, true, observer)
        registered = true
    }

    /**
     * Lắng nghe "danh bạ đổi" trong [scope] (thường là viewModelScope) → gọi [onChange] mỗi lần đổi.
     * Bỏ giá trị đầu ([drop] 1) để KHÔNG kích nạp thừa ngay lúc tạo ViewModel (đã có lần nạp ban đầu riêng).
     */
    fun observe(scope: CoroutineScope, onChange: () -> Unit): Job = scope.launch {
        snapshotFlow { ContactPhotoSignal.generation.intValue }
            .drop(1)
            .collect { onChange() }
    }
}

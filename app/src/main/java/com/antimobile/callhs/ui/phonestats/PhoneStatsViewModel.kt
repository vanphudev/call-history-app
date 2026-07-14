package com.antimobile.callhs.ui.phonestats

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antimobile.callhs.data.model.CallNumberDetail
import com.antimobile.callhs.data.repository.CallLogRepository
import com.antimobile.callhs.util.ContactsObserver
import com.antimobile.callhs.util.hasPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel cho màn "Phân tích cuộc gọi" (PhoneStatsScreen). Nạp TOÀN BỘ lịch sử của MỘT số qua
 * [CallLogRepository.loadDetail] (đã áp phạm vi SIM toàn app tại gốc, KHÔNG cắt số lượng) rồi để UI tự phân
 * tích bằng [com.antimobile.callhs.util.PhoneAnalysis]. Chỉ cần READ_CALL_LOG (không cần READ_PHONE_STATE).
 *
 * Cùng khuôn với [com.antimobile.callhs.ui.coststats.CostStatsViewModel]: tạo ở tầng điều hướng, [load] khi mở
 * số, [refresh] khi quay lại app / danh bạ đổi.
 */
class PhoneStatsViewModel(app: Application) : AndroidViewModel(app) {

    private val context: Context = app
    private val repo = CallLogRepository(app)

    var detail by mutableStateOf<CallNumberDetail?>(null)
        private set
    var loading by mutableStateOf(false)
        private set

    private var currentNumber: String? = null
    private var job: Job? = null

    /** Danh bạ đổi → nạp lại để tên/ảnh liên hệ ở tiêu đề màn phân tích luôn khớp liên hệ hiện tại. */
    private val contactsObserver = ContactsObserver(app) { refresh() }

    fun load(number: String) {
        // Đổi sang số khác → xoá dữ liệu số cũ NGAY để màn không hiện nhầm thống kê của số trước trong lúc nạp.
        if (number != currentNumber) detail = null
        currentNumber = number
        reload()
    }

    /** Quay về app / danh bạ đổi → nạp lại. No-op nếu chưa mở số nào. */
    fun refresh() {
        if (currentNumber != null) reload()
    }

    private fun reload() {
        val number = currentNumber ?: return
        if (!hasPermission(context, Manifest.permission.READ_CALL_LOG)) return
        contactsObserver.ensureRegistered()
        job?.cancel()
        job = viewModelScope.launch {
            loading = true
            val data = withContext(Dispatchers.IO) {
                runCatching { repo.loadDetail(number) }.getOrNull()
            }
            if (data != null) detail = data
            loading = false
        }
    }

    override fun onCleared() {
        contactsObserver.dispose()
    }
}

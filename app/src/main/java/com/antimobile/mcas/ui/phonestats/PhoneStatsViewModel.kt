package com.antimobile.mcas.ui.phonestats

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antimobile.mcas.data.model.CallNumberDetail
import com.antimobile.mcas.data.repository.CallLogRepository
import com.antimobile.mcas.util.ContactsSignal
import com.antimobile.mcas.util.hasPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel cho màn "Phân tích cuộc gọi" (PhoneStatsScreen). Nạp TOÀN BỘ lịch sử của MỘT số qua
 * [CallLogRepository.loadDetail] (đã áp phạm vi SIM toàn app tại gốc, KHÔNG cắt số lượng) rồi để UI tự phân
 * tích bằng [com.antimobile.mcas.util.PhoneAnalysis]. Chỉ cần READ_CALL_LOG (không cần READ_PHONE_STATE).
 *
 * Cùng khuôn với [com.antimobile.mcas.ui.coststats.CostStatsViewModel]: tạo ở tầng điều hướng, [load] khi mở
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
    init { ContactsSignal.observe(viewModelScope) { refresh() } }

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
        ContactsSignal.ensureRegistered(context)
        job?.cancel()
        job = viewModelScope.launch {
            loading = true
            val data = withContext(Dispatchers.IO) {
                runCatching { repo.loadDetail(number) }.getOrNull()
            }
            // Nạp lỗi → xoá dữ liệu (đồng nhất CallDetail/CostStatsViewModel), không giữ thống kê CŨ đã hết hiệu lực.
            detail = data
            loading = false
        }
    }

    override fun onCleared() {
        super.onCleared()
    }
}

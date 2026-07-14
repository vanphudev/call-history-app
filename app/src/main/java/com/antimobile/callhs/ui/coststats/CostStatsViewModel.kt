package com.antimobile.callhs.ui.coststats

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
import com.antimobile.callhs.util.CallCharge
import com.antimobile.callhs.util.ContactsObserver
import com.antimobile.callhs.util.CostCalculator
import com.antimobile.callhs.util.CostSummary
import com.antimobile.callhs.util.SimInfo
import com.antimobile.callhs.util.hasPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Toàn bộ dữ liệu cho màn ước tính cước của một số. */
data class CostState(
    val detail: CallNumberDetail,
    val slots: List<SimInfo.SlotInfo>, // nguồn CHUNG với màn "Số của tôi": nhà mạng SIM lắp máy HOẶC suy từ số đã nhập
    val charges: List<CallCharge>,     // cùng thứ tự mới → cũ như detail.entries
    val summary: CostSummary
)

/**
 * ViewModel cho màn "Cước cuộc gọi": nạp lịch sử của một số (qua [CallLogRepository]), dò các SIM đang
 * lắp (qua [SimInfo], cần READ_PHONE_STATE), rồi tính cước từng cuộc bằng [CostCalculator].
 */
class CostStatsViewModel(app: Application) : AndroidViewModel(app) {

    private val context: Context = app
    private val repo = CallLogRepository(app)

    var state by mutableStateOf<CostState?>(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var hasPhonePermission by mutableStateOf(hasPermission(app, Manifest.permission.READ_PHONE_STATE))
        private set

    private var currentNumber: String? = null
    private var job: Job? = null

    /** Danh bạ đổi → nạp lại để tên/ảnh liên hệ ở tiêu đề màn cước luôn khớp liên hệ hiện tại. */
    private val contactsObserver = ContactsObserver(app) { refresh() }

    fun load(number: String) {
        // Đổi sang số khác → xoá dữ liệu số cũ NGAY để màn không hiện nhầm cước của số trước trong lúc nạp.
        if (number != currentNumber) state = null
        currentNumber = number
        hasPhonePermission = hasPermission(context, Manifest.permission.READ_PHONE_STATE)
        reload()
    }

    /** Cập nhật lại trạng thái quyền + nạp lại (khi quay về app hoặc vừa cấp quyền). No-op nếu chưa mở số. */
    fun refresh() {
        hasPhonePermission = hasPermission(context, Manifest.permission.READ_PHONE_STATE)
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
                runCatching {
                    val detail = repo.loadDetail(number)
                    val sims = SimInfo.activeSims(context)
                    val byId = SimInfo.byAccountId(sims)
                    // Nguồn CHUNG để HIỂN THỊ nhà mạng (khớp màn "Số của tôi"): SIM đang lắp + số người dùng nhập.
                    val device = SimInfo.readDeviceSims(context)
                    // KHÔNG có SIM đang lắp → ước tính cước theo nhà mạng SUY TỪ SỐ người dùng đã nhập (SIM 1)
                    // cho MỌI cuộc. CÓ SIM → giữ nguyên cách khớp theo tài khoản (cuộc có SIM lạ vẫn để "chưa
                    // xác định", không đoán bừa nhà mạng khác).
                    val fallbackCarrier = if (sims.isEmpty()) device.slots.firstOrNull()?.effectiveCarrier else null
                    val charges = detail.entries.map { entry ->
                        val simCarrier = SimInfo.resolveCarrier(entry.phoneAccountId, byId, sims) ?: fallbackCarrier
                        CostCalculator.charge(entry, simCarrier)
                    }
                    CostState(detail, device.slots, charges, CostCalculator.summarize(charges))
                }.getOrNull()
            }
            state = data
            loading = false
        }
    }

    override fun onCleared() {
        contactsObserver.dispose()
    }
}

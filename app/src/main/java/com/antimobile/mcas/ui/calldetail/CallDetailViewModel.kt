package com.antimobile.mcas.ui.calldetail

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
import com.antimobile.mcas.data.model.CallNumberDetail
import com.antimobile.mcas.data.repository.CallLogRepository
import com.antimobile.mcas.util.ContactsSignal
import com.antimobile.mcas.util.SimScope
import com.antimobile.mcas.util.hasPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CallDetailViewModel(app: Application) : AndroidViewModel(app) {

    private val resolver = app.contentResolver
    private val repo = CallLogRepository(app)
    private val context: Context = app

    var detail by mutableStateOf<CallNumberDetail?>(null)
        private set
    var loading by mutableStateOf(false)
        private set

    private var currentNumber: String? = null
    // Phạm vi SIM toàn app lúc dữ liệu hiện tại được nạp — để phát hiện người dùng ĐỔI phạm vi (ở Cài đặt)
    // rồi mở lại ĐÚNG số này: khi đó phải nạp lại thay vì trả về sớm dữ liệu cũ (sai SIM). VM này DÙNG CHUNG
    // cho mọi lần mở chi tiết (hoisted ở AppNav) nên cần tự đối chiếu phạm vi.
    private var loadedScope: String? = null
    private var job: Job? = null
    private var observing = false

    /** Nhật ký cuộc gọi đổi (vd vừa gọi lại số này) → TỰ nạp lại số đang xem, không cần bấm "Làm mới". */
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) = refresh()
    }

    /**
     * Danh bạ đổi → nạp lại số đang xem để tên/ảnh ở tiêu đề + hàng "Thêm/Xem danh bạ" luôn khớp liên hệ
     * hiện tại (vd vừa lưu số này thành liên hệ, hoặc đổi tên). Bổ trợ [observer] (chỉ bắt nhật ký cuộc gọi).
     */
    init { ContactsSignal.observe(viewModelScope) { refresh() } }

    fun load(number: String) {
        // Cùng số + đã có dữ liệu + phạm vi SIM HIỆU LỰC không đổi → khỏi nạp lại. So [SimScope.effectiveLabel]
        // (nhãn thực sự đang lọc, đã tính SIM có lắp hay không) chứ KHÔNG so [SimScope.scope] thô — nếu không,
        // sau khi lắp lại SIM đã lưu, guard tưởng dữ liệu cũ (chưa lọc) còn hợp lệ.
        if (currentNumber == number && detail != null && loadedScope == SimScope.effectiveLabel) return
        // Đổi sang số khác → XOÁ dữ liệu số cũ NGAY để màn (và AllCalls/Timeline dùng chung VM) không vẽ nhầm
        // chi tiết + nút gọi/nhắn của số TRƯỚC trong lúc truy vấn số mới (khớp CostStats/PhoneStatsViewModel).
        if (number != currentNumber) {
            detail = null
            loadedScope = null
        }
        currentNumber = number
        reload()
    }

    /** Nạp lại số đang xem (khi quay lại app hoặc nhật ký đổi). No-op nếu chưa mở số nào. */
    fun refresh() {
        if (currentNumber != null) reload()
    }

    /** Nạp lại chi tiết cho [currentNumber]. Bỏ qua khi chưa có quyền; huỷ lần nạp trước còn dở. */
    private fun reload() {
        val number = currentNumber ?: return
        if (!hasPermission(context, Manifest.permission.READ_CALL_LOG)) return
        startObservingIfNeeded()
        job?.cancel()
        job = viewModelScope.launch {
            loading = true
            val data = withContext(Dispatchers.IO) {
                runCatching { repo.loadDetail(number) }.getOrNull()
            }
            detail = data
            loadedScope = SimScope.effectiveLabel   // ghi lại phạm vi HIỆU LỰC mà dữ liệu hiện tại được nạp theo
            loading = false
        }
    }

    /**
     * Đăng ký ContentObserver LƯỜI — CHỈ sau khi đã có quyền. registerContentObserver mở
     * CallLogProvider nên gọi lúc CHƯA có quyền sẽ ném SecurityException (gây crash lúc mở app lần
     * đầu). reload() đã kiểm tra quyền trước khi gọi hàm này.
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

package com.antimobile.callhs.ui.repeatstats

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antimobile.callhs.data.repository.CallLogRepository
import com.antimobile.callhs.util.ContactsObserver
import com.antimobile.callhs.util.RepeatAnalysis
import com.antimobile.callhs.util.RepeatFilter
import com.antimobile.callhs.util.RepeatNumber
import com.antimobile.callhs.util.RepeatSort
import com.antimobile.callhs.util.RepeatStats
import com.antimobile.callhs.util.hasPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

/**
 * ViewModel cho màn "Gọi lại & trùng số". Nạp MỘT LẦN tối đa [LOAD_LIMIT] cuộc gần nhất, phân tích cửa sổ
 * 30 ngày bằng [RepeatStats] (logic thuần), rồi CHỈ lọc/sắp lại danh sách khi người dùng đổi bộ lọc/sắp xếp
 * (không nạp lại máy). Cửa sổ tính đến thời điểm nạp (cuốn chiếu).
 */
class RepeatStatsViewModel(app: Application) : AndroidViewModel(app) {

    private val context: Context = app
    private val repo = CallLogRepository(app)

    var loading by mutableStateOf(false)
        private set
    var loaded by mutableStateOf(false)
        private set
    var hasCallLogPermission by mutableStateOf(hasPermission(app, Manifest.permission.READ_CALL_LOG))
        private set

    var filter by mutableStateOf(RepeatFilter.REPEAT)
        private set
    var sort by mutableStateOf(RepeatSort.TOTAL)
        private set

    var analysis by mutableStateOf(
        RepeatAnalysis.empty(RepeatStats.WINDOW_DAYS, 0L, 0L)
    )
        private set

    /** Danh sách số đã LỌC + SẮP theo lựa chọn hiện tại. */
    var numbers by mutableStateOf<List<RepeatNumber>>(emptyList())
        private set

    private var job: Job? = null

    /** Danh bạ đổi → nạp lại để tên/ảnh liên hệ trong danh sách số gọi lại/trùng số luôn mới. */
    private val contactsObserver = ContactsObserver(app) { load() }

    fun load() {
        hasCallLogPermission = hasPermission(context, Manifest.permission.READ_CALL_LOG)
        if (!hasCallLogPermission) return
        contactsObserver.ensureRegistered()
        job?.cancel()
        job = viewModelScope.launch {
            loading = true
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val entries = repo.loadRecent(LOAD_LIMIT)
                    val zone = ZoneId.systemDefault()
                    RepeatStats.analyze(entries, LocalDate.now(zone), zone)
                }.getOrNull()
            }
            if (result != null) {
                analysis = result
                recompute()
                loaded = true
            }
            loading = false
        }
    }

    /** Quay lại màn / vừa cấp quyền → nạp lại nếu đã từng nạp hoặc nay đã có quyền. */
    fun refresh() {
        val nowHasLog = hasPermission(context, Manifest.permission.READ_CALL_LOG)
        val changed = nowHasLog != hasCallLogPermission
        if (loaded || changed) load()
    }

    fun selectFilter(newFilter: RepeatFilter) {
        if (newFilter == filter) return
        filter = newFilter
        recompute()
    }

    fun selectSort(newSort: RepeatSort) {
        if (newSort == sort) return
        sort = newSort
        recompute()
    }

    private fun recompute() {
        numbers = RepeatStats.applySort(RepeatStats.applyFilter(analysis.numbers, filter), sort)
    }

    override fun onCleared() {
        contactsObserver.dispose()
    }

    private companion object {
        /** Cửa sổ nạp — rộng để 30 ngày đủ dữ liệu kể cả shipper gọi rất nhiều, vẫn tránh giật. */
        const val LOAD_LIMIT = 5000
    }
}

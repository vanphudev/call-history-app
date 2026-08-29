package com.antimobile.mcas.ui.stats

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antimobile.mcas.data.model.CallEntry
import com.antimobile.mcas.data.repository.CallLogRepository
import com.antimobile.mcas.util.CallCharge
import com.antimobile.mcas.util.CallStats
import com.antimobile.mcas.util.ContactsSignal
import com.antimobile.mcas.util.CostCalculator
import com.antimobile.mcas.util.CostSummary
import com.antimobile.mcas.util.DayActivity
import com.antimobile.mcas.util.PeriodOverview
import com.antimobile.mcas.util.PhoneKey
import com.antimobile.mcas.util.PhoneStat
import com.antimobile.mcas.util.SimInfo
import com.antimobile.mcas.util.StatsPeriod
import com.antimobile.mcas.util.TopNumbers
import com.antimobile.mcas.util.hasPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

/** Cước ước tính của MỘT số trong khoảng đang xem. */
data class NumberCost(
    val number: String,
    val cachedName: String?,
    val photoUri: String?,
    val cost: Long,
    val chargeableCalls: Int
)

/** Tổng hợp cước cho MỘT khoảng: tóm tắt nội/ngoại mạng + xếp hạng theo số + cờ thiếu thông tin SIM. */
data class PeriodCost(
    val summary: CostSummary,
    val byNumber: List<NumberCost>,
    val hasUnknownSim: Boolean
) {
    val totalCost: Long get() = summary.totalCost

    companion object {
        val EMPTY = PeriodCost(
            summary = CostSummary(0L, 0, 0L, 0, 0L, 0, 0L, 0, 0L, 0, 0),
            byNumber = emptyList(),
            hasUnknownSim = false
        )

        fun from(charges: List<CallCharge>): PeriodCost {
            val summary = CostCalculator.summarize(charges)
            val byNumber = charges
                .filter { it.chargeable && it.cost > 0 }
                // Gộp theo KHOÁ CHUẨN [PhoneKey] → xếp hạng cước theo NGƯỜI, không tách "+84…"/"0…" thành 2 dòng.
                .groupBy { PhoneKey.of(it.entry.number).ifEmpty { it.entry.number } }
                .map { (_, list) ->
                    val rep = list.first().entry
                    NumberCost(
                        number = rep.number,
                        cachedName = rep.cachedName?.takeIf { it.isNotBlank() },
                        photoUri = rep.photoUri,
                        cost = list.sumOf { it.cost },
                        chargeableCalls = list.size
                    )
                }
                .sortedByDescending { it.cost }
            return PeriodCost(summary, byNumber, summary.unknownSimCalls > 0)
        }
    }
}

/**
 * ViewModel cho màn "Thống kê chi tiết". Nạp MỘT LẦN tối đa [LOAD_LIMIT] cuộc gần nhất (để thống kê tháng
 * đủ dữ liệu), tính cước từng cuộc bằng [CostCalculator] (cần READ_PHONE_STATE để biết nhà mạng SIM), rồi
 * KÉO gọn theo khoảng [period] khi người dùng đổi Hôm nay/Tuần/Tháng (tính lại nhanh, không nạp lại máy).
 */
class DetailedStatsViewModel(app: Application) : AndroidViewModel(app) {

    private val context: Context = app
    private val repo = CallLogRepository(app)

    var loading by mutableStateOf(false)
        private set
    var loaded by mutableStateOf(false)
        private set
    var hasCallLogPermission by mutableStateOf(hasPermission(app, Manifest.permission.READ_CALL_LOG))
        private set
    var hasPhonePermission by mutableStateOf(hasPermission(app, Manifest.permission.READ_PHONE_STATE))
        private set

    var period by mutableStateOf(StatsPeriod.WEEK)
        private set

    // ---- Kết quả đã tính cho khoảng đang chọn (tab "Theo số" + "Cước phí") ----
    var overview by mutableStateOf(PeriodOverview.EMPTY)
        private set
    var phoneStats by mutableStateOf<List<PhoneStat>>(emptyList())
        private set
    var topNumbers by mutableStateOf(TopNumbers.EMPTY)
        private set
    var cost by mutableStateOf(PeriodCost.EMPTY)
        private set

    // ---- Không phụ thuộc khoảng: hoạt động 15 ngày gần nhất (tab "Khung giờ") ----
    var dayActivities by mutableStateOf<List<DayActivity>>(emptyList())
        private set

    private var entries: List<CallEntry> = emptyList()
    private var charges: List<CallCharge> = emptyList()
    private var job: Job? = null

    /** Danh bạ đổi → nạp lại để tên/ảnh liên hệ trong bảng xếp hạng "Theo số" / "Cước phí" luôn mới. */
    init { ContactsSignal.observe(viewModelScope) { load() } }

    fun load() {
        hasCallLogPermission = hasPermission(context, Manifest.permission.READ_CALL_LOG)
        hasPhonePermission = hasPermission(context, Manifest.permission.READ_PHONE_STATE)
        if (!hasCallLogPermission) return
        ContactsSignal.ensureRegistered(context)
        job?.cancel()
        job = viewModelScope.launch {
            loading = true
            val data = withContext(Dispatchers.IO) {
                runCatching {
                    val loaded = repo.loadRecent(LOAD_LIMIT)
                    val sims = SimInfo.activeSims(context)
                    val byId = SimInfo.byAccountId(sims)
                    val computed = loaded.map { e ->
                        CostCalculator.charge(e, SimInfo.resolveCarrier(e.phoneAccountId, byId, sims))
                    }
                    loaded to computed
                }.getOrNull()
            }
            if (data != null) {
                entries = data.first
                charges = data.second
                dayActivities = CallStats.dayActivities(entries, CallStats.MAX_ACTIVITY_DAYS, ZoneId.systemDefault())
                recompute()
                loaded = true
            }
            loading = false
        }
    }

    /** Quay lại màn / vừa cấp quyền → nạp lại nếu đã từng nạp hoặc nay đã có quyền. */
    fun refresh() {
        val nowHasLog = hasPermission(context, Manifest.permission.READ_CALL_LOG)
        val nowHasPhone = hasPermission(context, Manifest.permission.READ_PHONE_STATE)
        val changed = nowHasLog != hasCallLogPermission || nowHasPhone != hasPhonePermission
        if (loaded || changed) load()
    }

    fun selectPeriod(newPeriod: StatsPeriod) {
        if (newPeriod == period) return
        period = newPeriod
        recompute()
    }

    /** Tính lại các thống kê PHỤ THUỘC khoảng (nhanh — chỉ lọc + gộp danh sách đã nạp sẵn). */
    private fun recompute() {
        val zone = ZoneId.systemDefault()
        val range = CallStats.periodRange(period, LocalDate.now(zone), zone)
        val periodStats = CallStats.phoneStats(entries.filter { it.dateMillis in range })
        phoneStats = periodStats
        overview = CallStats.overview(periodStats)
        topNumbers = CallStats.topNumbers(periodStats)
        cost = PeriodCost.from(charges.filter { it.entry.dateMillis in range })
    }

    override fun onCleared() {
        super.onCleared()
    }

    private companion object {
        /** Cửa sổ nạp cho thống kê — rộng hơn danh sách (1000) để tháng này đủ dữ liệu, vẫn tránh giật. */
        const val LOAD_LIMIT = 5000
    }
}

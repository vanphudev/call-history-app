package com.antimobile.callhs.util

import com.antimobile.callhs.data.model.CallEntry
import com.antimobile.callhs.data.model.CallType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Phân tích CHUYÊN SÂU lịch sử cuộc gọi với MỘT số — LOGIC THUẦN (không phụ thuộc Android/Compose) để dễ
 * kiểm thử & tái dùng cho màn "Phân tích cuộc gọi". Mọi mốc ngày/giờ dùng [ZoneId] truyền vào (thống nhất với
 * [TimeFormat] = ZoneId.systemDefault()).
 *
 * QUY TẮC GỘP LOẠI khớp TOÀN app (xem [CallStats.bucketOf]/[CallLogRepository.loadDetail]):
 *  - Gọi ĐI  = OUTGOING
 *  - Gọi ĐẾN = INCOMING / ANSWERED_EXTERNALLY
 *  - NHỠ     = MISSED / REJECTED / BLOCKED
 *  - VOICEMAIL / UNKNOWN ([CallBucket.OTHER]) KHÔNG tính vào 3 nhóm → mọi con số & biểu đồ luôn khớp nhau.
 * "Đàm thoại thật" (talk time) theo [CallResults.isConnected] (MISSED/REJECTED/BLOCKED luôn = chưa kết nối,
 * kể cả khi cột DURATION > 0 do máy ghi thời gian đổ chuông).
 */

/** Phân bố 3 HƯỚNG (đi / đến / nhỡ) — dữ liệu cho BIỂU ĐỒ TRÒN. */
data class DirectionBreakdown(
    val outgoing: Int,
    val incoming: Int,
    val missed: Int
) {
    val total: Int get() = outgoing + incoming + missed

    /** Phần trăm NGUYÊN của [part] trên [total] (0 khi tổng = 0). Dùng cho nhãn "N · P%". */
    fun percentOf(part: Int): Int = if (total == 0) 0 else Math.round(part * 100f / total)
}

/** Buổi trong ngày — chia 24 giờ thành 4 buổi để đọc thói quen gọi. Mốc CỐ ĐỊNH, không phụ thuộc locale. */
enum class DayPart { MORNING, AFTERNOON, EVENING, NIGHT }

/** Thời lượng & CHẤT LƯỢNG kết nối của tập cuộc gọi. */
data class TalkStats(
    val connectedCount: Int,          // số cuộc ĐÃ đàm thoại (có talk time thật)
    val notConnectedCount: Int,       // gọi ĐI/ĐẾN nhưng 0 giây (bận, cúp sớm…) — KHÔNG gồm nhỡ
    val totalDurationSeconds: Long,   // tổng thời lượng đàm thoại
    val avgDurationSeconds: Long,     // trung bình trên số cuộc ĐÃ đàm thoại (0 nếu chưa có cuộc nào)
    val longestDurationSeconds: Long, // cuộc dài nhất (0 nếu chưa có)
    val longestCallMillis: Long?,     // thời điểm cuộc dài nhất (null nếu chưa có)
    val rejected: Int,                // trong nhóm nhỡ: từ chối
    val blocked: Int                  // trong nhóm nhỡ: chặn
)

/** Số cuộc dùng TÍNH NĂNG đặc biệt (đọc từ CallLog FEATURES). */
data class FeatureUsage(
    val videoCount: Int,   // cuộc gọi video
    val volteCount: Int    // cuộc gọi VoLTE (HD)
) {
    val hasAny: Boolean get() = videoCount > 0 || volteCount > 0
}

/** MỐI QUAN HỆ theo thời gian với số này. */
data class RelationshipSpan(
    val firstCallMillis: Long?,   // cuộc SỚM NHẤT
    val lastCallMillis: Long?,    // cuộc GẦN NHẤT
    val distinctDaysCalled: Int,  // số NGÀY LỊCH có ít nhất 1 cuộc
    val spanDays: Int,            // số ngày lịch giữa cuộc đầu và cuộc cuối (0 nếu ≤1 ngày/không có)
    val avgGapSeconds: Long?      // khoảng cách TRUNG BÌNH giữa các cuộc liên tiếp (null nếu < 2 cuộc)
)

/** Số cuộc theo từng SIM (chỉ các cuộc có nhãn SIM — SIM đã tháo có simLabel = null nên bị bỏ qua). */
data class SimCount(val label: String, val count: Int)

/**
 * Kết quả phân tích ĐẦY ĐỦ cho một tập cuộc gọi (đã lọc theo SIM đang xem, nếu có). Các mảng [hourly]/[weekday]
 * là ẢNH CHỤP chỉ-đọc để vẽ; KHÔNG so sánh bằng equals nên dùng mảng thô cho gọn.
 */
class PhoneAnalysisResult(
    val totalCalls: Int,                       // tổng 3 nhóm chính (= direction.total)
    val direction: DirectionBreakdown,
    val talk: TalkStats,
    val connectRate: Float,                    // connectedCount / totalCalls (0..1)
    val answerRate: Float?,                    // incoming / (incoming + missed) — % bắt máy khi họ gọi ĐẾN; null nếu không có
    val missedRate: Float,                     // missed / totalCalls (0..1)
    val hourlyOutgoing: IntArray,              // [24]
    val hourlyIncoming: IntArray,              // [24]
    val hourlyMissed: IntArray,                // [24]
    val peakHour: Int?,                        // 0..23 giờ nhiều cuộc nhất (null nếu 0)
    val peakHourCount: Int,
    val dayParts: List<Pair<DayPart, Int>>,    // theo thứ tự MORNING..NIGHT
    val weekday: IntArray,                      // [7] chỉ số 0 = Thứ 2 … 6 = Chủ nhật
    val peakWeekday: Int?,                      // 0..6 (Mon..Sun) hoặc null
    val daily: List<DayActivity>,              // hoạt động theo NGÀY (mới→cũ) cho BIỂU ĐỒ NGANG
    val features: FeatureUsage,
    val span: RelationshipSpan,
    val perSim: List<SimCount>
) {
    val isEmpty: Boolean get() = totalCalls == 0

    /** Tổng cuộc theo GIỜ (đi + đến + nhỡ) — tiện cho việc chuẩn hoá chiều cao cột 24 giờ. */
    fun hourlyTotal(hour: Int): Int = hourlyOutgoing[hour] + hourlyIncoming[hour] + hourlyMissed[hour]
}

object PhoneAnalysis {

    /** Số NGÀY gần nhất tối đa hiển thị ở biểu đồ ngang "Số cuộc theo ngày". */
    const val MAX_DAILY_BARS = 14

    /** Buổi trong ngày theo giờ 0..23: Sáng 5–10, Chiều 11–17, Tối 18–22, Đêm 23–4. */
    fun dayPartOf(hour: Int): DayPart = when (hour) {
        in 5..10 -> DayPart.MORNING
        in 11..17 -> DayPart.AFTERNOON
        in 18..22 -> DayPart.EVENING
        else -> DayPart.NIGHT
    }

    /**
     * Phân tích [entries] (thứ tự bất kỳ; thường mới→cũ). Tính TẤT CẢ số liệu trong MỘT lượt duyệt; phần
     * "theo ngày" ([DayActivity]) tái dùng [CallStats.dayActivities] để đồng nhất với màn thống kê tổng.
     */
    fun analyze(entries: List<CallEntry>, zone: ZoneId, maxDailyBars: Int = MAX_DAILY_BARS): PhoneAnalysisResult {
        var out = 0; var inc = 0; var miss = 0
        var rejected = 0; var blocked = 0
        var connected = 0; var notConnected = 0
        var totalDur = 0L
        var longest = 0L
        var longestAt: Long? = null
        val hourlyOut = IntArray(24)
        val hourlyIn = IntArray(24)
        val hourlyMiss = IntArray(24)
        val weekday = IntArray(7)
        var video = 0; var volte = 0
        val simCounts = LinkedHashMap<String, Int>()
        val distinctDays = HashSet<LocalDate>()
        val partCounts = IntArray(DayPart.entries.size)
        var minMillis = Long.MAX_VALUE
        var maxMillis = Long.MIN_VALUE

        for (e in entries) {
            val bucket = CallStats.bucketOf(e.type)
            if (bucket == CallBucket.OTHER) continue   // voicemail/unknown: không tính (khớp toàn app)

            when (bucket) {
                CallBucket.OUTGOING -> out++
                CallBucket.INCOMING -> inc++
                CallBucket.MISSED -> miss++
                CallBucket.OTHER -> {}
            }
            when (e.type) {
                CallType.REJECTED -> rejected++
                CallType.BLOCKED -> blocked++
                else -> {}
            }

            if (CallResults.isConnected(e.type, e.durationSeconds)) {
                connected++
                totalDur += e.durationSeconds
                if (e.durationSeconds > longest) {
                    longest = e.durationSeconds
                    longestAt = e.dateMillis
                }
            } else if (bucket != CallBucket.MISSED) {
                notConnected++   // gọi đi / nhận nhưng không có đàm thoại (bận, cúp sớm…)
            }

            if (e.isVideo) video++
            if (e.isVolte) volte++
            e.simLabel?.let { simCounts[it] = (simCounts[it] ?: 0) + 1 }

            val zdt = Instant.ofEpochMilli(e.dateMillis).atZone(zone)
            val hour = zdt.hour
            when (bucket) {
                CallBucket.OUTGOING -> hourlyOut[hour]++
                CallBucket.INCOMING -> hourlyIn[hour]++
                CallBucket.MISSED -> hourlyMiss[hour]++
                CallBucket.OTHER -> {}
            }
            weekday[zdt.dayOfWeek.value - 1]++   // Monday(1) → index 0 … Sunday(7) → index 6
            partCounts[dayPartOf(hour).ordinal]++
            distinctDays.add(zdt.toLocalDate())
            if (e.dateMillis < minMillis) minMillis = e.dateMillis
            if (e.dateMillis > maxMillis) maxMillis = e.dateMillis
        }

        val total = out + inc + miss
        val avg = if (connected > 0) totalDur / connected else 0L

        var peakHour: Int? = null
        var peakHourCount = 0
        for (h in 0..23) {
            val t = hourlyOut[h] + hourlyIn[h] + hourlyMiss[h]
            if (t > peakHourCount) { peakHourCount = t; peakHour = h }
        }
        var peakWeekday: Int? = null
        var peakWeekdayCount = 0
        for (d in 0..6) {
            if (weekday[d] > peakWeekdayCount) { peakWeekdayCount = weekday[d]; peakWeekday = d }
        }

        val hasFirst = minMillis != Long.MAX_VALUE
        val firstMillis = if (hasFirst) minMillis else null
        val lastMillis = if (maxMillis != Long.MIN_VALUE) maxMillis else null
        val spanDays = if (hasFirst && lastMillis != null) {
            val from = Instant.ofEpochMilli(minMillis).atZone(zone).toLocalDate()
            val to = Instant.ofEpochMilli(maxMillis).atZone(zone).toLocalDate()
            ChronoUnit.DAYS.between(from, to).toInt()
        } else 0
        // Khoảng cách TRUNG BÌNH giữa các cuộc = tổng nhịp / (số cuộc − 1). Cần ≥2 cuộc & có trải thời gian.
        val avgGap = if (total >= 2 && maxMillis > minMillis) (maxMillis - minMillis) / 1000 / (total - 1) else null

        val answerRate: Float? = if (inc + miss > 0) inc.toFloat() / (inc + miss) else null

        val dayParts = DayPart.entries.map { it to partCounts[it.ordinal] }
        val perSim = simCounts.entries
            .map { SimCount(it.key, it.value) }
            .sortedWith(compareByDescending<SimCount> { it.count }.thenBy { it.label })

        val daily = CallStats.dayActivities(entries, maxDailyBars, zone)

        return PhoneAnalysisResult(
            totalCalls = total,
            direction = DirectionBreakdown(out, inc, miss),
            talk = TalkStats(
                connectedCount = connected,
                notConnectedCount = notConnected,
                totalDurationSeconds = totalDur,
                avgDurationSeconds = avg,
                longestDurationSeconds = longest,
                longestCallMillis = longestAt,
                rejected = rejected,
                blocked = blocked
            ),
            connectRate = if (total > 0) connected.toFloat() / total else 0f,
            answerRate = answerRate,
            missedRate = if (total > 0) miss.toFloat() / total else 0f,
            hourlyOutgoing = hourlyOut,
            hourlyIncoming = hourlyIn,
            hourlyMissed = hourlyMiss,
            peakHour = peakHour,
            peakHourCount = peakHourCount,
            dayParts = dayParts,
            weekday = weekday,
            peakWeekday = peakWeekday,
            daily = daily,
            features = FeatureUsage(video, volte),
            span = RelationshipSpan(
                firstCallMillis = firstMillis,
                lastCallMillis = lastMillis,
                distinctDaysCalled = distinctDays.size,
                spanDays = spanDays,
                avgGapSeconds = avgGap
            ),
            perSim = perSim
        )
    }
}

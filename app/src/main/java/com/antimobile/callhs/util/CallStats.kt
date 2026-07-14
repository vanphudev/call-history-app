package com.antimobile.callhs.util

import com.antimobile.callhs.data.model.CallEntry
import com.antimobile.callhs.data.model.CallType
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

/**
 * Phân tích & thống kê lịch sử cuộc gọi — LOGIC THUẦN (không phụ thuộc Android/Compose) để dễ kiểm thử.
 * Mọi mốc ngày dùng [ZoneId] truyền vào (thống nhất với TimeFormat = ZoneId.systemDefault()).
 */

/** Khoảng phân tích canh theo LỊCH: HÔM NAY / TUẦN NÀY (thứ 2→CN) / THÁNG NÀY. */
enum class StatsPeriod {
    DAY,
    WEEK,
    MONTH
}

/** Gộp loại cuộc gọi về nhóm thống kê (khớp cách gộp ở màn danh sách: nhỡ gồm cả từ chối/chặn). */
enum class CallBucket { OUTGOING, INCOMING, MISSED, OTHER }

/**
 * Thống kê của MỘT số điện thoại trong một khoảng. CHỈ đếm 3 nhóm chính (gọi đi / nhận / nhỡ);
 * VOICEMAIL/UNKNOWN (CallBucket.OTHER) KHÔNG được tính để mọi con số & biểu đồ luôn khớp nhau.
 */
data class PhoneStat(
    val number: String,
    val cachedName: String?,   // tên DANH BẠ (null nếu chưa lưu); tên khẩn cấp 113/114/115 resolve ở UI
    val photoUri: String?,
    val outgoing: Int,
    val incoming: Int,
    val missed: Int,
    val connectedCount: Int,        // số cuộc ĐÃ đàm thoại
    val totalDurationSeconds: Long  // tổng thời lượng đàm thoại
) {
    val total: Int get() = outgoing + incoming + missed
}

/** Tổng quan một khoảng (totalCalls = gọi đi + nhận + nhỡ). */
data class PeriodOverview(
    val totalCalls: Int,
    val outgoing: Int,
    val incoming: Int,
    val missed: Int,
    val totalDurationSeconds: Long,
    val distinctNumbers: Int
) {
    companion object {
        val EMPTY = PeriodOverview(0, 0, 0, 0, 0L, 0)
    }
}

/** "Quán quân": số gọi đi / nhận / nhỡ NHIỀU NHẤT (null nếu không có cuộc nào thuộc nhóm đó). */
data class TopNumbers(
    val topOutgoing: PhoneStat?,
    val topIncoming: PhoneStat?,
    val topMissed: PhoneStat?
) {
    companion object {
        val EMPTY = TopNumbers(null, null, null)
    }
}

/**
 * Hoạt động của MỘT ngày: tổng + phân bố theo 24 GIỜ (gọi đi / nhận / nhỡ), khung giờ ĐỈNH gọi đi & nhận,
 * và chênh lệch tổng so với NGÀY LỊCH liền trước ([deltaVsPrevDay]).
 * Các mảng giờ có kích thước 24 (chỉ số = giờ 0..23).
 */
data class DayActivity(
    val date: LocalDate,
    val startOfDayMillis: Long,
    val total: Int,
    val outgoing: Int,
    val incoming: Int,
    val missed: Int,
    val hourlyOutgoing: IntArray,
    val hourlyIncoming: IntArray,
    val hourlyMissed: IntArray,
    val peakOutgoingHour: Int?,   // 0..23 hoặc null (không có cuộc gọi đi)
    val peakOutgoingCount: Int,
    val peakIncomingHour: Int?,
    val peakIncomingCount: Int,
    val deltaVsPrevDay: Int
)

object CallStats {

    /** Số ngày tối đa cho phần "Khung giờ hoạt động". */
    const val MAX_ACTIVITY_DAYS = 15

    fun bucketOf(type: CallType): CallBucket = when (type) {
        CallType.OUTGOING -> CallBucket.OUTGOING
        CallType.INCOMING, CallType.ANSWERED_EXTERNALLY -> CallBucket.INCOMING
        CallType.MISSED, CallType.REJECTED, CallType.BLOCKED -> CallBucket.MISSED
        CallType.VOICEMAIL, CallType.UNKNOWN -> CallBucket.OTHER
    }

    /** Nửa khoảng [startMillis, endMillis) của [period] chứa [today] theo [zone]. */
    fun periodRange(period: StatsPeriod, today: LocalDate, zone: ZoneId): LongRange {
        val startDate: LocalDate
        val endDateExclusive: LocalDate
        when (period) {
            StatsPeriod.DAY -> {
                startDate = today
                endDateExclusive = today.plusDays(1)
            }
            StatsPeriod.WEEK -> {
                startDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                endDateExclusive = startDate.plusDays(7)
            }
            StatsPeriod.MONTH -> {
                startDate = today.withDayOfMonth(1)
                endDateExclusive = startDate.plusMonths(1)
            }
        }
        val start = startDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = endDateExclusive.atStartOfDay(zone).toInstant().toEpochMilli()
        // LongRange dùng cận đóng → dừng ở end-1 để giữ nửa khoảng [start, end).
        return start until end
    }

    /** Lọc [entries] về đúng [period]. */
    fun inPeriod(entries: List<CallEntry>, period: StatsPeriod, today: LocalDate, zone: ZoneId): List<CallEntry> {
        val range = periodRange(period, today, zone)
        return entries.filter { it.dateMillis in range }
    }

    /**
     * Thống kê THEO TỪNG SỐ cho danh sách [entries] (giả định ĐÃ lọc theo khoảng, thứ tự mới→cũ);
     * sắp xếp theo TỔNG số cuộc giảm dần, đồng hạng thì số nhiều thời lượng hơn lên trước.
     */
    fun phoneStats(entries: List<CallEntry>): List<PhoneStat> {
        val byNumber = LinkedHashMap<String, MutableList<CallEntry>>()
        for (e in entries) byNumber.getOrPut(e.number) { ArrayList() }.add(e)
        return byNumber.map { (number, calls) ->
            var out = 0; var inc = 0; var miss = 0
            var conn = 0; var dur = 0L
            for (c in calls) {
                when (bucketOf(c.type)) {
                    CallBucket.OUTGOING -> out++
                    CallBucket.INCOMING -> inc++
                    CallBucket.MISSED -> miss++
                    CallBucket.OTHER -> {}   // voicemail/unknown: không tính vào 3 nhóm chính
                }
                if (CallResults.isConnected(c.type, c.durationSeconds)) {
                    conn++
                    dur += c.durationSeconds
                }
            }
            val rep = calls.first() // entries mới→cũ ⇒ đại diện = cuộc mới nhất (tên/ảnh mới nhất)
            PhoneStat(
                number = number,
                cachedName = rep.cachedName?.takeIf { it.isNotBlank() },
                photoUri = rep.photoUri,
                outgoing = out, incoming = inc, missed = miss,
                connectedCount = conn, totalDurationSeconds = dur
            )
        }
            .filter { it.total > 0 } // bỏ số chỉ có cuộc voicemail/unknown (không thuộc 3 nhóm)
            .sortedWith(compareByDescending<PhoneStat> { it.total }.thenByDescending { it.totalDurationSeconds })
    }

    fun overview(stats: List<PhoneStat>): PeriodOverview = PeriodOverview(
        totalCalls = stats.sumOf { it.total },
        outgoing = stats.sumOf { it.outgoing },
        incoming = stats.sumOf { it.incoming },
        missed = stats.sumOf { it.missed },
        totalDurationSeconds = stats.sumOf { it.totalDurationSeconds },
        distinctNumbers = stats.size
    )

    fun topNumbers(stats: List<PhoneStat>): TopNumbers = TopNumbers(
        topOutgoing = stats.filter { it.outgoing > 0 }.maxByOrNull { it.outgoing },
        topIncoming = stats.filter { it.incoming > 0 }.maxByOrNull { it.incoming },
        topMissed = stats.filter { it.missed > 0 }.maxByOrNull { it.missed }
    )

    /**
     * Hoạt động theo NGÀY cho tối đa [maxDays] ngày GẦN NHẤT CÓ cuộc gọi (mới→cũ).
     * [DayActivity.deltaVsPrevDay] so tổng với NGÀY LỊCH liền trước (0 nếu ngày đó không có cuộc gọi).
     */
    fun dayActivities(entries: List<CallEntry>, maxDays: Int, zone: ZoneId): List<DayActivity> {
        if (entries.isEmpty()) return emptyList()

        class Acc {
            var total = 0; var out = 0; var inc = 0; var miss = 0
            val ho = IntArray(24); val hi = IntArray(24); val hm = IntArray(24)
        }

        val byDate = HashMap<LocalDate, Acc>()
        for (e in entries) {
            val bucket = bucketOf(e.type)
            if (bucket == CallBucket.OTHER) continue // voicemail/unknown không đưa vào biểu đồ khung giờ
            val zdt = Instant.ofEpochMilli(e.dateMillis).atZone(zone)
            val date = zdt.toLocalDate()
            val hour = zdt.hour
            val acc = byDate.getOrPut(date) { Acc() }
            acc.total++
            when (bucket) {
                CallBucket.OUTGOING -> { acc.out++; acc.ho[hour]++ }
                CallBucket.INCOMING -> { acc.inc++; acc.hi[hour]++ }
                CallBucket.MISSED -> { acc.miss++; acc.hm[hour]++ }
                CallBucket.OTHER -> {}
            }
        }

        val recentDates = byDate.keys.sortedDescending().take(maxDays)
        return recentDates.map { date ->
            val a = byDate.getValue(date)
            val peakOut = a.ho.withIndex().filter { it.value > 0 }.maxByOrNull { it.value }
            val peakIn = a.hi.withIndex().filter { it.value > 0 }.maxByOrNull { it.value }
            val prevTotal = byDate[date.minusDays(1)]?.total ?: 0
            DayActivity(
                date = date,
                startOfDayMillis = date.atStartOfDay(zone).toInstant().toEpochMilli(),
                total = a.total, outgoing = a.out, incoming = a.inc, missed = a.miss,
                hourlyOutgoing = a.ho, hourlyIncoming = a.hi, hourlyMissed = a.hm,
                peakOutgoingHour = peakOut?.index, peakOutgoingCount = peakOut?.value ?: 0,
                peakIncomingHour = peakIn?.index, peakIncomingCount = peakIn?.value ?: 0,
                deltaVsPrevDay = a.total - prevTotal
            )
        }
    }
}

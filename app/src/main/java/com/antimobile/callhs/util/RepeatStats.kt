package com.antimobile.callhs.util

import com.antimobile.callhs.data.model.CallEntry
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Phân tích "GỌI LẠI & TRÙNG SỐ" trong CỬA SỔ 30 NGÀY GẦN NHẤT — LOGIC THUẦN (không phụ thuộc
 * Android/Compose) để dễ kiểm thử. Trả lời câu hỏi của shipper: trong 30 ngày, mỗi số bị gọi lặp lại
 * bao nhiêu lần, gọi lại vào bao nhiêu ngày khác nhau, mỗi ngày gọi mấy lần, và chu kỳ quay lại ra sao.
 *
 * Cửa sổ CUỐN CHIẾU theo lịch: từ 00:00 của ([windowDays]-1) ngày trước → hết hôm nay (gồm hôm nay) =
 * [windowDays] ngày dương lịch. Chỉ đếm 3 nhóm chính (gọi đi / nhận / nhỡ) — VOICEMAIL/UNKNOWN bị loại
 * để mọi con số khớp với các màn thống kê khác (giống [CallStats.phoneStats]). Mốc ngày dùng [ZoneId] truyền vào.
 */

/** Đếm cuộc gọi tới MỘT số trong MỘT ngày (dùng cho lịch nhiệt + cờ "gọi nhiều lần/ngày"). */
data class RepeatDay(
    val date: LocalDate,
    val startOfDayMillis: Long,
    /** Vị trí ngày trong cửa sổ: 0 = ngày cũ nhất, [RepeatAnalysis.windowDays]-1 = hôm nay. */
    val dayIndex: Int,
    val count: Int,
    val outgoing: Int,
    val incoming: Int,
    val missed: Int
)

/** Hồ sơ "gọi lại" của MỘT số trong cửa sổ. [days] chỉ gồm các ngày CÓ cuộc, sắp theo [RepeatDay.dayIndex] tăng dần. */
data class RepeatNumber(
    val number: String,
    val cachedName: String?,   // tên DANH BẠ (null nếu chưa lưu); tên khẩn cấp 113/114/115 resolve ở UI
    val photoUri: String?,
    val totalCalls: Int,       // = outgoing + incoming + missed trong cửa sổ
    val outgoing: Int,
    val incoming: Int,
    val missed: Int,
    val connectedCount: Int,        // số cuộc ĐÃ đàm thoại
    val totalDurationSeconds: Long, // tổng thời lượng đàm thoại
    val distinctDays: Int,          // số NGÀY khác nhau đã gọi số này
    val firstCallMillis: Long,      // cuộc SỚM NHẤT trong cửa sổ
    val lastCallMillis: Long,       // cuộc MUỘN NHẤT trong cửa sổ
    val maxCallsInADay: Int,        // ngày gọi nhiều nhất gọi mấy lần
    val days: List<RepeatDay>,
    /** Khoảng cách TRUNG BÌNH (ngày) giữa các NGÀY gọi liên tiếp; null nếu chỉ gọi trong 1 ngày. */
    val avgRecallGapDays: Double?,
    /** Số ngày giữa lần đầu & lần cuối (0 nếu chỉ trong 1 ngày). */
    val spanDays: Int
) {
    /** Số cuộc là "gọi lại" của riêng số này (mọi cuộc trừ cuộc đầu tiên). */
    val repeatCalls: Int get() = (totalCalls - 1).coerceAtLeast(0)

    /** Số bị gọi lặp (≥2 lần trong cửa sổ). */
    val isRepeat: Boolean get() = totalCalls >= 2

    /** Số quay lại vào ≥2 NGÀY khác nhau (khách "quay lại"). */
    val isMultiDay: Boolean get() = distinctDays >= 2

    /** Có ít nhất một ngày gọi số này ≥2 lần. */
    val hasSameDayRepeat: Boolean get() = maxCallsInADay >= 2

    /** Số cuộc theo từng ngày của cửa sổ (kích thước [windowDays]) — dùng vẽ lịch nhiệt. */
    fun dailyCounts(windowDays: Int): IntArray {
        val arr = IntArray(windowDays)
        for (d in days) if (d.dayIndex in 0 until windowDays) arr[d.dayIndex] = d.count
        return arr
    }
}

/** Tổng quan cửa sổ. */
data class RepeatOverview(
    val windowDays: Int,
    val windowStartMillis: Long,
    val windowEndMillis: Long,      // mốc cuối (hiện tại) để hiển thị "… đến hôm nay"
    val totalCalls: Int,
    val distinctNumbers: Int,
    val repeatNumberCount: Int,     // số máy bị gọi ≥2 lần
    val repeatCallCount: Int,       // tổng cuộc "gọi lại" = totalCalls − distinctNumbers
    val multiDayNumberCount: Int,   // số máy quay lại ≥2 ngày
    val sameDayRepeatNumberCount: Int, // số máy có ngày gọi ≥2 lần
    val maxRepeatCount: Int,        // số lần gọi cao nhất của một số
    val maxRepeatNumber: String?    // số ứng với [maxRepeatCount]
) {
    companion object {
        fun empty(windowDays: Int, startMillis: Long, endMillis: Long) =
            RepeatOverview(windowDays, startMillis, endMillis, 0, 0, 0, 0, 0, 0, 0, null)
    }
}

/** Nhóm phân bố theo SỐ LẦN gọi trong cửa sổ. Nhãn hiển thị ĐA NGÔN NGỮ resolve ở tầng UI. */
enum class RepeatBucket {
    ONCE,
    TWICE,
    THREE_FIVE,
    SIX_TEN,
    OVER_TEN;

    companion object {
        fun of(totalCalls: Int): RepeatBucket = when {
            totalCalls <= 1 -> ONCE
            totalCalls == 2 -> TWICE
            totalCalls <= 5 -> THREE_FIVE
            totalCalls <= 10 -> SIX_TEN
            else -> OVER_TEN
        }
    }
}

/** Một cột trong biểu đồ phân bố: [numberCount] = số MÁY thuộc [bucket]. */
data class RepeatDistribution(val bucket: RepeatBucket, val numberCount: Int)

/** Chu kỳ quay lại — nhóm các số quay lại theo KHOẢNG CÁCH TRUNG BÌNH giữa các ngày gọi lại. Nhãn resolve ở UI. */
enum class RecallCycle {
    WITHIN_WEEK,      // gap ≤ 7 ngày
    ONE_TWO_WEEKS,    // 7 < gap ≤ 14
    TWO_THREE_WEEKS,  // 14 < gap ≤ 21
    OVER_THREE_WEEKS; // gap > 21 (mở, tối đa ~29 ngày trong cửa sổ 30 ngày)

    companion object {
        fun of(avgGapDays: Double): RecallCycle = when {
            avgGapDays <= 7.0 -> WITHIN_WEEK
            avgGapDays <= 14.0 -> ONE_TWO_WEEKS
            avgGapDays <= 21.0 -> TWO_THREE_WEEKS
            else -> OVER_THREE_WEEKS
        }
    }
}

/** Một cột trong biểu đồ chu kỳ quay lại: [numberCount] = số MÁY thuộc [cycle]. */
data class RecallCycleBucket(val cycle: RecallCycle, val numberCount: Int)

/** Bộ lọc danh sách số. Nhãn hiển thị resolve ở tầng UI. */
enum class RepeatFilter {
    REPEAT,
    MULTI_DAY,
    ALL
}

/** Cách sắp xếp danh sách số. Nhãn hiển thị resolve ở tầng UI. */
enum class RepeatSort {
    TOTAL,
    DAYS,
    RECENT
}

/** Kết quả phân tích trọn cửa sổ. [numbers] = TẤT CẢ số trong cửa sổ (đã sắp theo tổng cuộc); UI tự lọc/sắp lại. */
data class RepeatAnalysis(
    val windowDays: Int,
    val numbers: List<RepeatNumber>,
    val overview: RepeatOverview,
    val distribution: List<RepeatDistribution>,
    val recallCycles: List<RecallCycleBucket>
) {
    companion object {
        fun empty(windowDays: Int, startMillis: Long, endMillis: Long) = RepeatAnalysis(
            windowDays = windowDays,
            numbers = emptyList(),
            overview = RepeatOverview.empty(windowDays, startMillis, endMillis),
            distribution = RepeatBucket.entries.map { RepeatDistribution(it, 0) },
            recallCycles = RecallCycle.entries.map { RecallCycleBucket(it, 0) }
        )
    }
}

object RepeatStats {

    /** Số ngày cửa sổ phân tích "gọi lại". */
    const val WINDOW_DAYS = 30

    /**
     * Phân tích [entries] (thứ tự mới→cũ) trong cửa sổ [windowDays] ngày kết thúc ở [today].
     * [nowMillis] là mốc "hiện tại" để hiển thị cận cuối (mặc định = cuối ngày [today]).
     */
    fun analyze(
        entries: List<CallEntry>,
        today: LocalDate,
        zone: ZoneId,
        windowDays: Int = WINDOW_DAYS,
        nowMillis: Long = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
    ): RepeatAnalysis {
        val windowStartDate = today.minusDays((windowDays - 1).toLong())
        val startMillis = windowStartDate.atStartOfDay(zone).toInstant().toEpochMilli()
        val endExclusive = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        // Gộp theo KHOÁ CHUẨN [PhoneKey] → một số bị gọi lại dù nhật ký lưu lẫn "+84…"/"0…" vẫn tính là MỘT số
        // (đúng bản chất "gọi lại/trùng số"); đại diện lấy số thật của cuộc mới nhất trong buildNumber.
        val byNumber = LinkedHashMap<String, MutableList<CallEntry>>()
        for (e in entries) {
            if (e.dateMillis < startMillis || e.dateMillis >= endExclusive) continue
            if (CallStats.bucketOf(e.type) == CallBucket.OTHER) continue
            byNumber.getOrPut(PhoneKey.of(e.number).ifEmpty { e.number }) { ArrayList() }.add(e)
        }

        val numbers = byNumber
            .mapNotNull { (_, calls) -> buildNumber(calls, windowStartDate, zone) }
            .sortedWith(
                compareByDescending<RepeatNumber> { it.totalCalls }
                    .thenByDescending { it.distinctDays }
                    .thenByDescending { it.lastCallMillis }
            )

        val overview = overview(numbers, windowDays, startMillis, nowMillis)
        return RepeatAnalysis(windowDays, numbers, overview, distribution(numbers), recallCycles(numbers))
    }

    private fun buildNumber(
        calls: List<CallEntry>,
        windowStartDate: LocalDate,
        zone: ZoneId
    ): RepeatNumber? {
        class Acc(val date: LocalDate, val startMillis: Long, val dayIndex: Int) {
            var count = 0; var out = 0; var inc = 0; var miss = 0
        }

        val byDate = HashMap<LocalDate, Acc>()
        var out = 0; var inc = 0; var miss = 0
        var conn = 0; var dur = 0L
        var firstMillis = Long.MAX_VALUE
        var lastMillis = Long.MIN_VALUE

        for (c in calls) {
            val bucket = CallStats.bucketOf(c.type)
            if (bucket == CallBucket.OTHER) continue
            val zdt = Instant.ofEpochMilli(c.dateMillis).atZone(zone)
            val date = zdt.toLocalDate()
            val acc = byDate.getOrPut(date) {
                Acc(
                    date = date,
                    startMillis = date.atStartOfDay(zone).toInstant().toEpochMilli(),
                    dayIndex = ChronoUnit.DAYS.between(windowStartDate, date).toInt()
                )
            }
            acc.count++
            when (bucket) {
                CallBucket.OUTGOING -> { out++; acc.out++ }
                CallBucket.INCOMING -> { inc++; acc.inc++ }
                CallBucket.MISSED -> { miss++; acc.miss++ }
                CallBucket.OTHER -> {}
            }
            if (CallResults.isConnected(c.type, c.durationSeconds)) {
                conn++
                dur += c.durationSeconds
            }
            if (c.dateMillis < firstMillis) firstMillis = c.dateMillis
            if (c.dateMillis > lastMillis) lastMillis = c.dateMillis
        }

        val total = out + inc + miss
        if (total == 0) return null // chỉ có voicemail/unknown → không tính

        val days = byDate.values
            .map { RepeatDay(it.date, it.startMillis, it.dayIndex, it.count, it.out, it.inc, it.miss) }
            .sortedBy { it.dayIndex }
        val distinctDays = days.size
        val span = ChronoUnit.DAYS.between(days.first().date, days.last().date).toInt()
        val avgGap = if (distinctDays >= 2) span.toDouble() / (distinctDays - 1) else null
        val rep = calls.first() // entries mới→cũ ⇒ đại diện = cuộc mới nhất (tên/ảnh/số mới nhất)

        return RepeatNumber(
            number = rep.number,
            cachedName = rep.cachedName?.takeIf { it.isNotBlank() },
            photoUri = rep.photoUri,
            totalCalls = total,
            outgoing = out, incoming = inc, missed = miss,
            connectedCount = conn, totalDurationSeconds = dur,
            distinctDays = distinctDays,
            firstCallMillis = firstMillis,
            lastCallMillis = lastMillis,
            maxCallsInADay = days.maxOf { it.count },
            days = days,
            avgRecallGapDays = avgGap,
            spanDays = span
        )
    }

    private fun overview(
        numbers: List<RepeatNumber>,
        windowDays: Int,
        startMillis: Long,
        endMillis: Long
    ): RepeatOverview {
        val totalCalls = numbers.sumOf { it.totalCalls }
        val distinct = numbers.size
        val top = numbers.maxByOrNull { it.totalCalls } // numbers đã sắp nên = firstOrNull, giữ maxByOrNull cho an toàn
        return RepeatOverview(
            windowDays = windowDays,
            windowStartMillis = startMillis,
            windowEndMillis = endMillis,
            totalCalls = totalCalls,
            distinctNumbers = distinct,
            repeatNumberCount = numbers.count { it.isRepeat },
            repeatCallCount = (totalCalls - distinct).coerceAtLeast(0),
            multiDayNumberCount = numbers.count { it.isMultiDay },
            sameDayRepeatNumberCount = numbers.count { it.hasSameDayRepeat },
            maxRepeatCount = top?.totalCalls ?: 0,
            maxRepeatNumber = top?.number
        )
    }

    /** Phân bố theo số lần gọi — LUÔN trả đủ 5 nhóm theo thứ tự enum (nhóm rỗng = 0). */
    fun distribution(numbers: List<RepeatNumber>): List<RepeatDistribution> {
        val counts = HashMap<RepeatBucket, Int>()
        for (n in numbers) counts.merge(RepeatBucket.of(n.totalCalls), 1, Int::plus)
        return RepeatBucket.entries.map { RepeatDistribution(it, counts[it] ?: 0) }
    }

    /** Chu kỳ quay lại — chỉ tính số quay lại ≥2 ngày; LUÔN trả đủ 4 nhóm theo thứ tự enum. */
    fun recallCycles(numbers: List<RepeatNumber>): List<RecallCycleBucket> {
        val counts = HashMap<RecallCycle, Int>()
        for (n in numbers) {
            val gap = n.avgRecallGapDays ?: continue
            counts.merge(RecallCycle.of(gap), 1, Int::plus)
        }
        return RecallCycle.entries.map { RecallCycleBucket(it, counts[it] ?: 0) }
    }

    /** Lọc danh sách theo [filter]. */
    fun applyFilter(numbers: List<RepeatNumber>, filter: RepeatFilter): List<RepeatNumber> = when (filter) {
        RepeatFilter.REPEAT -> numbers.filter { it.isRepeat }
        RepeatFilter.MULTI_DAY -> numbers.filter { it.isMultiDay }
        RepeatFilter.ALL -> numbers
    }

    /** Sắp xếp danh sách theo [sort] (đồng hạng: tổng cuộc giảm dần → gần đây hơn trước). */
    fun applySort(numbers: List<RepeatNumber>, sort: RepeatSort): List<RepeatNumber> {
        val comparator = when (sort) {
            RepeatSort.TOTAL -> compareByDescending<RepeatNumber> { it.totalCalls }
                .thenByDescending { it.distinctDays }
                .thenByDescending { it.lastCallMillis }
            RepeatSort.DAYS -> compareByDescending<RepeatNumber> { it.distinctDays }
                .thenByDescending { it.totalCalls }
                .thenByDescending { it.lastCallMillis }
            RepeatSort.RECENT -> compareByDescending<RepeatNumber> { it.lastCallMillis }
                .thenByDescending { it.totalCalls }
        }
        return numbers.sortedWith(comparator)
    }
}

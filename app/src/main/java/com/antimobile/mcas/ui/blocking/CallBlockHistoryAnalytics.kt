package com.antimobile.mcas.ui.blocking

import com.antimobile.mcas.data.blocking.BlockedCallHistory
import com.antimobile.mcas.util.DayPart
import com.antimobile.mcas.util.PhoneAnalysis
import com.antimobile.mcas.util.StatsPeriod
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/** Khoảng thống kê lịch sử chặn, tính theo ngày lịch tại múi giờ của thiết bị. */
internal data class BlockHistoryDateRange(
    val start: LocalDate,
    val endInclusive: LocalDate,
) {
    init {
        require(!endInclusive.isBefore(start))
    }

    val dayCount: Long get() = ChronoUnit.DAYS.between(start, endInclusive) + 1L
}

internal data class BlockHistoryDayBucket(
    val date: LocalDate,
    val count: Int,
)

internal data class BlockHistoryNumberCount(
    val phoneKey: String,
    val rawNumber: String,
    val count: Int,
)

/** Kết quả tổng hợp thuần dữ liệu để UI báo cáo và danh sách chi tiết luôn dùng cùng một nguồn. */
internal data class BlockHistoryAnalytics(
    val rows: List<BlockedCallHistory>,
    val previousPeriodCount: Int,
    val distinctNumbers: Int,
    val daily: List<BlockHistoryDayBucket>,
    val hourly: IntArray,
    val dayParts: List<Pair<DayPart, Int>>,
    val topNumbers: List<BlockHistoryNumberCount>,
) {
    val total: Int get() = rows.size
    val deltaVsPrevious: Int get() = total - previousPeriodCount
    val peakHour: Int? get() = hourly.indices.maxByOrNull { hourly[it] }?.takeIf { hourly[it] > 0 }
    val peakDay: BlockHistoryDayBucket? get() = daily.maxByOrNull { it.count }?.takeIf { it.count > 0 }
}

/**
 * Ngày = ngày người dùng chọn; tuần/tháng = đủ khoảng lịch hiện tại. Các ngày chưa có dữ liệu vẫn được
 * tạo bucket 0 để trục biểu đồ luôn đủ Thứ Hai–Chủ Nhật hoặc Ngày 01–ngày cuối tháng.
 */
internal fun blockHistoryRange(
    period: StatsPeriod,
    selectedDay: LocalDate,
    today: LocalDate,
): BlockHistoryDateRange = when (period) {
    StatsPeriod.DAY -> BlockHistoryDateRange(selectedDay, selectedDay)
    StatsPeriod.WEEK -> BlockHistoryDateRange(
        start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)),
        endInclusive = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)),
    )
    StatsPeriod.MONTH -> BlockHistoryDateRange(
        start = today.withDayOfMonth(1),
        endInclusive = today.withDayOfMonth(today.lengthOfMonth()),
    )
}

internal fun analyzeBlockHistory(
    allRows: List<BlockedCallHistory>,
    range: BlockHistoryDateRange,
    zone: ZoneId,
): BlockHistoryAnalytics {
    fun dateOf(row: BlockedCallHistory): LocalDate =
        Instant.ofEpochMilli(row.blockedAt).atZone(zone).toLocalDate()

    val rows = allRows.filter { dateOf(it) in range.start..range.endInclusive }
    val previousEnd = range.start.minusDays(1)
    val previousStart = previousEnd.minusDays(range.dayCount - 1L)
    val previousCount = allRows.count { dateOf(it) in previousStart..previousEnd }
    val countsByDay = rows.groupingBy(::dateOf).eachCount()
    val daily = generateSequence(range.start) { current ->
        current.plusDays(1).takeIf { !it.isAfter(range.endInclusive) }
    }.map { date -> BlockHistoryDayBucket(date, countsByDay[date] ?: 0) }.toList()
    val hourly = IntArray(24)
    val dayPartCounts = IntArray(DayPart.entries.size)
    rows.forEach { row ->
        val hour = Instant.ofEpochMilli(row.blockedAt).atZone(zone).hour
        hourly[hour]++
        dayPartCounts[PhoneAnalysis.dayPartOf(hour).ordinal]++
    }
    val topNumbers = rows
        .groupBy { it.phoneKey.ifBlank { it.rawNumber } }
        .map { (phoneKey, calls) ->
            BlockHistoryNumberCount(
                phoneKey = phoneKey,
                rawNumber = calls.first().rawNumber,
                count = calls.size,
            )
        }
        .sortedWith(compareByDescending<BlockHistoryNumberCount> { it.count }.thenBy { it.phoneKey })

    return BlockHistoryAnalytics(
        rows = rows,
        previousPeriodCount = previousCount,
        distinctNumbers = topNumbers.size,
        daily = daily,
        hourly = hourly,
        dayParts = DayPart.entries.map { part -> part to dayPartCounts[part.ordinal] },
        topNumbers = topNumbers,
    )
}

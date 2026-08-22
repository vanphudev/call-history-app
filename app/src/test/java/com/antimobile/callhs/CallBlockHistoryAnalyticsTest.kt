package com.antimobile.callhs

import com.antimobile.callhs.data.blocking.BlockedCallHistory
import com.antimobile.callhs.ui.blocking.BlockHistoryDateRange
import com.antimobile.callhs.ui.blocking.analyzeBlockHistory
import com.antimobile.callhs.ui.blocking.blockHistoryRange
import com.antimobile.callhs.util.DayPart
import com.antimobile.callhs.util.StatsPeriod
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class CallBlockHistoryAnalyticsTest {
    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")

    @Test
    fun dayRangeUsesSelectedDayWhileWeekAndMonthIncludeEveryCalendarBucket() {
        val today = LocalDate.of(2026, 8, 18)

        assertEquals(
            BlockHistoryDateRange(LocalDate.of(2026, 8, 4), LocalDate.of(2026, 8, 4)),
            blockHistoryRange(StatsPeriod.DAY, LocalDate.of(2026, 8, 4), today),
        )
        assertEquals(
            BlockHistoryDateRange(LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 23)),
            blockHistoryRange(StatsPeriod.WEEK, today, today),
        )
        assertEquals(
            BlockHistoryDateRange(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)),
            blockHistoryRange(StatsPeriod.MONTH, today, today),
        )
    }

    @Test
    fun analyticsBuildsCompleteDaysHourlyTopNumbersAndComparablePreviousPeriod() {
        val range = BlockHistoryDateRange(
            start = LocalDate.of(2026, 8, 17),
            endInclusive = LocalDate.of(2026, 8, 18),
        )
        val rows = listOf(
            row(1, "0901", LocalDate.of(2026, 8, 18), 10),
            row(2, "0901", LocalDate.of(2026, 8, 18), 10),
            row(3, "0902", LocalDate.of(2026, 8, 17), 22),
            row(4, "0903", LocalDate.of(2026, 8, 16), 9),
        )

        val result = analyzeBlockHistory(rows, range, zone)

        assertEquals(3, result.total)
        assertEquals(1, result.previousPeriodCount)
        assertEquals(2, result.distinctNumbers)
        assertEquals(listOf(1, 2), result.daily.map { it.count })
        assertEquals(2, result.hourly[10])
        assertEquals(10, result.peakHour)
        assertEquals(2, result.dayParts.first { it.first == DayPart.MORNING }.second)
        assertEquals(1, result.dayParts.first { it.first == DayPart.EVENING }.second)
        assertEquals(0, result.dayParts.first { it.first == DayPart.NIGHT }.second)
        assertEquals("0901", result.topNumbers.first().rawNumber)
        assertEquals(2, result.topNumbers.first().count)
    }

    private fun row(id: Long, number: String, date: LocalDate, hour: Int) = BlockedCallHistory(
        id = id,
        rawNumber = number,
        phoneKey = number,
        blockedAt = date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli(),
        ruleType = "any",
        ruleValue = "",
        consecutiveUnanswered = 0,
        blockedCountForNumber = 1,
    )
}

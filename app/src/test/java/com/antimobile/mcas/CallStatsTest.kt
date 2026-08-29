package com.antimobile.mcas

import com.antimobile.mcas.data.model.CallEntry
import com.antimobile.mcas.data.model.CallType
import com.antimobile.mcas.util.CallBucket
import com.antimobile.mcas.util.CallStats
import com.antimobile.mcas.util.StatsPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Kiểm thử LOGIC thuần cho phân tích & thống kê lịch sử cuộc gọi. */
class CallStatsTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private var seq = 0L

    private fun entry(
        number: String,
        type: CallType,
        date: LocalDate,
        hour: Int = 10,
        dur: Long = 0L
    ): CallEntry {
        val millis = date.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
        return CallEntry(
            id = seq++, number = number, cachedName = null, type = type, dateMillis = millis,
            durationSeconds = dur, photoUri = null, geocodedLocation = null, numberType = 0, numberTypeCustom = null,
            phoneAccountId = null, simLabel = null, carrier = null, isVideo = false, isVolte = false
        )
    }

    // ---- Gộp loại cuộc gọi ----

    @Test fun bucketOf_mapsTypesToBuckets() {
        assertEquals(CallBucket.OUTGOING, CallStats.bucketOf(CallType.OUTGOING))
        assertEquals(CallBucket.INCOMING, CallStats.bucketOf(CallType.INCOMING))
        assertEquals(CallBucket.INCOMING, CallStats.bucketOf(CallType.ANSWERED_EXTERNALLY))
        assertEquals(CallBucket.MISSED, CallStats.bucketOf(CallType.MISSED))
        assertEquals(CallBucket.MISSED, CallStats.bucketOf(CallType.REJECTED))
        assertEquals(CallBucket.MISSED, CallStats.bucketOf(CallType.BLOCKED))
        assertEquals(CallBucket.OTHER, CallStats.bucketOf(CallType.VOICEMAIL))
        assertEquals(CallBucket.OTHER, CallStats.bucketOf(CallType.UNKNOWN))
    }

    // ---- Khoảng thời gian (canh lịch) ----

    @Test fun periodRange_day_isSingleCalendarDay() {
        val today = LocalDate.of(2026, 7, 7) // thứ Ba
        val r = CallStats.periodRange(StatsPeriod.DAY, today, zone)
        assertEquals(today.atStartOfDay(zone).toInstant().toEpochMilli(), r.first)
        assertEquals(today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1, r.last)
    }

    @Test fun periodRange_week_isMondayToSunday() {
        val today = LocalDate.of(2026, 7, 7) // thứ Ba → tuần bắt đầu thứ 2 06/07
        val r = CallStats.periodRange(StatsPeriod.WEEK, today, zone)
        val monday = LocalDate.of(2026, 7, 6)
        assertEquals(monday.atStartOfDay(zone).toInstant().toEpochMilli(), r.first)
        assertEquals(monday.plusDays(7).atStartOfDay(zone).toInstant().toEpochMilli() - 1, r.last)
    }

    @Test fun periodRange_month_isCalendarMonth() {
        val today = LocalDate.of(2026, 7, 7)
        val r = CallStats.periodRange(StatsPeriod.MONTH, today, zone)
        assertEquals(LocalDate.of(2026, 7, 1).atStartOfDay(zone).toInstant().toEpochMilli(), r.first)
        assertEquals(LocalDate.of(2026, 8, 1).atStartOfDay(zone).toInstant().toEpochMilli() - 1, r.last)
    }

    @Test fun inPeriod_filtersByCalendarWindow() {
        val today = LocalDate.of(2026, 7, 7)
        val entries = listOf(
            entry("1", CallType.OUTGOING, LocalDate.of(2026, 7, 7)), // hôm nay
            entry("1", CallType.OUTGOING, LocalDate.of(2026, 7, 6)), // trong tuần (thứ 2)
            entry("1", CallType.OUTGOING, LocalDate.of(2026, 7, 5)), // CN tuần trước → ngoài tuần, trong tháng
            entry("1", CallType.OUTGOING, LocalDate.of(2026, 6, 30)) // tháng trước
        )
        assertEquals(1, CallStats.inPeriod(entries, StatsPeriod.DAY, today, zone).size)
        assertEquals(2, CallStats.inPeriod(entries, StatsPeriod.WEEK, today, zone).size)
        assertEquals(3, CallStats.inPeriod(entries, StatsPeriod.MONTH, today, zone).size)
    }

    // ---- Thống kê theo số ----

    @Test fun phoneStats_countsSortsAndSumsDuration() {
        val d = LocalDate.of(2026, 7, 7)
        val entries = listOf(
            entry("111", CallType.OUTGOING, d, dur = 60),
            entry("111", CallType.OUTGOING, d, dur = 30),
            entry("111", CallType.INCOMING, d, dur = 20),
            entry("111", CallType.MISSED, d),
            entry("222", CallType.MISSED, d),
            entry("222", CallType.MISSED, d)
        )
        val stats = CallStats.phoneStats(entries)
        assertEquals(2, stats.size)
        // Sắp xếp theo TỔNG giảm dần → 111 (4 cuộc) trước 222 (2 cuộc).
        val a = stats[0]
        assertEquals("111", a.number)
        assertEquals(4, a.total)
        assertEquals(2, a.outgoing)
        assertEquals(1, a.incoming)
        assertEquals(1, a.missed)
        assertEquals(3, a.connectedCount)            // 2 gọi đi + 1 nhận (đều có thời lượng)
        assertEquals(110L, a.totalDurationSeconds)   // 60 + 30 + 20
        assertEquals("222", stats[1].number)
        assertEquals(2, stats[1].missed)
    }

    @Test fun topNumbers_picksLeaderPerCategory() {
        val d = LocalDate.of(2026, 7, 7)
        val entries = listOf(
            entry("111", CallType.OUTGOING, d),
            entry("111", CallType.OUTGOING, d),
            entry("111", CallType.INCOMING, d),
            entry("111", CallType.MISSED, d),
            entry("222", CallType.MISSED, d),
            entry("222", CallType.MISSED, d)
        )
        val top = CallStats.topNumbers(CallStats.phoneStats(entries))
        assertEquals("111", top.topOutgoing?.number)
        assertEquals("111", top.topIncoming?.number)
        assertEquals("222", top.topMissed?.number) // 222 nhỡ 2 > 111 nhỡ 1
    }

    @Test fun topNumbers_nullWhenNoneInCategory() {
        val d = LocalDate.of(2026, 7, 7)
        val entries = listOf(entry("111", CallType.OUTGOING, d))
        val top = CallStats.topNumbers(CallStats.phoneStats(entries))
        assertEquals("111", top.topOutgoing?.number)
        assertNull(top.topIncoming)
        assertNull(top.topMissed)
    }

    // ---- Hoạt động theo ngày ----

    @Test fun dayActivities_peaksDeltaAndHourly() {
        val entries = listOf(
            entry("1", CallType.OUTGOING, LocalDate.of(2026, 7, 7), hour = 20),
            entry("1", CallType.OUTGOING, LocalDate.of(2026, 7, 7), hour = 20),
            entry("1", CallType.INCOMING, LocalDate.of(2026, 7, 7), hour = 9),
            entry("1", CallType.OUTGOING, LocalDate.of(2026, 7, 6), hour = 8)
        )
        val days = CallStats.dayActivities(entries, 15, zone)
        assertEquals(2, days.size)
        val today = days[0] // mới nhất trước
        assertEquals(LocalDate.of(2026, 7, 7), today.date)
        assertEquals(3, today.total)
        assertEquals(20, today.peakOutgoingHour)
        assertEquals(2, today.peakOutgoingCount)
        assertEquals(9, today.peakIncomingHour)
        assertEquals(1, today.peakIncomingCount)
        assertEquals(2, today.hourlyOutgoing[20])
        assertEquals(1, today.hourlyIncoming[9])
        assertEquals(2, today.deltaVsPrevDay) // 3 (07/07) − 1 (06/07)
        val prev = days[1]
        assertEquals(1, prev.deltaVsPrevDay)   // 1 − 0 (05/07 không có)
    }

    @Test fun dayActivities_cappedToMaxDays() {
        val base = LocalDate.of(2026, 7, 20)
        val entries = (0 until 20).map { entry("1", CallType.OUTGOING, base.minusDays(it.toLong())) }
        val days = CallStats.dayActivities(entries, CallStats.MAX_ACTIVITY_DAYS, zone)
        assertEquals(15, days.size)
        // Giữ 15 ngày MỚI nhất → ngày đầu là base.
        assertEquals(base, days.first().date)
        assertTrue(days.first().date.isAfter(days.last().date))
    }
}

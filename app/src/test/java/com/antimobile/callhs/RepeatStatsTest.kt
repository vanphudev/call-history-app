package com.antimobile.callhs

import com.antimobile.callhs.data.model.CallEntry
import com.antimobile.callhs.data.model.CallType
import com.antimobile.callhs.util.RecallCycle
import com.antimobile.callhs.util.RepeatBucket
import com.antimobile.callhs.util.RepeatFilter
import com.antimobile.callhs.util.RepeatSort
import com.antimobile.callhs.util.RepeatStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

/** Kiểm thử LOGIC thuần cho phân tích "gọi lại & trùng số" (cửa sổ 30 ngày). */
class RepeatStatsTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val today: LocalDate = LocalDate.of(2026, 7, 10)
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

    /** Bộ dữ liệu chuẩn (mới→cũ) dùng cho phần lớn test. */
    private fun sample(): List<CallEntry> = listOf(
        // 111: gọi 4 lần trong 3 ngày (2 lần cùng ngày 10/07), có 1 voicemail bị loại
        entry("111", CallType.OUTGOING, LocalDate.of(2026, 7, 10), hour = 11, dur = 30),
        entry("111", CallType.VOICEMAIL, LocalDate.of(2026, 7, 10), hour = 10), // OTHER → loại
        entry("111", CallType.OUTGOING, LocalDate.of(2026, 7, 10), hour = 10, dur = 20),
        entry("111", CallType.MISSED, LocalDate.of(2026, 7, 8)),
        entry("111", CallType.INCOMING, LocalDate.of(2026, 7, 1), dur = 15),
        // 222: gọi 1 lần
        entry("222", CallType.OUTGOING, LocalDate.of(2026, 7, 9)),
        // 333: gọi 2 lần, cách nhau 28 ngày
        entry("333", CallType.OUTGOING, LocalDate.of(2026, 7, 10), hour = 9),
        entry("333", CallType.OUTGOING, LocalDate.of(2026, 6, 12)),
        // NGOÀI cửa sổ (trước 11/06) → bị loại hoàn toàn
        entry("444", CallType.OUTGOING, LocalDate.of(2026, 6, 1)),
        entry("111", CallType.OUTGOING, LocalDate.of(2026, 5, 1))
    )

    @Test fun analyze_excludesOutOfWindowAndVoicemail() {
        val a = RepeatStats.analyze(sample(), today, zone)
        // 444 ngoài cửa sổ → không có; chỉ 111/222/333
        assertEquals(setOf("111", "222", "333"), a.numbers.map { it.number }.toSet())
        val n111 = a.numbers.first { it.number == "111" }
        assertEquals(4, n111.totalCalls) // voicemail + cuộc 01/05 KHÔNG tính
        assertEquals(2, n111.outgoing)   // 2 cuộc gọi đi trong cửa sổ (11/07 & …)
        assertEquals(1, n111.incoming)
        assertEquals(1, n111.missed)
    }

    @Test fun analyze_perNumberDaysSpanAndSameDay() {
        val a = RepeatStats.analyze(sample(), today, zone)
        val n111 = a.numbers.first { it.number == "111" }
        assertEquals(3, n111.distinctDays)
        assertEquals(2, n111.maxCallsInADay)   // 10/07 gọi 2 lần
        assertTrue(n111.hasSameDayRepeat)
        assertTrue(n111.isMultiDay)
        assertEquals(9, n111.spanDays)         // 01/07 → 10/07
        assertEquals(4.5, n111.avgRecallGapDays!!, 0.0001) // 9 / (3-1)

        val n333 = a.numbers.first { it.number == "333" }
        assertEquals(2, n333.distinctDays)
        assertEquals(28, n333.spanDays)        // 12/06 → 10/07
        assertEquals(28.0, n333.avgRecallGapDays!!, 0.0001)

        val n222 = a.numbers.first { it.number == "222" }
        assertEquals(1, n222.distinctDays)
        assertNull(n222.avgRecallGapDays)
        assertEquals(0, n222.spanDays)
    }

    @Test fun analyze_overviewAggregates() {
        val o = RepeatStats.analyze(sample(), today, zone).overview
        assertEquals(7, o.totalCalls)          // 4 + 1 + 2
        assertEquals(3, o.distinctNumbers)
        assertEquals(2, o.repeatNumberCount)   // 111, 333
        assertEquals(4, o.repeatCallCount)     // 7 − 3
        assertEquals(2, o.multiDayNumberCount) // 111, 333
        assertEquals(1, o.sameDayRepeatNumberCount) // 111
        assertEquals(4, o.maxRepeatCount)
        assertEquals("111", o.maxRepeatNumber)
    }

    @Test fun distribution_bucketsByCallCount() {
        val a = RepeatStats.analyze(sample(), today, zone)
        val byBucket = a.distribution.associate { it.bucket to it.numberCount }
        assertEquals(1, byBucket[RepeatBucket.ONCE])       // 222
        assertEquals(1, byBucket[RepeatBucket.TWICE])      // 333
        assertEquals(1, byBucket[RepeatBucket.THREE_FIVE]) // 111 (4)
        assertEquals(0, byBucket[RepeatBucket.SIX_TEN])
        assertEquals(0, byBucket[RepeatBucket.OVER_TEN])
        assertEquals(5, a.distribution.size) // luôn đủ 5 nhóm
    }

    @Test fun recallCycles_bucketsByAvgGap() {
        val a = RepeatStats.analyze(sample(), today, zone)
        val byCycle = a.recallCycles.associate { it.cycle to it.numberCount }
        assertEquals(1, byCycle[RecallCycle.WITHIN_WEEK])       // 111 (4.5 ngày)
        assertEquals(0, byCycle[RecallCycle.ONE_TWO_WEEKS])
        assertEquals(0, byCycle[RecallCycle.TWO_THREE_WEEKS])
        assertEquals(1, byCycle[RecallCycle.OVER_THREE_WEEKS])  // 333 (28 ngày)
        assertEquals(4, a.recallCycles.size)
    }

    @Test fun filter_and_sort() {
        val nums = RepeatStats.analyze(sample(), today, zone).numbers
        assertEquals(listOf("111", "333"), RepeatStats.applyFilter(nums, RepeatFilter.REPEAT).map { it.number })
        assertEquals(listOf("111", "333"), RepeatStats.applyFilter(nums, RepeatFilter.MULTI_DAY).map { it.number })
        assertEquals(3, RepeatStats.applyFilter(nums, RepeatFilter.ALL).size)

        // TOTAL: 111(4) > 333(2) > 222(1)
        assertEquals(listOf("111", "333", "222"), RepeatStats.applySort(nums, RepeatSort.TOTAL).map { it.number })
        // RECENT: 111 (10/07 11h) > 333 (10/07 9h) > 222 (09/07)
        assertEquals(listOf("111", "333", "222"), RepeatStats.applySort(nums, RepeatSort.RECENT).map { it.number })
    }

    @Test fun analyze_emptyWhenNoCallsInWindow() {
        val old = listOf(entry("111", CallType.OUTGOING, LocalDate.of(2026, 1, 1)))
        val a = RepeatStats.analyze(old, today, zone)
        assertTrue(a.numbers.isEmpty())
        assertEquals(0, a.overview.totalCalls)
        assertEquals(5, a.distribution.size)
        assertEquals(4, a.recallCycles.size)
    }
}

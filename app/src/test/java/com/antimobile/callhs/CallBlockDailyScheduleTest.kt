package com.antimobile.callhs

import com.antimobile.callhs.data.blocking.CallBlockDailySchedule
import com.antimobile.callhs.data.blocking.CallBlockDailyScheduleCodec
import com.antimobile.callhs.data.blocking.CallBlockScheduleAction
import com.antimobile.callhs.data.blocking.CallBlockSchedulePreset
import com.antimobile.callhs.data.blocking.CallBlockScheduleUpdate
import com.antimobile.callhs.data.blocking.CallBlockTimeWindow
import com.antimobile.callhs.data.blocking.ALL_WEEKDAYS_MASK
import com.antimobile.callhs.data.blocking.MAX_CALL_BLOCK_TIME_WINDOWS
import com.antimobile.callhs.data.blocking.weekdayBit
import java.time.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CallBlockDailyScheduleTest {

    @Test
    fun halfOpenBoundariesAndOvernightWindowsAreEvaluatedCorrectly() {
        val night = window("night", CallBlockScheduleAction.PAUSE, 22 * 60, 6 * 60)

        assertTrue(night.contains(22 * 60))
        assertTrue(night.contains(23 * 60 + 59))
        assertTrue(night.contains(0))
        assertTrue(night.contains(5 * 60 + 59))
        assertFalse(night.contains(6 * 60))
        assertFalse(night.contains(12 * 60))
    }

    @Test
    fun touchingWindowsAreAllowedButEveryKindOfOverlapIsRejected() {
        val morning = window("morning", CallBlockScheduleAction.BLOCK, 6 * 60, 12 * 60)
        val touching = window("afternoon", CallBlockScheduleAction.PAUSE, 12 * 60, 18 * 60)
        val overlap = window("overlap", CallBlockScheduleAction.PAUSE, 11 * 60, 13 * 60)
        val night = window("night", CallBlockScheduleAction.BLOCK, 22 * 60, 6 * 60)
        val afterMidnightOverlap = window("late", CallBlockScheduleAction.PAUSE, 5 * 60, 7 * 60)

        assertFalse(CallBlockDailySchedule.overlaps(morning, touching))
        assertTrue(CallBlockDailySchedule.overlaps(morning, overlap))
        assertTrue(CallBlockDailySchedule.overlaps(night, afterMidnightOverlap))
        assertTrue(CallBlockDailySchedule.upsert(listOf(morning), overlap) is CallBlockScheduleUpdate.Overlap)
    }

    @Test
    fun editIgnoresItsOwnWindowAndMaximumIsFour() {
        val original = window("same", CallBlockScheduleAction.BLOCK, 60, 120)
        val edited = original.copy(action = CallBlockScheduleAction.PAUSE, startMinute = 120, endMinute = 180)
        assertTrue(CallBlockDailySchedule.upsert(listOf(original), edited) is CallBlockScheduleUpdate.Success)

        val full = (0 until MAX_CALL_BLOCK_TIME_WINDOWS).map { index ->
            window("$index", CallBlockScheduleAction.BLOCK, index * 60, (index + 1) * 60)
        }
        val fifth = window("fifth", CallBlockScheduleAction.PAUSE, 10 * 60, 11 * 60)
        assertEquals(CallBlockScheduleUpdate.TooManyWindows, CallBlockDailySchedule.upsert(full, fifth))
    }

    @Test
    fun scheduleOverridesBaseWhileOneShotPauseHasHighestPriority() {
        val block = window("block", CallBlockScheduleAction.BLOCK, 6 * 60, 12 * 60)
        val pause = window("pause", CallBlockScheduleAction.PAUSE, 12 * 60, 18 * 60)
        val schedule = listOf(block, pause)

        assertTrue(CallBlockDailySchedule.isBlockingEnabled(false, false, schedule, 7 * 60))
        assertFalse(CallBlockDailySchedule.isBlockingEnabled(true, false, schedule, 13 * 60))
        assertFalse(CallBlockDailySchedule.isBlockingEnabled(true, true, schedule, 7 * 60))
        assertTrue(CallBlockDailySchedule.isBlockingEnabled(true, false, schedule, 20 * 60))
        assertFalse(CallBlockDailySchedule.isBlockingEnabled(false, false, schedule, 20 * 60))
    }

    @Test
    fun codecRoundTripsAndRejectsCorruptOrConflictingPayloadsAsAWhole() {
        val schedule = listOf(
            window(
                "morning-id",
                CallBlockScheduleAction.BLOCK,
                6 * 60,
                12 * 60,
                CallBlockSchedulePreset.MORNING.storageKey,
            ),
            window("night-id", CallBlockScheduleAction.PAUSE, 22 * 60, 6 * 60),
        )
        assertEquals(schedule, CallBlockDailyScheduleCodec.decode(CallBlockDailyScheduleCodec.encode(schedule)))
        assertTrue(CallBlockDailyScheduleCodec.decode("v1;bad").isEmpty())
        assertTrue(
            CallBlockDailyScheduleCodec.decode(
                "v1;a,block,60,180,;b,pause,120,240,"
            ).isEmpty()
        )
        assertNull(CallBlockDailySchedule.actionAt(emptyList(), 100))
    }

    @Test
    fun weekdaySelectionAndOvernightCarryUseTheStartingDay() {
        val mondayNight = window(
            id = "monday-night",
            action = CallBlockScheduleAction.PAUSE,
            start = 22 * 60,
            end = 6 * 60,
            weekdaysMask = weekdayBit(DayOfWeek.MONDAY),
        )

        assertTrue(mondayNight.isActiveAt(DayOfWeek.MONDAY, 23 * 60))
        assertTrue(mondayNight.isActiveAt(DayOfWeek.TUESDAY, 5 * 60 + 59))
        assertFalse(mondayNight.isActiveAt(DayOfWeek.TUESDAY, 6 * 60))
        assertFalse(mondayNight.isActiveAt(DayOfWeek.MONDAY, 5 * 60))
    }

    @Test
    fun conflictsRequireIntersectingCalendarDaysAndEnabledWindows() {
        val monday = window(
            "monday",
            CallBlockScheduleAction.BLOCK,
            8 * 60,
            10 * 60,
            weekdaysMask = weekdayBit(DayOfWeek.MONDAY),
        )
        val tuesday = window(
            "tuesday",
            CallBlockScheduleAction.PAUSE,
            8 * 60,
            10 * 60,
            weekdaysMask = weekdayBit(DayOfWeek.TUESDAY),
        )
        val disabledMonday = monday.copy(id = "disabled", enabled = false)
        val mondayNight = monday.copy(id = "night", startMinute = 22 * 60, endMinute = 6 * 60)
        val tuesdayMorning = tuesday.copy(id = "morning", startMinute = 5 * 60, endMinute = 7 * 60)

        assertFalse(CallBlockDailySchedule.overlaps(monday, tuesday))
        assertFalse(CallBlockDailySchedule.overlaps(disabledMonday, monday))
        assertTrue(CallBlockDailySchedule.overlaps(mondayNight, tuesdayMorning))
        assertTrue(
            CallBlockDailySchedule.upsert(listOf(monday), disabledMonday) is
                CallBlockScheduleUpdate.Success
        )
        assertTrue(
            CallBlockDailySchedule.upsert(listOf(monday), disabledMonday.copy(enabled = true)) is
                CallBlockScheduleUpdate.Overlap
        )
    }

    @Test
    fun equalTimeWindowsKeepAStableOrderWhenOneIsUpdated() {
        val monday = window(
            id = "a-monday",
            action = CallBlockScheduleAction.BLOCK,
            start = 8 * 60,
            end = 10 * 60,
            enabled = false,
            weekdaysMask = weekdayBit(DayOfWeek.MONDAY),
        )
        val tuesday = window(
            id = "b-tuesday",
            action = CallBlockScheduleAction.PAUSE,
            start = 8 * 60,
            end = 10 * 60,
            weekdaysMask = weekdayBit(DayOfWeek.TUESDAY),
        )
        val original = CallBlockDailySchedule.sort(listOf(monday, tuesday))

        val updated = CallBlockDailySchedule.upsert(original, monday.copy(enabled = true))

        assertTrue(updated is CallBlockScheduleUpdate.Success)
        assertEquals(
            listOf("a-monday", "b-tuesday"),
            (updated as CallBlockScheduleUpdate.Success).windows.map(CallBlockTimeWindow::id),
        )
    }

    @Test
    fun legacyScheduleDefaultsToEnabledEveryDay() {
        val decoded = CallBlockDailyScheduleCodec.decode("v1;legacy,block,360,720,morning")
        assertEquals(1, decoded.size)
        assertTrue(decoded.single().enabled)
        assertEquals(ALL_WEEKDAYS_MASK, decoded.single().weekdaysMask)
    }

    private fun window(
        id: String,
        action: CallBlockScheduleAction,
        start: Int,
        end: Int,
        presetKey: String? = null,
        enabled: Boolean = true,
        weekdaysMask: Int = ALL_WEEKDAYS_MASK,
    ) = CallBlockTimeWindow(id, action, start, end, presetKey, enabled, weekdaysMask)
}

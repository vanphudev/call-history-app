package com.antimobile.callhs.data.blocking

import java.time.Instant
import java.time.DayOfWeek
import java.time.ZoneId
import java.util.UUID

const val MINUTES_PER_DAY = 24 * 60
const val MAX_CALL_BLOCK_TIME_WINDOWS = 4
const val ALL_WEEKDAYS_MASK = (1 shl 7) - 1

/** The state CallHS should force while a recurring daily window is active. */
enum class CallBlockScheduleAction(val storageKey: String) {
    BLOCK("block"),
    PAUSE("pause");

    companion object {
        fun fromStorage(value: String?): CallBlockScheduleAction? =
            entries.firstOrNull { it.storageKey == value }
    }
}

/** Alarm-like presets. A night window deliberately crosses midnight. */
enum class CallBlockSchedulePreset(
    val storageKey: String,
    val startMinute: Int,
    val endMinute: Int,
) {
    MORNING("morning", 6 * 60, 12 * 60),
    AFTERNOON("afternoon", 12 * 60, 18 * 60),
    EVENING("evening", 18 * 60, 22 * 60),
    NIGHT("night", 22 * 60, 6 * 60);

    companion object {
        fun fromStorage(value: String?): CallBlockSchedulePreset? =
            entries.firstOrNull { it.storageKey == value }
    }
}

/**
 * One recurring local-clock interval. Intervals are half-open: start is included and end is not.
 * An end earlier than the start means the interval continues through midnight.
 */
data class CallBlockTimeWindow(
    val id: String,
    val action: CallBlockScheduleAction,
    val startMinute: Int,
    val endMinute: Int,
    val presetKey: String? = null,
    val enabled: Boolean = true,
    val weekdaysMask: Int = ALL_WEEKDAYS_MASK,
) {
    val crossesMidnight: Boolean get() = endMinute < startMinute

    fun contains(minuteOfDay: Int): Boolean {
        if (!isValid || minuteOfDay !in 0 until MINUTES_PER_DAY) return false
        return if (startMinute < endMinute) {
            minuteOfDay in startMinute until endMinute
        } else {
            minuteOfDay >= startMinute || minuteOfDay < endMinute
        }
    }

    fun appliesOn(dayOfWeek: DayOfWeek): Boolean =
        weekdaysMask and weekdayBit(dayOfWeek) != 0

    /** For an overnight window, early-morning minutes belong to the previous selected weekday. */
    fun isActiveAt(dayOfWeek: DayOfWeek, minuteOfDay: Int): Boolean {
        if (!enabled || !isValid || minuteOfDay !in 0 until MINUTES_PER_DAY) return false
        return if (startMinute < endMinute) {
            appliesOn(dayOfWeek) && minuteOfDay in startMinute until endMinute
        } else if (minuteOfDay >= startMinute) {
            appliesOn(dayOfWeek)
        } else {
            minuteOfDay < endMinute && appliesOn(previousWeekday(dayOfWeek))
        }
    }

    val isValid: Boolean
        get() = id.isNotBlank() && id.length <= 64 &&
            id.all { it.isLetterOrDigit() || it == '-' || it == '_' } &&
            startMinute in 0 until MINUTES_PER_DAY &&
            endMinute in 0 until MINUTES_PER_DAY &&
            startMinute != endMinute &&
            weekdaysMask in 1..ALL_WEEKDAYS_MASK

    companion object {
        fun create(
            action: CallBlockScheduleAction,
            startMinute: Int,
            endMinute: Int,
            preset: CallBlockSchedulePreset? = null,
        ): CallBlockTimeWindow = CallBlockTimeWindow(
            id = UUID.randomUUID().toString(),
            action = action,
            startMinute = startMinute,
            endMinute = endMinute,
            presetKey = preset?.storageKey,
        )
    }
}

sealed interface CallBlockScheduleUpdate {
    data class Success(val windows: List<CallBlockTimeWindow>) : CallBlockScheduleUpdate
    data class Overlap(val conflicting: CallBlockTimeWindow) : CallBlockScheduleUpdate
    data object TooManyWindows : CallBlockScheduleUpdate
    data object InvalidWindow : CallBlockScheduleUpdate
    data object StorageFailure : CallBlockScheduleUpdate
}

/** Pure schedule rules shared by settings UI, persistence and the screening hot path. */
object CallBlockDailySchedule {
    fun actionAt(
        windows: List<CallBlockTimeWindow>,
        minuteOfDay: Int,
    ): CallBlockScheduleAction? = actionAt(windows, DayOfWeek.MONDAY, minuteOfDay)

    fun actionAt(
        windows: List<CallBlockTimeWindow>,
        dayOfWeek: DayOfWeek,
        minuteOfDay: Int,
    ): CallBlockScheduleAction? =
        windows.firstOrNull { it.isActiveAt(dayOfWeek, minuteOfDay) }?.action

    fun actionAt(
        windows: List<CallBlockTimeWindow>,
        nowMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): CallBlockScheduleAction? {
        val local = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
        return actionAt(windows, local.dayOfWeek, local.hour * 60 + local.minute)
    }

    /** A one-shot pause is an explicit emergency override and always wins over a recurring block. */
    fun isBlockingEnabled(
        baseEnabled: Boolean,
        oneShotPaused: Boolean,
        windows: List<CallBlockTimeWindow>,
        minuteOfDay: Int,
        dayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    ): Boolean {
        if (oneShotPaused) return false
        return when (actionAt(windows, dayOfWeek, minuteOfDay)) {
            CallBlockScheduleAction.BLOCK -> true
            CallBlockScheduleAction.PAUSE -> false
            null -> baseEnabled
        }
    }

    fun upsert(
        current: List<CallBlockTimeWindow>,
        candidate: CallBlockTimeWindow,
    ): CallBlockScheduleUpdate {
        if (!candidate.isValid) return CallBlockScheduleUpdate.InvalidWindow
        val retained = current.filterNot { it.id == candidate.id }
        if (retained.size >= MAX_CALL_BLOCK_TIME_WINDOWS) {
            return CallBlockScheduleUpdate.TooManyWindows
        }
        val conflict = retained.firstOrNull { overlaps(it, candidate) }
        if (conflict != null) return CallBlockScheduleUpdate.Overlap(conflict)
        return CallBlockScheduleUpdate.Success(sort(retained + candidate))
    }

    fun validateAll(windows: List<CallBlockTimeWindow>): Boolean {
        if (windows.size > MAX_CALL_BLOCK_TIME_WINDOWS || windows.any { !it.isValid }) return false
        if (windows.map { it.id }.toSet().size != windows.size) return false
        return windows.indices.none { left ->
            (left + 1 until windows.size).any { right -> overlaps(windows[left], windows[right]) }
        }
    }

    fun sort(windows: List<CallBlockTimeWindow>): List<CallBlockTimeWindow> =
        windows.sortedWith(
            compareBy(
                CallBlockTimeWindow::startMinute,
                CallBlockTimeWindow::endMinute,
                CallBlockTimeWindow::id,
            )
        )

    fun overlaps(left: CallBlockTimeWindow, right: CallBlockTimeWindow): Boolean {
        if (!left.enabled || !right.enabled) return false
        return DayOfWeek.entries.any { day ->
            segmentsOnDay(left, day).any { a ->
                segmentsOnDay(right, day).any { b -> a.first < b.second && b.first < a.second }
            }
        }
    }

    /** Segments use an exclusive end and are convenient for both validation and timeline drawing. */
    fun segments(window: CallBlockTimeWindow): List<Pair<Int, Int>> = when {
        !window.isValid -> emptyList()
        window.startMinute < window.endMinute -> listOf(window.startMinute to window.endMinute)
        else -> listOf(window.startMinute to MINUTES_PER_DAY, 0 to window.endMinute)
    }

    /** Segments visible on one calendar day, including carry-over from yesterday's overnight window. */
    fun segmentsOnDay(
        window: CallBlockTimeWindow,
        dayOfWeek: DayOfWeek,
    ): List<Pair<Int, Int>> {
        if (!window.isValid) return emptyList()
        if (window.startMinute < window.endMinute) {
            return if (window.appliesOn(dayOfWeek)) {
                listOf(window.startMinute to window.endMinute)
            } else {
                emptyList()
            }
        }
        return buildList {
            if (window.appliesOn(previousWeekday(dayOfWeek))) add(0 to window.endMinute)
            if (window.appliesOn(dayOfWeek)) add(window.startMinute to MINUTES_PER_DAY)
        }
    }

    fun minuteOfDay(nowMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): Int {
        val localTime = Instant.ofEpochMilli(nowMillis).atZone(zoneId).toLocalTime()
        return localTime.hour * 60 + localTime.minute
    }

    fun dayOfWeek(nowMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): DayOfWeek =
        Instant.ofEpochMilli(nowMillis).atZone(zoneId).dayOfWeek
}

fun weekdayBit(dayOfWeek: DayOfWeek): Int = 1 shl (dayOfWeek.value - 1)

private fun previousWeekday(dayOfWeek: DayOfWeek): DayOfWeek =
    if (dayOfWeek == DayOfWeek.MONDAY) DayOfWeek.SUNDAY else DayOfWeek.of(dayOfWeek.value - 1)

/** Compact, versioned SharedPreferences representation. Invalid data is rejected as a whole. */
internal object CallBlockDailyScheduleCodec {
    private const val VERSION = "v2"
    private const val LEGACY_VERSION = "v1"
    private const val ENTRY_SEPARATOR = ";"
    private const val FIELD_SEPARATOR = ","

    fun encode(windows: List<CallBlockTimeWindow>): String {
        require(CallBlockDailySchedule.validateAll(windows))
        if (windows.isEmpty()) return VERSION
        return buildString {
            append(VERSION)
            CallBlockDailySchedule.sort(windows).forEach { window ->
                append(ENTRY_SEPARATOR)
                append(window.id)
                append(FIELD_SEPARATOR)
                append(window.action.storageKey)
                append(FIELD_SEPARATOR)
                append(window.startMinute)
                append(FIELD_SEPARATOR)
                append(window.endMinute)
                append(FIELD_SEPARATOR)
                append(window.presetKey.orEmpty())
                append(FIELD_SEPARATOR)
                append(window.enabled)
                append(FIELD_SEPARATOR)
                append(window.weekdaysMask)
            }
        }
    }

    fun decode(raw: String?): List<CallBlockTimeWindow> {
        if (raw.isNullOrBlank()) return emptyList()
        val tokens = raw.split(ENTRY_SEPARATOR)
        val version = tokens.firstOrNull()
        if (version != VERSION && version != LEGACY_VERSION) return emptyList()
        val windows = tokens.drop(1).map { encoded ->
            val fields = encoded.split(FIELD_SEPARATOR)
            val expectedFields = if (version == LEGACY_VERSION) 5 else 7
            if (fields.size != expectedFields) return emptyList()
            val action = CallBlockScheduleAction.fromStorage(fields[1]) ?: return emptyList()
            val start = fields[2].toIntOrNull() ?: return emptyList()
            val end = fields[3].toIntOrNull() ?: return emptyList()
            val preset = fields[4].takeIf(String::isNotBlank)
            if (preset != null && CallBlockSchedulePreset.fromStorage(preset) == null) return emptyList()
            val enabled = if (version == LEGACY_VERSION) {
                true
            } else {
                fields[5].toBooleanStrictOrNull() ?: return emptyList()
            }
            val weekdaysMask = if (version == LEGACY_VERSION) {
                ALL_WEEKDAYS_MASK
            } else {
                fields[6].toIntOrNull() ?: return emptyList()
            }
            CallBlockTimeWindow(
                id = fields[0],
                action = action,
                startMinute = start,
                endMinute = end,
                presetKey = preset,
                enabled = enabled,
                weekdaysMask = weekdaysMask,
            )
        }
        return if (CallBlockDailySchedule.validateAll(windows)) {
            CallBlockDailySchedule.sort(windows)
        } else {
            emptyList()
        }
    }
}

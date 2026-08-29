package com.antimobile.mcas.data.blocking

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Notification riêng của MCAS sau khi đã chặn thành công. */
enum class BlockNotificationMode(val storageKey: String) {
    OFF("off"),
    EVERY_BLOCK("every");

    companion object {
        fun fromStorage(value: String?): BlockNotificationMode = when (value) {
            OFF.storageKey -> OFF
            // Cadence 5/10 was removed. Existing installs/backups are upgraded to the only
            // enabled behavior instead of silently losing blocked-call notifications.
            EVERY_BLOCK.storageKey,
            "every_5",
            "every_10",
            -> EVERY_BLOCK
            else -> OFF
        }
    }
}

/**
 * Cách Telecom xử lý một cuộc gọi sau khi quy tắc đã khớp.
 *
 * [storageKey] là hợp đồng bền vững dùng cho SharedPreferences/backup. Không lưu trực tiếp tổ hợp
 * boolean của Android để các trạng thái không hợp lệ (vừa disallow vừa silence) không thể xuất hiện.
 */
enum class CallBlockMethod(val storageKey: String) {
    BLOCK_AND_REJECT("block_and_reject"),
    BLOCK_WITHOUT_REJECT("block_without_reject"),
    SILENCE_ONLY("silence_only"),
    ALLOW("allow");

    companion object {
        fun fromStorage(value: String?): CallBlockMethod =
            entries.firstOrNull { it.storageKey == value } ?: BLOCK_AND_REJECT
    }
}

/** Pure policy mapped by [CallBlockScreeningService] to CallResponse.Builder. */
data class CallResponsePolicy(
    val disallowCall: Boolean,
    val rejectCall: Boolean,
    val silenceCall: Boolean,
    val skipNotification: Boolean,
) {
    /** Only disallowed calls are app-owned blocked events eligible for history/notification. */
    val blocksCall: Boolean get() = disallowCall

    companion object {
        /**
         * Resolves the last authoritative settings read before responding to Telecom.
         * A disabled/paused protection state or an explicit ALLOW method must fail open.
         */
        fun forActiveBlocking(
            enabled: Boolean,
            method: CallBlockMethod,
        ): CallResponsePolicy? =
            if (!enabled || method == CallBlockMethod.ALLOW) null else forMethod(method)

        fun forMethod(method: CallBlockMethod): CallResponsePolicy = when (method) {
            CallBlockMethod.BLOCK_AND_REJECT -> CallResponsePolicy(
                disallowCall = true,
                rejectCall = true,
                silenceCall = false,
                skipNotification = true,
            )
            CallBlockMethod.BLOCK_WITHOUT_REJECT -> CallResponsePolicy(
                disallowCall = true,
                rejectCall = false,
                silenceCall = false,
                skipNotification = true,
            )
            CallBlockMethod.SILENCE_ONLY -> CallResponsePolicy(
                disallowCall = false,
                rejectCall = false,
                silenceCall = true,
                skipNotification = false,
            )
            CallBlockMethod.ALLOW -> CallResponsePolicy(
                disallowCall = false,
                rejectCall = false,
                silenceCall = false,
                skipNotification = false,
            )
        }
    }
}

/** Preset durations offered by the temporary protection pause control. */
enum class CallBlockPauseDuration(
    val minutes: Int,
) {
    MINUTES_10(10),
    MINUTES_30(30),
    MINUTES_60(60);

    val durationMillis: Long get() = minutes * 60_000L
}

/**
 * Persisted protection schedule, independent from Android and therefore safe to evaluate in the
 * screening hot path and in local unit tests.
 *
 * A pause occupies the half-open wall-clock interval `[pauseStartedAtMillis, pauseUntilMillis)`.
 */
data class CallBlockProtectionState(
    val baseEnabled: Boolean = true,
    val pauseStartedAtMillis: Long? = null,
    val pauseUntilMillis: Long? = null,
) {
    private val hasValidPause: Boolean
        get() = pauseStartedAtMillis != null &&
            pauseUntilMillis != null &&
            pauseUntilMillis > pauseStartedAtMillis

    fun isPausedAt(nowMillis: Long): Boolean =
        baseEnabled && hasValidPause &&
            nowMillis >= requireNotNull(pauseStartedAtMillis) &&
            nowMillis < requireNotNull(pauseUntilMillis)

    fun isEffectivelyEnabledAt(nowMillis: Long): Boolean =
        baseEnabled && !isPausedAt(nowMillis)

    fun remainingPauseMillisAt(nowMillis: Long): Long {
        if (!isPausedAt(nowMillis)) return 0L
        val until = requireNotNull(pauseUntilMillis)
        val difference = until - nowMillis
        // The subtraction can overflow only for synthetic clocks spanning the entire Long range.
        return if (difference < 0L) Long.MAX_VALUE else difference
    }

    /** Removes expired, disabled or incomplete pause metadata from the observable snapshot. */
    fun normalizedAt(nowMillis: Long): CallBlockProtectionState =
        if (isPausedAt(nowMillis)) this else withoutPause()

    fun withPermanentEnabled(value: Boolean): CallBlockProtectionState =
        CallBlockProtectionState(baseEnabled = value)

    fun withPause(duration: CallBlockPauseDuration, nowMillis: Long): CallBlockProtectionState =
        CallBlockProtectionState(
            baseEnabled = true,
            pauseStartedAtMillis = nowMillis,
            pauseUntilMillis = saturatingAdd(nowMillis, duration.durationMillis),
        )

    fun withoutPause(): CallBlockProtectionState = copy(
        pauseStartedAtMillis = null,
        pauseUntilMillis = null,
    )

    private fun saturatingAdd(left: Long, right: Long): Long =
        if (right > 0L && left > Long.MAX_VALUE - right) Long.MAX_VALUE else left + right
}

/** Small persistence boundary so timing and concurrent state transitions are JVM-testable. */
internal interface CallBlockProtectionPersistence {
    fun read(): CallBlockProtectionState
    /** User-triggered transitions require a durable result before their state is published. */
    fun write(state: CallBlockProtectionState): Boolean

    /** Terminal clock normalization must update memory without putting disk latency on screening. */
    fun writeAsync(state: CallBlockProtectionState)
}

/** Serializes every protection read/transition that can otherwise tear the three persisted keys. */
internal class CallBlockProtectionCoordinator {
    private val lock = Any()

    fun readRaw(
        persistence: CallBlockProtectionPersistence,
    ): CallBlockProtectionState = synchronized(lock) {
        persistence.read()
    }

    fun read(
        persistence: CallBlockProtectionPersistence,
        nowMillis: Long,
    ): CallBlockProtectionState = synchronized(lock) {
        val raw = persistence.read()
        val normalized = raw.normalizedAt(nowMillis)
        // Persist terminal normalization so a later wall-clock rollback cannot resurrect a pause
        // that was already observed as expired or invalid/before its recorded start. apply() updates
        // SharedPreferences memory atomically without putting filesystem latency in screening.
        if (normalized != raw) persistence.writeAsync(normalized)
        normalized
    }

    fun setPermanentEnabled(
        persistence: CallBlockProtectionPersistence,
        value: Boolean,
    ): CallBlockProtectionState = synchronized(lock) {
        val updated = persistence.read().withPermanentEnabled(value)
        persistOrRestore(persistence, updated)
    }

    fun pause(
        persistence: CallBlockProtectionPersistence,
        duration: CallBlockPauseDuration,
        nowMillis: Long,
    ): CallBlockProtectionState = synchronized(lock) {
        val updated = persistence.read().withPause(duration, nowMillis)
        persistOrRestore(persistence, updated)
    }

    fun clearPause(
        persistence: CallBlockProtectionPersistence,
    ): CallBlockProtectionState = synchronized(lock) {
        val updated = persistence.read().withoutPause()
        persistOrRestore(persistence, updated)
    }

    private fun persistOrRestore(
        persistence: CallBlockProtectionPersistence,
        updated: CallBlockProtectionState,
    ): CallBlockProtectionState {
        val previous = persistence.read()
        if (persistence.write(updated)) return updated
        // SharedPreferences.commit() updates its in-memory map before reporting a disk failure.
        // Restore the previous snapshot in memory best-effort, then publish only an authoritative read.
        runCatching { persistence.write(previous) }
        return persistence.read()
    }
}

/**
 * Cài đặt process-safe cho bộ chặn. Service luôn đọc SharedPreferences trực tiếp để hoạt động
 * cả khi Android khởi chạy process chỉ cho CallScreeningService; snapshot Compose chỉ phục vụ UI.
 */
object CallBlockSettings {
    private const val PREFS = "call_block_settings"
    private const val RUNTIME_PREFS = "call_block_runtime_state"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_PAUSE_STARTED_AT = "pause_started_at"
    private const val KEY_PAUSE_UNTIL = "pause_until"
    private const val KEY_NOTIFICATION = "notification_mode"
    private const val KEY_BLOCK_METHOD = "block_method"
    private const val KEY_DAILY_SCHEDULE = "daily_schedule"
    private const val KEY_REPEAT_UNKNOWN_CALLER_GUARD_ENABLED = "repeat_unknown_caller_guard_enabled"
    private const val KEY_REPEAT_UNKNOWN_CALLER_GUARD_THRESHOLD = "repeat_unknown_caller_guard_threshold"
    private const val KEY_REPEAT_UNKNOWN_CALLER_GUARD_WINDOW_MINUTES = "repeat_unknown_caller_guard_window_minutes"
    private const val KEY_REPEAT_UNKNOWN_CALLER_GUARD_SESSION_GENERATION = "repeat_unknown_caller_guard_session_generation"
    private const val LEGACY_REPEAT_UNKNOWN_CALLER_BYPASS_ENABLED = "repeat_unknown_caller_bypass_enabled"
    private const val LEGACY_REPEAT_UNKNOWN_CALLER_BYPASS_THRESHOLD = "repeat_unknown_caller_bypass_threshold"
    private const val LEGACY_REPEAT_UNKNOWN_CALLER_BYPASS_WINDOW_MINUTES = "repeat_unknown_caller_bypass_window_minutes"
    private const val LEGACY_REPEAT_UNKNOWN_CALLER_SESSION_GENERATION = "repeat_unknown_caller_session_generation"

    private val protectionCoordinator = CallBlockProtectionCoordinator()
    private val dailyScheduleLock = Any()
    private val repeatUnknownCallerConfigLock = Any()
    private val transientMigrationLock = Any()

    val REPEAT_UNKNOWN_CALLER_GUARD_THRESHOLD_PRESETS: List<Int> = listOf(2, 3, 4)
    const val DEFAULT_REPEAT_UNKNOWN_CALLER_GUARD_THRESHOLD = 2
    const val DEFAULT_REPEAT_UNKNOWN_CALLER_GUARD_WINDOW_MINUTES = 15
    const val MIN_REPEAT_UNKNOWN_CALLER_GUARD_WINDOW_MINUTES = 1
    const val MAX_REPEAT_UNKNOWN_CALLER_GUARD_WINDOW_MINUTES = 24 * 60

    var protectionState by mutableStateOf(CallBlockProtectionState())
        private set

    var dailySchedule by mutableStateOf<List<CallBlockTimeWindow>>(emptyList())
        private set

    /** Permanent preference. Unlike [enabled], this remains true while protection is paused. */
    val baseEnabled: Boolean get() = protectionState.baseEnabled

    /** Effective Compose snapshot; recurring windows override the base switch outside one-shot pauses. */
    val enabled: Boolean
        get() = isEffectivelyEnabledAt(System.currentTimeMillis())

    val pauseStartedAtMillis: Long? get() = protectionState.pauseStartedAtMillis
    val pauseUntilMillis: Long? get() = protectionState.pauseUntilMillis
    val isTemporarilyPaused: Boolean
        get() = protectionState.isPausedAt(System.currentTimeMillis())
    val pauseRemainingMillis: Long
        get() = protectionState.remainingPauseMillisAt(System.currentTimeMillis())

    var notificationMode by mutableStateOf(BlockNotificationMode.EVERY_BLOCK)
        private set
    var blockMethod by mutableStateOf(CallBlockMethod.BLOCK_AND_REJECT)
        private set
    var repeatUnknownCallerGuardEnabled by mutableStateOf(false)
        private set
    var repeatUnknownCallerGuardThreshold by mutableStateOf(DEFAULT_REPEAT_UNKNOWN_CALLER_GUARD_THRESHOLD)
        private set
    var repeatUnknownCallerGuardWindowMinutes by mutableStateOf(DEFAULT_REPEAT_UNKNOWN_CALLER_GUARD_WINDOW_MINUTES)
        private set

    fun init(context: Context) {
        normalizeLegacyAllowMethod(context)
        refresh(context)
        refreshDailySchedule(context)
        notificationMode = notificationMode(context)
        blockMethod = blockMethod(context)
        repeatUnknownCallerGuardConfig(context).also { config ->
            repeatUnknownCallerGuardEnabled = config.enabled
            repeatUnknownCallerGuardThreshold = config.threshold
            repeatUnknownCallerGuardWindowMinutes = config.windowMinutes
        }
    }

    /** Re-reads persisted state and publishes one internally consistent Compose snapshot. */
    fun refresh(
        context: Context,
        nowMillis: Long = System.currentTimeMillis(),
    ): CallBlockProtectionState = protectionCoordinator
        .read(protectionPersistence(context), nowMillis)
        .also { protectionState = it }

    fun refreshDailySchedule(context: Context): List<CallBlockTimeWindow> =
        dailySchedule(context).also { dailySchedule = it }

    fun scheduledActionAt(nowMillis: Long = System.currentTimeMillis()): CallBlockScheduleAction? =
        CallBlockDailySchedule.actionAt(dailySchedule, nowMillis)

    fun isEffectivelyEnabledAt(nowMillis: Long): Boolean =
        CallBlockDailySchedule.isBlockingEnabled(
            baseEnabled = protectionState.baseEnabled,
            oneShotPaused = protectionState.isPausedAt(nowMillis),
            windows = dailySchedule,
            minuteOfDay = CallBlockDailySchedule.minuteOfDay(nowMillis),
            dayOfWeek = CallBlockDailySchedule.dayOfWeek(nowMillis),
        )

    /** Authoritative effective value for the screening hot path; never relies on Compose state. */
    fun isBlockingEnabled(
        context: Context,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean {
        val protection = protectionCoordinator.read(protectionPersistence(context), nowMillis)
        return CallBlockDailySchedule.isBlockingEnabled(
            baseEnabled = protection.baseEnabled,
            oneShotPaused = protection.isPausedAt(nowMillis),
            windows = dailySchedule(context),
            minuteOfDay = CallBlockDailySchedule.minuteOfDay(nowMillis),
            dayOfWeek = CallBlockDailySchedule.dayOfWeek(nowMillis),
        )
    }

    /** Authoritative permanent/base preference for backup and settings UI. */
    fun isBaseEnabled(context: Context): Boolean =
        protectionCoordinator.readRaw(protectionPersistence(context)).baseEnabled

    fun isTemporarilyPaused(
        context: Context,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean = protectionCoordinator
        .read(protectionPersistence(context), nowMillis)
        .isPausedAt(nowMillis)

    fun pauseRemainingMillis(
        context: Context,
        nowMillis: Long = System.currentTimeMillis(),
    ): Long = protectionCoordinator
        .read(protectionPersistence(context), nowMillis)
        .remainingPauseMillisAt(nowMillis)

    /** Authoritative durable schedule read. Invalid/corrupt storage safely falls back to no override. */
    fun dailySchedule(context: Context): List<CallBlockTimeWindow> = synchronized(dailyScheduleLock) {
        CallBlockDailyScheduleCodec.decode(prefs(context).getString(KEY_DAILY_SCHEDULE, null))
    }

    fun upsertDailyWindow(
        context: Context,
        candidate: CallBlockTimeWindow,
    ): CallBlockScheduleUpdate = synchronized(dailyScheduleLock) {
        val preferences = prefs(context)
        val current = CallBlockDailyScheduleCodec.decode(preferences.getString(KEY_DAILY_SCHEDULE, null))
        when (val result = CallBlockDailySchedule.upsert(current, candidate)) {
            is CallBlockScheduleUpdate.Success -> {
                if (preferences.edit()
                        .putString(KEY_DAILY_SCHEDULE, CallBlockDailyScheduleCodec.encode(result.windows))
                        .commit()
                ) {
                    dailySchedule = result.windows
                    result
                } else {
                    CallBlockScheduleUpdate.StorageFailure
                }
            }
            else -> result
        }
    }

    fun removeDailyWindow(context: Context, id: String): Boolean = synchronized(dailyScheduleLock) {
        val preferences = prefs(context)
        val current = CallBlockDailyScheduleCodec.decode(preferences.getString(KEY_DAILY_SCHEDULE, null))
        val updated = current.filterNot { it.id == id }
        if (updated.size == current.size) return@synchronized false
        preferences.edit()
            .putString(KEY_DAILY_SCHEDULE, CallBlockDailyScheduleCodec.encode(updated))
            .commit()
            .also { saved -> if (saved) dailySchedule = updated }
    }

    /** Used by restore: the whole portable schedule is validated before replacing current data. */
    fun replaceDailySchedule(context: Context, windows: List<CallBlockTimeWindow>): Boolean =
        synchronized(dailyScheduleLock) {
            if (!CallBlockDailySchedule.validateAll(windows)) return@synchronized false
            val sorted = CallBlockDailySchedule.sort(windows)
            prefs(context).edit()
                .putString(KEY_DAILY_SCHEDULE, CallBlockDailyScheduleCodec.encode(sorted))
                .commit()
                .also { saved -> if (saved) dailySchedule = sorted }
        }

    fun notificationMode(context: Context): BlockNotificationMode =
        BlockNotificationMode.fromStorage(
            prefs(context).getString(KEY_NOTIFICATION, BlockNotificationMode.EVERY_BLOCK.storageKey)
        )

    fun blockMethod(context: Context): CallBlockMethod =
        CallBlockMethod.fromStorage(
            prefs(context).getString(KEY_BLOCK_METHOD, CallBlockMethod.BLOCK_AND_REJECT.storageKey)
        )

    /** One internally consistent runtime read for the no-rule guard. Invalid storage fails open. */
    fun repeatUnknownCallerGuardConfig(context: Context): RepeatUnknownCallerGuardConfig =
        synchronized(repeatUnknownCallerConfigLock) {
            readRepeatUnknownCallerGuardConfigLocked(
                preferences = prefs(context),
                runtimePreferences = runtimePrefs(context),
            )
        }

    /**
     * Invalidates every repeated-call attempt recorded before a rules restore without touching the
     * user's durable guard configuration. In-flight callbacks also fail their final config check
     * because [RepeatUnknownCallerGuardConfig.sessionGeneration] changes atomically first.
     */
    fun resetRepeatUnknownCallerGuardSession(context: Context): Boolean =
        synchronized(repeatUnknownCallerConfigLock) {
            advanceSessionGeneration(runtimePrefs(context))
        }

    /** Selects a permanent state. Either value cancels an existing temporary pause. */
    fun setEnabled(
        context: Context,
        value: Boolean,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        protectionState = protectionCoordinator
            .setPermanentEnabled(protectionPersistence(context), value)
            .normalizedAt(nowMillis)
    }

    /** Temporarily pauses protection, enabling the permanent base state for automatic resumption. */
    fun pause(
        context: Context,
        duration: CallBlockPauseDuration,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        protectionState = protectionCoordinator
            .pause(protectionPersistence(context), duration, nowMillis)
            .normalizedAt(nowMillis)
    }

    fun clearPause(
        context: Context,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        protectionState = protectionCoordinator
            .clearPause(protectionPersistence(context))
            .normalizedAt(nowMillis)
    }

    fun setNotificationMode(context: Context, value: BlockNotificationMode) {
        prefs(context).edit().putString(KEY_NOTIFICATION, value.storageKey).apply()
        notificationMode = value
    }

    fun setBlockMethod(context: Context, value: CallBlockMethod) {
        if (value == CallBlockMethod.ALLOW) {
            // v4 represents this state only through base protection OFF. Publish OFF first so a
            // process death between writes remains permissive instead of unexpectedly blocking.
            setEnabled(context, false)
            prefs(context).edit()
                .putString(KEY_BLOCK_METHOD, CallBlockMethod.BLOCK_AND_REJECT.storageKey)
                .apply()
            blockMethod = CallBlockMethod.BLOCK_AND_REJECT
        } else {
            prefs(context).edit().putString(KEY_BLOCK_METHOD, value.storageKey).apply()
            blockMethod = value
        }
    }

    /** Disabling (and a later fresh re-enable) cannot inherit attempts from the previous session. */
    fun setRepeatUnknownCallerGuardEnabled(context: Context, value: Boolean) {
        synchronized(repeatUnknownCallerConfigLock) {
            val preferences = prefs(context)
            val runtimePreferences = runtimePrefs(context)
            val previous = preferences.safeBoolean(KEY_REPEAT_UNKNOWN_CALLER_GUARD_ENABLED, false)
            val current = readRepeatUnknownCallerGuardConfigLocked(preferences, runtimePreferences)
            if (previous != value || (value && !current.enabled)) {
                // Publish the fresh runtime namespace first. A crash between these two writes can
                // only reset the count while retaining the old durable policy; it cannot inherit
                // attempts from the previous policy and allow a call early.
                if (!advanceSessionGeneration(runtimePreferences)) {
                    publishRepeatUnknownCallerGuardStateLocked(preferences, runtimePreferences)
                    return@synchronized
                }
                preferences.edit().apply {
                    putBoolean(KEY_REPEAT_UNKNOWN_CALLER_GUARD_ENABLED, value)
                    // An explicit ON action is also the recovery path for corrupt persisted
                    // parameters. The UI cannot edit threshold/window while the guard is
                    // effectively off, so repair only those invalid values to documented defaults.
                    if (value) {
                        val threshold = runCatching {
                            preferences.getInt(
                                KEY_REPEAT_UNKNOWN_CALLER_GUARD_THRESHOLD,
                                DEFAULT_REPEAT_UNKNOWN_CALLER_GUARD_THRESHOLD,
                            )
                        }.getOrNull()
                        if (threshold?.let(::isValidRepeatUnknownCallerGuardThreshold) != true) {
                            putInt(
                                KEY_REPEAT_UNKNOWN_CALLER_GUARD_THRESHOLD,
                                DEFAULT_REPEAT_UNKNOWN_CALLER_GUARD_THRESHOLD,
                            )
                        }
                        val windowMinutes = runCatching {
                            preferences.getInt(
                                KEY_REPEAT_UNKNOWN_CALLER_GUARD_WINDOW_MINUTES,
                                DEFAULT_REPEAT_UNKNOWN_CALLER_GUARD_WINDOW_MINUTES,
                            )
                        }.getOrNull()
                        if (windowMinutes?.let(::isValidRepeatUnknownCallerGuardWindowMinutes) != true) {
                            putInt(
                                KEY_REPEAT_UNKNOWN_CALLER_GUARD_WINDOW_MINUTES,
                                DEFAULT_REPEAT_UNKNOWN_CALLER_GUARD_WINDOW_MINUTES,
                            )
                        }
                    }
                }.apply()
            }
            publishRepeatUnknownCallerGuardStateLocked(preferences, runtimePreferences)
        }
    }

    /** Returns false and leaves both disk/UI state unchanged for values outside the 2/3/4 presets. */
    fun setRepeatUnknownCallerGuardThreshold(context: Context, value: Int): Boolean {
        if (!isValidRepeatUnknownCallerGuardThreshold(value)) return false
        synchronized(repeatUnknownCallerConfigLock) {
            val preferences = prefs(context)
            val runtimePreferences = runtimePrefs(context)
            val storedValue = if (preferences.contains(KEY_REPEAT_UNKNOWN_CALLER_GUARD_THRESHOLD)) {
                runCatching { preferences.getInt(KEY_REPEAT_UNKNOWN_CALLER_GUARD_THRESHOLD, 0) }.getOrNull()
            } else {
                DEFAULT_REPEAT_UNKNOWN_CALLER_GUARD_THRESHOLD
            }
            if (storedValue != value) {
                if (!advanceSessionGeneration(runtimePreferences)) return false
                preferences.edit()
                    .putInt(KEY_REPEAT_UNKNOWN_CALLER_GUARD_THRESHOLD, value)
                    .apply()
            }
            publishRepeatUnknownCallerGuardStateLocked(preferences, runtimePreferences)
        }
        return true
    }

    /** Returns false instead of silently clamping user-entered minutes. */
    fun setRepeatUnknownCallerGuardWindowMinutes(context: Context, value: Int): Boolean {
        if (!isValidRepeatUnknownCallerGuardWindowMinutes(value)) return false
        synchronized(repeatUnknownCallerConfigLock) {
            val preferences = prefs(context)
            val runtimePreferences = runtimePrefs(context)
            val storedValue = if (preferences.contains(KEY_REPEAT_UNKNOWN_CALLER_GUARD_WINDOW_MINUTES)) {
                runCatching { preferences.getInt(KEY_REPEAT_UNKNOWN_CALLER_GUARD_WINDOW_MINUTES, 0) }.getOrNull()
            } else {
                DEFAULT_REPEAT_UNKNOWN_CALLER_GUARD_WINDOW_MINUTES
            }
            if (storedValue != value) {
                if (!advanceSessionGeneration(runtimePreferences)) return false
                preferences.edit()
                    .putInt(KEY_REPEAT_UNKNOWN_CALLER_GUARD_WINDOW_MINUTES, value)
                    .apply()
            }
            publishRepeatUnknownCallerGuardStateLocked(preferences, runtimePreferences)
        }
        return true
    }

    fun isValidRepeatUnknownCallerGuardThreshold(value: Int): Boolean =
        value in REPEAT_UNKNOWN_CALLER_GUARD_THRESHOLD_PRESETS

    fun isValidRepeatUnknownCallerGuardWindowMinutes(value: Int): Boolean =
        value in MIN_REPEAT_UNKNOWN_CALLER_GUARD_WINDOW_MINUTES..MAX_REPEAT_UNKNOWN_CALLER_GUARD_WINDOW_MINUTES

    fun hasExplicitSettings(context: Context): Boolean {
        val preferences = prefs(context)
        val runtimePreferences = runtimePrefs(context)
        return preferences.contains(KEY_ENABLED) ||
            runtimePreferences.contains(KEY_PAUSE_STARTED_AT) ||
            runtimePreferences.contains(KEY_PAUSE_UNTIL) ||
            preferences.contains(KEY_NOTIFICATION) ||
            preferences.contains(KEY_BLOCK_METHOD) ||
            preferences.contains(KEY_DAILY_SCHEDULE) ||
            preferences.contains(KEY_REPEAT_UNKNOWN_CALLER_GUARD_ENABLED) ||
            preferences.contains(KEY_REPEAT_UNKNOWN_CALLER_GUARD_THRESHOLD) ||
            preferences.contains(KEY_REPEAT_UNKNOWN_CALLER_GUARD_WINDOW_MINUTES) ||
            runtimePreferences.contains(KEY_REPEAT_UNKNOWN_CALLER_GUARD_SESSION_GENERATION)
    }

    fun shouldNotify(mode: BlockNotificationMode): Boolean =
        mode == BlockNotificationMode.EVERY_BLOCK

    private fun prefs(context: Context) = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * Pause deadlines and the repeated-call namespace are deliberately separated from durable
     * blocker settings so Android Auto Backup cannot restore transient state onto another device.
     */
    private fun runtimePrefs(context: Context): SharedPreferences {
        val appContext = context.applicationContext
        val preferences = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val runtimePreferences = appContext.getSharedPreferences(RUNTIME_PREFS, Context.MODE_PRIVATE)
        discardLegacyTransientState(preferences, runtimePreferences)
        return runtimePreferences
    }

    private fun protectionPersistence(context: Context): CallBlockProtectionPersistence =
        SharedPreferencesProtectionPersistence(
            preferences = prefs(context),
            runtimePreferences = runtimePrefs(context),
        )

    private class SharedPreferencesProtectionPersistence(
        private val preferences: SharedPreferences,
        private val runtimePreferences: SharedPreferences,
    ) : CallBlockProtectionPersistence {
        override fun read(): CallBlockProtectionState = CallBlockProtectionState(
            baseEnabled = preferences.getBoolean(KEY_ENABLED, true),
            pauseStartedAtMillis = runtimePreferences.optionalLong(KEY_PAUSE_STARTED_AT),
            pauseUntilMillis = runtimePreferences.optionalLong(KEY_PAUSE_UNTIL),
        )

        override fun write(state: CallBlockProtectionState): Boolean {
            val hasPause = state.pauseStartedAtMillis != null && state.pauseUntilMillis != null
            return if (hasPause) {
                // Fail safe across a process death: protection becomes active before the temporary
                // permissive window is committed. A torn write may over-block, never over-allow.
                if (!preferences.edit().putBoolean(KEY_ENABLED, true).commit()) return false
                runtimePreferences.edit()
                    .putLong(KEY_PAUSE_STARTED_AT, requireNotNull(state.pauseStartedAtMillis))
                    .putLong(KEY_PAUSE_UNTIL, requireNotNull(state.pauseUntilMillis))
                    .commit()
            } else {
                // Remove a permissive timer before changing the durable base choice. This ordering
                // also makes both OFF and ON transitions fail toward active blocking if interrupted.
                if (!runtimePreferences.edit()
                        .remove(KEY_PAUSE_STARTED_AT)
                        .remove(KEY_PAUSE_UNTIL)
                        .commit()
                ) return false
                preferences.edit()
                    .putBoolean(KEY_ENABLED, state.baseEnabled)
                    .commit()
            }
        }

        override fun writeAsync(state: CallBlockProtectionState) {
            // Only terminal normalization calls this path, so the durable base value is unchanged.
            runtimePreferences.edit()
                .remove(KEY_PAUSE_STARTED_AT)
                .remove(KEY_PAUSE_UNTIL)
                .apply()
        }
    }

    private fun SharedPreferences.optionalLong(key: String): Long? =
        if (contains(key)) getLong(key, 0L) else null

    private fun SharedPreferences.safeBoolean(key: String, default: Boolean): Boolean =
        runCatching { getBoolean(key, default) }.getOrDefault(default)

    private fun SharedPreferences.safeInt(key: String, default: Int): Int =
        runCatching { getInt(key, default) }.getOrDefault(default)

    private fun SharedPreferences.safeLong(key: String, default: Long): Long =
        runCatching { getLong(key, default) }.getOrDefault(default)

    private fun readRepeatUnknownCallerGuardConfigLocked(
        preferences: SharedPreferences,
        runtimePreferences: SharedPreferences,
    ): RepeatUnknownCallerGuardConfig {
        val storedThreshold = if (preferences.contains(KEY_REPEAT_UNKNOWN_CALLER_GUARD_THRESHOLD)) {
            runCatching { preferences.getInt(KEY_REPEAT_UNKNOWN_CALLER_GUARD_THRESHOLD, 0) }.getOrNull()
        } else {
            DEFAULT_REPEAT_UNKNOWN_CALLER_GUARD_THRESHOLD
        }
        val storedWindowMinutes = if (preferences.contains(KEY_REPEAT_UNKNOWN_CALLER_GUARD_WINDOW_MINUTES)) {
            runCatching { preferences.getInt(KEY_REPEAT_UNKNOWN_CALLER_GUARD_WINDOW_MINUTES, 0) }.getOrNull()
        } else {
            DEFAULT_REPEAT_UNKNOWN_CALLER_GUARD_WINDOW_MINUTES
        }
        val storedGeneration = if (runtimePreferences.contains(KEY_REPEAT_UNKNOWN_CALLER_GUARD_SESSION_GENERATION)) {
            runCatching {
                runtimePreferences.getLong(KEY_REPEAT_UNKNOWN_CALLER_GUARD_SESSION_GENERATION, 0L)
            }.getOrNull()
        } else {
            0L
        }
        val threshold = storedThreshold
            ?.takeIf(::isValidRepeatUnknownCallerGuardThreshold)
            ?: DEFAULT_REPEAT_UNKNOWN_CALLER_GUARD_THRESHOLD
        val windowMinutes = storedWindowMinutes
            ?.takeIf(::isValidRepeatUnknownCallerGuardWindowMinutes)
            ?: DEFAULT_REPEAT_UNKNOWN_CALLER_GUARD_WINDOW_MINUTES
        val storageIsValid = storedThreshold?.let(::isValidRepeatUnknownCallerGuardThreshold) == true &&
            storedWindowMinutes?.let(::isValidRepeatUnknownCallerGuardWindowMinutes) == true &&
            storedGeneration != null && storedGeneration >= 0L
        return RepeatUnknownCallerGuardConfig(
            // This policy can block a call without any user-authored rule. Corrupt persisted
            // parameters must therefore disable it instead of silently substituting aggressive
            // defaults. Missing parameters on a clean install still use the published defaults.
            enabled = storageIsValid && preferences.safeBoolean(KEY_REPEAT_UNKNOWN_CALLER_GUARD_ENABLED, false),
            threshold = threshold,
            windowMinutes = windowMinutes,
            sessionGeneration = storedGeneration?.coerceAtLeast(0L) ?: 0L,
        )
    }

    private fun publishRepeatUnknownCallerGuardStateLocked(
        preferences: SharedPreferences,
        runtimePreferences: SharedPreferences,
    ): RepeatUnknownCallerGuardConfig =
        readRepeatUnknownCallerGuardConfigLocked(preferences, runtimePreferences).also { config ->
            repeatUnknownCallerGuardEnabled = config.enabled
            repeatUnknownCallerGuardThreshold = config.threshold
            repeatUnknownCallerGuardWindowMinutes = config.windowMinutes
        }

    private fun advanceSessionGeneration(runtimePreferences: SharedPreferences): Boolean =
        runtimePreferences.edit()
            .putLong(
                KEY_REPEAT_UNKNOWN_CALLER_GUARD_SESSION_GENERATION,
                nextGeneration(
                    runtimePreferences.safeLong(KEY_REPEAT_UNKNOWN_CALLER_GUARD_SESSION_GENERATION, 0L)
                ),
            )
            .commit()

    /**
     * One-time fail-closed upgrade from the pre-split preference layout.
     *
     * Legacy pause/counter values must not be copied into the runtime-only file: the durable file
     * may already have crossed devices through Android Auto Backup before this version is launched.
     * Dropping them resets only temporary state and prevents an old pause from disabling blocking
     * on a restored device.
     */
    private fun discardLegacyTransientState(
        preferences: SharedPreferences,
        runtimePreferences: SharedPreferences,
    ) = synchronized(transientMigrationLock) {
        val legacyDurableKeys = listOf(
            KEY_PAUSE_STARTED_AT,
            KEY_PAUSE_UNTIL,
            LEGACY_REPEAT_UNKNOWN_CALLER_SESSION_GENERATION,
            LEGACY_REPEAT_UNKNOWN_CALLER_BYPASS_ENABLED,
            LEGACY_REPEAT_UNKNOWN_CALLER_BYPASS_THRESHOLD,
            LEGACY_REPEAT_UNKNOWN_CALLER_BYPASS_WINDOW_MINUTES,
        )
        val hasLegacyDurable = legacyDurableKeys.any(preferences::contains)
        val hasLegacyRuntime = runtimePreferences.contains(LEGACY_REPEAT_UNKNOWN_CALLER_SESSION_GENERATION)
        if (!hasLegacyDurable && !hasLegacyRuntime) return@synchronized

        // Memory removal is immediate; disk cleanup stays off the screening deadline.
        if (hasLegacyDurable) {
            preferences.edit().apply {
                legacyDurableKeys.forEach { key -> remove(key) }
            }.apply()
        }
        if (hasLegacyRuntime) {
            runtimePreferences.edit()
                .remove(LEGACY_REPEAT_UNKNOWN_CALLER_SESSION_GENERATION)
                .apply()
        }
    }

    private fun nextGeneration(current: Long): Long =
        if (current in 0 until Long.MAX_VALUE) current + 1L else 1L

    /** One-time source-compatible normalization of the removed global ALLOW method. */
    private fun normalizeLegacyAllowMethod(context: Context) {
        val preferences = prefs(context)
        if (CallBlockMethod.fromStorage(preferences.getString(KEY_BLOCK_METHOD, null)) != CallBlockMethod.ALLOW) {
            return
        }
        protectionCoordinator.setPermanentEnabled(protectionPersistence(context), false)
        preferences.edit()
            .putString(KEY_BLOCK_METHOD, CallBlockMethod.BLOCK_AND_REJECT.storageKey)
            .apply()
    }

}

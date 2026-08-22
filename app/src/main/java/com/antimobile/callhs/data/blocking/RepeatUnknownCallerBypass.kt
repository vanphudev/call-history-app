package com.antimobile.callhs.data.blocking

import android.content.Context
import android.content.SharedPreferences
import com.antimobile.callhs.util.PhoneKey

/** Persisted, locale-independent unknown-call guard configuration. */
data class RepeatUnknownCallerGuardConfig(
    val enabled: Boolean,
    val threshold: Int,
    val windowMinutes: Int,
    /** Persisted namespace/revision; never shown or backed up as user data. */
    val sessionGeneration: Long = 0L,
)

/** Result after atomically adding the current, distinct Telecom call attempt to the sliding window. */
data class RepeatUnknownCallerAttemptDecision(
    val attemptCount: Int,
    val reachedThreshold: Boolean,
    /** False means the event was not durably accepted and a no-rule gate must fail open. */
    val recorded: Boolean,
)

/**
 * Pure no-rule gate. Only a confirmed non-contact with a distinct recorded Telecom event may block;
 * every uncertain state is ALLOW because there is no user-authored rule to fall back to.
 */
object RepeatUnknownCallerGuardPolicy {
    fun shouldEvaluate(
        config: RepeatUnknownCallerGuardConfig,
        isPrivateNumber: Boolean,
        isVoip: Boolean,
    ): Boolean = config.enabled && !isPrivateNumber && !isVoip

    fun shouldBlock(
        config: RepeatUnknownCallerGuardConfig,
        attempt: RepeatUnknownCallerAttemptDecision,
        contactStatus: ContactLookupStatus,
        isPrivateNumber: Boolean = false,
        isVoip: Boolean = false,
    ): Boolean = shouldEvaluate(config, isPrivateNumber, isVoip) &&
        config.threshold in CallBlockSettings.REPEAT_UNKNOWN_CALLER_GUARD_THRESHOLD_PRESETS &&
        contactStatus == ContactLookupStatus.NOT_IN_CONTACTS &&
        attempt.recorded &&
        attempt.attemptCount in 1 until config.threshold

    fun syntheticMatch(
        config: RepeatUnknownCallerGuardConfig,
        attemptCount: Int,
        ruleSnapshotGeneration: Long? = null,
    ): CallBlockMatch {
        val displayValue = SpecialCallCondition.encode(setOf(SpecialCallCondition.UNKNOWN_CONTACT))
        val persistedValue = RepeatUnknownCallerGuardReasonCodec.encode(
            RepeatUnknownCallerGuardReason(
                attempt = attemptCount,
                threshold = config.threshold,
                windowMinutes = config.windowMinutes,
            )
        )
        return CallBlockMatch(
            rule = CallBlockRule(
                id = SYNTHETIC_RULE_ID,
                type = CallBlockRuleType.SPECIAL,
                rawValue = displayValue,
                matchValue = displayValue,
                enabled = true,
                createdAt = 0L,
            ),
            historyReasonType = REPEAT_UNKNOWN_CALLER_GUARD_REASON_TYPE,
            historyReasonValue = persistedValue,
            guardConfigSnapshot = config,
            action = CallBlockAction.BLOCK,
            decisionTier = CallBlockDecisionTier.UNKNOWN_CALLER_POLICY,
            ruleSnapshotGeneration = ruleSnapshotGeneration,
        )
    }

    private const val SYNTHETIC_RULE_ID = Long.MIN_VALUE
}

/** Minimal store abstraction keeps the concurrency/clock rules testable without Android. */
internal interface RepeatUnknownCallerAttemptStore {
    fun entries(): Map<String, String>

    /** All removals and the replacement value must become visible as one in-memory mutation. */
    fun update(removeKeys: Set<String>, putKey: String, putValue: String)

    fun clear()
}

/**
 * Process-local serialization plus an atomic store mutation prevents two simultaneous callbacks for
 * the same number from both observing the same previous count. The durable representation retains at
 * most four distinct event timestamps because the largest supported threshold is four.
 */
internal class RepeatUnknownCallerAttemptCounter(
    private val store: RepeatUnknownCallerAttemptStore,
    private val maxTrackedNumbers: Int = DEFAULT_MAX_TRACKED_NUMBERS,
) {
    private val lock = Any()

    fun record(
        phoneKey: String,
        eventId: Long,
        eventAtMillis: Long,
        nowMillis: Long,
        threshold: Int,
        windowMillis: Long,
        namespace: Long = 0L,
        canRecord: () -> Boolean = { true },
    ): RepeatUnknownCallerAttemptDecision = synchronized(lock) {
        if (
            phoneKey.isBlank() ||
            // Without Telecom's stable creation timestamp, an OEM retry could be mistaken for the
            // next call. A no-rule guard cannot safely block that uncertain event, so leave it
            // unrecorded and let the caller fail open.
            eventId <= 0L ||
            threshold !in CallBlockSettings.REPEAT_UNKNOWN_CALLER_GUARD_THRESHOLD_PRESETS ||
            windowMillis <= 0L ||
            nowMillis <= 0L
        ) {
            return@synchronized RepeatUnknownCallerAttemptDecision(0, false, recorded = false)
        }
        if (!canRecord()) {
            return@synchronized RepeatUnknownCallerAttemptDecision(0, false, recorded = false)
        }

        val allEntries = store.entries()
        val storageKey = storageKey(namespace, phoneKey)
        val cutoff = subtractSaturated(nowMillis, windowMillis)
        val observedAt = eventAtMillis.takeIf { it in cutoff..nowMillis } ?: nowMillis
        val stableEventId = eventId
        val retained = decode(allEntries[storageKey])
            .asSequence()
            .filter { it.observedAtMillis in cutoff..nowMillis }
            .distinctBy(Attempt::eventId)
            .toMutableList()

        if (retained.none { it.eventId == stableEventId }) {
            retained += Attempt(stableEventId, observedAt)
        }
        val canonical = retained
            .sortedWith(compareBy<Attempt> { it.observedAtMillis }.thenBy { it.eventId })
            .takeLast(MAX_ATTEMPTS_PER_NUMBER)

        val removals = if (allEntries.size >= maxTrackedNumbers && storageKey !in allEntries) {
            evictionKeys(
                entries = allEntries,
                protectedKey = storageKey,
                nowMillis = nowMillis,
                capacityAfterInsert = (maxTrackedNumbers - 1).coerceAtLeast(0),
            )
        } else {
            emptySet()
        }
        store.update(removals, storageKey, encode(canonical))

        RepeatUnknownCallerAttemptDecision(
            attemptCount = canonical.size,
            reachedThreshold = canonical.size >= threshold,
            recorded = true,
        )
    }

    fun clear() = synchronized(lock) { store.clear() }

    private fun evictionKeys(
        entries: Map<String, String>,
        protectedKey: String,
        nowMillis: Long,
        capacityAfterInsert: Int,
    ): Set<String> {
        val retentionCutoff = subtractSaturated(nowMillis, MAX_RETENTION_MILLIS)
        val ranked = entries.asSequence()
            .filter { (key, _) -> key.startsWith(NUMBER_PREFIX) && key != protectedKey }
            .map { (key, value) ->
                val latest = decode(value)
                    .map(Attempt::observedAtMillis)
                    .filter { it in 1L..nowMillis }
                    .maxOrNull() ?: Long.MIN_VALUE
                key to latest
            }
            .sortedWith(compareBy<Pair<String, Long>> { it.second }.thenBy { it.first })
            .toList()
        val expired = ranked.filter { (_, latest) -> latest < retentionCutoff }.mapTo(linkedSetOf()) { it.first }
        val remaining = ranked.filterNot { (key, _) -> key in expired }
        val extraCount = (remaining.size - capacityAfterInsert).coerceAtLeast(0)
        remaining.take(extraCount).mapTo(expired) { it.first }
        return expired
    }

    private data class Attempt(val eventId: Long, val observedAtMillis: Long)

    private companion object {
        const val NUMBER_PREFIX = "number:"
        const val MAX_ATTEMPTS_PER_NUMBER = 4
        const val DEFAULT_MAX_TRACKED_NUMBERS = 256
        const val MAX_RETENTION_MILLIS = 24L * 60L * 60L * 1000L

        fun storageKey(namespace: Long, phoneKey: String): String = "$NUMBER_PREFIX$namespace:$phoneKey"

        fun encode(attempts: List<Attempt>): String = attempts.joinToString(",") {
            "${it.eventId}:${it.observedAtMillis}"
        }

        fun decode(raw: String?): List<Attempt> {
            if (raw.isNullOrBlank()) return emptyList()
            return raw.split(',').mapNotNull { token ->
                val pieces = token.split(':', limit = 2)
                if (pieces.size != 2) return@mapNotNull null
                val eventId = pieces[0].toLongOrNull()?.takeIf { it > 0L } ?: return@mapNotNull null
                val observedAt = pieces[1].toLongOrNull()?.takeIf { it > 0L } ?: return@mapNotNull null
                Attempt(eventId, observedAt)
            }
        }

        fun subtractSaturated(value: Long, amount: Long): Long =
            if (amount > value) Long.MIN_VALUE else value - amount
    }
}

/** Runtime facade. There is deliberately one counter/lock for the app process. */
object RepeatUnknownCallerGuardTracker {
    private const val PREFS = "call_block_repeat_unknown_guard_attempts"

    @Volatile
    private var runtimeCounter: RepeatUnknownCallerAttemptCounter? = null

    fun recordUnknownAttempt(
        context: Context,
        number: String,
        eventId: Long,
        eventAtMillis: Long,
        nowMillis: Long,
        config: RepeatUnknownCallerGuardConfig,
    ): RepeatUnknownCallerAttemptDecision {
        val phoneKey = PhoneKey.of(number)
        if (
            phoneKey.isEmpty() ||
            !CallHistoryRuleCodec.isSelectableNumber(number) ||
            !config.enabled ||
            !CallBlockSettings.isValidRepeatUnknownCallerGuardThreshold(config.threshold) ||
            !CallBlockSettings.isValidRepeatUnknownCallerGuardWindowMinutes(config.windowMinutes)
        ) {
            return RepeatUnknownCallerAttemptDecision(0, false, recorded = false)
        }
        return counter(context).record(
            phoneKey = phoneKey,
            eventId = eventId,
            eventAtMillis = eventAtMillis,
            nowMillis = nowMillis,
            threshold = config.threshold,
            windowMillis = config.windowMinutes.toLong() * MILLIS_PER_MINUTE,
            namespace = config.sessionGeneration,
            // Evaluate under the same lock as the ledger mutation. If the user disables or edits
            // this permissive policy while a screening callback is in flight, that stale callback
            // cannot repopulate a just-cleared tracker or apply the old threshold/window.
            canRecord = {
                CallBlockSettings.repeatUnknownCallerGuardConfig(context) == config
            },
        )
    }

    fun clear(context: Context) {
        counter(context).clear()
    }

    private fun counter(context: Context): RepeatUnknownCallerAttemptCounter =
        runtimeCounter ?: synchronized(this) {
            runtimeCounter ?: RepeatUnknownCallerAttemptCounter(
                SharedPreferencesAttemptStore(
                    context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                )
            ).also { runtimeCounter = it }
        }

    private class SharedPreferencesAttemptStore(
        private val prefs: SharedPreferences,
    ) : RepeatUnknownCallerAttemptStore {
        override fun entries(): Map<String, String> = prefs.all.mapNotNull { (key, value) ->
            (value as? String)?.let { key to it }
        }.toMap()

        override fun update(removeKeys: Set<String>, putKey: String, putValue: String) {
            // apply() commits to SharedPreferences memory synchronously, so the next concurrent callback
            // observes this attempt without putting filesystem latency inside Telecom's response budget.
            prefs.edit().apply {
                removeKeys.forEach(::remove)
                putString(putKey, putValue)
            }.apply()
        }

        override fun clear() {
            prefs.edit().clear().apply()
        }
    }

    private const val MILLIS_PER_MINUTE = 60_000L
}

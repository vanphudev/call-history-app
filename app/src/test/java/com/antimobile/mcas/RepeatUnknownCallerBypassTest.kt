package com.antimobile.mcas

import com.antimobile.mcas.data.blocking.ContactLookupStatus
import com.antimobile.mcas.data.blocking.CallBlockHistoryReasonCodec
import com.antimobile.mcas.data.blocking.CallBlockSettings
import com.antimobile.mcas.data.blocking.CallBlockRuleType
import com.antimobile.mcas.data.blocking.REPEAT_UNKNOWN_CALLER_GUARD_REASON_TYPE
import com.antimobile.mcas.data.blocking.RepeatUnknownCallerAttemptCounter
import com.antimobile.mcas.data.blocking.RepeatUnknownCallerAttemptDecision
import com.antimobile.mcas.data.blocking.RepeatUnknownCallerAttemptStore
import com.antimobile.mcas.data.blocking.RepeatUnknownCallerGuardConfig
import com.antimobile.mcas.data.blocking.RepeatUnknownCallerGuardPolicy
import com.antimobile.mcas.data.blocking.RepeatUnknownCallerGuardReasonCodec
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RepeatUnknownCallerBypassTest {

    @Test
    fun currentAttemptCountsAndSecondAttemptReachesDefaultThreshold() {
        val counter = RepeatUnknownCallerAttemptCounter(MemoryStore())

        val first = counter.record("912345678", 101L, 1_000L, 1_000L, 2, 60_000L)
        val second = counter.record("912345678", 102L, 2_000L, 2_000L, 2, 60_000L)
        val third = counter.record("912345678", 103L, 3_000L, 3_000L, 2, 60_000L)

        assertEquals(1, first.attemptCount)
        assertTrue(first.recorded)
        assertFalse(first.reachedThreshold)
        assertEquals(2, second.attemptCount)
        assertTrue(second.reachedThreshold)
        assertEquals(3, third.attemptCount)
        assertTrue(third.reachedThreshold)
    }

    @Test
    fun inclusiveWindowBoundaryAndOneMillisecondPastBoundary() {
        val inclusive = RepeatUnknownCallerAttemptCounter(MemoryStore())
        inclusive.record("912345678", 1L, 100_000L, 100_000L, 2, 60_000L)
        val atBoundary = inclusive.record("912345678", 2L, 160_000L, 160_000L, 2, 60_000L)
        assertEquals(2, atBoundary.attemptCount)
        assertTrue(atBoundary.reachedThreshold)

        val exclusive = RepeatUnknownCallerAttemptCounter(MemoryStore())
        exclusive.record("912345678", 1L, 100_000L, 100_000L, 2, 60_000L)
        val pastBoundary = exclusive.record("912345678", 2L, 160_001L, 160_001L, 2, 60_000L)
        assertEquals(1, pastBoundary.attemptCount)
        assertFalse(pastBoundary.reachedThreshold)
    }

    @Test
    fun duplicateTelecomEventIsCountedOnce() {
        val counter = RepeatUnknownCallerAttemptCounter(MemoryStore())

        val first = counter.record("912345678", 42L, 1_000L, 1_000L, 2, 60_000L)
        val duplicate = counter.record("912345678", 42L, 1_000L, 1_050L, 2, 60_000L)

        assertEquals(1, first.attemptCount)
        assertEquals(1, duplicate.attemptCount)
        assertTrue(duplicate.recorded)
        assertFalse(duplicate.reachedThreshold)
    }

    @Test
    fun clearingTrackerPreventsAReenabledSessionFromInheritingAttempts() {
        val counter = RepeatUnknownCallerAttemptCounter(MemoryStore())
        counter.record("912345678", 1L, 1_000L, 1_000L, 2, 60_000L)

        counter.clear()
        val firstAfterReenable = counter.record("912345678", 2L, 2_000L, 2_000L, 2, 60_000L)

        assertEquals(1, firstAfterReenable.attemptCount)
        assertFalse(firstAfterReenable.reachedThreshold)
    }

    @Test
    fun persistedSessionNamespacePreventsReenableFromInheritingOldAttempts() {
        val store = MemoryStore()
        val counter = RepeatUnknownCallerAttemptCounter(store)
        counter.record(
            phoneKey = "912345678",
            eventId = 1L,
            eventAtMillis = 1_000L,
            nowMillis = 1_000L,
            threshold = 2,
            windowMillis = 60_000L,
            namespace = 41L,
        )

        val firstInNewSession = counter.record(
            phoneKey = "912345678",
            eventId = 2L,
            eventAtMillis = 2_000L,
            nowMillis = 2_000L,
            threshold = 2,
            windowMillis = 60_000L,
            namespace = 42L,
        )

        assertEquals(1, firstInNewSession.attemptCount)
        assertFalse(firstInNewSession.reachedThreshold)
        assertTrue("number:41:912345678" in store.entries())
        assertTrue("number:42:912345678" in store.entries())
    }

    @Test
    fun staleConfigCannotRepopulateTrackerAfterDisable() {
        val store = MemoryStore()
        val counter = RepeatUnknownCallerAttemptCounter(store)

        val ignored = counter.record(
            phoneKey = "912345678",
            eventId = 1L,
            eventAtMillis = 1_000L,
            nowMillis = 1_000L,
            threshold = 2,
            windowMillis = 60_000L,
            canRecord = { false },
        )

        assertEquals(0, ignored.attemptCount)
        assertFalse(ignored.reachedThreshold)
        assertTrue(store.entries().isEmpty())
    }

    @Test
    fun concurrentCallbacksCannotLoseInMemoryAttempts() {
        val counter = RepeatUnknownCallerAttemptCounter(MemoryStore())
        val executor = Executors.newFixedThreadPool(8)
        val start = CountDownLatch(1)
        val done = CountDownLatch(32)
        val decisions = Collections.synchronizedList(mutableListOf<Boolean>())

        repeat(32) { index ->
            executor.execute {
                start.await()
                val decision = counter.record(
                    phoneKey = "912345678",
                    eventId = (index + 1).toLong(),
                    eventAtMillis = 10_000L + index,
                    nowMillis = 20_000L,
                    threshold = 4,
                    windowMillis = 60_000L,
                )
                decisions += decision.reachedThreshold
                done.countDown()
            }
        }
        start.countDown()

        assertTrue(done.await(5, TimeUnit.SECONDS))
        executor.shutdownNow()
        assertEquals(32, decisions.size)
        assertEquals(3, decisions.count { !it })
        assertEquals(29, decisions.count { it })
    }

    @Test
    fun corruptLedgerRecoversAndInvalidInputsRemainUnrecorded() {
        val store = MemoryStore(mutableMapOf("number:0:912345678" to "broken,1:nope,-2:3"))
        val counter = RepeatUnknownCallerAttemptCounter(store)

        val recovered = counter.record("912345678", 10L, 1_000L, 1_000L, 2, 60_000L)
        val invalidThreshold = counter.record("912345678", 11L, 2_000L, 2_000L, 5, 60_000L)
        val missingStableEventId = counter.record("912345678", 0L, 3_000L, 3_000L, 2, 60_000L)

        assertEquals(1, recovered.attemptCount)
        assertFalse(recovered.reachedThreshold)
        assertEquals(0, invalidThreshold.attemptCount)
        assertFalse(invalidThreshold.reachedThreshold)
        assertEquals(0, missingStableEventId.attemptCount)
        assertFalse(missingStableEventId.reachedThreshold)
    }

    @Test
    fun ledgerIsBoundedAcrossManyDifferentNumbers() {
        val store = MemoryStore()
        val counter = RepeatUnknownCallerAttemptCounter(store, maxTrackedNumbers = 3)

        repeat(20) { index ->
            counter.record(
                phoneKey = "900000${index.toString().padStart(3, '0')}",
                eventId = (index + 1).toLong(),
                eventAtMillis = 1_000L + index,
                nowMillis = 2_000L,
                threshold = 2,
                windowMillis = 60_000L,
            )
        }

        assertTrue(store.entries().size <= 3)
        assertTrue("number:0:900000019" in store.entries())
    }

    @Test
    fun guardBlocksOnlyRecordedConfirmedUnknownCallsBeforeThreshold() {
        val enabled = RepeatUnknownCallerGuardConfig(enabled = true, threshold = 2, windowMinutes = 15)
        val disabled = enabled.copy(enabled = false)
        val first = RepeatUnknownCallerAttemptDecision(
            attemptCount = 1,
            reachedThreshold = false,
            recorded = true,
        )
        val threshold = RepeatUnknownCallerAttemptDecision(
            attemptCount = 2,
            reachedThreshold = true,
            recorded = true,
        )
        val uncertain = first.copy(recorded = false)

        assertTrue(RepeatUnknownCallerGuardPolicy.shouldBlock(enabled, first, ContactLookupStatus.NOT_IN_CONTACTS))
        assertFalse(RepeatUnknownCallerGuardPolicy.shouldBlock(enabled, threshold, ContactLookupStatus.NOT_IN_CONTACTS))
        assertFalse(RepeatUnknownCallerGuardPolicy.shouldBlock(enabled, first, ContactLookupStatus.IN_CONTACTS))
        assertFalse(RepeatUnknownCallerGuardPolicy.shouldBlock(enabled, first, ContactLookupStatus.UNKNOWN))
        assertFalse(RepeatUnknownCallerGuardPolicy.shouldBlock(enabled, uncertain, ContactLookupStatus.NOT_IN_CONTACTS))
        assertFalse(RepeatUnknownCallerGuardPolicy.shouldBlock(disabled, first, ContactLookupStatus.NOT_IN_CONTACTS))
        assertFalse(
            RepeatUnknownCallerGuardPolicy.shouldBlock(
                enabled,
                first,
                ContactLookupStatus.NOT_IN_CONTACTS,
                isPrivateNumber = true,
            )
        )
        assertFalse(
            RepeatUnknownCallerGuardPolicy.shouldBlock(
                enabled,
                first,
                ContactLookupStatus.NOT_IN_CONTACTS,
                isVoip = true,
            )
        )
    }

    @Test
    fun syntheticGuardMatchKeepsDedicatedPortableHistoryReason() {
        val config = RepeatUnknownCallerGuardConfig(enabled = true, threshold = 3, windowMinutes = 20)

        val match = RepeatUnknownCallerGuardPolicy.syntheticMatch(config, attemptCount = 2)
        val decoded = RepeatUnknownCallerGuardReasonCodec.decode(match.historyReasonValue)

        assertEquals(CallBlockRuleType.SPECIAL, match.rule.type)
        assertEquals(REPEAT_UNKNOWN_CALLER_GUARD_REASON_TYPE, match.historyReasonType)
        assertEquals(config, match.guardConfigSnapshot)
        assertEquals(2, decoded?.attempt)
        assertEquals(3, decoded?.threshold)
        assertEquals(20, decoded?.windowMinutes)
        assertTrue(
            CallBlockHistoryReasonCodec.isSupported(
                match.historyReasonType,
                match.historyReasonValue,
            )
        )
        assertFalse(
            CallBlockHistoryReasonCodec.isSupported(
                REPEAT_UNKNOWN_CALLER_GUARD_REASON_TYPE,
                "v1|3|3|20",
            )
        )
        assertEquals(null, RepeatUnknownCallerGuardReasonCodec.decode("v1|3|3|20"))
        assertEquals(null, RepeatUnknownCallerGuardReasonCodec.decode("future|1|2|15"))
    }

    @Test
    fun settingsValidationAcceptsOnlyPublishedPresetsAndWindowRange() {
        assertEquals(listOf(2, 3, 4), CallBlockSettings.REPEAT_UNKNOWN_CALLER_GUARD_THRESHOLD_PRESETS)
        assertTrue(CallBlockSettings.isValidRepeatUnknownCallerGuardThreshold(2))
        assertTrue(CallBlockSettings.isValidRepeatUnknownCallerGuardThreshold(4))
        assertFalse(CallBlockSettings.isValidRepeatUnknownCallerGuardThreshold(1))
        assertFalse(CallBlockSettings.isValidRepeatUnknownCallerGuardThreshold(5))

        assertTrue(CallBlockSettings.isValidRepeatUnknownCallerGuardWindowMinutes(1))
        assertTrue(CallBlockSettings.isValidRepeatUnknownCallerGuardWindowMinutes(1_440))
        assertFalse(CallBlockSettings.isValidRepeatUnknownCallerGuardWindowMinutes(0))
        assertFalse(CallBlockSettings.isValidRepeatUnknownCallerGuardWindowMinutes(1_441))
    }

    private class MemoryStore(
        initial: MutableMap<String, String> = linkedMapOf(),
    ) : RepeatUnknownCallerAttemptStore {
        private val values = initial

        override fun entries(): Map<String, String> = values.toMap()

        override fun update(removeKeys: Set<String>, putKey: String, putValue: String) {
            removeKeys.forEach(values::remove)
            values[putKey] = putValue
        }

        override fun clear() {
            values.clear()
        }
    }
}

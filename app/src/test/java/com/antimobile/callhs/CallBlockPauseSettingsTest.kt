package com.antimobile.callhs

import com.antimobile.callhs.data.blocking.CallBlockPauseDuration
import com.antimobile.callhs.data.blocking.CallBlockProtectionCoordinator
import com.antimobile.callhs.data.blocking.CallBlockProtectionPersistence
import com.antimobile.callhs.data.blocking.CallBlockProtectionState
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CallBlockPauseSettingsTest {

    @Test
    fun pauseUsesHalfOpenClockBoundaryAndAutomaticallyReEnablesAtDeadline() {
        val startedAt = 1_000_000L
        val state = CallBlockProtectionState()
            .withPause(CallBlockPauseDuration.MINUTES_10, startedAt)
        val until = startedAt + 10L * 60_000L

        assertEquals(startedAt, state.pauseStartedAtMillis)
        assertEquals(until, state.pauseUntilMillis)
        assertFalse(state.isPausedAt(startedAt - 1L))
        assertTrue(state.isEffectivelyEnabledAt(startedAt - 1L))
        assertNull(state.normalizedAt(startedAt - 1L).pauseStartedAtMillis)
        assertNull(state.normalizedAt(startedAt - 1L).pauseUntilMillis)
        assertTrue(state.isPausedAt(startedAt))
        assertFalse(state.isEffectivelyEnabledAt(startedAt))
        assertEquals(1L, state.remainingPauseMillisAt(until - 1L))

        assertFalse(state.isPausedAt(until))
        assertTrue(state.isEffectivelyEnabledAt(until))
        assertEquals(0L, state.remainingPauseMillisAt(until))
        assertNull(state.normalizedAt(until).pauseStartedAtMillis)
        assertNull(state.normalizedAt(until).pauseUntilMillis)
    }

    @Test
    fun explicitPermanentChoiceClearsPauseAndNewPauseEnablesBaseState() {
        val paused = CallBlockProtectionState()
            .withPause(CallBlockPauseDuration.MINUTES_30, nowMillis = 2_000L)

        val permanentlyOff = paused.withPermanentEnabled(false)
        assertFalse(permanentlyOff.baseEnabled)
        assertFalse(permanentlyOff.isEffectivelyEnabledAt(2_001L))
        assertNull(permanentlyOff.pauseStartedAtMillis)
        assertNull(permanentlyOff.pauseUntilMillis)

        val pausedAgain = permanentlyOff.withPause(
            CallBlockPauseDuration.MINUTES_60,
            nowMillis = 3_000L,
        )
        assertTrue(pausedAgain.baseEnabled)
        assertTrue(pausedAgain.isPausedAt(3_000L))
        assertEquals(3_000L + 60L * 60_000L, pausedAgain.pauseUntilMillis)

        val resumed = pausedAgain.withoutPause()
        assertTrue(resumed.baseEnabled)
        assertTrue(resumed.isEffectivelyEnabledAt(3_001L))
        assertNull(resumed.pauseStartedAtMillis)
        assertNull(resumed.pauseUntilMillis)
    }

    @Test
    fun normalizedPauseIsPersistentlyClearedAndCannotReviveAfterClockRollback() {
        val coordinator = CallBlockProtectionCoordinator()
        val startedAt = 100_000L
        val until = startedAt + CallBlockPauseDuration.MINUTES_10.durationMillis
        val scheduled = CallBlockProtectionState().withPause(
            CallBlockPauseDuration.MINUTES_10,
            startedAt,
        )

        val beforeStart = YieldingPersistence(scheduled)
        assertNull(coordinator.read(beforeStart, startedAt - 1L).pauseStartedAtMillis)
        assertFalse(coordinator.read(beforeStart, startedAt).isPausedAt(startedAt))

        val expired = YieldingPersistence(scheduled)
        assertNull(coordinator.read(expired, until).pauseStartedAtMillis)
        assertFalse(coordinator.read(expired, until - 1L).isPausedAt(until - 1L))
    }

    @Test
    fun screeningNormalizationNeverCallsTheDurableWriter() {
        val startedAt = 100_000L
        val persistence = NormalizationPersistence(
            CallBlockProtectionState().withPause(
                CallBlockPauseDuration.MINUTES_10,
                startedAt,
            )
        )
        val coordinator = CallBlockProtectionCoordinator()

        val normalized = coordinator.read(
            persistence,
            startedAt + CallBlockPauseDuration.MINUTES_10.durationMillis,
        )

        assertNull(normalized.pauseStartedAtMillis)
        assertEquals(0, persistence.durableWriteCount)
        assertEquals(1, persistence.asyncWriteCount)
    }

    @Test
    fun pausePresetsHaveStableDurations() {
        assertEquals(10L * 60_000L, CallBlockPauseDuration.MINUTES_10.durationMillis)
        assertEquals(30L * 60_000L, CallBlockPauseDuration.MINUTES_30.durationMillis)
        assertEquals(60L * 60_000L, CallBlockPauseDuration.MINUTES_60.durationMillis)
    }

    @Test
    fun coordinatorPreventsTornPauseStateDuringConcurrentReadsAndWrites() {
        val coordinator = CallBlockProtectionCoordinator()
        val persistence = YieldingPersistence(CallBlockProtectionState())
        val pool = Executors.newFixedThreadPool(8)
        val start = CountDownLatch(1)
        val sequence = AtomicLong(1L)

        val tasks = List(8) { worker ->
            pool.submit {
                start.await()
                repeat(400) { iteration ->
                    if ((worker + iteration) % 4 == 0) {
                        coordinator.setPermanentEnabled(persistence, value = false)
                    } else {
                        val now = sequence.getAndIncrement() * 10_000_000L
                        coordinator.pause(
                            persistence,
                            CallBlockPauseDuration.entries[(worker + iteration) % 3],
                            nowMillis = now,
                        )
                    }

                    assertAtomicPauseState(
                        coordinator.readRaw(persistence)
                    )
                }
            }
        }

        start.countDown()
        tasks.forEach { it.get(15, TimeUnit.SECONDS) }
        pool.shutdown()
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS))
        assertAtomicPauseState(coordinator.readRaw(persistence))
    }

    @Test
    fun failedPersistenceNeverPublishesRequestedState() {
        val initial = CallBlockProtectionState(baseEnabled = true)
        val persistence = MutatingFailingPersistence(initial)
        val coordinator = CallBlockProtectionCoordinator()

        val result = coordinator.pause(
            persistence,
            CallBlockPauseDuration.MINUTES_10,
            nowMillis = 10_000L,
        )

        assertEquals(initial, result)
        assertEquals(initial, persistence.read())
    }

    private fun assertAtomicPauseState(state: CallBlockProtectionState) {
        val startedAt = state.pauseStartedAtMillis
        val until = state.pauseUntilMillis
        if (!state.baseEnabled) {
            assertNull(startedAt)
            assertNull(until)
            return
        }
        if (startedAt == null || until == null) {
            assertNull(startedAt)
            assertNull(until)
            return
        }

        assertTrue(
            until - startedAt in CallBlockPauseDuration.entries.map { it.durationMillis }
        )
    }

    /**
     * Deliberately stores fields separately and yields between them. Only the coordinator's lock
     * prevents a reader from observing a mixture of two writes.
     */
    private class YieldingPersistence(
        initial: CallBlockProtectionState,
    ) : CallBlockProtectionPersistence {
        private var baseEnabled = initial.baseEnabled
        private var pauseStartedAtMillis = initial.pauseStartedAtMillis
        private var pauseUntilMillis = initial.pauseUntilMillis

        override fun read(): CallBlockProtectionState {
            val base = baseEnabled
            Thread.yield()
            val startedAt = pauseStartedAtMillis
            Thread.yield()
            val until = pauseUntilMillis
            return CallBlockProtectionState(base, startedAt, until)
        }

        override fun write(state: CallBlockProtectionState): Boolean {
            writeFields(state)
            return true
        }

        override fun writeAsync(state: CallBlockProtectionState) {
            writeFields(state)
        }

        private fun writeFields(state: CallBlockProtectionState) {
            baseEnabled = state.baseEnabled
            Thread.yield()
            pauseStartedAtMillis = state.pauseStartedAtMillis
            Thread.yield()
            pauseUntilMillis = state.pauseUntilMillis
        }
    }

    /** Models commitToMemory succeeding while the durable SharedPreferences write reports failure. */
    private class MutatingFailingPersistence(
        initial: CallBlockProtectionState,
    ) : CallBlockProtectionPersistence {
        private var state = initial

        override fun read(): CallBlockProtectionState = state

        override fun write(state: CallBlockProtectionState): Boolean {
            this.state = state
            return false
        }

        override fun writeAsync(state: CallBlockProtectionState) {
            this.state = state
        }
    }

    private class NormalizationPersistence(
        initial: CallBlockProtectionState,
    ) : CallBlockProtectionPersistence {
        private var state = initial
        var durableWriteCount = 0
            private set
        var asyncWriteCount = 0
            private set

        override fun read(): CallBlockProtectionState = state

        override fun write(state: CallBlockProtectionState): Boolean {
            durableWriteCount++
            error("Durable writer must not be used by read normalization")
        }

        override fun writeAsync(state: CallBlockProtectionState) {
            asyncWriteCount++
            this.state = state
        }
    }
}

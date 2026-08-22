package com.antimobile.callhs

import com.antimobile.callhs.data.blocking.CallBlockMethod
import com.antimobile.callhs.data.blocking.CallResponsePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallBlockPolicyTest {

    @Test
    fun blockAndRejectMapsToSupportedBlockingFlags() {
        val policy = CallResponsePolicy.forMethod(CallBlockMethod.BLOCK_AND_REJECT)

        assertTrue(policy.disallowCall)
        assertTrue(policy.rejectCall)
        assertFalse(policy.silenceCall)
        assertTrue(policy.skipNotification)
        assertTrue(policy.blocksCall)
    }

    @Test
    fun blockWithoutRejectDisallowsWithoutManualRejectSignal() {
        val policy = CallResponsePolicy.forMethod(CallBlockMethod.BLOCK_WITHOUT_REJECT)

        assertTrue(policy.disallowCall)
        assertFalse(policy.rejectCall)
        assertFalse(policy.silenceCall)
        assertTrue(policy.skipNotification)
        assertTrue(policy.blocksCall)
    }

    @Test
    fun silenceAndAllowNeverProduceBlockedEvents() {
        val silence = CallResponsePolicy.forMethod(CallBlockMethod.SILENCE_ONLY)
        val allow = CallResponsePolicy.forMethod(CallBlockMethod.ALLOW)

        assertEquals(
            CallResponsePolicy(false, false, true, false),
            silence,
        )
        assertFalse(silence.blocksCall)
        assertEquals(
            CallResponsePolicy(false, false, false, false),
            allow,
        )
        assertFalse(allow.blocksCall)
    }

    @Test
    fun finalAuthoritativeSettingsCanCancelOrChangeAStaleMatch() {
        assertEquals(
            null,
            CallResponsePolicy.forActiveBlocking(
                enabled = false,
                method = CallBlockMethod.BLOCK_AND_REJECT,
            ),
        )
        assertEquals(
            null,
            CallResponsePolicy.forActiveBlocking(
                enabled = true,
                method = CallBlockMethod.ALLOW,
            ),
        )
        assertEquals(
            CallResponsePolicy.forMethod(CallBlockMethod.SILENCE_ONLY),
            CallResponsePolicy.forActiveBlocking(
                enabled = true,
                method = CallBlockMethod.SILENCE_ONLY,
            ),
        )
    }

    @Test
    fun storageFallbackKeepsLegacyBlockingBehaviour() {
        assertEquals(CallBlockMethod.BLOCK_AND_REJECT, CallBlockMethod.fromStorage(null))
        assertEquals(CallBlockMethod.BLOCK_AND_REJECT, CallBlockMethod.fromStorage("future_value"))
        assertEquals(CallBlockMethod.SILENCE_ONLY, CallBlockMethod.fromStorage("silence_only"))
    }
}

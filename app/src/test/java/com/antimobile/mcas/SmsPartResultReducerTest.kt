package com.antimobile.mcas

import com.antimobile.mcas.data.messaging.transport.SmsPartResultReducer
import com.antimobile.mcas.data.messaging.transport.SmsPartSendResolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsPartResultReducerTest {
    @Test
    fun remainsPendingUntilEveryPartHasCallback() {
        assertTrue(SmsPartResultReducer.sent(listOf(0, null, 0), 0) is SmsPartSendResolution.Pending)
    }

    @Test
    fun succeedsOnlyWhenEveryPartSucceeded() {
        assertTrue(SmsPartResultReducer.sent(listOf(0, 0, 0), 0) is SmsPartSendResolution.Success)
    }

    @Test
    fun returnsFirstFailedPart() {
        assertEquals(
            SmsPartSendResolution.Failure(1),
            SmsPartResultReducer.sent(listOf(0, 7, 8), 0),
        )
    }
}

package com.antimobile.callhs.data.messaging.transport

import android.telephony.SmsMessage
import com.antimobile.callhs.data.messaging.model.SmsSegmentInfo

object SmsSegmentCalculator {
    fun calculate(text: String): SmsSegmentInfo {
        if (text.isEmpty()) return SmsSegmentInfo(parts = 0, remainingInPart = 160, codeUnitSize = 1)
        val result = SmsMessage.calculateLength(text, false)
        return SmsSegmentInfo(
            parts = result.getOrElse(0) { 1 }.coerceAtLeast(1),
            remainingInPart = result.getOrElse(2) { 0 }.coerceAtLeast(0),
            codeUnitSize = result.getOrElse(3) { 1 },
        )
    }
}


package com.antimobile.mcas.data.messaging.transport

/** Quyết định trạng thái chung của một SMS nhiều phần từ callback sent của từng phần. */
sealed interface SmsPartSendResolution {
    data object Pending : SmsPartSendResolution
    data object Success : SmsPartSendResolution
    data class Failure(val partIndex: Int) : SmsPartSendResolution
}

object SmsPartResultReducer {
    fun sent(results: List<Int?>, successCode: Int): SmsPartSendResolution {
        if (results.isEmpty() || results.any { it == null }) return SmsPartSendResolution.Pending
        val failedIndex = results.indexOfFirst { it != successCode }
        return if (failedIndex < 0) SmsPartSendResolution.Success else SmsPartSendResolution.Failure(failedIndex)
    }
}

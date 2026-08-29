package com.antimobile.mcas.data.outgoing

import android.net.Uri
import android.telecom.CallRedirectionService
import android.telecom.PhoneAccountHandle
import android.util.Log

/**
 * Điểm nhận cuộc gọi đi đáng tin cậy trên Android 10+. Dịch vụ không chuyển hướng và không trì hoãn
 * cuộc gọi: Telecom được cho tiếp tục nguyên trạng trước khi công việc phân tích chạy bất đồng bộ.
 */
class OutgoingCallRedirectionService : CallRedirectionService() {
    override fun onPlaceCall(
        handle: Uri,
        initialPhoneAccount: PhoneAccountHandle,
        allowInteractiveResponse: Boolean,
    ) {
        // Keep this as the first executable statement. Telecom cancels the user's outgoing call if
        // its redirection adapter does not receive a response within five seconds.
        try {
            placeCallUnmodified()
        } catch (error: RuntimeException) {
            Log.e(LOG_TAG, "Unable to release outgoing call unchanged", error)
            return
        }

        // Everything observational is handed to the default app process only after Telecom has
        // accepted the unchanged call. Settings and Room must not be read in this lightweight process.
        OutgoingCallPostResponseReceiver.enqueue(
            context = applicationContext,
            handle = handle,
            phoneAccount = initialPhoneAccount,
        )
    }

    override fun onRedirectionTimeout() {
        Log.e(LOG_TAG, "Telecom timed out before receiving the unchanged-call response")
    }

    private companion object {
        const val LOG_TAG = "OutgoingCallRedirect"
    }
}

package com.antimobile.callhs.data.outgoing

import android.net.Uri
import android.os.SystemClock
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
        val startedAt = SystemClock.elapsedRealtime()
        // This feature is observational only. Never turn an exceptional framework response into an
        // explicit cancellation of the user's call; Telecom owns its own timeout/error handling.
        runCatching { placeCallUnmodified() }
            .onFailure { error ->
                Log.e(LOG_TAG, "Unable to release outgoing call unchanged", error)
            }

        OutgoingCallAlertDispatcher.onOutgoingCall(
            context = applicationContext,
            handle = handle,
            phoneAccount = initialPhoneAccount,
            createdAtMillis = System.currentTimeMillis(),
            source = OutgoingCallEventSource.REDIRECTION,
        )
        Log.i(
            LOG_TAG,
            "Outgoing callback released in ${SystemClock.elapsedRealtime() - startedAt} ms; " +
                "interactive=$allowInteractiveResponse",
        )
    }

    private companion object {
        const val LOG_TAG = "OutgoingCallRedirect"
    }
}

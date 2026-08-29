package com.antimobile.mcas.data.outgoing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telecom.PhoneAccountHandle
import android.util.Log

/**
 * Receives observational work only after the dedicated Telecom process has released the call.
 * This component intentionally runs in the default app process, where preferences remain
 * single-process and current.
 */
class OutgoingCallPostResponseReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_PROCESS_RELEASED_CALL) return
        val handle = intent.data ?: return
        val phoneAccount = intent.phoneAccountExtra() ?: return

        // This call only enqueues onto Dispatchers.IO; no settings, Room, SIM or UI work is allowed
        // on the receiver's main thread.
        OutgoingCallAlertDispatcher.onOutgoingCall(
            context = context.applicationContext,
            handle = handle,
            phoneAccount = phoneAccount,
            createdAtMillis = intent.getLongExtra(EXTRA_CREATED_AT_MILLIS, 0L),
            source = OutgoingCallEventSource.REDIRECTION,
        )
    }

    private fun Intent.phoneAccountExtra(): PhoneAccountHandle? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(EXTRA_PHONE_ACCOUNT, PhoneAccountHandle::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(EXTRA_PHONE_ACCOUNT)
        }

    companion object {
        private const val LOG_TAG = "OutgoingCallPostWork"
        private const val ACTION_PROCESS_RELEASED_CALL =
            "com.antimobile.mcas.action.PROCESS_RELEASED_OUTGOING_CALL"
        private const val EXTRA_PHONE_ACCOUNT = "phone_account"
        private const val EXTRA_CREATED_AT_MILLIS = "created_at_millis"

        /** Called only after placeCallUnmodified() has succeeded. Losing an alert must never lose a call. */
        fun enqueue(
            context: Context,
            handle: Uri,
            phoneAccount: PhoneAccountHandle,
        ) {
            val intent = Intent(context, OutgoingCallPostResponseReceiver::class.java).apply {
                action = ACTION_PROCESS_RELEASED_CALL
                data = handle
                putExtra(EXTRA_PHONE_ACCOUNT, phoneAccount)
                putExtra(EXTRA_CREATED_AT_MILLIS, System.currentTimeMillis())
                addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            }
            runCatching { context.sendBroadcast(intent) }
                .onFailure { error ->
                    Log.e(LOG_TAG, "Unable to enqueue post-response outgoing-call work", error)
                }
        }
    }
}

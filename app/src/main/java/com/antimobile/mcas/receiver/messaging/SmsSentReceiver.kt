package com.antimobile.mcas.receiver.messaging

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.antimobile.mcas.data.messaging.transport.SmsCallbackContract
import com.antimobile.mcas.data.messaging.transport.SmsCallbackProcessor
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors

class SmsSentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != SmsCallbackContract.ACTION_SENT) return
        val attemptId = intent.getStringExtra(SmsCallbackContract.EXTRA_ATTEMPT_ID) ?: return
        val partIndex = intent.getIntExtra(SmsCallbackContract.EXTRA_PART_INDEX, -1).takeIf { it >= 0 } ?: return
        val callbackResult = resultCode
        val radioError = intent.getIntExtra("errorCode", Int.MIN_VALUE).takeIf { it != Int.MIN_VALUE }
        val pending = goAsync()
        EXECUTOR.execute {
            try {
                runBlocking { SmsCallbackProcessor.onSent(context.applicationContext, attemptId, partIndex, callbackResult, radioError) }
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val EXECUTOR = Executors.newSingleThreadExecutor { r -> Thread(r, "mcas-sms-sent") }
    }
}

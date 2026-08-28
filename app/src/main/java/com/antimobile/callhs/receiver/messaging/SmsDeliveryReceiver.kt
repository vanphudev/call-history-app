package com.antimobile.callhs.receiver.messaging

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.antimobile.callhs.data.messaging.transport.SmsCallbackContract
import com.antimobile.callhs.data.messaging.transport.SmsCallbackProcessor
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors

class SmsDeliveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != SmsCallbackContract.ACTION_DELIVERED) return
        val attemptId = intent.getStringExtra(SmsCallbackContract.EXTRA_ATTEMPT_ID) ?: return
        val partIndex = intent.getIntExtra(SmsCallbackContract.EXTRA_PART_INDEX, -1).takeIf { it >= 0 } ?: return
        val callbackResult = resultCode
        val pending = goAsync()
        EXECUTOR.execute {
            try {
                runBlocking { SmsCallbackProcessor.onDelivered(context.applicationContext, attemptId, partIndex, callbackResult) }
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val EXECUTOR = Executors.newSingleThreadExecutor { r -> Thread(r, "callhs-sms-delivery") }
    }
}


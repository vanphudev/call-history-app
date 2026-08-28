package com.antimobile.callhs.receiver.messaging

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.antimobile.callhs.data.messaging.inbound.InboundSmsProcessor
import java.util.concurrent.Executors

class SmsDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return
        val pending = goAsync()
        EXECUTOR.execute {
            try {
                runCatching { InboundSmsProcessor.process(context.applicationContext, intent) }
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val EXECUTOR = Executors.newSingleThreadExecutor { r -> Thread(r, "callhs-sms-inbound") }
    }
}


package com.antimobile.mcas.receiver.messaging

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.antimobile.mcas.data.messaging.inbound.InboundSmsProcessor
import java.util.concurrent.Executors

class SmsDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return
        val pending = goAsync()
        EXECUTOR.execute {
            try {
                runCatching { InboundSmsProcessor.process(context.applicationContext, intent) }
                    // Không ghi số điện thoại/nội dung/PDU; chỉ giữ stack trace kỹ thuật để chẩn đoán
                    // trường hợp SMS_DELIVER đã tới nhưng Provider/Room thất bại.
                    .onFailure { error -> Log.e(TAG, "Unable to persist inbound SMS", error) }
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "MCAS-InboundSms"
        val EXECUTOR = Executors.newSingleThreadExecutor { r -> Thread(r, "mcas-sms-inbound") }
    }
}

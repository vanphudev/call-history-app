package com.antimobile.callhs.receiver.messaging

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.antimobile.callhs.data.messaging.notification.MessageNotifier

/** Sàn alpha: cảnh báo rõ MMS chưa được tải, không im lặng nuốt WAP push. */
class MmsDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION) {
            MessageNotifier.notifyMmsUnsupported(context.applicationContext)
        }
    }
}


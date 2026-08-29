package com.antimobile.mcas.receiver.messaging

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.antimobile.mcas.data.messaging.notification.MessageNotifier
import com.antimobile.mcas.data.messaging.provider.TelephonyMessageRepository
import java.util.concurrent.Executors

class MarkConversationReadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val threadId = intent.getLongExtra(MessageNotifier.EXTRA_THREAD_ID, -1L).takeIf { it >= 0L } ?: return
        val pending = goAsync()
        EXECUTOR.execute {
            try {
                runCatching { TelephonyMessageRepository(context.applicationContext).markThreadRead(threadId) }
                MessageNotifier.cancelThread(context, threadId)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val EXECUTOR = Executors.newSingleThreadExecutor { r -> Thread(r, "mcas-sms-read") }
    }
}


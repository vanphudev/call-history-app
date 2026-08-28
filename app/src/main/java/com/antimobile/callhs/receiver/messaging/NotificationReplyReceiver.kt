package com.antimobile.callhs.receiver.messaging

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.antimobile.callhs.data.messaging.model.SendMessageResult
import com.antimobile.callhs.data.messaging.notification.MessageNotifier
import com.antimobile.callhs.data.messaging.provider.TelephonyMessageRepository
import com.antimobile.callhs.data.messaging.sim.MessagingSimRepository
import com.antimobile.callhs.data.messaging.transport.SmsSendCoordinator
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors

class NotificationReplyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val text = RemoteInput.getResultsFromIntent(intent)?.getCharSequence(MessageNotifier.REMOTE_INPUT_KEY)
            ?.toString()?.takeIf(String::isNotBlank) ?: return
        val threadId = intent.getLongExtra(MessageNotifier.EXTRA_THREAD_ID, -1L).takeIf { it >= 0L } ?: return
        val address = intent.getStringExtra(MessageNotifier.EXTRA_ADDRESS)?.takeIf(String::isNotBlank) ?: return
        val explicitSubId = intent.getIntExtra(MessageNotifier.EXTRA_SUB_ID, -1).takeIf { it >= 0 }
        val pending = goAsync()
        EXECUTOR.execute {
            try {
                runBlocking {
                    val subId = MessagingSimRepository(context).resolve(threadId, explicitSubId)
                    val result = if (subId == null) null else SmsSendCoordinator(context).send(address, text, subId)
                    if (result is SendMessageResult.Queued) {
                        runCatching { TelephonyMessageRepository(context).markThreadRead(threadId) }
                        MessageNotifier.cancelThread(context, threadId)
                    } else {
                        MessageNotifier.notifyReplyFailed(context, threadId, address)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val EXECUTOR = Executors.newSingleThreadExecutor { r -> Thread(r, "callhs-sms-reply") }
    }
}

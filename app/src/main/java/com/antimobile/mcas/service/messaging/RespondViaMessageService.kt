package com.antimobile.mcas.service.messaging

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.antimobile.mcas.data.messaging.provider.TelephonyMessageRepository
import com.antimobile.mcas.data.messaging.sim.MessagingSimRepository
import com.antimobile.mcas.data.messaging.transport.SmsSendCoordinator
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors

/** Xử lý "Từ chối và trả lời bằng tin nhắn" từ giao diện cuộc gọi hệ thống. */
class RespondViaMessageService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val address = intent?.data?.schemeSpecificPart?.substringBefore('?')?.trim().orEmpty()
        val body = intent?.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
        if (address.isBlank() || body.isBlank()) {
            stopSelfResult(startId)
            return START_NOT_STICKY
        }
        EXECUTOR.execute {
            try {
                runBlocking {
                    val provider = TelephonyMessageRepository(applicationContext)
                    val threadId = runCatching { provider.threadIdForAddress(address) }.getOrNull()
                    val subId = MessagingSimRepository(applicationContext).resolve(threadId, null)
                    if (subId != null) SmsSendCoordinator(applicationContext).send(address, body, subId)
                }
            } finally {
                stopSelfResult(startId)
            }
        }
        return START_NOT_STICKY
    }

    private companion object {
        val EXECUTOR = Executors.newSingleThreadExecutor { r -> Thread(r, "mcas-respond-message") }
    }
}


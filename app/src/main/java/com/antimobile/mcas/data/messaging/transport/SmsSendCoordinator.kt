package com.antimobile.mcas.data.messaging.transport

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import com.antimobile.mcas.data.messaging.local.MessagingDatabase
import com.antimobile.mcas.data.messaging.local.SmsPartAttemptEntity
import com.antimobile.mcas.data.messaging.model.SendFailure
import com.antimobile.mcas.data.messaging.model.SendMessageResult
import com.antimobile.mcas.data.messaging.SmsRecipientParser
import com.antimobile.mcas.data.messaging.provider.TelephonyMessageRepository
import com.antimobile.mcas.data.messaging.role.SmsRole
import com.antimobile.mcas.receiver.messaging.SmsDeliveryReceiver
import com.antimobile.mcas.receiver.messaging.SmsSentReceiver
import com.antimobile.mcas.util.SimInfo
import com.antimobile.mcas.util.SmsSettings
import com.antimobile.mcas.util.SmsText
import java.util.UUID

/** Tạo Outbox + attempt bền trước khi gọi modem. Caller chạy hàm này ngoài main thread. */
class SmsSendCoordinator(private val context: Context) {
    private val provider = TelephonyMessageRepository(context)
    private val dao = MessagingDatabase.get(context).messagingDao()

    suspend fun send(address: String, rawBody: String, subscriptionId: Int): SendMessageResult {
        if (!SmsRole.isMessagingSupported(context)) return SendMessageResult.Failed(SendFailure.NO_TELEPHONY)
        if (!SmsRole.isHeld(context)) return SendMessageResult.Failed(SendFailure.NOT_DEFAULT_APP)
        if (!SmsRole.canSend(context)) return SendMessageResult.Failed(SendFailure.MISSING_PERMISSION)
        val recipient = address.trim()
        if (!SmsRecipientParser.isValidAddress(recipient)) {
            return SendMessageResult.Failed(SendFailure.INVALID_RECIPIENT)
        }
        if (rawBody.isBlank()) return SendMessageResult.Failed(SendFailure.EMPTY_BODY)
        val active = SimInfo.activeSims(context).any { it.subscriptionId == subscriptionId }
        if (!active) return SendMessageResult.Failed(SendFailure.SIM_UNAVAILABLE)

        val body = if (SmsSettings.isRemoveDiacritics(context)) SmsText.toGsm7(rawBody) else rawBody
        if (body.isBlank()) return SendMessageResult.Failed(SendFailure.EMPTY_BODY)
        val inserted = runCatching { provider.insertOutgoing(recipient, body, subscriptionId) }
            .getOrElse { return SendMessageResult.Failed(SendFailure.PROVIDER_ERROR) }
        val manager = runCatching { SmsManagerFactory.forSubscription(context, subscriptionId) }
            .getOrElse {
                runCatching { provider.updateOutgoingFailed(inserted.id, null) }
                return SendMessageResult.Failed(SendFailure.MODEM_ERROR)
            }
        val parts = runCatching { manager.divideMessage(body) }
            .getOrElse {
                runCatching { provider.updateOutgoingFailed(inserted.id, null) }
                return SendMessageResult.Failed(SendFailure.MODEM_ERROR)
            }
        if (parts.isEmpty()) {
            runCatching { provider.updateOutgoingFailed(inserted.id, null) }
            return SendMessageResult.Failed(SendFailure.EMPTY_BODY)
        }

        val now = System.currentTimeMillis()
        val attemptId = UUID.randomUUID().toString()
        val ledgerReady = runCatching {
            dao.insertParts(parts.indices.map { index ->
                SmsPartAttemptEntity(
                    attemptId = attemptId,
                    partIndex = index,
                    totalParts = parts.size,
                    providerId = inserted.id,
                    threadId = inserted.threadId,
                    address = recipient,
                    subscriptionId = subscriptionId,
                    sentResult = null,
                    delivered = false,
                    errorCode = null,
                    createdAt = now,
                    updatedAt = now,
                )
            })
            dao.deleteOldAttempts(now - ATTEMPT_TTL_MS)
        }.isSuccess
        if (!ledgerReady) {
            runCatching { provider.updateOutgoingFailed(inserted.id, null) }
            return SendMessageResult.Failed(SendFailure.PROVIDER_ERROR)
        }

        val sentIntents = ArrayList<PendingIntent>(parts.size)
        val deliveryIntents = ArrayList<PendingIntent>(parts.size)
        parts.indices.forEach { index ->
            sentIntents += callbackIntent(
                receiver = SmsSentReceiver::class.java,
                action = SmsCallbackContract.ACTION_SENT,
                attemptId = attemptId,
                partIndex = index,
                providerId = inserted.id,
                delivery = false,
            )
            deliveryIntents += callbackIntent(
                receiver = SmsDeliveryReceiver::class.java,
                action = SmsCallbackContract.ACTION_DELIVERED,
                attemptId = attemptId,
                partIndex = index,
                providerId = inserted.id,
                delivery = true,
            )
        }

        return runCatching {
            if (parts.size == 1) {
                manager.sendTextMessage(recipient, null, parts[0], sentIntents[0], deliveryIntents[0])
            } else {
                manager.sendMultipartTextMessage(recipient, null, parts, sentIntents, deliveryIntents)
            }
            SendMessageResult.Queued(inserted.id, inserted.threadId)
        }.getOrElse {
            parts.indices.forEach { index ->
                runCatching { dao.markPartSent(
                    attemptId,
                    index,
                    SmsManager.RESULT_ERROR_GENERIC_FAILURE,
                    null,
                    System.currentTimeMillis(),
                ) }
            }
            runCatching { provider.updateOutgoingFailed(inserted.id, null) }
            SendMessageResult.Failed(SendFailure.MODEM_ERROR)
        }
    }

    private fun callbackIntent(
        receiver: Class<*>,
        action: String,
        attemptId: String,
        partIndex: Int,
        providerId: Long,
        delivery: Boolean,
    ): PendingIntent {
        val intent = Intent(context, receiver).apply {
            this.action = action
            putExtra(SmsCallbackContract.EXTRA_ATTEMPT_ID, attemptId)
            putExtra(SmsCallbackContract.EXTRA_PART_INDEX, partIndex)
            putExtra(SmsCallbackContract.EXTRA_PROVIDER_ID, providerId)
        }
        val typeSalt = if (delivery) 0x40000000 else 0
        val requestCode = (attemptId.hashCode() * 31 + partIndex) xor typeSalt
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private companion object {
        const val ATTEMPT_TTL_MS = 7L * 24L * 60L * 60L * 1000L
    }
}

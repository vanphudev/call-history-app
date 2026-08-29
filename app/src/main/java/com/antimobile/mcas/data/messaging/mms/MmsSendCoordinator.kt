package com.antimobile.mcas.data.messaging.mms

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.TelephonyManager
import androidx.core.content.FileProvider
import com.antimobile.mcas.data.messaging.local.MessagingDatabase
import com.antimobile.mcas.data.messaging.local.MmsTransferEntity
import com.antimobile.mcas.data.messaging.model.SendFailure
import com.antimobile.mcas.data.messaging.model.SendMessageResult
import com.antimobile.mcas.data.messaging.provider.TelephonyMessageRepository
import com.antimobile.mcas.data.messaging.role.SmsRole
import com.antimobile.mcas.data.messaging.transport.SmsManagerFactory
import com.antimobile.mcas.receiver.messaging.MmsSentReceiver
import java.io.File
import java.util.UUID

class MmsSendCoordinator(private val context: Context) {
    private val repository = TelephonyMessageRepository(context)
    private val dao = MessagingDatabase.get(context).messagingDao()

    @SuppressLint("MissingPermission") // ROLE_SMS permission is checked; telephony access is also caught defensively.
    suspend fun send(
        address: String,
        body: String,
        subject: String?,
        image: PreparedMmsImage?,
        subscriptionId: Int,
    ): SendMessageResult {
        if (!SmsRole.isHeld(context)) return SendMessageResult.Failed(SendFailure.NOT_DEFAULT_APP)
        val limits = MmsCarrierLimits.load(context, subscriptionId)
        if (!limits.enabled) return SendMessageResult.Failed(SendFailure.MMS_DISABLED)
        val dataEnabled = runCatching {
            context.getSystemService(TelephonyManager::class.java)
                .createForSubscriptionId(subscriptionId)
                .isDataEnabled
        }.getOrDefault(true)
        if (!dataEnabled) return SendMessageResult.Failed(SendFailure.MOBILE_DATA_DISABLED)
        val safeSubject = subject?.trim()?.takeIf(String::isNotBlank)
        if (safeSubject != null && safeSubject.length > limits.maxSubjectLength) {
            return SendMessageResult.Failed(SendFailure.SUBJECT_TOO_LONG)
        }
        val parts = buildList {
            body.trim().takeIf(String::isNotBlank)?.let {
                add(MmsPduCodec.OutgoingPart(MmsPduCodec.MIME_TEXT, "text.txt", it.toByteArray(), charsetMib = 106))
            }
            image?.let { add(MmsPduCodec.OutgoingPart(MmsPduCodec.MIME_JPEG, "image.jpg", it.jpeg)) }
        }
        if (parts.isEmpty()) return SendMessageResult.Failed(SendFailure.EMPTY_BODY)
        val transactionId = "mcas-${UUID.randomUUID()}"
        val pdu = runCatching {
            MmsPduCodec.composeSendRequest(transactionId, listOf(address), safeSubject, parts)
        }.getOrElse { return SendMessageResult.Failed(SendFailure.PROVIDER_ERROR) }
        if (pdu.size > limits.maxMessageBytes) return SendMessageResult.Failed(SendFailure.IMAGE_TOO_LARGE)

        val inserted = runCatching {
            repository.insertOutgoingMms(address, safeSubject, parts, subscriptionId, transactionId)
        }.getOrElse { return SendMessageResult.Failed(SendFailure.PROVIDER_ERROR) }
        val directory = sendDirectory(context)
        val file = File(directory, "mms-${inserted.id}-${UUID.randomUUID()}.pdu")
        val uri = runCatching {
            file.writeBytes(pdu)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        }.getOrElse {
            repository.updateOutgoingMmsFailed(inserted.id, android.telephony.SmsManager.MMS_ERROR_IO_ERROR)
            file.delete()
            return SendMessageResult.Failed(SendFailure.PROVIDER_ERROR)
        }
        MmsDownloadCoordinator.grantMmsServiceAccess(context, uri, write = false)
        val callback = PendingIntent.getBroadcast(
            context,
            inserted.id.requestCode(),
            Intent(context, MmsSentReceiver::class.java).apply {
                data = Uri.parse("mcas://mms/sent/${inserted.id}/${file.name}")
                putExtra(MmsSentReceiver.EXTRA_PROVIDER_ID, inserted.id)
                putExtra(MmsSentReceiver.EXTRA_FILE_NAME, file.name)
                putExtra(MmsSentReceiver.EXTRA_CONTENT_URI, uri.toString())
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val now = System.currentTimeMillis()
        dao.saveMmsTransfer(
            MmsTransferEntity(
                providerId = inserted.id,
                transactionId = transactionId,
                threadId = inserted.threadId,
                address = address,
                contentLocation = "",
                subscriptionId = subscriptionId,
                direction = "OUTGOING",
                state = STATE_SENDING,
                tempFileName = file.name,
                attemptCount = 1,
                resultCode = null,
                httpStatus = null,
                createdAt = now,
                updatedAt = now,
            )
        )
        return runCatching {
            SmsManagerFactory.forSubscription(context, subscriptionId).sendMultimediaMessage(
                context,
                uri,
                null,
                null,
                callback,
            )
            SendMessageResult.Queued(inserted.id, inserted.threadId)
        }.getOrElse {
            repository.updateOutgoingMmsFailed(inserted.id, android.telephony.SmsManager.MMS_ERROR_UNSPECIFIED)
            dao.finishMmsTransfer(inserted.id, MmsDownloadCoordinator.STATE_FAILED, android.telephony.SmsManager.MMS_ERROR_UNSPECIFIED, null, System.currentTimeMillis())
            context.revokeUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            file.delete()
            SendMessageResult.Failed(SendFailure.MODEM_ERROR)
        }
    }

    companion object {
        const val STATE_SENDING = "SENDING"
        const val STATE_SENT = "SENT"

        fun sendDirectory(context: Context): File = File(context.cacheDir, "mms_send").apply { mkdirs() }

        fun resolveSendFile(context: Context, fileName: String): File? {
            if (!fileName.matches(Regex("mms-[0-9]+-[A-Za-z0-9-]+\\.pdu"))) return null
            val directory = sendDirectory(context).canonicalFile
            val file = File(directory, fileName).canonicalFile
            return file.takeIf { it.parentFile == directory }
        }

        private fun Long.requestCode(): Int = ((this xor (this ushr 32)).toInt() xor 0x4D4D5302) and 0x7fffffff
    }
}

package com.antimobile.mcas.receiver.messaging

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager
import com.antimobile.mcas.data.messaging.local.MessagingDatabase
import com.antimobile.mcas.data.messaging.mms.MmsDownloadCoordinator
import com.antimobile.mcas.data.messaging.mms.MmsPduCodec
import com.antimobile.mcas.data.messaging.notification.MessageNotifier
import com.antimobile.mcas.data.messaging.provider.TelephonyMessageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MmsDownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val providerId = intent.getLongExtra(EXTRA_PROVIDER_ID, -1L)
        val fileName = intent.getStringExtra(EXTRA_FILE_NAME).orEmpty()
        if (providerId <= 0L) return
        val appContext = context.applicationContext
        val callbackResult = resultCode
        val httpStatus = intent.getIntExtra(SmsManager.EXTRA_MMS_HTTP_STATUS, 0).takeIf { it > 0 }
        val contentUri = intent.getStringExtra(EXTRA_CONTENT_URI)?.let(Uri::parse)
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val repository = TelephonyMessageRepository(appContext)
            val dao = MessagingDatabase.get(appContext).messagingDao()
            val file = MmsDownloadCoordinator.resolveDownloadFile(appContext, fileName)
            try {
                val parsed = if (callbackResult == Activity.RESULT_OK && file?.isFile == true) {
                    MmsPduCodec.parseRetrieved(file.readBytes()).getOrNull()
                } else null
                if (parsed != null) {
                    val info = repository.mmsDownloadInfo(providerId)
                    repository.persistRetrievedMms(providerId, parsed, info?.timestampMillis ?: System.currentTimeMillis())
                    dao.finishMmsTransfer(
                        providerId,
                        MmsDownloadCoordinator.STATE_DOWNLOADED,
                        callbackResult,
                        httpStatus,
                        System.currentTimeMillis(),
                    )
                    if (info != null) {
                        val title = repository.identityForAddress(info.address)?.name ?: info.address
                        val preview = parsed.subject ?: parsed.text.takeIf(String::isNotBlank)
                            ?: if (parsed.parts.any { it.mimeType.startsWith("image/") }) "Đã nhận ảnh MMS" else "Đã nhận MMS"
                        MessageNotifier.notifyIncoming(
                            appContext,
                            info.threadId,
                            info.address,
                            title,
                            preview,
                            parsed.dateMillis ?: info.timestampMillis,
                            info.subscriptionId,
                        )
                    }
                } else {
                    val error = callbackResult.takeUnless { it == Activity.RESULT_OK } ?: SmsManager.MMS_ERROR_IO_ERROR
                    repository.markMmsDownloadFailed(providerId, error)
                    dao.finishMmsTransfer(
                        providerId,
                        MmsDownloadCoordinator.STATE_FAILED,
                        error,
                        httpStatus,
                        System.currentTimeMillis(),
                    )
                }
            } finally {
                file?.delete()
                contentUri?.let {
                    appContext.revokeUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                    )
                }
                pending.finish()
            }
        }
    }

    companion object {
        const val REQUEST_SALT = 0x4D4D5301
        const val EXTRA_PROVIDER_ID = "mms_provider_id"
        const val EXTRA_FILE_NAME = "mms_file_name"
        const val EXTRA_CONTENT_URI = "mms_content_uri"
    }
}

package com.antimobile.mcas.receiver.messaging

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager
import com.antimobile.mcas.data.messaging.local.MessagingDatabase
import com.antimobile.mcas.data.messaging.mms.MmsDownloadCoordinator
import com.antimobile.mcas.data.messaging.mms.MmsSendCoordinator
import com.antimobile.mcas.data.messaging.provider.TelephonyMessageRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MmsSentReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val providerId = intent.getLongExtra(EXTRA_PROVIDER_ID, -1L)
        if (providerId <= 0L) return
        val callbackResult = resultCode
        val httpStatus = intent.getIntExtra(SmsManager.EXTRA_MMS_HTTP_STATUS, 0).takeIf { it > 0 }
        val fileName = intent.getStringExtra(EXTRA_FILE_NAME).orEmpty()
        val contentUri = intent.getStringExtra(EXTRA_CONTENT_URI)?.let(Uri::parse)
        val appContext = context.applicationContext
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val repository = TelephonyMessageRepository(appContext)
                val dao = MessagingDatabase.get(appContext).messagingDao()
                if (callbackResult == Activity.RESULT_OK) {
                    repository.updateOutgoingMmsSent(providerId)
                    dao.finishMmsTransfer(providerId, MmsSendCoordinator.STATE_SENT, callbackResult, httpStatus, System.currentTimeMillis())
                } else {
                    repository.updateOutgoingMmsFailed(providerId, callbackResult)
                    dao.finishMmsTransfer(providerId, MmsDownloadCoordinator.STATE_FAILED, callbackResult, httpStatus, System.currentTimeMillis())
                }
            } finally {
                MmsSendCoordinator.resolveSendFile(appContext, fileName)?.delete()
                contentUri?.let { appContext.revokeUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_PROVIDER_ID = "mms_provider_id"
        const val EXTRA_FILE_NAME = "mms_file_name"
        const val EXTRA_CONTENT_URI = "mms_content_uri"
    }
}

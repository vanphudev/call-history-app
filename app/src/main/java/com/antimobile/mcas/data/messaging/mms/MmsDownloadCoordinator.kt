package com.antimobile.mcas.data.messaging.mms

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.SubscriptionManager
import androidx.core.content.FileProvider
import com.antimobile.mcas.data.messaging.local.MessagingDatabase
import com.antimobile.mcas.data.messaging.provider.TelephonyMessageRepository
import com.antimobile.mcas.data.messaging.transport.SmsManagerFactory
import com.antimobile.mcas.receiver.messaging.MmsDownloadReceiver
import java.io.File
import java.util.UUID

class MmsDownloadCoordinator(private val context: Context) {
    private val repository = TelephonyMessageRepository(context)
    private val dao = MessagingDatabase.get(context).messagingDao()

    suspend fun start(providerId: Long): Boolean {
        val info = repository.mmsDownloadInfo(providerId) ?: return false
        if (info.contentLocation.isBlank()) return false
        val subId = info.subscriptionId?.takeIf(SubscriptionManager::isValidSubscriptionId)
            ?: SubscriptionManager.getDefaultSmsSubscriptionId().takeIf(SubscriptionManager::isValidSubscriptionId)
            ?: return false
        val directory = downloadDirectory(context)
        val file = File(directory, "mms-${providerId}-${UUID.randomUUID()}.pdu")
        check(file.createNewFile()) { "Unable to create MMS download target" }
        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        grantMmsServiceAccess(context, contentUri, write = true)
        val callback = PendingIntent.getBroadcast(
            context,
            providerId.requestCode(MmsDownloadReceiver.REQUEST_SALT),
            Intent(context, MmsDownloadReceiver::class.java).apply {
                data = Uri.parse("mcas://mms/download/$providerId/${file.name}")
                putExtra(MmsDownloadReceiver.EXTRA_PROVIDER_ID, providerId)
                putExtra(MmsDownloadReceiver.EXTRA_FILE_NAME, file.name)
                putExtra(MmsDownloadReceiver.EXTRA_CONTENT_URI, contentUri.toString())
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        dao.markMmsTransferStarted(providerId, STATE_DOWNLOADING, file.name, System.currentTimeMillis())
        return runCatching {
            SmsManagerFactory.forSubscription(context, subId).downloadMultimediaMessage(
                context,
                info.contentLocation,
                contentUri,
                null,
                callback,
            )
            true
        }.getOrElse {
            context.revokeUriPermission(contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            file.delete()
            repository.markMmsDownloadFailed(providerId, android.telephony.SmsManager.MMS_ERROR_UNSPECIFIED)
            dao.finishMmsTransfer(providerId, STATE_FAILED, android.telephony.SmsManager.MMS_ERROR_UNSPECIFIED, null, System.currentTimeMillis())
            false
        }
    }

    companion object {
        const val STATE_PENDING = "PENDING"
        const val STATE_DOWNLOADING = "DOWNLOADING"
        const val STATE_DOWNLOADED = "DOWNLOADED"
        const val STATE_FAILED = "FAILED"

        fun downloadDirectory(context: Context): File = File(context.cacheDir, "mms_download").apply { mkdirs() }

        fun resolveDownloadFile(context: Context, fileName: String): File? {
            if (!fileName.matches(Regex("mms-[0-9]+-[A-Za-z0-9-]+\\.pdu"))) return null
            val directory = downloadDirectory(context).canonicalFile
            val file = File(directory, fileName).canonicalFile
            return file.takeIf { it.parentFile == directory }
        }

        fun grantMmsServiceAccess(context: Context, uri: Uri, write: Boolean) {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                (if (write) Intent.FLAG_GRANT_WRITE_URI_PERMISSION else 0)
            val packages = linkedSetOf("com.android.phone", "com.android.mms.service")
            runCatching {
                context.packageManager.getPackagesHoldingPermissions(
                    arrayOf(Manifest.permission.RECEIVE_MMS, Manifest.permission.RECEIVE_WAP_PUSH),
                    PackageManager.MATCH_UNINSTALLED_PACKAGES,
                ).mapTo(packages) { it.packageName }
            }
            packages.forEach { packageName -> runCatching { context.grantUriPermission(packageName, uri, flags) } }
        }

        private fun Long.requestCode(salt: Int): Int = ((this xor (this ushr 32)).toInt() xor salt) and 0x7fffffff
    }
}

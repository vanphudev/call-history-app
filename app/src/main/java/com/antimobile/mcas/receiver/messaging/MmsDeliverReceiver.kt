package com.antimobile.mcas.receiver.messaging

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SubscriptionManager
import com.antimobile.mcas.data.messaging.local.MessagingDatabase
import com.antimobile.mcas.data.messaging.local.MmsTransferEntity
import com.antimobile.mcas.data.messaging.mms.MmsDownloadCoordinator
import com.antimobile.mcas.data.messaging.mms.MmsDownloadPolicy
import com.antimobile.mcas.data.messaging.mms.MmsPduCodec
import com.antimobile.mcas.data.messaging.notification.MessageNotifier
import com.antimobile.mcas.data.messaging.provider.TelephonyMessageRepository
import com.antimobile.mcas.data.messaging.role.SmsRole
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Nhận notification.ind, lưu placeholder vào Telephony Provider rồi tải qua SmsManager công khai. */
class MmsDeliverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION || !SmsRole.isHeld(context)) return
        val pdu = intent.getByteArrayExtra("data") ?: return
        val subId = sequenceOf(
            intent.getIntExtra(SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX, SubscriptionManager.INVALID_SUBSCRIPTION_ID),
            intent.getIntExtra("subscription", SubscriptionManager.INVALID_SUBSCRIPTION_ID),
            intent.getIntExtra("subscription_id", SubscriptionManager.INVALID_SUBSCRIPTION_ID),
        ).firstOrNull(SubscriptionManager::isValidSubscriptionId)
        val appContext = context.applicationContext
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val notification = MmsPduCodec.parseNotification(pdu).getOrNull() ?: return@launch
                val repository = TelephonyMessageRepository(appContext)
                val inserted = repository.insertIncomingMmsNotification(notification, System.currentTimeMillis(), subId)
                val senderAddress = notification.from.ifBlank { "unknown" }
                val dao = MessagingDatabase.get(appContext).messagingDao()
                val existing = dao.getMmsTransferByTransaction(notification.transactionId)
                if (existing == null) {
                    val now = System.currentTimeMillis()
                    dao.saveMmsTransfer(
                        MmsTransferEntity(
                            providerId = inserted.id,
                            transactionId = notification.transactionId,
                            threadId = inserted.threadId,
                            address = senderAddress,
                            contentLocation = notification.contentLocation,
                            subscriptionId = subId,
                            direction = "INCOMING",
                            state = MmsDownloadCoordinator.STATE_PENDING,
                            tempFileName = null,
                            attemptCount = 0,
                            resultCode = null,
                            httpStatus = null,
                            createdAt = now,
                            updatedAt = now,
                        )
                    )
                    val title = repository.identityForAddress(senderAddress)?.name ?: senderAddress
                    MessageNotifier.notifyIncoming(
                        appContext,
                        inserted.threadId,
                        senderAddress,
                        title,
                        notification.subject ?: "MMS • chạm để tải nội dung",
                        now,
                        subId,
                    )
                }
                if (
                    existing?.state != MmsDownloadCoordinator.STATE_DOWNLOADED &&
                    MmsDownloadPolicy.shouldAutoDownload(appContext, subId)
                ) {
                    MmsDownloadCoordinator(appContext).start(inserted.id)
                }
            } finally {
                pending.finish()
            }
        }
    }
}

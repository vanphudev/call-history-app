package com.antimobile.callhs.data.messaging.inbound

import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SubscriptionManager
import com.antimobile.callhs.data.messaging.local.InboundFingerprintEntity
import com.antimobile.callhs.data.messaging.local.MessagingDatabase
import com.antimobile.callhs.data.messaging.notification.MessageNotifier
import com.antimobile.callhs.data.messaging.provider.TelephonyMessageRepository
import com.antimobile.callhs.data.messaging.role.SmsRole
import java.security.MessageDigest

object InboundSmsProcessor {
    private const val FINGERPRINT_TTL_MS = 14L * 24L * 60L * 60L * 1000L
    private val lock = Any()

    fun process(context: Context, intent: Intent) {
        if (!SmsRole.isHeld(context)) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return
        val address = messages.firstNotNullOfOrNull { it.displayOriginatingAddress?.takeIf(String::isNotBlank) }
            ?: return
        val body = messages.joinToString(separator = "") { it.messageBody.orEmpty() }
        val timestamp = messages.minOfOrNull { it.timestampMillis }?.takeIf { it > 0L } ?: System.currentTimeMillis()
        val subId = subscriptionId(intent)
        val fingerprint = fingerprint(intent, address, body, timestamp, subId)
        val dao = MessagingDatabase.get(context).messagingDao()
        synchronized(lock) {
            kotlinx.coroutines.runBlocking {
                if (dao.hasFingerprint(fingerprint) > 0) return@runBlocking
                val inserted = TelephonyMessageRepository(context).insertIncoming(address, body, timestamp, subId)
                dao.insertFingerprint(InboundFingerprintEntity(fingerprint, System.currentTimeMillis()))
                dao.deleteOldFingerprints(System.currentTimeMillis() - FINGERPRINT_TTL_MS)
                val identity = TelephonyMessageRepository(context).identityForAddress(address)
                MessageNotifier.notifyIncoming(
                    context = context,
                    threadId = inserted.threadId,
                    address = address,
                    title = identity?.name?.takeIf(String::isNotBlank) ?: address,
                    body = body,
                    timestamp = timestamp,
                    subscriptionId = subId,
                )
            }
        }
    }

    private fun subscriptionId(intent: Intent): Int? {
        val direct = intent.getIntExtra("subscription", SubscriptionManager.INVALID_SUBSCRIPTION_ID)
        if (direct != SubscriptionManager.INVALID_SUBSCRIPTION_ID) return direct
        val indexed = intent.getIntExtra(
            SubscriptionManager.EXTRA_SUBSCRIPTION_INDEX,
            SubscriptionManager.INVALID_SUBSCRIPTION_ID,
        )
        return indexed.takeIf { it != SubscriptionManager.INVALID_SUBSCRIPTION_ID }
    }

    private fun fingerprint(intent: Intent, address: String, body: String, timestamp: Long, subId: Int?): String {
        val digest = MessageDigest.getInstance("SHA-256")
        @Suppress("DEPRECATION") // Bundle.get là cách tương thích API 29 cho mảng PDU byte[] của telephony.
        val pdus = intent.extras?.get("pdus") as? Array<*>
        pdus?.forEach { pdu -> (pdu as? ByteArray)?.let(digest::update) }
        digest.update(address.toByteArray(Charsets.UTF_8))
        digest.update(body.toByteArray(Charsets.UTF_8))
        digest.update(timestamp.toString().toByteArray(Charsets.US_ASCII))
        digest.update((subId ?: -1).toString().toByteArray(Charsets.US_ASCII))
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

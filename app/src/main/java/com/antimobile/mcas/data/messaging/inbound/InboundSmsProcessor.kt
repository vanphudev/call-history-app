package com.antimobile.mcas.data.messaging.inbound

import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SubscriptionManager
import android.util.Log
import com.antimobile.mcas.data.messaging.local.InboundFingerprintEntity
import com.antimobile.mcas.data.messaging.local.MessagingDatabase
import com.antimobile.mcas.data.messaging.notification.MessageNotifier
import com.antimobile.mcas.data.messaging.provider.TelephonyMessageRepository
import com.antimobile.mcas.data.messaging.role.SmsRole
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
        // Sidecar Room chỉ phục vụ chống broadcast lặp. Nếu database riêng lỗi/migration lỗi, vẫn phải
        // ưu tiên ghi SMS vào Telephony Provider; mất dedupe tạm thời ít nghiêm trọng hơn làm mất tin nhắn.
        val dao = runCatching { MessagingDatabase.get(context).messagingDao() }
            .onFailure { error -> Log.w(TAG, "SMS dedupe database unavailable; continuing safely", error) }
            .getOrNull()
        synchronized(lock) {
            kotlinx.coroutines.runBlocking {
                val duplicate = dao?.let {
                    runCatching { it.hasFingerprint(fingerprint) > 0 }
                        .onFailure { error -> Log.w(TAG, "Unable to check SMS fingerprint", error) }
                        .getOrDefault(false)
                } ?: false
                if (duplicate) return@runBlocking
                val inserted = TelephonyMessageRepository(context).insertIncoming(address, body, timestamp, subId)
                dao?.let {
                    runCatching {
                        it.insertFingerprint(InboundFingerprintEntity(fingerprint, System.currentTimeMillis()))
                        it.deleteOldFingerprints(System.currentTimeMillis() - FINGERPRINT_TTL_MS)
                    }.onFailure { error -> Log.w(TAG, "Unable to update SMS fingerprint", error) }
                }
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

    private const val TAG = "MCAS-InboundSms"
}

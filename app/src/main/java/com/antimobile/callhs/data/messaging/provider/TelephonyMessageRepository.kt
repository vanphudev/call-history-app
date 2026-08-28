package com.antimobile.callhs.data.messaging.provider

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import com.antimobile.callhs.data.messaging.model.ConversationSummary
import com.antimobile.callhs.data.messaging.model.MessageDirection
import com.antimobile.callhs.data.messaging.model.MessageState
import com.antimobile.callhs.data.messaging.model.SmsMessageItem
import com.antimobile.callhs.data.messaging.role.SmsRole
import java.util.concurrent.ConcurrentHashMap

/** Đọc/ghi SMS Provider. Mọi entry point tự kiểm tra ROLE_SMS thay vì tin state của UI. */
class TelephonyMessageRepository(private val context: Context) {
    private val resolver = context.contentResolver
    private val contactCache = ConcurrentHashMap<String, ContactIdentity>()
    private val missingContactCache = ConcurrentHashMap.newKeySet<String>()

    data class InsertedMessage(val id: Long, val uri: Uri, val threadId: Long)
    data class ContactIdentity(val name: String?, val photoUri: String?)

    fun loadConversations(maxMessagesToScan: Int = 10_000): List<ConversationSummary> {
        checkReadAccess()
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
            Telephony.Sms.READ,
            Telephony.Sms.STATUS,
            Telephony.Sms.SUBSCRIPTION_ID,
        )
        data class Builder(
            val threadId: Long,
            val address: String,
            val snippet: String,
            val timestamp: Long,
            val direction: MessageDirection,
            val state: MessageState,
            val subId: Int?,
            var unread: Int = 0,
            var count: Int = 0,
        )
        val builders = LinkedHashMap<Long, Builder>()
        resolver.query(Telephony.Sms.CONTENT_URI, projection, null, null, "${Telephony.Sms.DATE} DESC")?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(Telephony.Sms._ID)
            val threadIdx = c.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val addressIdx = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIdx = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIdx = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val typeIdx = c.getColumnIndexOrThrow(Telephony.Sms.TYPE)
            val readIdx = c.getColumnIndexOrThrow(Telephony.Sms.READ)
            val statusIdx = c.getColumnIndex(Telephony.Sms.STATUS)
            val subIdx = c.getColumnIndex(Telephony.Sms.SUBSCRIPTION_ID)
            var scanned = 0
            while (c.moveToNext() && scanned < maxMessagesToScan) {
                scanned++
                @Suppress("UNUSED_VARIABLE") val providerId = c.getLong(idIdx)
                val threadId = c.getLong(threadIdx)
                if (threadId <= 0L) continue
                val address = c.getString(addressIdx)?.trim().orEmpty()
                val type = c.getInt(typeIdx)
                val status = if (statusIdx >= 0 && !c.isNull(statusIdx)) c.getInt(statusIdx) else null
                val builder = builders.getOrPut(threadId) {
                    Builder(
                        threadId = threadId,
                        address = address,
                        snippet = c.getString(bodyIdx).orEmpty(),
                        timestamp = c.getLong(dateIdx),
                        direction = directionOf(type),
                        state = stateOf(type, status),
                        subId = readSubId(c, subIdx),
                    )
                }
                builder.count++
                if (type == Telephony.Sms.MESSAGE_TYPE_INBOX && c.getInt(readIdx) == 0) builder.unread++
            }
        }
        return builders.values.map { b ->
            val identity = contactIdentity(b.address)
            ConversationSummary(
                threadId = b.threadId,
                address = b.address,
                displayName = identity?.name,
                photoUri = identity?.photoUri,
                snippet = b.snippet,
                timestampMillis = b.timestamp,
                unreadCount = b.unread,
                messageCount = b.count,
                lastDirection = b.direction,
                lastState = b.state,
                lastSubscriptionId = b.subId,
            )
        }
    }

    fun loadMessages(threadId: Long, limit: Int = 200): List<SmsMessageItem> {
        checkReadAccess()
        val result = ArrayList<SmsMessageItem>()
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.THREAD_ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
            Telephony.Sms.STATUS,
            Telephony.Sms.SUBSCRIPTION_ID,
            Telephony.Sms.ERROR_CODE,
        )
        resolver.query(
            Telephony.Sms.CONTENT_URI,
            projection,
            "${Telephony.Sms.THREAD_ID} = ?",
            arrayOf(threadId.toString()),
            "${Telephony.Sms.DATE} DESC, ${Telephony.Sms._ID} DESC",
        )?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(Telephony.Sms._ID)
            val threadIdx = c.getColumnIndexOrThrow(Telephony.Sms.THREAD_ID)
            val addressIdx = c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIdx = c.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIdx = c.getColumnIndexOrThrow(Telephony.Sms.DATE)
            val typeIdx = c.getColumnIndexOrThrow(Telephony.Sms.TYPE)
            val statusIdx = c.getColumnIndex(Telephony.Sms.STATUS)
            val subIdx = c.getColumnIndex(Telephony.Sms.SUBSCRIPTION_ID)
            val errorIdx = c.getColumnIndex(Telephony.Sms.ERROR_CODE)
            while (c.moveToNext() && result.size < limit) {
                val type = c.getInt(typeIdx)
                val status = if (statusIdx >= 0 && !c.isNull(statusIdx)) c.getInt(statusIdx) else null
                result += SmsMessageItem(
                    id = c.getLong(idIdx),
                    threadId = c.getLong(threadIdx),
                    address = c.getString(addressIdx).orEmpty(),
                    body = c.getString(bodyIdx).orEmpty(),
                    timestampMillis = c.getLong(dateIdx),
                    direction = directionOf(type),
                    state = stateOf(type, status),
                    subscriptionId = readSubId(c, subIdx),
                    errorCode = if (errorIdx >= 0 && !c.isNull(errorIdx)) c.getInt(errorIdx) else null,
                )
            }
        }
        result.reverse()
        return result
    }

    fun threadIdForAddress(address: String): Long {
        checkReadAccess()
        return Telephony.Threads.getOrCreateThreadId(context, setOf(address.trim()))
    }

    fun insertOutgoing(address: String, body: String, subId: Int, now: Long = System.currentTimeMillis()): InsertedMessage {
        checkWriteAccess()
        val threadId = threadIdForAddress(address)
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, address)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE, now)
            put(Telephony.Sms.DATE_SENT, 0L)
            put(Telephony.Sms.READ, 1)
            put(Telephony.Sms.SEEN, 1)
            put(Telephony.Sms.THREAD_ID, threadId)
            put(Telephony.Sms.SUBSCRIPTION_ID, subId)
            put(Telephony.Sms.STATUS, Telephony.Sms.STATUS_PENDING)
        }
        val uri = resolver.insert(Telephony.Sms.Outbox.CONTENT_URI, values)
            ?: error("Unable to insert SMS outbox row")
        val id = ContentUris.parseId(uri)
        return InsertedMessage(id, uri, threadId)
    }

    fun insertIncoming(
        address: String,
        body: String,
        timestamp: Long,
        subId: Int?,
    ): InsertedMessage {
        checkWriteAccess()
        val threadId = threadIdForAddress(address)
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, address)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE, timestamp)
            put(Telephony.Sms.DATE_SENT, timestamp)
            put(Telephony.Sms.READ, 0)
            put(Telephony.Sms.SEEN, 0)
            put(Telephony.Sms.THREAD_ID, threadId)
            subId?.let { put(Telephony.Sms.SUBSCRIPTION_ID, it) }
        }
        val uri = resolver.insert(Telephony.Sms.Inbox.CONTENT_URI, values)
            ?: error("Unable to insert SMS inbox row")
        return InsertedMessage(ContentUris.parseId(uri), uri, threadId)
    }

    fun markThreadRead(threadId: Long): Int {
        checkWriteAccess()
        val values = ContentValues().apply {
            put(Telephony.Sms.READ, 1)
            put(Telephony.Sms.SEEN, 1)
        }
        return resolver.update(
            Telephony.Sms.CONTENT_URI,
            values,
            "${Telephony.Sms.THREAD_ID} = ? AND (${Telephony.Sms.READ} = 0 OR ${Telephony.Sms.SEEN} = 0)",
            arrayOf(threadId.toString()),
        )
    }

    fun markThreadUnread(threadId: Long): Int {
        checkWriteAccess()
        val latest = loadMessages(threadId, limit = 1).lastOrNull() ?: return 0
        return resolver.update(
            ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, latest.id),
            ContentValues().apply { put(Telephony.Sms.READ, 0); put(Telephony.Sms.SEEN, 0) },
            null,
            null,
        )
    }

    fun deleteMessage(messageId: Long): Boolean {
        checkWriteAccess()
        return resolver.delete(ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, messageId), null, null) > 0
    }

    fun deleteThread(threadId: Long): Boolean {
        checkWriteAccess()
        return resolver.delete(
            Telephony.Sms.CONTENT_URI,
            "${Telephony.Sms.THREAD_ID} = ?",
            arrayOf(threadId.toString()),
        ) > 0
    }

    fun updateOutgoingSent(providerId: Long, delivered: Boolean = false) {
        checkWriteAccess()
        val values = ContentValues().apply {
            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT)
            put(Telephony.Sms.DATE_SENT, System.currentTimeMillis())
            put(Telephony.Sms.STATUS, if (delivered) Telephony.Sms.STATUS_COMPLETE else Telephony.Sms.STATUS_PENDING)
            put(Telephony.Sms.ERROR_CODE, 0)
        }
        resolver.update(ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, providerId), values, null, null)
    }

    fun updateOutgoingDelivered(providerId: Long) {
        checkWriteAccess()
        resolver.update(
            ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, providerId),
            ContentValues().apply { put(Telephony.Sms.STATUS, Telephony.Sms.STATUS_COMPLETE) },
            null,
            null,
        )
    }

    fun updateOutgoingFailed(providerId: Long, errorCode: Int?) {
        checkWriteAccess()
        val values = ContentValues().apply {
            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_FAILED)
            put(Telephony.Sms.STATUS, Telephony.Sms.STATUS_FAILED)
            errorCode?.let { put(Telephony.Sms.ERROR_CODE, it) }
        }
        resolver.update(ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, providerId), values, null, null)
    }

    fun latestIncomingSubId(threadId: Long): Int? {
        checkReadAccess()
        resolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.SUBSCRIPTION_ID),
            "${Telephony.Sms.THREAD_ID} = ?",
            arrayOf(threadId.toString()),
            "${Telephony.Sms.DATE} DESC",
        )?.use { c ->
            val index = c.getColumnIndex(Telephony.Sms.SUBSCRIPTION_ID)
            if (index >= 0 && c.moveToFirst() && !c.isNull(index)) return c.getInt(index).takeIf { it >= 0 }
        }
        return null
    }

    fun identityForAddress(address: String): ContactIdentity? = contactIdentity(address)

    fun clearContactCache() {
        contactCache.clear()
        missingContactCache.clear()
    }

    private fun contactIdentity(address: String): ContactIdentity? {
        val key = address.trim()
        contactCache[key]?.let { return it }
        if (missingContactCache.contains(key)) return null
        val identity = try {
            val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(address))
            resolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup.PHOTO_URI),
                null,
                null,
                null,
            )?.use { c ->
                if (!c.moveToFirst()) null else ContactIdentity(c.getString(0), c.getString(1))
            }
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
        if (identity == null) missingContactCache.add(key) else contactCache[key] = identity
        return identity
    }

    private fun checkReadAccess() {
        check(SmsRole.canRead(context)) { "CallHS is not allowed to read SMS" }
    }

    private fun checkWriteAccess() {
        check(SmsRole.isHeld(context)) { "CallHS is not the default SMS app" }
    }

    private fun readSubId(cursor: android.database.Cursor, index: Int): Int? =
        if (index >= 0 && !cursor.isNull(index)) cursor.getInt(index).takeIf { it >= 0 } else null

    private fun directionOf(type: Int): MessageDirection =
        if (type == Telephony.Sms.MESSAGE_TYPE_INBOX) MessageDirection.INCOMING else MessageDirection.OUTGOING

    private fun stateOf(type: Int, status: Int?): MessageState = when (type) {
        Telephony.Sms.MESSAGE_TYPE_INBOX -> MessageState.RECEIVED
        Telephony.Sms.MESSAGE_TYPE_OUTBOX -> MessageState.SENDING
        Telephony.Sms.MESSAGE_TYPE_QUEUED -> MessageState.QUEUED
        Telephony.Sms.MESSAGE_TYPE_FAILED -> MessageState.FAILED
        Telephony.Sms.MESSAGE_TYPE_SENT -> if (status == Telephony.Sms.STATUS_COMPLETE) {
            MessageState.DELIVERED
        } else {
            MessageState.SENT_TO_NETWORK
        }
        else -> MessageState.QUEUED
    }
}

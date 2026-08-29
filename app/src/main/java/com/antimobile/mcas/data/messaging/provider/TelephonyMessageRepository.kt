package com.antimobile.mcas.data.messaging.provider

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Telephony
import com.antimobile.mcas.data.messaging.model.ConversationSummary
import com.antimobile.mcas.data.messaging.model.MessageAttachment
import com.antimobile.mcas.data.messaging.model.MessageDirection
import com.antimobile.mcas.data.messaging.model.MessageState
import com.antimobile.mcas.data.messaging.model.MessageTransport
import com.antimobile.mcas.data.messaging.model.MmsDownloadState
import com.antimobile.mcas.data.messaging.model.SmsMessageItem
import com.antimobile.mcas.data.messaging.mms.MmsPduCodec
import com.antimobile.mcas.data.messaging.role.SmsRole
import java.util.concurrent.ConcurrentHashMap
import java.io.ByteArrayOutputStream
import java.io.InputStream

/** Đọc/ghi SMS Provider. Mọi entry point tự kiểm tra ROLE_SMS thay vì tin state của UI. */
class TelephonyMessageRepository(private val context: Context) {
    private val resolver = context.contentResolver
    private val contactCache = ConcurrentHashMap<String, ContactIdentity>()
    private val missingContactCache = ConcurrentHashMap.newKeySet<String>()

    data class InsertedMessage(val id: Long, val uri: Uri, val threadId: Long)
    data class ContactIdentity(val name: String?, val photoUri: String?)
    data class MmsDownloadInfo(
        val providerId: Long,
        val threadId: Long,
        val address: String,
        val contentLocation: String,
        val transactionId: String,
        val subscriptionId: Int?,
        val timestampMillis: Long,
    )

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
            var address: String,
            var snippet: String,
            var timestamp: Long,
            var direction: MessageDirection,
            var state: MessageState,
            var subId: Int?,
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
        val mmsProjection = arrayOf(
            Telephony.Mms._ID,
            Telephony.Mms.THREAD_ID,
            Telephony.Mms.DATE,
            Telephony.Mms.MESSAGE_BOX,
            Telephony.Mms.READ,
            Telephony.Mms.SUBJECT,
            Telephony.Mms.SUBSCRIPTION_ID,
            Telephony.Mms.MESSAGE_TYPE,
            Telephony.Mms.RESPONSE_STATUS,
        )
        resolver.query(Telephony.Mms.CONTENT_URI, mmsProjection, null, null, "${Telephony.Mms.DATE} DESC")?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(Telephony.Mms._ID)
            val threadIdx = c.getColumnIndexOrThrow(Telephony.Mms.THREAD_ID)
            val dateIdx = c.getColumnIndexOrThrow(Telephony.Mms.DATE)
            val boxIdx = c.getColumnIndexOrThrow(Telephony.Mms.MESSAGE_BOX)
            val readIdx = c.getColumnIndexOrThrow(Telephony.Mms.READ)
            val subjectIdx = c.getColumnIndex(Telephony.Mms.SUBJECT)
            val subIdx = c.getColumnIndex(Telephony.Mms.SUBSCRIPTION_ID)
            val responseIdx = c.getColumnIndex(Telephony.Mms.RESPONSE_STATUS)
            var scanned = 0
            while (c.moveToNext() && scanned < maxMessagesToScan) {
                scanned++
                val id = c.getLong(idIdx)
                val threadId = c.getLong(threadIdx)
                if (threadId <= 0L) continue
                val box = c.getInt(boxIdx)
                val timestamp = c.getLong(dateIdx).safeMmsDateMillis()
                val builder = builders[threadId]
                if (builder == null) {
                    val direction = mmsDirectionOf(box)
                    val address = mmsAddress(id, direction)
                    val subject = if (subjectIdx >= 0) c.getString(subjectIdx).orEmpty() else ""
                    val response = if (responseIdx >= 0 && !c.isNull(responseIdx)) c.getInt(responseIdx) else null
                    builders[threadId] = Builder(
                        threadId = threadId,
                        address = address,
                        snippet = mmsSnippet(id, subject),
                        timestamp = timestamp,
                        direction = direction,
                        state = mmsStateOf(box, response),
                        subId = readSubId(c, subIdx),
                        unread = if (box == Telephony.Mms.MESSAGE_BOX_INBOX && c.getInt(readIdx) == 0) 1 else 0,
                        count = 1,
                    )
                } else {
                    builder.count++
                    if (box == Telephony.Mms.MESSAGE_BOX_INBOX && c.getInt(readIdx) == 0) builder.unread++
                    if (timestamp > builder.timestamp) {
                        val direction = mmsDirectionOf(box)
                        val subject = if (subjectIdx >= 0) c.getString(subjectIdx).orEmpty() else ""
                        val response = if (responseIdx >= 0 && !c.isNull(responseIdx)) c.getInt(responseIdx) else null
                        val address = mmsAddress(id, direction)
                        builder.address = address
                        builder.snippet = mmsSnippet(id, subject)
                        builder.timestamp = timestamp
                        builder.direction = direction
                        builder.state = mmsStateOf(box, response)
                        builder.subId = readSubId(c, subIdx)
                    }
                }
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
        }.sortedByDescending(ConversationSummary::timestampMillis)
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
        val mmsProjection = arrayOf(
            Telephony.Mms._ID,
            Telephony.Mms.THREAD_ID,
            Telephony.Mms.DATE,
            Telephony.Mms.MESSAGE_BOX,
            Telephony.Mms.SUBJECT,
            Telephony.Mms.SUBSCRIPTION_ID,
            Telephony.Mms.MESSAGE_TYPE,
            Telephony.Mms.RESPONSE_STATUS,
        )
        resolver.query(
            Telephony.Mms.CONTENT_URI,
            mmsProjection,
            "${Telephony.Mms.THREAD_ID} = ?",
            arrayOf(threadId.toString()),
            "${Telephony.Mms.DATE} DESC, ${Telephony.Mms._ID} DESC",
        )?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(Telephony.Mms._ID)
            val threadIdx = c.getColumnIndexOrThrow(Telephony.Mms.THREAD_ID)
            val dateIdx = c.getColumnIndexOrThrow(Telephony.Mms.DATE)
            val boxIdx = c.getColumnIndexOrThrow(Telephony.Mms.MESSAGE_BOX)
            val subjectIdx = c.getColumnIndex(Telephony.Mms.SUBJECT)
            val subIdx = c.getColumnIndex(Telephony.Mms.SUBSCRIPTION_ID)
            val typeIdx = c.getColumnIndex(Telephony.Mms.MESSAGE_TYPE)
            val responseIdx = c.getColumnIndex(Telephony.Mms.RESPONSE_STATUS)
            while (c.moveToNext() && result.size < limit * 2) {
                val id = c.getLong(idIdx)
                val box = c.getInt(boxIdx)
                val direction = mmsDirectionOf(box)
                val messageType = if (typeIdx >= 0 && !c.isNull(typeIdx)) c.getInt(typeIdx) else null
                val response = if (responseIdx >= 0 && !c.isNull(responseIdx)) c.getInt(responseIdx) else null
                val parts = mmsParts(id)
                val body = parts.text
                result += SmsMessageItem(
                    id = id,
                    threadId = c.getLong(threadIdx),
                    address = mmsAddress(id, direction),
                    body = body,
                    timestampMillis = c.getLong(dateIdx).safeMmsDateMillis(),
                    direction = direction,
                    state = mmsStateOf(box, response),
                    subscriptionId = readSubId(c, subIdx),
                    errorCode = response?.takeUnless { it == MMS_RESPONSE_OK },
                    transport = MessageTransport.MMS,
                    subject = if (subjectIdx >= 0) c.getString(subjectIdx)?.takeIf(String::isNotBlank) else null,
                    attachments = parts.attachments,
                    mmsDownloadState = when {
                        messageType == MMS_NOTIFICATION_IND && response != null && response != MMS_RESPONSE_OK -> MmsDownloadState.FAILED
                        messageType == MMS_NOTIFICATION_IND -> MmsDownloadState.PENDING
                        box == Telephony.Mms.MESSAGE_BOX_FAILED -> MmsDownloadState.FAILED
                        else -> MmsDownloadState.DOWNLOADED
                    },
                )
            }
        }
        return result.sortedWith(compareBy<SmsMessageItem> { it.timestampMillis }.thenBy { it.id })
            .takeLast(limit)
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

    fun insertIncomingMmsNotification(
        notification: MmsPduCodec.Notification,
        timestamp: Long,
        subId: Int?,
    ): InsertedMessage {
        checkWriteAccess()
        findMmsByTransactionId(notification.transactionId)?.let { return it }
        val address = notification.from.ifBlank { "unknown" }
        val threadId = threadIdForAddress(address)
        val values = ContentValues().apply {
            put(Telephony.Mms.THREAD_ID, threadId)
            put(Telephony.Mms.DATE, timestamp / 1000L)
            put(Telephony.Mms.DATE_SENT, 0L)
            put(Telephony.Mms.READ, 0)
            put(Telephony.Mms.SEEN, 0)
            put(Telephony.Mms.MESSAGE_BOX, Telephony.Mms.MESSAGE_BOX_INBOX)
            put(Telephony.Mms.MESSAGE_TYPE, MMS_NOTIFICATION_IND)
            put(Telephony.Mms.MMS_VERSION, MMS_VERSION_1_2)
            put(Telephony.Mms.CONTENT_TYPE, MmsPduCodec.MIME_MMS)
            put(Telephony.Mms.CONTENT_LOCATION, notification.contentLocation)
            put(Telephony.Mms.TRANSACTION_ID, notification.transactionId)
            put(Telephony.Mms.MESSAGE_CLASS, "personal")
            put(Telephony.Mms.PRIORITY, MMS_PRIORITY_NORMAL)
            notification.subject?.let { put(Telephony.Mms.SUBJECT, it); put(Telephony.Mms.SUBJECT_CHARSET, UTF_8_MIB) }
            notification.expiryMillis?.let { put(Telephony.Mms.EXPIRY, it / 1000L) }
            notification.messageSize?.let { put(Telephony.Mms.MESSAGE_SIZE, it) }
            subId?.let { put(Telephony.Mms.SUBSCRIPTION_ID, it) }
        }
        val uri = resolver.insert(Telephony.Mms.Inbox.CONTENT_URI, values)
            ?: error("Unable to insert MMS notification")
        val id = ContentUris.parseId(uri)
        insertMmsAddress(id, address, MMS_ADDRESS_FROM)
        return InsertedMessage(id, uri, threadId)
    }

    fun persistRetrievedMms(providerId: Long, value: MmsPduCodec.Retrieved, fallbackTimestamp: Long) {
        checkWriteAccess()
        resolver.delete(
            Telephony.Mms.Part.CONTENT_URI,
            "${Telephony.Mms.Part.MSG_ID} = ?",
            arrayOf(providerId.toString()),
        )
        value.parts.forEachIndexed { index, part -> insertMmsPart(providerId, index, part) }
        val values = ContentValues().apply {
            put(Telephony.Mms.MESSAGE_TYPE, MMS_RETRIEVE_CONF)
            put(Telephony.Mms.MESSAGE_BOX, Telephony.Mms.MESSAGE_BOX_INBOX)
            put(Telephony.Mms.RETRIEVE_STATUS, value.retrieveStatus ?: MMS_RETRIEVE_OK)
            put(Telephony.Mms.DATE, (value.dateMillis ?: fallbackTimestamp) / 1000L)
            put(Telephony.Mms.TEXT_ONLY, if (value.parts.none { it.mimeType.startsWith("image/") }) 1 else 0)
            value.subject?.let { put(Telephony.Mms.SUBJECT, it); put(Telephony.Mms.SUBJECT_CHARSET, UTF_8_MIB) }
            value.messageId?.let { put(Telephony.Mms.MESSAGE_ID, it) }
            putNull(Telephony.Mms.RESPONSE_STATUS)
        }
        resolver.update(ContentUris.withAppendedId(Telephony.Mms.CONTENT_URI, providerId), values, null, null)
        value.from.takeIf(String::isNotBlank)?.let { from ->
            resolver.delete(
                mmsAddrUri(providerId),
                "${Telephony.Mms.Addr.TYPE} = ?",
                arrayOf(MMS_ADDRESS_FROM.toString()),
            )
            insertMmsAddress(providerId, from, MMS_ADDRESS_FROM)
        }
    }

    fun markMmsDownloadFailed(providerId: Long, resultCode: Int) {
        checkWriteAccess()
        resolver.update(
            ContentUris.withAppendedId(Telephony.Mms.CONTENT_URI, providerId),
            ContentValues().apply { put(Telephony.Mms.RESPONSE_STATUS, mmsFailureStatus(resultCode)) },
            null,
            null,
        )
    }

    fun insertOutgoingMms(
        address: String,
        subject: String?,
        parts: List<MmsPduCodec.OutgoingPart>,
        subId: Int,
        transactionId: String,
        now: Long = System.currentTimeMillis(),
    ): InsertedMessage {
        checkWriteAccess()
        val threadId = threadIdForAddress(address)
        val values = ContentValues().apply {
            put(Telephony.Mms.THREAD_ID, threadId)
            put(Telephony.Mms.DATE, now / 1000L)
            put(Telephony.Mms.DATE_SENT, 0L)
            put(Telephony.Mms.READ, 1)
            put(Telephony.Mms.SEEN, 1)
            put(Telephony.Mms.MESSAGE_BOX, Telephony.Mms.MESSAGE_BOX_OUTBOX)
            put(Telephony.Mms.MESSAGE_TYPE, MMS_SEND_REQ)
            put(Telephony.Mms.MMS_VERSION, MMS_VERSION_1_2)
            put(Telephony.Mms.CONTENT_TYPE, "application/vnd.wap.multipart.mixed")
            put(Telephony.Mms.TRANSACTION_ID, transactionId)
            put(Telephony.Mms.MESSAGE_CLASS, "personal")
            put(Telephony.Mms.PRIORITY, MMS_PRIORITY_NORMAL)
            put(Telephony.Mms.SUBSCRIPTION_ID, subId)
            put(Telephony.Mms.TEXT_ONLY, if (parts.none { it.mimeType.startsWith("image/") }) 1 else 0)
            put(Telephony.Mms.MESSAGE_SIZE, parts.sumOf { it.data.size })
            subject?.trim()?.takeIf(String::isNotBlank)?.let {
                put(Telephony.Mms.SUBJECT, it)
                put(Telephony.Mms.SUBJECT_CHARSET, UTF_8_MIB)
            }
        }
        val uri = resolver.insert(Telephony.Mms.Outbox.CONTENT_URI, values)
            ?: error("Unable to insert MMS outbox row")
        val id = ContentUris.parseId(uri)
        insertMmsAddress(id, address, MMS_ADDRESS_TO)
        parts.forEachIndexed { index, part -> insertOutgoingMmsPart(id, index, part) }
        return InsertedMessage(id, uri, threadId)
    }

    fun updateOutgoingMmsSent(providerId: Long) {
        checkWriteAccess()
        resolver.update(
            ContentUris.withAppendedId(Telephony.Mms.CONTENT_URI, providerId),
            ContentValues().apply {
                put(Telephony.Mms.MESSAGE_BOX, Telephony.Mms.MESSAGE_BOX_SENT)
                put(Telephony.Mms.DATE_SENT, System.currentTimeMillis() / 1000L)
                put(Telephony.Mms.RESPONSE_STATUS, MMS_RESPONSE_OK)
            },
            null,
            null,
        )
    }

    fun updateOutgoingMmsFailed(providerId: Long, resultCode: Int) {
        checkWriteAccess()
        resolver.update(
            ContentUris.withAppendedId(Telephony.Mms.CONTENT_URI, providerId),
            ContentValues().apply {
                put(Telephony.Mms.MESSAGE_BOX, Telephony.Mms.MESSAGE_BOX_FAILED)
                put(Telephony.Mms.RESPONSE_STATUS, mmsFailureStatus(resultCode))
            },
            null,
            null,
        )
    }

    fun mmsDownloadInfo(providerId: Long): MmsDownloadInfo? {
        checkReadAccess()
        resolver.query(
            ContentUris.withAppendedId(Telephony.Mms.CONTENT_URI, providerId),
            arrayOf(
                Telephony.Mms._ID,
                Telephony.Mms.THREAD_ID,
                Telephony.Mms.CONTENT_LOCATION,
                Telephony.Mms.TRANSACTION_ID,
                Telephony.Mms.SUBSCRIPTION_ID,
                Telephony.Mms.DATE,
            ),
            null,
            null,
            null,
        )?.use { c ->
            if (!c.moveToFirst()) return null
            val subIdx = c.getColumnIndex(Telephony.Mms.SUBSCRIPTION_ID)
            return MmsDownloadInfo(
                providerId = c.getLong(c.getColumnIndexOrThrow(Telephony.Mms._ID)),
                threadId = c.getLong(c.getColumnIndexOrThrow(Telephony.Mms.THREAD_ID)),
                address = mmsAddress(providerId, MessageDirection.INCOMING),
                contentLocation = c.getString(c.getColumnIndexOrThrow(Telephony.Mms.CONTENT_LOCATION)).orEmpty(),
                transactionId = c.getString(c.getColumnIndexOrThrow(Telephony.Mms.TRANSACTION_ID)).orEmpty(),
                subscriptionId = readSubId(c, subIdx),
                timestampMillis = c.getLong(c.getColumnIndexOrThrow(Telephony.Mms.DATE)).safeMmsDateMillis(),
            )
        }
        return null
    }

    fun markThreadRead(threadId: Long): Int {
        checkWriteAccess()
        val values = ContentValues().apply {
            put(Telephony.Sms.READ, 1)
            put(Telephony.Sms.SEEN, 1)
        }
        val sms = resolver.update(
            Telephony.Sms.CONTENT_URI,
            values,
            "${Telephony.Sms.THREAD_ID} = ? AND (${Telephony.Sms.READ} = 0 OR ${Telephony.Sms.SEEN} = 0)",
            arrayOf(threadId.toString()),
        )
        val mms = resolver.update(
            Telephony.Mms.CONTENT_URI,
            ContentValues().apply { put(Telephony.Mms.READ, 1); put(Telephony.Mms.SEEN, 1) },
            "${Telephony.Mms.THREAD_ID} = ? AND (${Telephony.Mms.READ} = 0 OR ${Telephony.Mms.SEEN} = 0)",
            arrayOf(threadId.toString()),
        )
        return sms + mms
    }

    fun markThreadUnread(threadId: Long): Int {
        checkWriteAccess()
        val latest = loadMessages(threadId, limit = 1).lastOrNull() ?: return 0
        val baseUri = if (latest.transport == MessageTransport.MMS) Telephony.Mms.CONTENT_URI else Telephony.Sms.CONTENT_URI
        return resolver.update(
            ContentUris.withAppendedId(baseUri, latest.id),
            ContentValues().apply { put("read", 0); put("seen", 0) },
            null,
            null,
        )
    }

    fun deleteMessage(messageId: Long, transport: MessageTransport = MessageTransport.SMS): Boolean {
        checkWriteAccess()
        val baseUri = if (transport == MessageTransport.MMS) Telephony.Mms.CONTENT_URI else Telephony.Sms.CONTENT_URI
        return resolver.delete(ContentUris.withAppendedId(baseUri, messageId), null, null) > 0
    }

    fun deleteThread(threadId: Long): Boolean {
        checkWriteAccess()
        val sms = resolver.delete(
            Telephony.Sms.CONTENT_URI,
            "${Telephony.Sms.THREAD_ID} = ?",
            arrayOf(threadId.toString()),
        )
        val mms = resolver.delete(
            Telephony.Mms.CONTENT_URI,
            "${Telephony.Mms.THREAD_ID} = ?",
            arrayOf(threadId.toString()),
        )
        return sms + mms > 0
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
        var latestDate = Long.MIN_VALUE
        var latestSubId: Int? = null
        resolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Sms.SUBSCRIPTION_ID, Telephony.Sms.DATE),
            "${Telephony.Sms.THREAD_ID} = ?",
            arrayOf(threadId.toString()),
            "${Telephony.Sms.DATE} DESC",
        )?.use { c ->
            val index = c.getColumnIndex(Telephony.Sms.SUBSCRIPTION_ID)
            if (index >= 0 && c.moveToFirst() && !c.isNull(index)) {
                latestSubId = c.getInt(index).takeIf { it >= 0 }
                latestDate = c.getLong(c.getColumnIndexOrThrow(Telephony.Sms.DATE))
            }
        }
        resolver.query(
            Telephony.Mms.Inbox.CONTENT_URI,
            arrayOf(Telephony.Mms.SUBSCRIPTION_ID, Telephony.Mms.DATE),
            "${Telephony.Mms.THREAD_ID} = ?",
            arrayOf(threadId.toString()),
            "${Telephony.Mms.DATE} DESC",
        )?.use { c ->
            val index = c.getColumnIndex(Telephony.Mms.SUBSCRIPTION_ID)
            if (index >= 0 && c.moveToFirst() && !c.isNull(index)) {
                val date = c.getLong(c.getColumnIndexOrThrow(Telephony.Mms.DATE)).safeMmsDateMillis()
                if (date > latestDate) latestSubId = c.getInt(index).takeIf { it >= 0 }
            }
        }
        return latestSubId
    }

    fun identityForAddress(address: String): ContactIdentity? = contactIdentity(address)

    fun clearContactCache() {
        contactCache.clear()
        missingContactCache.clear()
    }

    private data class MmsParts(val text: String, val attachments: List<MessageAttachment>)

    private fun mmsParts(messageId: Long): MmsParts {
        val texts = mutableListOf<String>()
        val attachments = mutableListOf<MessageAttachment>()
        val uri = mmsPartCollectionUri(messageId)
        resolver.query(
            uri,
            arrayOf(
                Telephony.Mms.Part._ID,
                Telephony.Mms.Part.CONTENT_TYPE,
                Telephony.Mms.Part.NAME,
                Telephony.Mms.Part.FILENAME,
                Telephony.Mms.Part.TEXT,
            ),
            null,
            null,
            "${Telephony.Mms.Part.SEQ} ASC",
        )?.use { c ->
            val idIdx = c.getColumnIndexOrThrow(Telephony.Mms.Part._ID)
            val typeIdx = c.getColumnIndexOrThrow(Telephony.Mms.Part.CONTENT_TYPE)
            val nameIdx = c.getColumnIndex(Telephony.Mms.Part.NAME)
            val fileIdx = c.getColumnIndex(Telephony.Mms.Part.FILENAME)
            val textIdx = c.getColumnIndex(Telephony.Mms.Part.TEXT)
            while (c.moveToNext()) {
                val partId = c.getLong(idIdx)
                val mime = c.getString(typeIdx)?.lowercase().orEmpty()
                val partUri = ContentUris.withAppendedId(Telephony.Mms.Part.CONTENT_URI, partId)
                if (mime == MmsPduCodec.MIME_TEXT) {
                    val stored = if (textIdx >= 0) c.getString(textIdx) else null
                    val text = stored ?: runCatching {
                        resolver.openInputStream(partUri)?.use { stream ->
                            stream.readAtMost(MAX_MMS_TEXT_BYTES).toString(Charsets.UTF_8)
                        }
                    }.getOrNull()
                    text?.takeIf(String::isNotBlank)?.let(texts::add)
                } else if (mime.startsWith("image/")) {
                    attachments += MessageAttachment(
                        partId = partId,
                        contentUri = partUri.toString(),
                        mimeType = mime,
                        name = (if (fileIdx >= 0) c.getString(fileIdx) else null)
                            ?: (if (nameIdx >= 0) c.getString(nameIdx) else null),
                    )
                }
            }
        }
        return MmsParts(texts.joinToString("\n"), attachments)
    }

    private fun mmsAddress(messageId: Long, direction: MessageDirection): String {
        val preferredType = if (direction == MessageDirection.INCOMING) MMS_ADDRESS_FROM else MMS_ADDRESS_TO
        var fallback = ""
        resolver.query(
            mmsAddrUri(messageId),
            arrayOf(Telephony.Mms.Addr.ADDRESS, Telephony.Mms.Addr.TYPE),
            null,
            null,
            null,
        )?.use { c ->
            val addressIdx = c.getColumnIndexOrThrow(Telephony.Mms.Addr.ADDRESS)
            val typeIdx = c.getColumnIndexOrThrow(Telephony.Mms.Addr.TYPE)
            while (c.moveToNext()) {
                val address = c.getString(addressIdx).orEmpty().substringBefore('/').trim()
                if (address.isBlank() || address.equals("insert-address-token", ignoreCase = true)) continue
                if (fallback.isBlank()) fallback = address
                if (c.getInt(typeIdx) == preferredType) return address
            }
        }
        return fallback
    }

    private fun mmsSnippet(messageId: Long, subject: String): String {
        if (subject.isNotBlank()) return subject
        val parts = mmsParts(messageId)
        return parts.text.takeIf(String::isNotBlank)
            ?: if (parts.attachments.isNotEmpty()) "📷 MMS" else "MMS"
    }

    private fun insertMmsAddress(messageId: Long, address: String, type: Int) {
        resolver.insert(
            mmsAddrUri(messageId),
            ContentValues().apply {
                put(Telephony.Mms.Addr.MSG_ID, messageId)
                put(Telephony.Mms.Addr.ADDRESS, address)
                put(Telephony.Mms.Addr.TYPE, type)
                put(Telephony.Mms.Addr.CHARSET, UTF_8_MIB)
            },
        ) ?: error("Unable to insert MMS address")
    }

    private fun insertMmsPart(messageId: Long, index: Int, part: MmsPduCodec.Part) {
        val values = ContentValues().apply {
            put(Telephony.Mms.Part.MSG_ID, messageId)
            put(Telephony.Mms.Part.SEQ, index)
            put(Telephony.Mms.Part.CONTENT_TYPE, part.mimeType.lowercase())
            put(Telephony.Mms.Part.NAME, part.name ?: part.contentLocation ?: "part-${index + 1}")
            put(Telephony.Mms.Part.CONTENT_LOCATION, part.contentLocation)
            put(Telephony.Mms.Part.CONTENT_ID, part.contentId)
            part.charsetMib?.let { put(Telephony.Mms.Part.CHARSET, it) }
            if (part.mimeType.equals(MmsPduCodec.MIME_TEXT, ignoreCase = true)) {
                put(Telephony.Mms.Part.TEXT, part.text().orEmpty())
            }
        }
        val uri = resolver.insert(mmsPartCollectionUri(messageId), values)
            ?: error("Unable to insert MMS part")
        if (!part.mimeType.equals(MmsPduCodec.MIME_TEXT, ignoreCase = true)) {
            resolver.openOutputStream(uri, "w")?.use { it.write(part.data) }
                ?: error("Unable to write MMS part")
        }
    }

    private fun insertOutgoingMmsPart(messageId: Long, index: Int, part: MmsPduCodec.OutgoingPart) {
        val values = ContentValues().apply {
            put(Telephony.Mms.Part.MSG_ID, messageId)
            put(Telephony.Mms.Part.SEQ, index)
            put(Telephony.Mms.Part.CONTENT_TYPE, part.mimeType.lowercase())
            put(Telephony.Mms.Part.NAME, part.name)
            put(Telephony.Mms.Part.FILENAME, part.name)
            put(Telephony.Mms.Part.CONTENT_LOCATION, part.name)
            part.charsetMib?.let { put(Telephony.Mms.Part.CHARSET, it) }
            if (part.mimeType.equals(MmsPduCodec.MIME_TEXT, ignoreCase = true)) {
                put(Telephony.Mms.Part.TEXT, part.data.toString(Charsets.UTF_8))
            }
        }
        val uri = resolver.insert(mmsPartCollectionUri(messageId), values)
            ?: error("Unable to insert outgoing MMS part")
        if (!part.mimeType.equals(MmsPduCodec.MIME_TEXT, ignoreCase = true)) {
            resolver.openOutputStream(uri, "w")?.use { it.write(part.data) }
                ?: error("Unable to write outgoing MMS part")
        }
    }

    private fun findMmsByTransactionId(transactionId: String): InsertedMessage? {
        resolver.query(
            Telephony.Mms.CONTENT_URI,
            arrayOf(Telephony.Mms._ID, Telephony.Mms.THREAD_ID),
            "${Telephony.Mms.TRANSACTION_ID} = ?",
            arrayOf(transactionId),
            null,
        )?.use { c ->
            if (c.moveToFirst()) {
                val id = c.getLong(c.getColumnIndexOrThrow(Telephony.Mms._ID))
                val thread = c.getLong(c.getColumnIndexOrThrow(Telephony.Mms.THREAD_ID))
                return InsertedMessage(id, ContentUris.withAppendedId(Telephony.Mms.CONTENT_URI, id), thread)
            }
        }
        return null
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
        check(SmsRole.canRead(context)) { "MCAS is not allowed to read SMS" }
    }

    private fun checkWriteAccess() {
        check(SmsRole.isHeld(context)) { "MCAS is not the default SMS app" }
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

    private fun mmsDirectionOf(box: Int): MessageDirection =
        if (box == Telephony.Mms.MESSAGE_BOX_INBOX) MessageDirection.INCOMING else MessageDirection.OUTGOING

    private fun mmsStateOf(box: Int, responseStatus: Int?): MessageState = when (box) {
        Telephony.Mms.MESSAGE_BOX_INBOX -> MessageState.RECEIVED
        Telephony.Mms.MESSAGE_BOX_OUTBOX -> MessageState.SENDING
        Telephony.Mms.MESSAGE_BOX_FAILED -> MessageState.FAILED
        Telephony.Mms.MESSAGE_BOX_SENT -> if (responseStatus == MMS_RESPONSE_OK) MessageState.SENT_TO_NETWORK else MessageState.SENT_TO_NETWORK
        else -> MessageState.QUEUED
    }

    private fun Long.safeMmsDateMillis(): Long = if (this in 1..9_999_999_999L) this * 1000L else this

    private fun mmsAddrUri(messageId: Long): Uri = Telephony.Mms.CONTENT_URI.buildUpon()
        .appendPath(messageId.toString()).appendPath("addr").build()

    private fun mmsPartCollectionUri(messageId: Long): Uri = Telephony.Mms.CONTENT_URI.buildUpon()
        .appendPath(messageId.toString()).appendPath("part").build()

    private fun InputStream.readAtMost(limit: Int): ByteArray {
        val output = ByteArrayOutputStream(minOf(limit, 8 * 1024))
        val buffer = ByteArray(4 * 1024)
        var remaining = limit
        while (remaining > 0) {
            val read = read(buffer, 0, minOf(buffer.size, remaining))
            if (read <= 0) break
            output.write(buffer, 0, read)
            remaining -= read
        }
        return output.toByteArray()
    }

    private fun mmsFailureStatus(resultCode: Int): Int = when (resultCode) {
        android.telephony.SmsManager.MMS_ERROR_RETRY,
        android.telephony.SmsManager.MMS_ERROR_UNABLE_CONNECT_MMS,
        android.telephony.SmsManager.MMS_ERROR_NO_DATA_NETWORK,
        android.telephony.SmsManager.MMS_ERROR_HTTP_FAILURE -> MMS_RESPONSE_TRANSIENT_FAILURE
        else -> MMS_RESPONSE_PERMANENT_FAILURE
    }

    companion object {
        private const val MMS_NOTIFICATION_IND = 0x82
        private const val MMS_SEND_REQ = 0x80
        private const val MMS_RETRIEVE_CONF = 0x84
        private const val MMS_VERSION_1_2 = 0x12
        private const val MMS_RETRIEVE_OK = 0x80
        private const val MMS_RESPONSE_OK = 0x80
        private const val MMS_RESPONSE_TRANSIENT_FAILURE = 0xC0
        private const val MMS_RESPONSE_PERMANENT_FAILURE = 0xE0
        private const val MMS_PRIORITY_NORMAL = 0x81
        private const val MMS_ADDRESS_FROM = 0x89
        private const val MMS_ADDRESS_TO = 0x97
        private const val UTF_8_MIB = 106
        private const val MAX_MMS_TEXT_BYTES = 64 * 1024
    }
}

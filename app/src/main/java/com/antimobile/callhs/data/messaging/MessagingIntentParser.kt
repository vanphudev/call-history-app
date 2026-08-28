package com.antimobile.callhs.data.messaging

import android.content.Intent
import android.net.Uri
import com.antimobile.callhs.data.messaging.model.MessagingLaunch

object MessagingIntentParser {
    const val ACTION_OPEN_CONVERSATION = "com.antimobile.callhs.action.OPEN_CONVERSATION"
    const val EXTRA_THREAD_ID = "messaging_thread_id"
    const val EXTRA_ADDRESS = "messaging_address"

    fun parse(intent: Intent?): MessagingLaunch? {
        intent ?: return null
        if (intent.action == ACTION_OPEN_CONVERSATION) {
            val threadId = intent.getLongExtra(EXTRA_THREAD_ID, -1L).takeIf { it >= 0L }
            val address = intent.getStringExtra(EXTRA_ADDRESS)?.trim()?.takeIf(String::isNotEmpty)
            if (threadId == null && address == null) return null
            return MessagingLaunch(threadId = threadId, recipient = address)
        }
        if (intent.action != Intent.ACTION_SENDTO) return null
        val uri = intent.data ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme !in setOf("sms", "smsto", "mms", "mmsto")) return null
        val rawRecipient = uri.schemeSpecificPart.substringBefore('?').trim()
        val recipients = SmsRecipientParser.parse(rawRecipient)
        val uriParameters = opaqueUriParameters(uri)
        val body = sequenceOf(
            intent.getCharSequenceExtra("sms_body")?.toString(),
            intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString(),
            uriParameters["body"],
        ).firstOrNull { !it.isNullOrBlank() }.orEmpty()
        val hasMmsPayload = scheme.startsWith("mms") ||
            !intent.getCharSequenceExtra("subject").isNullOrBlank() ||
            !uriParameters["subject"].isNullOrBlank() ||
            intent.clipData != null ||
            intent.hasExtra(Intent.EXTRA_STREAM)
        return MessagingLaunch(
            recipient = recipients.singleOrNull(),
            body = body,
            unsupportedMultipleRecipients = recipients.size > 1,
            unsupportedMmsPayload = hasMmsPayload,
        )
    }

    /** sms:/smsto: là opaque URI trên Android nên getQueryParameter() không đọc được phần sau dấu ?. */
    private fun opaqueUriParameters(uri: Uri): Map<String, String> {
        val encodedQuery = uri.encodedSchemeSpecificPart?.substringAfter('?', missingDelimiterValue = "").orEmpty()
        if (encodedQuery.isEmpty()) return emptyMap()
        return encodedQuery.split('&').mapNotNull { pair ->
            val encodedKey = pair.substringBefore('=', missingDelimiterValue = pair)
            if (encodedKey.isBlank()) return@mapNotNull null
            val encodedValue = pair.substringAfter('=', missingDelimiterValue = "")
            val key = Uri.decode(encodedKey.replace('+', ' ')).lowercase()
            key to Uri.decode(encodedValue.replace('+', ' '))
        }.toMap()
    }
}

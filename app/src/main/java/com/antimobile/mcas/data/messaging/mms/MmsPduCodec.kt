package com.antimobile.mcas.data.messaging.mms

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Codec MMS 1.2 tối thiểu cho giai đoạn ảnh + văn bản.
 *
 * Không dùng các lớp com.google.android.mms ẩn của framework. Dữ liệu đầu vào luôn bị chặn
 * kích thước/số part trước khi cấp phát để một WAP push lỗi không thể làm cạn bộ nhớ ứng dụng.
 */
object MmsPduCodec {
    const val MIME_MMS = "application/vnd.wap.mms-message"
    const val MIME_TEXT = "text/plain"
    const val MIME_JPEG = "image/jpeg"
    const val MIME_PNG = "image/png"
    const val MIME_GIF = "image/gif"
    const val MIME_SMIL = "application/smil"

    private const val MAX_PDU_BYTES = 4 * 1024 * 1024
    private const val MAX_PART_BYTES = 3 * 1024 * 1024
    private const val MAX_PARTS = 24
    private const val MAX_TEXT_BYTES = 64 * 1024

    private const val MESSAGE_TYPE = 0x8C
    private const val TRANSACTION_ID = 0x98
    private const val MMS_VERSION = 0x8D
    private const val FROM = 0x89
    private const val TO = 0x97
    private const val SUBJECT = 0x96
    private const val DATE = 0x85
    private const val MESSAGE_CLASS = 0x8A
    private const val EXPIRY = 0x88
    private const val PRIORITY = 0x8F
    private const val DELIVERY_REPORT = 0x86
    private const val READ_REPORT = 0x90
    private const val MESSAGE_SIZE = 0x8E
    private const val MESSAGE_ID = 0x8B
    private const val CONTENT_LOCATION = 0x83
    private const val RETRIEVE_STATUS = 0x99
    private const val RETRIEVE_TEXT = 0x9A
    private const val CONTENT_TYPE = 0x84

    private const val SEND_REQ = 0x80
    private const val NOTIFICATION_IND = 0x82
    private const val RETRIEVE_CONF = 0x84
    private const val MMS_1_2 = 0x12
    private const val UTF_8_MIB = 106
    private const val FROM_INSERT_ADDRESS_TOKEN = 0x81
    private const val FROM_ADDRESS_PRESENT_TOKEN = 0x80
    private const val RELATIVE_TOKEN = 0x81

    private const val PARAM_CHARSET = 0x81
    private const val PARAM_TYPE = 0x89
    private const val PARAM_DEP_NAME = 0x85
    private const val PARAM_NAME = 0x97
    private const val PART_CONTENT_LOCATION = 0x8E
    private const val PART_CONTENT_ID = 0xC0
    private const val PART_CONTENT_DISPOSITION = 0xC5
    private const val PART_DEP_CONTENT_DISPOSITION = 0xAE
    private const val PART_CONTENT_TRANSFER_ENCODING = 0xC8

    data class Notification(
        val transactionId: String,
        val contentLocation: String,
        val from: String,
        val subject: String?,
        val expiryMillis: Long?,
        val messageSize: Long?,
    )

    data class Retrieved(
        val from: String,
        val recipients: List<String>,
        val subject: String?,
        val dateMillis: Long?,
        val messageId: String?,
        val retrieveStatus: Int?,
        val parts: List<Part>,
    ) {
        val text: String
            get() = parts.filter { it.mimeType.equals(MIME_TEXT, ignoreCase = true) }
                .joinToString("\n") { it.text().orEmpty() }
                .trim()
    }

    data class Part(
        val mimeType: String,
        val name: String?,
        val contentId: String?,
        val contentLocation: String?,
        val charsetMib: Int?,
        val data: ByteArray,
    ) {
        fun text(): String? {
            if (!mimeType.startsWith("text/", ignoreCase = true)) return null
            val charset = when (charsetMib) {
                3 -> StandardCharsets.US_ASCII
                4 -> StandardCharsets.ISO_8859_1
                UTF_8_MIB, null -> StandardCharsets.UTF_8
                else -> StandardCharsets.UTF_8
            }
            return data.toString(charset).trimEnd('\u0000')
        }
    }

    data class OutgoingPart(
        val mimeType: String,
        val name: String,
        val data: ByteArray,
        val charsetMib: Int? = null,
    )

    fun parseNotification(data: ByteArray): Result<Notification> = runCatching {
        requirePduSize(data)
        val reader = Reader(data)
        var type: Int? = null
        var transactionId: String? = null
        var location: String? = null
        var from = ""
        var subject: String? = null
        var expiry: Long? = null
        var size: Long? = null

        while (reader.remaining > 0) {
            val field = reader.read()
            when (field) {
                MESSAGE_TYPE -> type = reader.read()
                TRANSACTION_ID -> transactionId = reader.readTextString()
                CONTENT_LOCATION -> location = reader.readTextString()
                FROM -> from = reader.readFrom()
                SUBJECT -> subject = reader.readEncodedString().takeIf(String::isNotBlank)
                EXPIRY -> expiry = reader.readExpiryMillis()
                MESSAGE_SIZE -> size = reader.readLongInteger()
                MMS_VERSION -> reader.readIntegerValue()
                else -> reader.skipHeaderValue(field)
            }
        }
        require(type == NOTIFICATION_IND) { "Not an MMS notification.ind" }
        Notification(
            transactionId = requireNotNull(transactionId?.takeIf(String::isNotBlank)) { "Missing transaction id" },
            contentLocation = requireNotNull(location?.takeIf(String::isNotBlank)) { "Missing content location" },
            from = normalizeAddress(from),
            subject = subject,
            expiryMillis = expiry,
            messageSize = size,
        )
    }

    fun parseRetrieved(data: ByteArray): Result<Retrieved> = runCatching {
        requirePduSize(data)
        val reader = Reader(data)
        var type: Int? = null
        var from = ""
        val recipients = mutableListOf<String>()
        var subject: String? = null
        var dateMillis: Long? = null
        var messageId: String? = null
        var retrieveStatus: Int? = null
        var bodyFound = false

        while (reader.remaining > 0 && !bodyFound) {
            val field = reader.read()
            when (field) {
                MESSAGE_TYPE -> type = reader.read()
                FROM -> from = reader.readFrom()
                TO -> recipients += normalizeAddress(reader.readEncodedString())
                SUBJECT -> subject = reader.readEncodedString().takeIf(String::isNotBlank)
                DATE -> dateMillis = reader.readLongInteger().safeSecondsToMillis()
                MESSAGE_ID -> messageId = reader.readTextString().takeIf(String::isNotBlank)
                RETRIEVE_STATUS -> retrieveStatus = reader.read()
                RETRIEVE_TEXT -> reader.readEncodedString()
                TRANSACTION_ID -> reader.readTextString()
                MMS_VERSION -> reader.readIntegerValue()
                CONTENT_TYPE -> {
                    val contentType = reader.readContentType().mimeType
                    require(contentType.contains("multipart", ignoreCase = true)) {
                        "Unsupported MMS body type: $contentType"
                    }
                    bodyFound = true
                }
                else -> reader.skipHeaderValue(field)
            }
        }
        require(type == RETRIEVE_CONF) { "Not an MMS retrieve.conf" }
        require(bodyFound) { "MMS has no body" }
        val count = reader.readUintvar()
        require(count in 0..MAX_PARTS) { "Invalid MMS part count" }
        val parts = ArrayList<Part>(count)
        repeat(count) {
            val headerLength = reader.readUintvar()
            val dataLength = reader.readUintvar()
            require(headerLength in 1..reader.remaining) { "Invalid MMS part header" }
            require(dataLength in 0..MAX_PART_BYTES && dataLength <= reader.remaining - headerLength) {
                "Invalid MMS part data"
            }
            val headerEnd = reader.position + headerLength
            val contentType = reader.readContentType()
            var name = contentType.name
            var contentId: String? = null
            var contentLocation: String? = null
            var transferEncoding: String? = null
            while (reader.position < headerEnd) {
                when (val partHeader = reader.read()) {
                    PART_CONTENT_LOCATION -> contentLocation = reader.readTextString()
                    PART_CONTENT_ID -> contentId = reader.readQuotedOrTextString().trim('<', '>')
                    PART_CONTENT_DISPOSITION, PART_DEP_CONTENT_DISPOSITION -> {
                        val length = reader.readValueLength()
                        val end = reader.position + length.coerceAtMost(headerEnd - reader.position)
                        if (reader.position < end) reader.read() // inline/attachment/from-data
                        while (reader.position < end) {
                            when (reader.read()) {
                                PARAM_NAME, 0x98 -> name = reader.readTextString()
                                else -> reader.skipGenericValue(end)
                            }
                        }
                        reader.position = end
                    }
                    PART_CONTENT_TRANSFER_ENCODING -> transferEncoding = reader.readTextString()
                    in 0x20..0x7F -> {
                        reader.position--
                        reader.readTextString()
                        reader.skipGenericValue(headerEnd)
                    }
                    else -> reader.skipGenericValue(headerEnd)
                }
            }
            reader.position = headerEnd
            var partData = reader.readBytes(dataLength)
            partData = when {
                transferEncoding.equals("base64", ignoreCase = true) -> Base64.getMimeDecoder().decode(partData)
                transferEncoding.equals("quoted-printable", ignoreCase = true) -> decodeQuotedPrintable(partData)
                else -> partData
            }
            require(partData.size <= MAX_PART_BYTES) { "Decoded MMS part is too large" }
            requireAllowedIncomingMime(contentType.mimeType, partData.size)
            parts += Part(
                mimeType = contentType.mimeType.lowercase(),
                name = name?.safeFileName(),
                contentId = contentId,
                contentLocation = contentLocation?.safeFileName(),
                charsetMib = contentType.charsetMib,
                data = partData,
            )
        }
        Retrieved(
            from = normalizeAddress(from),
            recipients = recipients.filter(String::isNotBlank),
            subject = subject,
            dateMillis = dateMillis,
            messageId = messageId,
            retrieveStatus = retrieveStatus,
            parts = parts,
        )
    }

    fun composeSendRequest(
        transactionId: String,
        recipients: List<String>,
        subject: String?,
        parts: List<OutgoingPart>,
        nowSeconds: Long = System.currentTimeMillis() / 1000L,
    ): ByteArray {
        require(transactionId.isNotBlank())
        val safeRecipients = recipients.map(::normalizeAddress).filter(String::isNotBlank).distinct()
        require(safeRecipients.isNotEmpty())
        require(parts.isNotEmpty() && parts.size <= MAX_PARTS)
        parts.forEach {
            requireAllowedOutgoingMime(it.mimeType, it.data.size)
            require(it.name.isNotBlank())
        }

        val out = Writer()
        out.octet(MESSAGE_TYPE).octet(SEND_REQ)
        out.octet(TRANSACTION_ID).textString(transactionId)
        out.octet(MMS_VERSION).shortInteger(MMS_1_2)
        out.octet(DATE).longInteger(nowSeconds)
        out.octet(FROM).octet(1).octet(FROM_INSERT_ADDRESS_TOKEN)
        safeRecipients.forEach { recipient ->
            val typed = if ('@' in recipient) recipient else "$recipient/TYPE=PLMN"
            out.octet(TO).encodedString(typed)
        }
        subject?.trim()?.takeIf(String::isNotBlank)?.let {
            out.octet(SUBJECT).encodedString(it.take(80))
        }
        out.octet(MESSAGE_CLASS).octet(0x80) // personal
        out.octet(EXPIRY).valueLengthBytes(Writer().octet(RELATIVE_TOKEN).longInteger(7 * 24 * 60 * 60L).bytes())
        out.octet(PRIORITY).octet(0x81) // normal
        out.octet(DELIVERY_REPORT).octet(0x81) // no
        out.octet(READ_REPORT).octet(0x81) // no
        out.octet(CONTENT_TYPE)
        val bodyType = Writer().shortInteger(0x23) // application/vnd.wap.multipart.mixed
            .octet(PARAM_TYPE).textString(parts.first().mimeType.lowercase()).bytes()
        out.valueLengthBytes(bodyType)
        out.uintvar(parts.size)

        parts.forEachIndexed { index, part ->
            val mime = part.mimeType.lowercase()
            val safeName = part.name.safeFileName().ifBlank { "part-${index + 1}" }
            val contentTypeValue = Writer().apply {
                CONTENT_TYPE_INDEX[mime]?.let(::shortInteger) ?: textString(mime)
                octet(PARAM_DEP_NAME).textString(safeName)
                if (part.charsetMib != null) octet(PARAM_CHARSET).shortInteger(part.charsetMib)
            }.bytes()
            val header = Writer().valueLengthBytes(contentTypeValue)
                .octet(PART_CONTENT_LOCATION).textString(safeName)
                .bytes()
            out.uintvar(header.size).uintvar(part.data.size).raw(header).raw(part.data)
        }
        return out.bytes().also { requirePduSize(it) }
    }

    private fun requirePduSize(data: ByteArray) {
        require(data.isNotEmpty() && data.size <= MAX_PDU_BYTES) { "Invalid MMS PDU size" }
    }

    private fun requireAllowedIncomingMime(mime: String, size: Int) {
        val normalized = mime.lowercase()
        require(normalized in ALLOWED_INCOMING_MIME) { "Unsupported MMS part type" }
        if (normalized.startsWith("text/") || normalized == MIME_SMIL) {
            require(size <= MAX_TEXT_BYTES) { "MMS text part is too large" }
        }
    }

    private fun requireAllowedOutgoingMime(mime: String, size: Int) {
        val normalized = mime.lowercase()
        require(normalized in ALLOWED_OUTGOING_MIME) { "Unsupported outgoing MMS part type" }
        require(size in 1..MAX_PART_BYTES) { "Invalid outgoing MMS part size" }
        if (normalized == MIME_TEXT) require(size <= MAX_TEXT_BYTES) { "MMS text part is too large" }
    }

    private fun normalizeAddress(value: String): String = value.substringBefore('/').trim()

    private fun String.safeFileName(): String = replace(Regex("[^A-Za-z0-9._-]"), "_").take(96)

    private fun Long.safeSecondsToMillis(): Long? = if (this in 1..(Long.MAX_VALUE / 1000L)) this * 1000L else null

    private fun decodeQuotedPrintable(input: ByteArray): ByteArray {
        val out = ByteArrayOutputStream(input.size)
        var i = 0
        while (i < input.size) {
            if (input[i] == '='.code.toByte() && i + 2 < input.size) {
                if (input[i + 1] == '\r'.code.toByte() && input[i + 2] == '\n'.code.toByte()) {
                    i += 3
                    continue
                }
                val high = input[i + 1].toInt().toChar().digitToIntOrNull(16)
                val low = input[i + 2].toInt().toChar().digitToIntOrNull(16)
                if (high != null && low != null) {
                    out.write((high shl 4) or low)
                    i += 3
                    continue
                }
            }
            out.write(input[i].toInt())
            i++
        }
        return out.toByteArray()
    }

    private data class ParsedContentType(val mimeType: String, val name: String?, val charsetMib: Int?)

    private class Reader(private val data: ByteArray) {
        var position: Int = 0
        val remaining: Int get() = data.size - position

        fun read(): Int {
            require(position < data.size) { "Unexpected end of MMS PDU" }
            return data[position++].toInt() and 0xFF
        }

        fun peek(): Int {
            require(position < data.size) { "Unexpected end of MMS PDU" }
            return data[position].toInt() and 0xFF
        }

        fun readBytes(length: Int): ByteArray {
            require(length >= 0 && length <= remaining)
            return data.copyOfRange(position, position + length).also { position += length }
        }

        fun readUintvar(): Int {
            var value = 0L
            repeat(5) {
                val octet = read()
                value = (value shl 7) or (octet and 0x7F).toLong()
                require(value <= Int.MAX_VALUE)
                if (octet and 0x80 == 0) return value.toInt()
            }
            error("Invalid uintvar")
        }

        fun readValueLength(): Int = when (val first = read()) {
            in 0..30 -> first
            31 -> readUintvar()
            else -> error("Invalid value length")
        }

        fun readLongInteger(): Long {
            val length = read()
            require(length in 1..8 && length <= remaining) { "Invalid long integer" }
            var value = 0L
            repeat(length) { value = (value shl 8) or read().toLong() }
            return value
        }

        fun readIntegerValue(): Long = if (peek() >= 0x80) (read() and 0x7F).toLong() else readLongInteger()

        fun readTextString(): String {
            if (remaining == 0) return ""
            if (peek() == 0x7F || peek() == 0x22) read()
            val start = position
            while (position < data.size && data[position].toInt() != 0) position++
            require(position < data.size) { "Unterminated MMS string" }
            val bytes = data.copyOfRange(start, position)
            position++
            return bytes.toString(StandardCharsets.UTF_8)
        }

        fun readQuotedOrTextString(): String = readTextString()

        fun readEncodedString(): String {
            if (remaining == 0) return ""
            if (peek() in 1..31) {
                val length = readValueLength()
                val end = position + length
                require(end <= data.size)
                val charsetMib = if (position < end) readIntegerValue().toInt() else UTF_8_MIB
                val bytesStart = position + if (position < end && peek() == 0x7F) 1 else 0
                if (bytesStart != position) position++
                while (position < end && data[position].toInt() != 0) position++
                val bytes = data.copyOfRange(bytesStart, position)
                position = end
                return bytes.toString(charsetForMib(charsetMib))
            }
            return readTextString()
        }

        fun readFrom(): String {
            val length = readValueLength()
            val end = position + length
            require(end <= data.size && position < end)
            val token = read()
            val value = if (token == FROM_ADDRESS_PRESENT_TOKEN && position < end) readEncodedString() else ""
            position = end
            return value
        }

        fun readExpiryMillis(): Long? {
            val length = readValueLength()
            val end = position + length
            require(end <= data.size && position < end)
            val token = read()
            val seconds = if (position < end) readLongInteger() else 0L
            position = end
            val absoluteSeconds = if (token == RELATIVE_TOKEN) System.currentTimeMillis() / 1000L + seconds else seconds
            return absoluteSeconds.safeSecondsToMillis()
        }

        fun readContentType(): ParsedContentType {
            var name: String? = null
            var charset: Int? = null
            if (peek() in 0..31) {
                val length = readValueLength()
                val end = position + length
                require(end <= data.size)
                val mime = readMediaType()
                while (position < end) {
                    when (read()) {
                        PARAM_CHARSET -> charset = readIntegerValue().toInt()
                        PARAM_DEP_NAME, PARAM_NAME -> name = readTextString()
                        PARAM_TYPE, 0x83 -> if (peek() >= 0x80) read() else readTextString()
                        0x8A, 0x99 -> readTextString() // start
                        else -> skipGenericValue(end)
                    }
                }
                position = end
                return ParsedContentType(mime, name, charset)
            }
            return ParsedContentType(readMediaType(), null, null)
        }

        private fun readMediaType(): String {
            return if (peek() >= 0x80) {
                val index = read() and 0x7F
                CONTENT_TYPES.getOrElse(index) { "application/octet-stream" }
            } else {
                readTextString().ifBlank { "application/octet-stream" }
            }
        }

        fun skipHeaderValue(field: Int) {
            when (field) {
                DATE, MESSAGE_SIZE -> readLongInteger()
                EXPIRY -> {
                    val length = readValueLength()
                    val end = position + length
                    require(end <= data.size)
                    position = end
                }
                TO -> readEncodedString()
                DELIVERY_REPORT, READ_REPORT, PRIORITY, RETRIEVE_STATUS -> read()
                MESSAGE_CLASS -> if (peek() >= 0x80) read() else readTextString()
                CONTENT_LOCATION, TRANSACTION_ID, MESSAGE_ID -> readTextString()
                SUBJECT, RETRIEVE_TEXT -> readEncodedString()
                CONTENT_TYPE -> readContentType()
                else -> skipGenericValue(data.size)
            }
        }

        fun skipGenericValue(bound: Int) {
            if (position >= bound) return
            when (peek()) {
                in 0..30 -> {
                    val end = position + 1 + peek()
                    require(end <= bound)
                    position = end
                }
                31 -> {
                    read()
                    val length = readUintvar()
                    require(length <= bound - position)
                    position += length
                }
                in 32..127 -> readTextString()
                else -> read()
            }
        }
    }

    private class Writer {
        private val out = ByteArrayOutputStream()

        fun octet(value: Int) = apply { out.write(value and 0xFF) }
        fun raw(value: ByteArray) = apply { out.write(value) }
        fun bytes(): ByteArray = out.toByteArray()

        fun shortInteger(value: Int) = octet((value and 0x7F) or 0x80)

        fun textString(value: String) = apply {
            val bytes = value.toByteArray(StandardCharsets.UTF_8)
            if (bytes.firstOrNull()?.toInt()?.and(0xFF) ?: 0 >= 0x80) octet(0x7F)
            raw(bytes).octet(0)
        }

        fun encodedString(value: String) = apply {
            val inner = Writer().shortInteger(UTF_8_MIB).textString(value).bytes()
            valueLengthBytes(inner)
        }

        fun longInteger(value: Long) = apply {
            require(value >= 0)
            var byteCount = 1
            while (byteCount < 8 && value ushr (byteCount * 8) != 0L) byteCount++
            octet(byteCount)
            for (shift in (byteCount - 1) * 8 downTo 0 step 8) octet((value ushr shift).toInt())
        }

        fun uintvar(value: Int) = apply {
            require(value >= 0)
            var groups = 1
            var max = 0x7F
            while (value > max && groups < 5) {
                groups++
                max = (max shl 7) or 0x7F
            }
            for (index in groups - 1 downTo 0) {
                val fragment = (value ushr (index * 7)) and 0x7F
                octet(if (index == 0) fragment else fragment or 0x80)
            }
        }

        fun valueLengthBytes(value: ByteArray) = apply {
            if (value.size < 31) octet(value.size) else octet(31).uintvar(value.size)
            raw(value)
        }
    }

    private fun charsetForMib(mib: Int): Charset = when (mib) {
        3 -> StandardCharsets.US_ASCII
        4 -> StandardCharsets.ISO_8859_1
        UTF_8_MIB -> StandardCharsets.UTF_8
        else -> StandardCharsets.UTF_8
    }

    private val ALLOWED_INCOMING_MIME = setOf(
        MIME_TEXT,
        MIME_JPEG,
        "image/jpg",
        MIME_PNG,
        MIME_GIF,
        "image/bmp",
        "image/webp",
        MIME_SMIL,
    )
    private val ALLOWED_OUTGOING_MIME = setOf(MIME_TEXT, MIME_JPEG)

    private val CONTENT_TYPE_INDEX = mapOf(
        MIME_TEXT to 0x03,
        MIME_GIF to 0x1D,
        MIME_JPEG to 0x1E,
        MIME_PNG to 0x20,
    )

    private val CONTENT_TYPES = arrayOf(
        "*/*", "text/*", "text/html", MIME_TEXT, "text/x-hdml", "text/x-ttml",
        "text/x-vCalendar", "text/x-vCard", "text/vnd.wap.wml", "text/vnd.wap.wmlscript",
        "text/vnd.wap.wta-event", "multipart/*", "multipart/mixed", "multipart/form-data",
        "multipart/byteranges", "multipart/alternative", "application/*", "application/java-vm",
        "application/x-www-form-urlencoded", "application/x-hdmlc", "application/vnd.wap.wmlc",
        "application/vnd.wap.wmlscriptc", "application/vnd.wap.wta-eventc", "application/vnd.wap.uaprof",
        "application/vnd.wap.wtls-ca-certificate", "application/vnd.wap.wtls-user-certificate",
        "application/x-x509-ca-cert", "application/x-x509-user-cert", "image/*", MIME_GIF, MIME_JPEG,
        "image/tiff", MIME_PNG, "image/vnd.wap.wbmp", "application/vnd.wap.multipart.*",
        "application/vnd.wap.multipart.mixed", "application/vnd.wap.multipart.form-data",
        "application/vnd.wap.multipart.byteranges", "application/vnd.wap.multipart.alternative",
        "application/xml", "text/xml", "application/vnd.wap.wbxml", "application/x-x968-cross-cert",
        "application/x-x968-ca-cert", "application/x-x968-user-cert", "text/vnd.wap.si",
        "application/vnd.wap.sic", "text/vnd.wap.sl", "application/vnd.wap.slc", "text/vnd.wap.co",
        "application/vnd.wap.coc", "application/vnd.wap.multipart.related", "application/vnd.wap.sia",
        "text/vnd.wap.connectivity-xml", "application/vnd.wap.connectivity-wbxml",
        "application/pkcs7-mime", "application/vnd.wap.hashed-certificate",
        "application/vnd.wap.signed-certificate", "application/vnd.wap.cert-response",
        "application/xhtml+xml", "application/wml+xml", "text/css", MIME_MMS,
    )
}

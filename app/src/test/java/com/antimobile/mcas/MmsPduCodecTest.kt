package com.antimobile.mcas

import com.antimobile.mcas.data.messaging.mms.MmsPduCodec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MmsPduCodecTest {
    @Test
    fun parsesNotificationInd() {
        val pdu = byteArrayOf(
            0x8C.toByte(), 0x82.toByte(),
            0x98.toByte(), 't'.code.toByte(), 'x'.code.toByte(), 0,
            0x8D.toByte(), 0x92.toByte(),
            0x89.toByte(), 0x0E, 0x80.toByte(), 0x0D, 0xEA.toByte(),
            '0'.code.toByte(), '9'.code.toByte(), '1'.code.toByte(), '2'.code.toByte(),
            '3'.code.toByte(), '4'.code.toByte(), '5'.code.toByte(), '6'.code.toByte(),
            '7'.code.toByte(), '8'.code.toByte(), 0,
            0x8E.toByte(), 0x02, 0x04, 0x00,
            0x83.toByte(), 'h'.code.toByte(), 't'.code.toByte(), 't'.code.toByte(), 'p'.code.toByte(),
            ':'.code.toByte(), '/'.code.toByte(), '/'.code.toByte(), 'm'.code.toByte(), 0,
        )

        val value = MmsPduCodec.parseNotification(pdu).getOrThrow()

        assertEquals("tx", value.transactionId)
        assertEquals("0912345678", value.from)
        assertEquals("http://m", value.contentLocation)
        assertEquals(1024L, value.messageSize)
    }

    @Test
    fun composedBodyCanBeParsedAsRetrieveConf() {
        val text = "Xin chào".toByteArray()
        val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1, 2, 3, 0xFF.toByte(), 0xD9.toByte())
        val sendReq = MmsPduCodec.composeSendRequest(
            transactionId = "mcas-1",
            recipients = listOf("0912345678"),
            subject = "Ảnh",
            parts = listOf(
                MmsPduCodec.OutgoingPart(MmsPduCodec.MIME_TEXT, "text.txt", text, charsetMib = 106),
                MmsPduCodec.OutgoingPart(MmsPduCodec.MIME_JPEG, "image.jpg", jpeg),
            ),
            nowSeconds = 1_700_000_000L,
        )
        // send.req và retrieve.conf dùng cùng encoding header/body; đổi type để kiểm thử parser độc lập.
        val retrieveConf = sendReq.copyOf().also { it[1] = 0x84.toByte() }

        val parsed = MmsPduCodec.parseRetrieved(retrieveConf).getOrThrow()

        assertEquals(listOf("0912345678"), parsed.recipients)
        assertEquals("Ảnh", parsed.subject)
        assertEquals("Xin chào", parsed.text)
        assertEquals(2, parsed.parts.size)
        assertArrayEquals(jpeg, parsed.parts[1].data)
    }

    @Test
    fun rejectsUnsupportedOutgoingMime() {
        val result = runCatching {
            MmsPduCodec.composeSendRequest(
                transactionId = "tx",
                recipients = listOf("0912345678"),
                subject = null,
                parts = listOf(MmsPduCodec.OutgoingPart("application/pdf", "file.pdf", byteArrayOf(1))),
            )
        }

        assertTrue(result.isFailure)
    }
}

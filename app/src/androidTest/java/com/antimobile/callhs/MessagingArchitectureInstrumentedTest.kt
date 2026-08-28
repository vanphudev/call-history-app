package com.antimobile.callhs

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.antimobile.callhs.data.messaging.MessagingIntentParser
import com.antimobile.callhs.receiver.messaging.MmsDeliverReceiver
import com.antimobile.callhs.receiver.messaging.SmsDeliverReceiver
import com.antimobile.callhs.service.messaging.RespondViaMessageService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MessagingArchitectureInstrumentedTest {
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun sendToIntentParsesRecipientAndBody() {
        val parsed = MessagingIntentParser.parse(
            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:0901234567?body=Xin%20ch%C3%A0o")),
        )

        requireNotNull(parsed)
        assertEquals("0901234567", parsed.recipient)
        assertEquals("Xin chào", parsed.body)
        assertFalse(parsed.unsupportedMultipleRecipients)
        assertFalse(parsed.unsupportedMmsPayload)
    }

    @Test
    fun multipleRecipientsAndMmsAreRejectedExplicitly() {
        val multiple = requireNotNull(
            MessagingIntentParser.parse(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:0901,0902"))),
        )
        val mms = requireNotNull(
            MessagingIntentParser.parse(Intent(Intent.ACTION_SENDTO, Uri.parse("mmsto:0901"))),
        )

        assertTrue(multiple.unsupportedMultipleRecipients)
        assertTrue(mms.unsupportedMmsPayload)
    }

    @Test
    fun defaultSmsComponentsHaveRequiredProtection() {
        val pm = context.packageManager
        val smsReceiver = pm.getReceiverInfo(ComponentName(context, SmsDeliverReceiver::class.java), 0)
        val mmsReceiver = pm.getReceiverInfo(ComponentName(context, MmsDeliverReceiver::class.java), 0)
        val respondService = pm.getServiceInfo(ComponentName(context, RespondViaMessageService::class.java), 0)

        assertTrue(smsReceiver.exported)
        assertEquals(Manifest.permission.BROADCAST_SMS, smsReceiver.permission)
        assertTrue(mmsReceiver.exported)
        assertEquals(Manifest.permission.BROADCAST_WAP_PUSH, mmsReceiver.permission)
        assertTrue(respondService.exported)
        assertEquals(Manifest.permission.SEND_RESPOND_VIA_MESSAGE, respondService.permission)
    }

    @Test
    fun mainActivityHandlesSmsSendTo() {
        val handlers = context.packageManager.queryIntentActivities(
            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:0901234567")),
            0,
        )
        assertTrue(handlers.any { it.activityInfo.packageName == context.packageName })
    }
}

package com.antimobile.mcas

import android.Manifest
import android.app.NotificationManager
import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.antimobile.mcas.data.blocking.CallBlockAction
import com.antimobile.mcas.data.blocking.CallBlockRepository
import com.antimobile.mcas.data.blocking.SaveNumberEntryResult
import com.antimobile.mcas.data.outgoing.OutgoingCallAlertDispatcher
import com.antimobile.mcas.data.outgoing.OutgoingCallAlertEvent
import com.antimobile.mcas.data.outgoing.OutgoingCallAlertReason
import com.antimobile.mcas.data.outgoing.OutgoingCallConfig
import com.antimobile.mcas.data.outgoing.OutgoingCallEventSource
import com.antimobile.mcas.data.outgoing.OutgoingCallNotifier
import com.antimobile.mcas.data.outgoing.OutgoingCallOverlay
import com.antimobile.mcas.data.outgoing.OutgoingCallPostResponseReceiver
import com.antimobile.mcas.data.outgoing.OutgoingCallPresentation
import com.antimobile.mcas.data.outgoing.OutgoingCallRedirectionService
import com.antimobile.mcas.data.outgoing.OutgoingCallRole
import com.antimobile.mcas.data.outgoing.OutgoingCallSettings
import com.antimobile.mcas.data.outgoing.OutgoingNumberList
import com.antimobile.mcas.util.Carrier
import com.antimobile.mcas.util.SimInfo
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OutgoingCallDeviceInstrumentedTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun telecomFastPathIsIsolatedAndPostWorkReturnsToMainAppProcess() {
        val packageManager = context.packageManager
        val service = packageManager.getServiceInfo(
            ComponentName(context, OutgoingCallRedirectionService::class.java),
            PackageManager.GET_META_DATA,
        )
        val receiver = packageManager.getReceiverInfo(
            ComponentName(context, OutgoingCallPostResponseReceiver::class.java),
            PackageManager.GET_META_DATA,
        )

        assertEquals("${context.packageName}:call_redirection", service.processName)
        assertTrue(service.exported)
        assertEquals(context.packageName, receiver.processName)
        assertFalse(receiver.exported)
    }

    @Test
    fun deviceHasDedicatedRoleAndPhonePermission() {
        val roles = context.getSystemService(RoleManager::class.java)
        assertTrue(roles.isRoleAvailable(RoleManager.ROLE_CALL_REDIRECTION))
        assertTrue(OutgoingCallRole.isHeld(context))
        assertEquals(
            PackageManager.PERMISSION_GRANTED,
            context.checkSelfPermission(Manifest.permission.READ_PHONE_STATE),
        )
    }

    @Test
    fun activeSimCarrierIsReadableWhenDeviceHasAnInsertedSim() {
        val sims = SimInfo.activeSims(context)
        assumeTrue("Device has no active SIM", sims.isNotEmpty())
        assertTrue("No carrier could be resolved for an active SIM", sims.any { it.carrier != null })
    }

    @Test
    fun offNetworkAnalysisPostsHeadsUpChannelQuicklyOnRealDevice() {
        val phoneAccount = context.getSystemService(TelecomManager::class.java)
            .callCapablePhoneAccounts
            .firstOrNull { carrierForPhoneAccount(it) != null }
        assumeTrue("No cellular PhoneAccount is available", phoneAccount != null)
        val simCarrier = carrierForPhoneAccount(requireNotNull(phoneAccount))
        assumeTrue("Selected PhoneAccount carrier unavailable", simCarrier != null)
        val targetNumber = sampleNumberForDifferentCarrier(requireNotNull(simCarrier))
        assertNotNull(Carrier.of(targetNumber))

        val original = OutgoingCallSettings.read(context)
        val notifications = context.getSystemService(NotificationManager::class.java)
        try {
            OutgoingCallSettings.replace(
                context,
                OutgoingCallConfig(
                    enabled = true,
                    notifyOffNetwork = true,
                    notifyBlocklist = false,
                    notifyAllowlist = false,
                    presentation = OutgoingCallPresentation.HEADS_UP,
                ),
            )
            OutgoingCallNotifier.ensureChannel(context)
            notifications.cancelAll()
            val started = android.os.SystemClock.elapsedRealtime()
            OutgoingCallAlertDispatcher.onOutgoingCall(
                context = context,
                handle = Uri.fromParts("tel", targetNumber, null),
                phoneAccount = phoneAccount,
                createdAtMillis = System.currentTimeMillis(),
                source = OutgoingCallEventSource.REDIRECTION,
            )

            assertTrue("Outgoing notification was not posted", waitForOutgoingNotification(notifications))
            assertTrue(
                "Outgoing alert exceeded 1500 ms",
                android.os.SystemClock.elapsedRealtime() - started < 1_500L,
            )
        } finally {
            notifications.cancelAll()
            OutgoingCallSettings.replace(context, original)
        }
    }

    @Test
    fun exactBlocklistAnalysisPostsNotificationFromColdDatabasePath() = runBlocking {
        val number = "0123456789"
        val repository = CallBlockRepository(context)
        val original = OutgoingCallSettings.read(context)
        val notifications = context.getSystemService(NotificationManager::class.java)
        try {
            assertEquals(
                SaveNumberEntryResult.SAVED,
                repository.upsertNumberEntry(CallBlockAction.BLOCK, number, "Device smoke test"),
            )
            OutgoingCallSettings.replace(
                context,
                OutgoingCallConfig(
                    enabled = true,
                    notifyOffNetwork = false,
                    notifyBlocklist = true,
                    notifyAllowlist = false,
                    presentation = OutgoingCallPresentation.HEADS_UP,
                ),
            )
            OutgoingCallNotifier.ensureChannel(context)
            notifications.cancelAll()
            val started = android.os.SystemClock.elapsedRealtime()
            OutgoingCallAlertDispatcher.onOutgoingCall(
                context = context,
                handle = Uri.fromParts("tel", number, null),
                phoneAccount = null,
                createdAtMillis = System.currentTimeMillis(),
                source = OutgoingCallEventSource.REDIRECTION,
            )

            assertTrue("Blocklist notification was not posted", waitForOutgoingNotification(notifications))
            assertTrue(
                "Blocklist alert exceeded 1500 ms",
                android.os.SystemClock.elapsedRealtime() - started < 1_500L,
            )
        } finally {
            repository.findEnabledExactNumberEntry(number)?.let { repository.deleteNumberEntry(it.id) }
            notifications.cancelAll()
            OutgoingCallSettings.replace(context, original)
        }
    }

    @Test
    fun overlayWindowCanAttachImmediatelyOnRealDevice() {
        assumeTrue("Overlay access unavailable", OutgoingCallOverlay.canDraw(context))
        val event = OutgoingCallAlertEvent(
            number = "0912345678",
            simCarrier = "Viettel",
            targetCarrier = "VinaPhone",
            membership = OutgoingNumberList.NONE,
            reasons = listOf(OutgoingCallAlertReason.OFF_NETWORK),
            createdAtMillis = System.currentTimeMillis(),
        )
        var shown = false
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            shown = OutgoingCallOverlay.show(context, event)
        }
        try {
            assertTrue("TYPE_APPLICATION_OVERLAY could not attach", shown)
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                OutgoingCallOverlay.dismiss()
            }
        }
    }

    private fun waitForOutgoingNotification(manager: NotificationManager): Boolean {
        repeat(75) {
            if (manager.activeNotifications.any { it.notification.channelId == OutgoingCallNotifier.CHANNEL_ID }) {
                return true
            }
            Thread.sleep(20L)
        }
        return false
    }

    private fun carrierForPhoneAccount(account: PhoneAccountHandle): String? {
        val base = context.getSystemService(TelephonyManager::class.java)
        val manager = base.createForPhoneAccountHandle(account) ?: return null
        val operator = manager.simOperator.orEmpty()
        return SimInfo.normalizeCarrier(
            name = manager.simOperatorName,
            mcc = operator.takeIf { it.length >= 5 }?.take(3),
            mnc = operator.takeIf { it.length >= 5 }?.drop(3),
        )
    }

    private fun sampleNumberForDifferentCarrier(simCarrier: String): String = when (simCarrier) {
        "Viettel" -> "0912345678"
        "VinaPhone" -> "0981234567"
        else -> "0981234567"
    }
}

package com.antimobile.mcas

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import com.antimobile.mcas.data.blocking.BlockNotificationMode
import com.antimobile.mcas.data.blocking.BlockRecordResult
import com.antimobile.mcas.data.blocking.CallBlockNotifier
import com.antimobile.mcas.data.blocking.CallBlockRuleType
import com.antimobile.mcas.data.blocking.CallBlockSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CallBlockNotificationInstrumentedTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun urgentChannelUsesPackagedAudibleAppSound() {
        CallBlockNotifier.ensureChannel(context)

        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = requireNotNull(manager.getNotificationChannel(CallBlockNotifier.CHANNEL_ID))
        val expectedSound = CallBlockNotifier.notificationSoundUri(context)

        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
        assertEquals(expectedSound, channel.sound)
        assertTrue(channel.shouldVibrate())
        assertTrue(channel.vibrationPattern?.any { it > 0L } == true)

        val descriptor = context.contentResolver.openAssetFileDescriptor(expectedSound, "r")
        assertNotNull(descriptor)
        descriptor!!.use { assertTrue(it.length != 0L) }
    }

    @Test
    fun blockedCallPostsAVisibleMaxPriorityNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.grantRuntimePermission(
                context.packageName,
                Manifest.permission.POST_NOTIFICATIONS,
            )
        }
        CallBlockSettings.setNotificationMode(context, BlockNotificationMode.EVERY_BLOCK)

        val eventId = 9_101_337L
        val notificationId = CallBlockNotifier.notificationId(eventId)
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.cancel(notificationId)

        val delivery = CallBlockNotifier.notifyBlocked(
            context = context,
            result = BlockRecordResult(
                historyId = eventId,
                rawNumber = "0901234567",
                ruleType = CallBlockRuleType.EXACT_NUMBER.storageKey,
                ruleValue = "0901234567",
                totalForNumber = 1,
                isNew = true,
            ),
            notificationEventId = eventId,
        )

        assertEquals(CallBlockNotifier.DeliveryResult.POSTED, delivery)
        val posted = requireNotNull(
            manager.activeNotifications.firstOrNull { it.id == notificationId }
        ).notification
        assertEquals(CallBlockNotifier.CHANNEL_ID, posted.channelId)
        assertEquals(Notification.CATEGORY_CALL, posted.category)
        assertEquals(Notification.PRIORITY_MAX, posted.priority)
        assertTrue(posted.flags and Notification.FLAG_ONLY_ALERT_ONCE != 0)

        manager.cancel(notificationId)
    }
}

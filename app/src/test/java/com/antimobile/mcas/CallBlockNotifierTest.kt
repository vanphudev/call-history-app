package com.antimobile.mcas

import android.app.NotificationManager
import com.antimobile.mcas.data.blocking.BlockNotificationMode
import com.antimobile.mcas.data.blocking.BlockRecordResult
import com.antimobile.mcas.data.blocking.CallBlockRuleType
import com.antimobile.mcas.data.blocking.CallBlockNotifier
import com.antimobile.mcas.data.blocking.CallBlockSettings
import com.antimobile.mcas.data.blocking.REPEAT_UNKNOWN_CALLER_GUARD_REASON_TYPE
import com.antimobile.mcas.data.blocking.RepeatUnknownCallerGuardReason
import com.antimobile.mcas.data.blocking.RepeatUnknownCallerGuardReasonCodec
import com.antimobile.mcas.data.blocking.SpecialCallCondition
import com.antimobile.mcas.data.blocking.SpamRiskReason
import com.antimobile.mcas.data.blocking.SpamRiskReasonCodec
import com.antimobile.mcas.data.blocking.SpamRiskReasonKind
import com.antimobile.mcas.i18n.EnStrings
import com.antimobile.mcas.i18n.ViStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CallBlockNotifierTest {

    @Test
    fun readinessReportsEverySystemGateInPriorityOrder() {
        assertEquals(
            CallBlockNotifier.Readiness.RUNTIME_PERMISSION_REQUIRED,
            readiness(permission = false),
        )
        assertEquals(
            CallBlockNotifier.Readiness.APP_NOTIFICATIONS_DISABLED,
            readiness(appEnabled = false),
        )
        assertEquals(
            CallBlockNotifier.Readiness.CHANNEL_DISABLED,
            readiness(importance = NotificationManager.IMPORTANCE_NONE),
        )
        assertEquals(
            CallBlockNotifier.Readiness.CHANNEL_NOT_URGENT,
            readiness(importance = NotificationManager.IMPORTANCE_DEFAULT),
        )
        assertEquals(
            CallBlockNotifier.Readiness.CHANNEL_MUTED,
            readiness(sound = false),
        )
        assertEquals(CallBlockNotifier.Readiness.READY, readiness())
    }

    @Test
    fun notificationIdsAreStablePositiveAndFoldTheWholeRoomId() {
        val low = CallBlockNotifier.notificationId(7L)
        assertEquals(low, CallBlockNotifier.notificationId(7L))
        assertTrue(low > 0)
        assertTrue(CallBlockNotifier.notificationId(0L) > 0)
        assertNotEquals(low, CallBlockNotifier.notificationId((1L shl 40) + 7L))
    }

    @Test
    fun notificationModeIsOnlyOffOrEveryBlock() {
        assertTrue(CallBlockSettings.shouldNotify(BlockNotificationMode.EVERY_BLOCK))
        assertFalse(CallBlockSettings.shouldNotify(BlockNotificationMode.OFF))
        assertEquals(BlockNotificationMode.EVERY_BLOCK, BlockNotificationMode.fromStorage("every_5"))
        assertEquals(BlockNotificationMode.EVERY_BLOCK, BlockNotificationMode.fromStorage("every_10"))
    }

    @Test
    fun readinessRejectsAChannelThatLostTheDedicatedAppSound() {
        assertEquals(
            CallBlockNotifier.Readiness.CHANNEL_MUTED,
            readiness(sound = true, appSound = false),
        )
    }

    @Test
    fun statusBarPresentationAcceptsDefaultChannelImportance() {
        assertEquals(
            CallBlockNotifier.Readiness.READY,
            CallBlockNotifier.evaluateReadiness(
                permissionGranted = true,
                appNotificationsEnabled = true,
                channelImportance = NotificationManager.IMPORTANCE_DEFAULT,
                channelHasSound = true,
                minimumChannelImportance = NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
        assertEquals(
            CallBlockNotifier.Readiness.CHANNEL_NOT_URGENT,
            CallBlockNotifier.evaluateReadiness(
                permissionGranted = true,
                appNotificationsEnabled = true,
                channelImportance = NotificationManager.IMPORTANCE_DEFAULT,
                channelHasSound = true,
            ),
        )
    }

    @Test
    fun guardNotificationUsesDedicatedLocalizedReasonInsteadOfPretendingItMatchedARule() {
        val displayValue = SpecialCallCondition.encode(setOf(SpecialCallCondition.UNKNOWN_CONTACT))
        val persistedReason = RepeatUnknownCallerGuardReasonCodec.encode(
            RepeatUnknownCallerGuardReason(attempt = 1, threshold = 3, windowMinutes = 20)
        )
        val result = BlockRecordResult(
            historyId = 42L,
            rawNumber = "0901234567",
            ruleType = CallBlockRuleType.SPECIAL.storageKey,
            ruleValue = displayValue,
            totalForNumber = 1,
            isNew = true,
            historyReasonType = REPEAT_UNKNOWN_CALLER_GUARD_REASON_TYPE,
            historyReasonValue = persistedReason,
        )

        assertEquals(
            ViStrings.blocker.repeatCallerGuardReason(attempt = 1, threshold = 3, minutes = 20),
            CallBlockNotifier.notificationReason(result, ViStrings.blocker),
        )
        assertEquals(
            EnStrings.blocker.repeatCallerGuardReason(attempt = 1, threshold = 3, minutes = 20),
            CallBlockNotifier.notificationReason(result, EnStrings.blocker),
        )
        assertNotEquals(
            EnStrings.blocker.ruleSummary(CallBlockRuleType.SPECIAL.storageKey, displayValue),
            CallBlockNotifier.notificationReason(result, EnStrings.blocker),
        )
    }

    @Test
    fun spamRiskNotificationExplainsTheExactMatchedSignal() {
        val cases = listOf(
            SpamRiskReason(SpamRiskReasonKind.PREFIX, "024") to
                ViStrings.blocker.spamRiskReasonPrefix("024"),
            SpamRiskReason(SpamRiskReasonKind.UNKNOWN_MOBILE_PREFIX, "054") to
                ViStrings.blocker.spamRiskReasonUnknownMobilePrefix("054"),
            SpamRiskReason(SpamRiskReasonKind.VERIFICATION_FAILED) to
                ViStrings.blocker.spamRiskReasonVerificationFailed,
        )

        cases.forEachIndexed { index, (reason, expected) ->
            val encoded = SpamRiskReasonCodec.encode(reason)
            val result = BlockRecordResult(
                historyId = index.toLong() + 1L,
                rawNumber = "02412345678",
                ruleType = CallBlockRuleType.SPAM_RISK.storageKey,
                ruleValue = encoded,
                totalForNumber = 1,
                isNew = true,
                historyReasonType = CallBlockRuleType.SPAM_RISK.storageKey,
                historyReasonValue = encoded,
            )
            assertEquals(expected, CallBlockNotifier.notificationReason(result, ViStrings.blocker))
        }
    }

    private fun readiness(
        permission: Boolean = true,
        appEnabled: Boolean = true,
        importance: Int? = NotificationManager.IMPORTANCE_HIGH,
        sound: Boolean = true,
        appSound: Boolean = true,
    ): CallBlockNotifier.Readiness = CallBlockNotifier.evaluateReadiness(
        permissionGranted = permission,
        appNotificationsEnabled = appEnabled,
        channelImportance = importance,
        channelHasSound = sound,
        channelUsesAppSound = appSound,
    )
}

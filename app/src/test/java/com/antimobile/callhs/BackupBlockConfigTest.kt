package com.antimobile.callhs

import com.antimobile.callhs.data.backup.BackupBlockConfig
import com.antimobile.callhs.data.backup.BackupBlockRule
import com.antimobile.callhs.data.backup.BackupManager
import com.antimobile.callhs.data.blocking.BlockNotificationMode
import com.antimobile.callhs.data.blocking.CallBlockAction
import com.antimobile.callhs.data.blocking.CallBlockScope
import com.antimobile.callhs.data.blocking.CallBlockCallHistorySelection
import com.antimobile.callhs.data.blocking.CallBlockContactSelection
import com.antimobile.callhs.data.blocking.CallBlockRuleMatcher
import com.antimobile.callhs.data.blocking.CallBlockRuleType
import com.antimobile.callhs.data.blocking.CallBlockSettings
import com.antimobile.callhs.data.blocking.CallHistoryRuleCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Test

class BackupBlockConfigTest {

    @Test
    @Ignore("Android org.json contract is exercised by CallBlockBackupInstrumentedTest")
    fun versionFourParserCanonicalizesLegacyNotificationCadence() {
        val parsed = BackupManager.parse(
            """{"_format":"callhs-backup","version":3,"sections":{"callBlockRules":{"notificationMode":"every_10","rules":[]}}}"""
        )
        assertEquals(BlockNotificationMode.EVERY_BLOCK.storageKey, parsed?.blockRules?.notificationMode)
    }

    @Test
    @Ignore("Android org.json contract is exercised by CallBlockBackupInstrumentedTest")
    fun legacyGlobalAllowBecomesDisabledProtectionAndSafeExecutionMethod() {
        val parsed = BackupManager.parse(
            """{"_format":"callhs-backup","version":3,"sections":{"callBlockRules":{"enabled":true,"blockMethod":"allow","rules":[]}}}"""
        )
        assertEquals(false, parsed?.blockRules?.enabled)
        assertEquals("block_and_reject", parsed?.blockRules?.blockMethod)
    }

    @Test
    @Ignore("Android org.json contract is exercised by CallBlockBackupInstrumentedTest")
    fun legacyExactContactAndCallLogRulesExplodeToExactBlockEntries() {
        val contacts = com.antimobile.callhs.data.blocking.ContactRuleCodec.encode(
            listOf(com.antimobile.callhs.data.blocking.CallBlockContactSelection("Lan", listOf("0912345678")))
        )
        val history = CallHistoryRuleCodec.encode(
            listOf(CallBlockCallHistorySelection("Minh", "0987654321"))
        )
        val json = """
            {"_format":"callhs-backup","version":3,"sections":{"callBlockRules":{"rules":[
              {"type":"exact","rawValue":"0900000000","enabled":true,"createdAt":1},
              {"type":"contacts","rawValue":"$contacts","enabled":true,"createdAt":2},
              {"type":"call_history","rawValue":"$history","enabled":true,"createdAt":3}
            ]}}}
        """.trimIndent()

        val config = requireNotNull(BackupManager.parse(json)?.blockRules)
        assertEquals(3, config.numberEntries.size)
        assertTrue(config.numberEntries.all { it.action == CallBlockAction.BLOCK.storageKey })
        assertTrue(config.rules.isEmpty())
    }

    @Test
    @Ignore("Android org.json contract is exercised by CallBlockBackupInstrumentedTest")
    fun versionFourAllowWinsCorruptExactListOverlap() {
        val json = """
            {"_format":"callhs-backup","version":4,"sections":{"callBlockRules":{"numberEntries":[
              {"action":"block","rawNumber":"0912345678","origin":"manual","enabled":true,"createdAt":20},
              {"action":"allow","rawNumber":"+84 912 345 678","origin":"contact_picker","enabled":true,"createdAt":10}
            ],"rules":[]}}}
        """.trimIndent()
        val entries = requireNotNull(BackupManager.parse(json)?.blockRules).numberEntries
        assertEquals(1, entries.size)
        assertEquals(CallBlockAction.ALLOW.storageKey, entries.single().action)
    }

    @Test
    @Ignore("Android org.json contract is exercised by CallBlockBackupInstrumentedTest")
    fun versionFourKeepsSameMatcherAcrossDifferentScopesButRejectsInvalidScope() {
        val json = """
            {"_format":"callhs-backup","version":4,"sections":{"callBlockRules":{"rules":[
              {"type":"prefix","rawValue":"028","enabled":true,"action":"block","scope":"not_saved","userOrder":0},
              {"type":"prefix","rawValue":"028","enabled":true,"action":"block","scope":"saved_contact","userOrder":1},
              {"type":"prefix","rawValue":"028","enabled":true,"action":"block","scope":"bad_scope","userOrder":2}
            ]}}}
        """.trimIndent()
        val rules = requireNotNull(BackupManager.parse(json)?.blockRules).rules
        assertEquals(2, rules.size)
        assertEquals(
            setOf(CallBlockScope.NOT_SAVED.storageKey, CallBlockScope.SAVED_CONTACT.storageKey),
            rules.map { it.scope }.toSet(),
        )
    }

    @Test
    @Ignore("Android org.json contract is exercised by CallBlockBackupInstrumentedTest")
    fun legacySavedContactBypassBecomesExplicitGroupAllowWithoutNarrowingBroadRules() {
        val json = """
            {"_format":"callhs-backup","version":3,"sections":{"callBlockRules":{
              "allowSavedContactsEnabled":true,
              "rules":[{"type":"prefix","rawValue":"028","enabled":true,"createdAt":5}]
            }}}
        """.trimIndent()
        val rules = requireNotNull(BackupManager.parse(json)?.blockRules).rules
        val broad = rules.single { it.type == CallBlockRuleType.PREFIX.storageKey }
        val savedAllow = rules.single { it.type == CallBlockRuleType.ANY.storageKey }
        assertEquals(CallBlockScope.ALL_VISIBLE_NUMBERS.storageKey, broad.scope)
        assertEquals(CallBlockAction.BLOCK.storageKey, broad.action)
        assertEquals(CallBlockScope.SAVED_CONTACT.storageKey, savedAllow.scope)
        assertEquals(CallBlockAction.ALLOW.storageKey, savedAllow.action)
    }

    @Test
    fun versionTwoConfigWithoutBlockMethodRemainsValid() {
        val config = BackupBlockConfig(
            enabled = true,
            notificationMode = "every_5",
            blockMethod = null,
            rules = emptyList(),
        )

        assertTrue(config.hasSettings)
        assertTrue(config.hasAny)
    }

    @Test
    fun blockMethodAloneMakesSectionRestorable() {
        val config = BackupBlockConfig(
            enabled = null,
            notificationMode = null,
            blockMethod = "silence_only",
            rules = emptyList(),
        )

        assertTrue(config.hasSettings)
        assertTrue(config.hasAny)
    }

    @Test
    fun completelyMissingConfigIsEmpty() {
        val config = BackupBlockConfig(
            enabled = null,
            notificationMode = null,
            blockMethod = null,
            rules = emptyList(),
        )

        assertFalse(config.hasSettings)
        assertFalse(config.hasAny)
    }

    @Test
    fun repeatedUnknownCallerGuardSettingsAloneMakeSectionRestorable() {
        val config = BackupBlockConfig(
            enabled = null,
            notificationMode = null,
            blockMethod = null,
            rules = emptyList(),
            repeatUnknownCallerGuardEnabled = true,
            repeatUnknownCallerGuardThreshold = 3,
            repeatUnknownCallerGuardWindowMinutes = 20,
        )

        assertTrue(config.hasSettings)
        assertTrue(config.hasAny)
    }

    @Test
    fun explicitEmptyDailyScheduleCanClearRestoredWindows() {
        val config = BackupBlockConfig(
            enabled = null,
            notificationMode = null,
            blockMethod = null,
            rules = emptyList(),
            dailySchedule = emptyList(),
        )

        assertTrue(config.hasSettings)
        assertTrue(config.hasAny)
    }

    @Test
    fun savedContactBypassSettingAloneMakesSectionRestorable() {
        val config = BackupBlockConfig(
            enabled = null,
            notificationMode = null,
            blockMethod = null,
            rules = emptyList(),
            allowSavedContactsEnabled = false,
        )

        // `false` is an explicit durable choice, not the same as a missing field in an older backup.
        assertTrue(config.hasSettings)
        assertTrue(config.hasAny)
    }

    @Test
    fun repeatedUnknownCallerGuardBackupValuesUseStrictRuntimeBounds() {
        assertTrue(CallBlockSettings.isValidRepeatUnknownCallerGuardThreshold(2))
        assertTrue(CallBlockSettings.isValidRepeatUnknownCallerGuardThreshold(3))
        assertTrue(CallBlockSettings.isValidRepeatUnknownCallerGuardThreshold(4))
        assertFalse(CallBlockSettings.isValidRepeatUnknownCallerGuardThreshold(1))
        assertFalse(CallBlockSettings.isValidRepeatUnknownCallerGuardThreshold(5))

        assertTrue(CallBlockSettings.isValidRepeatUnknownCallerGuardWindowMinutes(1))
        assertTrue(CallBlockSettings.isValidRepeatUnknownCallerGuardWindowMinutes(24 * 60))
        assertFalse(CallBlockSettings.isValidRepeatUnknownCallerGuardWindowMinutes(0))
        assertFalse(CallBlockSettings.isValidRepeatUnknownCallerGuardWindowMinutes(24 * 60 + 1))
    }

    @Test
    fun geographicRuleUsesGenericBackupContractAndRecomputesCanonicalMatchValue() {
        val backup = BackupBlockRule(
            type = "geographic",
            rawValue = "024,cn",
            matchValue = "untrusted-value-from-file",
            enabled = true,
            createdAt = 123L,
        )

        val type = CallBlockRuleType.fromStorage(backup.type)!!
        assertEquals(CallBlockRuleType.GEOGRAPHIC, type)
        assertTrue(CallBlockRuleMatcher.isValid(type, backup.rawValue))

        val canonicalRaw = CallBlockRuleMatcher.canonicalRawValue(type, backup.rawValue)
        assertEquals("cn,024", canonicalRaw)
        assertEquals(
            "cn,024",
            CallBlockRuleMatcher.normalizedValue(type, canonicalRaw),
        )
    }

    @Test
    fun malformedGeographicBackupPayloadFailsRestoreValidation() {
        assertFalse(
            CallBlockRuleMatcher.isValid(
                CallBlockRuleType.GEOGRAPHIC,
                "cn,future_key",
            )
        )
    }

    @Test
    fun callHistoryRuleUsesGenericBackupContractAndRecomputesCanonicalMatchValue() {
        val rawValue = CallHistoryRuleCodec.encode(
            listOf(
                CallBlockCallHistorySelection("Binh", "0987 654 321"),
                CallBlockCallHistorySelection("An", "+84 912 345 678"),
                CallBlockCallHistorySelection("An duplicate", "0912 345 678"),
            )
        )
        val backup = BackupBlockRule(
            type = "call_history",
            rawValue = rawValue,
            matchValue = "untrusted-value-from-file",
            enabled = true,
            createdAt = 456L,
        )

        val type = CallBlockRuleType.fromStorage(backup.type)!!
        assertEquals(CallBlockRuleType.CALL_HISTORY, type)
        assertTrue(CallBlockRuleMatcher.isValid(type, backup.rawValue))
        assertEquals(rawValue, CallBlockRuleMatcher.canonicalRawValue(type, backup.rawValue))
        assertEquals(
            "912345678,987654321",
            CallBlockRuleMatcher.normalizedValue(type, backup.rawValue),
        )
    }

    @Test
    fun malformedCallHistoryBackupPayloadFailsRestoreValidation() {
        assertFalse(
            CallBlockRuleMatcher.isValid(
                CallBlockRuleType.CALL_HISTORY,
                "v1|not-a-valid-selection",
            )
        )
    }
}

package com.antimobile.callhs

import androidx.test.platform.app.InstrumentationRegistry
import com.antimobile.callhs.data.backup.BackupBlockedCall
import com.antimobile.callhs.data.backup.BackupManager
import com.antimobile.callhs.data.backup.BackupSection
import com.antimobile.callhs.data.backup.MergeMode
import com.antimobile.callhs.data.blocking.CallBlockPauseDuration
import com.antimobile.callhs.data.blocking.CallBlockScheduleAction
import com.antimobile.callhs.data.blocking.CallBlockTimeWindow
import com.antimobile.callhs.data.blocking.CallBlockRepository
import com.antimobile.callhs.data.blocking.CallBlockSettings
import com.antimobile.callhs.data.blocking.BlockNotificationMode
import com.antimobile.callhs.data.blocking.CallBlockAction
import com.antimobile.callhs.data.blocking.CallBlockCallHistorySelection
import com.antimobile.callhs.data.blocking.CallBlockContactSelection
import com.antimobile.callhs.data.blocking.CallBlockRuleType
import com.antimobile.callhs.data.blocking.CallBlockScope
import com.antimobile.callhs.data.blocking.CallHistoryRuleCodec
import com.antimobile.callhs.data.blocking.ContactRuleCodec
import com.antimobile.callhs.data.blocking.SavedContactGroupPolicy
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CallBlockBackupInstrumentedTest {
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    @After
    fun clearBlockingPreferences() {
        context.getSharedPreferences("call_block_settings", 0).edit().clear().commit()
        context.getSharedPreferences("call_block_runtime_state", 0).edit().clear().commit()
        context.getSharedPreferences("call_block_repeat_unknown_attempts", 0).edit().clear().commit()
        context.getSharedPreferences("call_block_repeat_unknown_guard_attempts", 0).edit().clear().commit()
        runBlocking {
            CallBlockRepository(context).restoreBlockingData(emptyList(), emptyList(), MergeMode.REPLACE)
        }
        CallBlockSettings.init(context)
    }

    @Test
    fun exportUsesPermanentEnabledAndNeverSerializesTransientPauseOrAttemptLedger() = runBlocking {
        val now = System.currentTimeMillis()
        CallBlockSettings.setEnabled(context, true, now)
        CallBlockSettings.pause(context, CallBlockPauseDuration.MINUTES_30, now)
        CallBlockRepository(context).setSavedContactGroupPolicy(SavedContactGroupPolicy.ALLOW)
        CallBlockSettings.setRepeatUnknownCallerGuardEnabled(context, true)
        CallBlockSettings.setRepeatUnknownCallerGuardThreshold(context, 3)
        CallBlockSettings.setRepeatUnknownCallerGuardWindowMinutes(context, 20)

        assertFalse(CallBlockSettings.isBlockingEnabled(context, now + 1L))
        val root = JSONObject(
            BackupManager.buildJson(context, setOf(BackupSection.BLOCK_RULES))
        )
        assertEquals(6, root.getInt("version"))
        val blockConfig = root
            .getJSONObject("sections")
            .getJSONObject(BackupSection.BLOCK_RULES.jsonKey)

        assertTrue(blockConfig.getBoolean("enabled"))
        assertFalse(blockConfig.has("allowSavedContactsEnabled"))
        val exportedRules = blockConfig.getJSONArray("rules")
        assertTrue((0 until exportedRules.length()).any { index ->
            exportedRules.getJSONObject(index).let { rule ->
                rule.getString("type") == "any" &&
                    rule.getString("action") == "allow" &&
                    rule.getString("scope") == "saved_contact"
            }
        })
        assertTrue(blockConfig.getBoolean("repeatUnknownCallerGuardEnabled"))
        assertEquals(3, blockConfig.getInt("repeatUnknownCallerGuardThreshold"))
        assertEquals(20, blockConfig.getInt("repeatUnknownCallerGuardWindowMinutes"))
        assertFalse(blockConfig.has("repeatUnknownCallerBypassEnabled"))
        assertFalse(blockConfig.has("repeatUnknownCallerBypassThreshold"))
        assertFalse(blockConfig.has("repeatUnknownCallerBypassWindowMinutes"))
        assertFalse(blockConfig.has("pauseStartedAt"))
        assertFalse(blockConfig.has("pauseUntil"))
        assertFalse(root.toString().contains("call_block_repeat_unknown_attempts"))
        assertFalse(root.toString().contains("call_block_repeat_unknown_guard_attempts"))

        val durable = context.getSharedPreferences("call_block_settings", 0)
        val runtime = context.getSharedPreferences("call_block_runtime_state", 0)
        assertTrue(durable.contains("enabled"))
        assertTrue(durable.contains("repeat_unknown_caller_guard_enabled"))
        assertFalse(durable.contains("repeat_unknown_caller_bypass_enabled"))
        assertFalse(durable.contains("pause_started_at"))
        assertFalse(durable.contains("pause_until"))
        assertFalse(durable.contains("repeat_unknown_caller_guard_session_generation"))
        assertTrue(runtime.contains("pause_started_at"))
        assertTrue(runtime.contains("pause_until"))
        assertTrue(runtime.contains("repeat_unknown_caller_guard_session_generation"))
    }

    @Test
    fun recurringBlockAndPauseScheduleRoundTripsWithDaysAndEnabledState() = runBlocking {
        val original = CallBlockSettings.dailySchedule(context)
        val backedUp = listOf(
            CallBlockTimeWindow(
                id = "weekday_pause",
                action = CallBlockScheduleAction.PAUSE,
                startMinute = 8 * 60,
                endMinute = 9 * 60,
                enabled = true,
                weekdaysMask = 0b0011111,
            ),
            CallBlockTimeWindow(
                id = "weekend_block",
                action = CallBlockScheduleAction.BLOCK,
                startMinute = 22 * 60,
                endMinute = 6 * 60,
                presetKey = "night",
                enabled = false,
                weekdaysMask = 0b1100000,
            ),
        )

        try {
            assertTrue(CallBlockSettings.replaceDailySchedule(context, backedUp))
            val json = BackupManager.buildJson(context, setOf(BackupSection.BLOCK_RULES))
            val parsed = requireNotNull(BackupManager.parse(json))
            assertEquals(backedUp, parsed.blockRules?.dailySchedule?.map { value ->
                CallBlockTimeWindow(
                    id = value.id,
                    action = requireNotNull(CallBlockScheduleAction.fromStorage(value.action)),
                    startMinute = value.startMinute,
                    endMinute = value.endMinute,
                    presetKey = value.presetKey,
                    enabled = value.enabled,
                    weekdaysMask = value.weekdaysMask,
                )
            })

            assertTrue(CallBlockSettings.replaceDailySchedule(context, emptyList()))
            BackupManager.restore(
                context,
                parsed,
                setOf(BackupSection.BLOCK_RULES),
                MergeMode.UPDATE,
            )
            assertEquals(backedUp, CallBlockSettings.dailySchedule(context))
        } finally {
            CallBlockSettings.replaceDailySchedule(context, original)
        }
    }

    @Test
    fun legacyTransientKeysAreDiscardedInsteadOfRestoredAcrossDevices() {
        val startedAt = System.currentTimeMillis()
        val durable = context.getSharedPreferences("call_block_settings", 0)
        val runtime = context.getSharedPreferences("call_block_runtime_state", 0)
        durable.edit()
            .putBoolean("enabled", true)
            .putLong("pause_started_at", startedAt)
            .putLong("pause_until", startedAt + 10L * 60_000L)
            .putLong("repeat_unknown_caller_session_generation", 7L)
            .commit()

        val refreshed = CallBlockSettings.refresh(context, startedAt + 1L)
        assertTrue(refreshed.baseEnabled)
        assertFalse(refreshed.isPausedAt(startedAt + 1L))
        assertFalse(durable.contains("pause_started_at"))
        assertFalse(durable.contains("pause_until"))
        assertFalse(durable.contains("repeat_unknown_caller_session_generation"))
        assertFalse(runtime.contains("pause_started_at"))
        assertFalse(runtime.contains("pause_until"))
        assertFalse(runtime.contains("repeat_unknown_caller_session_generation"))
    }

    @Test
    fun legacyBypassPreferencesAreRemovedWithoutEnablingTheNewGuard() {
        val durable = context.getSharedPreferences("call_block_settings", 0)
        durable.edit()
            .putBoolean("repeat_unknown_caller_bypass_enabled", true)
            .putInt("repeat_unknown_caller_bypass_threshold", 4)
            .putInt("repeat_unknown_caller_bypass_window_minutes", 60)
            .commit()

        CallBlockSettings.init(context)

        val config = CallBlockSettings.repeatUnknownCallerGuardConfig(context)
        assertFalse(config.enabled)
        assertFalse(durable.contains("repeat_unknown_caller_bypass_enabled"))
        assertFalse(durable.contains("repeat_unknown_caller_bypass_threshold"))
        assertFalse(durable.contains("repeat_unknown_caller_bypass_window_minutes"))
    }

    @Test
    fun malformedPersistedGuardParametersDisableTheNoRuleGuard() {
        val durable = context.getSharedPreferences("call_block_settings", 0)
        val runtime = context.getSharedPreferences("call_block_runtime_state", 0)
        durable.edit()
            .putBoolean("repeat_unknown_caller_guard_enabled", true)
            .putString("repeat_unknown_caller_guard_threshold", "2")
            .putInt("repeat_unknown_caller_guard_window_minutes", 15)
            .commit()

        var config = CallBlockSettings.repeatUnknownCallerGuardConfig(context)
        assertFalse(config.enabled)
        assertEquals(2, config.threshold)
        assertEquals(15, config.windowMinutes)

        durable.edit()
            .putInt("repeat_unknown_caller_guard_threshold", 2)
            .commit()
        runtime.edit()
            .putString("repeat_unknown_caller_guard_session_generation", "1")
            .commit()

        config = CallBlockSettings.repeatUnknownCallerGuardConfig(context)
        assertFalse(config.enabled)

        CallBlockSettings.setRepeatUnknownCallerGuardEnabled(context, true)
        config = CallBlockSettings.repeatUnknownCallerGuardConfig(context)
        assertTrue(config.enabled)
        assertEquals(2, config.threshold)
        assertEquals(15, config.windowMinutes)
    }

    @Test
    fun parserKeepsOlderBackupsIgnoresLegacyBypassAndRejectsMalformedGuardWithoutCoercion() {
        val parsed = BackupManager.parse(
            """
            {
              "_format": "callhs-backup",
              "version": 2,
              "sections": {
                "callBlockRules": {
                  "blockMethod": "silence_only",
                  "allowSavedContactsEnabled": "true",
                  "repeatUnknownCallerBypassEnabled": true,
                  "repeatUnknownCallerBypassThreshold": 4,
                  "repeatUnknownCallerBypassWindowMinutes": 30,
                  "repeatUnknownCallerGuardEnabled": "true",
                  "repeatUnknownCallerGuardThreshold": 2.5,
                  "repeatUnknownCallerGuardWindowMinutes": "15",
                  "rules": []
                }
              }
            }
            """.trimIndent()
        )

        val config = requireNotNull(parsed?.blockRules)
        assertEquals("silence_only", config.blockMethod)
        assertNull(config.allowSavedContactsEnabled)
        assertNull(config.repeatUnknownCallerGuardEnabled)
        assertNull(config.repeatUnknownCallerGuardThreshold)
        assertNull(config.repeatUnknownCallerGuardWindowMinutes)
    }

    @Test
    fun parserPreservesSyntheticUnknownCallerGuardHistoryReasonInVersionThree() {
        val parsed = requireNotNull(
            BackupManager.parse(
                """
                {
                  "_format": "callhs-backup",
                  "version": 3,
                  "sections": {
                    "blockedCalls": [
                      {
                        "rawNumber": "0912345678",
                        "phoneKey": "912345678",
                        "blockedAt": 123456789,
                        "ruleType": "repeat_unknown_caller_guard",
                        "ruleValue": "v1|1|2|15",
                        "consecutiveUnanswered": 0
                      }
                    ]
                  }
                }
                """.trimIndent()
            )
        )

        val history = requireNotNull(parsed.blockedCalls).single()
        assertEquals("repeat_unknown_caller_guard", history.ruleType)
        assertEquals("v1|1|2|15", history.ruleValue)
    }

    @Test
    fun syntheticUnknownCallerGuardHistoryRoundTripsThroughRestoreAndExport() = runBlocking {
        val original = BackupBlockedCall(
            rawNumber = "0912345678",
            phoneKey = "912345678",
            blockedAt = 123456789L,
            ruleType = "repeat_unknown_caller_guard",
            ruleValue = "v1|1|2|15",
            consecutiveUnanswered = 0,
        )

        val restored = CallBlockRepository(context).restoreHistory(listOf(original), MergeMode.REPLACE)
        assertEquals(1, restored.added)
        assertEquals(0, restored.skipped)

        val root = JSONObject(
            BackupManager.buildJson(context, setOf(BackupSection.BLOCK_HISTORY))
        )
        val exported = root
            .getJSONObject("sections")
            .getJSONArray(BackupSection.BLOCK_HISTORY.jsonKey)
            .getJSONObject(0)
        assertEquals(original.rawNumber, exported.getString("rawNumber"))
        assertEquals(original.ruleType, exported.getString("ruleType"))
        assertEquals(original.ruleValue, exported.getString("ruleValue"))
        assertEquals(original.blockedAt, exported.getLong("blockedAt"))

        val reparsed = requireNotNull(BackupManager.parse(root.toString()))
        val roundTripped = requireNotNull(reparsed.blockedCalls).single()
        assertEquals(original.ruleType, roundTripped.ruleType)
        assertEquals(original.ruleValue, roundTripped.ruleValue)
    }

    @Test
    fun retiredRepeatRuleHistoryRemainsViewableAndPortable() = runBlocking {
        val original = BackupBlockedCall(
            rawNumber = "0901234567",
            phoneKey = "901234567",
            blockedAt = 223456789L,
            ruleType = "repeat_unanswered",
            ruleValue = "5",
            consecutiveUnanswered = 5,
            ruleScope = CallBlockScope.NOT_SAVED.storageKey,
        )

        val repository = CallBlockRepository(context)
        val restored = repository.restoreHistory(listOf(original), MergeMode.REPLACE)
        assertEquals(1, restored.added)
        val visible = repository.observeHistory().first().single()
        assertEquals("repeat_unanswered", visible.ruleType)
        assertEquals(5, visible.consecutiveUnanswered)

        val exported = JSONObject(
            BackupManager.buildJson(context, setOf(BackupSection.BLOCK_HISTORY))
        ).getJSONObject("sections")
            .getJSONArray(BackupSection.BLOCK_HISTORY.jsonKey)
            .getJSONObject(0)
        assertEquals(original.ruleType, exported.getString("ruleType"))
        assertEquals(original.ruleValue, exported.getString("ruleValue"))
        assertEquals(5, exported.getInt("consecutiveUnanswered"))
    }

    @Test
    fun legacyBypassBackupCannotEnableTheNewUnknownCallerGuard() = runBlocking {
        CallBlockSettings.setRepeatUnknownCallerGuardEnabled(context, false)
        val parsed = requireNotNull(
            BackupManager.parse(
                """
                {
                  "_format": "callhs-backup",
                  "version": 3,
                  "sections": {
                    "callBlockRules": {
                      "blockMethod": "block_and_reject",
                      "repeatUnknownCallerBypassEnabled": true,
                      "repeatUnknownCallerBypassThreshold": 4,
                      "repeatUnknownCallerBypassWindowMinutes": 60,
                      "rules": []
                    }
                  }
                }
                """.trimIndent()
            )
        )

        BackupManager.restore(
            context = context,
            parsed = parsed,
            sections = setOf(BackupSection.BLOCK_RULES),
            mode = MergeMode.UPDATE,
        )

        assertFalse(CallBlockSettings.repeatUnknownCallerGuardConfig(context).enabled)
    }

    @Test
    fun missingGuardFieldsKeepCurrentGuardConfigurationOnUpdate() = runBlocking {
        CallBlockSettings.setRepeatUnknownCallerGuardEnabled(context, true)
        CallBlockSettings.setRepeatUnknownCallerGuardThreshold(context, 4)
        CallBlockSettings.setRepeatUnknownCallerGuardWindowMinutes(context, 45)
        val before = CallBlockSettings.repeatUnknownCallerGuardConfig(context)
        val parsed = requireNotNull(
            BackupManager.parse(
                """
                {
                  "_format": "callhs-backup",
                  "version": 2,
                  "sections": {
                    "callBlockRules": {
                      "blockMethod": "silence_only",
                      "rules": []
                    }
                  }
                }
                """.trimIndent()
            )
        )

        BackupManager.restore(
            context = context,
            parsed = parsed,
            sections = setOf(BackupSection.BLOCK_RULES),
            mode = MergeMode.UPDATE,
        )

        val after = CallBlockSettings.repeatUnknownCallerGuardConfig(context)
        assertTrue(after.enabled)
        assertEquals(before.threshold, after.threshold)
        assertEquals(before.windowMinutes, after.windowMinutes)
    }

    @Test
    fun guardSettingsAreIgnoredForAddAndAppliedForUpdate() = runBlocking {
        CallBlockSettings.setRepeatUnknownCallerGuardEnabled(context, false)
        CallBlockSettings.setRepeatUnknownCallerGuardThreshold(context, 2)
        CallBlockSettings.setRepeatUnknownCallerGuardWindowMinutes(context, 15)
        val generationBeforeAdd = CallBlockSettings.repeatUnknownCallerGuardConfig(context).sessionGeneration
        val parsed = requireNotNull(
            BackupManager.parse(
                """
                {
                  "_format": "callhs-backup",
                  "version": 3,
                  "sections": {
                    "callBlockRules": {
                      "repeatUnknownCallerGuardEnabled": true,
                      "repeatUnknownCallerGuardThreshold": 4,
                      "repeatUnknownCallerGuardWindowMinutes": 45,
                      "rules": []
                    }
                  }
                }
                """.trimIndent()
            )
        )

        BackupManager.restore(
            context = context,
            parsed = parsed,
            sections = setOf(BackupSection.BLOCK_RULES),
            mode = MergeMode.ADD,
        )
        var config = CallBlockSettings.repeatUnknownCallerGuardConfig(context)
        assertFalse(config.enabled)
        assertEquals(2, config.threshold)
        assertEquals(15, config.windowMinutes)
        assertEquals(generationBeforeAdd, config.sessionGeneration)
        val generationAfterAdd = config.sessionGeneration

        BackupManager.restore(
            context = context,
            parsed = parsed,
            sections = setOf(BackupSection.BLOCK_RULES),
            mode = MergeMode.UPDATE,
        )
        config = CallBlockSettings.repeatUnknownCallerGuardConfig(context)
        assertTrue(config.enabled)
        assertEquals(4, config.threshold)
        assertEquals(45, config.windowMinutes)
        assertTrue(config.sessionGeneration != generationAfterAdd)
    }

    @Test
    fun missingLegacySavedContactSettingKeepsCurrentGroupPolicyOnUpdate() = runBlocking {
        val repository = CallBlockRepository(context)
        assertTrue(repository.setSavedContactGroupPolicy(SavedContactGroupPolicy.ALLOW))
        val parsed = requireNotNull(
            BackupManager.parse(
                """
                {
                  "_format": "callhs-backup",
                  "version": 2,
                  "sections": {
                    "callBlockRules": {
                      "blockMethod": "silence_only",
                      "rules": []
                    }
                  }
                }
                """.trimIndent()
            )
        )

        BackupManager.restore(
            context = context,
            parsed = parsed,
            sections = setOf(BackupSection.BLOCK_RULES),
            mode = MergeMode.UPDATE,
        )

        assertEquals(SavedContactGroupPolicy.ALLOW, repository.savedContactGroupPolicy())
    }

    @Test
    fun legacySavedContactSettingRestoresAsGroupPolicyForNonAddModes() = runBlocking {
        val repository = CallBlockRepository(context)
        assertTrue(repository.setSavedContactGroupPolicy(SavedContactGroupPolicy.ALLOW))
        val parsed = requireNotNull(
            BackupManager.parse(
                """
                {
                  "_format": "callhs-backup",
                  "version": 3,
                  "sections": {
                    "callBlockRules": {
                      "allowSavedContactsEnabled": false,
                      "rules": []
                    }
                  }
                }
                """.trimIndent()
            )
        )

        BackupManager.restore(
            context = context,
            parsed = parsed,
            sections = setOf(BackupSection.BLOCK_RULES),
            mode = MergeMode.ADD,
        )
        assertEquals(SavedContactGroupPolicy.ALLOW, repository.savedContactGroupPolicy())

        BackupManager.restore(
            context = context,
            parsed = parsed,
            sections = setOf(BackupSection.BLOCK_RULES),
            mode = MergeMode.UPDATE,
        )
        assertEquals(SavedContactGroupPolicy.FOLLOW_ADVANCED, repository.savedContactGroupPolicy())

        assertTrue(repository.setSavedContactGroupPolicy(SavedContactGroupPolicy.ALLOW))
        BackupManager.restore(
            context = context,
            parsed = parsed,
            sections = setOf(BackupSection.BLOCK_RULES),
            mode = MergeMode.REPLACE,
        )
        assertEquals(SavedContactGroupPolicy.FOLLOW_ADVANCED, repository.savedContactGroupPolicy())
    }

    @Test
    fun restoringRulesInvalidatesAttemptsEvenWhenGuardSettingsAreUnchanged() = runBlocking {
        CallBlockSettings.setRepeatUnknownCallerGuardEnabled(context, true)
        CallBlockSettings.setRepeatUnknownCallerGuardThreshold(context, 2)
        CallBlockSettings.setRepeatUnknownCallerGuardWindowMinutes(context, 15)
        val before = CallBlockSettings.repeatUnknownCallerGuardConfig(context)
        val parsed = requireNotNull(
            BackupManager.parse(
                """
                {
                  "_format": "callhs-backup",
                  "version": 3,
                  "sections": {
                    "callBlockRules": {
                      "repeatUnknownCallerGuardEnabled": true,
                      "repeatUnknownCallerGuardThreshold": 2,
                      "repeatUnknownCallerGuardWindowMinutes": 15,
                      "rules": []
                    }
                  }
                }
                """.trimIndent()
            )
        )

        BackupManager.restore(
            context = context,
            parsed = parsed,
            sections = setOf(BackupSection.BLOCK_RULES),
            mode = MergeMode.UPDATE,
        )

        val after = CallBlockSettings.repeatUnknownCallerGuardConfig(context)
        assertTrue(after.enabled)
        assertEquals(before.threshold, after.threshold)
        assertEquals(before.windowMinutes, after.windowMinutes)
        assertTrue(after.sessionGeneration != before.sessionGeneration)
    }

    @Test
    fun v4ParserCanonicalizesRemovedNotificationCadenceAndLegacyGlobalAllow() {
        val parsed = requireNotNull(
            BackupManager.parse(
                """{"_format":"callhs-backup","version":3,"sections":{"callBlockRules":{"enabled":true,"blockMethod":"allow","notificationMode":"every_10","rules":[]}}}"""
            )?.blockRules
        )

        assertFalse(requireNotNull(parsed.enabled))
        assertEquals("block_and_reject", parsed.blockMethod)
        assertEquals(BlockNotificationMode.EVERY_BLOCK.storageKey, parsed.notificationMode)
    }

    @Test
    fun v3PickerRulesBecomeIndependentExactBlockEntries() {
        val contacts = ContactRuleCodec.encode(
            listOf(CallBlockContactSelection("Lan", listOf("0912345678")))
        )
        val history = CallHistoryRuleCodec.encode(
            listOf(CallBlockCallHistorySelection("Minh", "0987654321"))
        )
        val parsed = requireNotNull(
            BackupManager.parse(
                """
                {"_format":"callhs-backup","version":3,"sections":{"callBlockRules":{"rules":[
                  {"type":"exact","rawValue":"0900000000","enabled":true,"createdAt":1},
                  {"type":"contacts","rawValue":"$contacts","enabled":true,"createdAt":2},
                  {"type":"call_history","rawValue":"$history","enabled":true,"createdAt":3}
                ]}}}
                """.trimIndent()
            )?.blockRules
        )

        assertEquals(3, parsed.numberEntries.size)
        assertTrue(parsed.numberEntries.all { it.action == CallBlockAction.BLOCK.storageKey })
        assertTrue(parsed.rules.isEmpty())
    }

    @Test
    fun v4AllowWinsCorruptExactListOverlap() {
        val parsed = requireNotNull(
            BackupManager.parse(
                """
                {"_format":"callhs-backup","version":4,"sections":{"callBlockRules":{"numberEntries":[
                  {"action":"block","rawNumber":"0912345678","origin":"manual","enabled":true,"createdAt":20},
                  {"action":"allow","rawNumber":"+84 912 345 678","origin":"contact_picker","enabled":true,"createdAt":10}
                ],"rules":[]}}}
                """.trimIndent()
            )?.blockRules
        )

        assertEquals(1, parsed.numberEntries.size)
        assertEquals(CallBlockAction.ALLOW.storageKey, parsed.numberEntries.single().action)
    }

    @Test
    fun v4KeepsSameMatcherAcrossScopesAndRejectsInvalidScope() {
        val parsed = requireNotNull(
            BackupManager.parse(
                """
                {"_format":"callhs-backup","version":4,"sections":{"callBlockRules":{"rules":[
                  {"type":"prefix","rawValue":"028","enabled":true,"action":"block","scope":"not_saved","userOrder":0},
                  {"type":"prefix","rawValue":"028","enabled":true,"action":"block","scope":"saved_contact","userOrder":1}
                ]}}}
                """.trimIndent()
            )?.blockRules
        )

        assertEquals(2, parsed.rules.size)
        assertEquals(
            setOf(CallBlockScope.NOT_SAVED.storageKey, CallBlockScope.SAVED_CONTACT.storageKey),
            parsed.rules.map { it.scope }.toSet(),
        )

        // v4 is authoritative: one malformed row invalidates this section instead of allowing a
        // REPLACE restore to erase local data with a partially accepted payload.
        val malformed = BackupManager.parse(
            """
            {"_format":"callhs-backup","version":4,"sections":{"callBlockRules":{"rules":[
              {"type":"prefix","rawValue":"028","enabled":true,"action":"block","scope":"not_saved","userOrder":0},
              {"type":"prefix","rawValue":"028","enabled":true,"action":"block","scope":"bad_scope","userOrder":1}
            ]}}}
            """.trimIndent()
        )
        assertNull(malformed?.blockRules)
    }

    @Test
    fun shippedV4BackupSkipsRetiredRepeatRuleAndRestoresOtherRules() = runBlocking {
        val parsed = requireNotNull(
            BackupManager.parse(
                """
                {"_format":"callhs-backup","version":4,"sections":{"callBlockRules":{"rules":[
                  {"type":"repeat_unanswered","rawValue":"5","matchValue":"5","enabled":true,"action":"block","scope":"not_saved","userOrder":0},
                  {"type":"repeat_unanswered","rawValue":"5","matchValue":"5","enabled":true,"action":"allow","scope":"saved_contact","userOrder":1},
                  {"type":"prefix","rawValue":"028","matchValue":"28","enabled":true,"action":"block","scope":"not_saved","userOrder":2}
                ]}}}
                """.trimIndent()
            )?.blockRules
        )
        assertEquals(1, parsed.rules.size)
        assertEquals(CallBlockRuleType.PREFIX.storageKey, parsed.rules.single().type)

        val repository = CallBlockRepository(context)
        val result = repository.restoreBlockingData(parsed.numberEntries, parsed.rules, MergeMode.REPLACE)
        assertEquals(1, result.added)
        val restored = repository.observeRules().first()
        assertEquals(listOf(CallBlockRuleType.PREFIX), restored.map { it.type })

        val exportedRules = JSONObject(
            BackupManager.buildJson(context, setOf(BackupSection.BLOCK_RULES))
        ).getJSONObject("sections")
            .getJSONObject(BackupSection.BLOCK_RULES.jsonKey)
            .getJSONArray("rules")
        assertEquals(1, exportedRules.length())
        assertEquals(CallBlockRuleType.PREFIX.storageKey, exportedRules.getJSONObject(0).getString("type"))
        assertTrue((0 until exportedRules.length()).none { index ->
            exportedRules.getJSONObject(index).getString("type") == "repeat_unanswered"
        })

        val malformedRetired = BackupManager.parse(
            """
            {"_format":"callhs-backup","version":4,"sections":{"callBlockRules":{"rules":[
              {"type":"repeat_unanswered","rawValue":"4","enabled":true,"action":"block","scope":"not_saved"}
            ]}}}
            """.trimIndent()
        )
        assertNull(malformedRetired?.blockRules)

        val mismatchedRetired = BackupManager.parse(
            """
            {"_format":"callhs-backup","version":4,"sections":{"callBlockRules":{"rules":[
              {"type":"repeat_unanswered","rawValue":"5","matchValue":"4","enabled":true,"action":"block","scope":"not_saved"}
            ]}}}
            """.trimIndent()
        )
        assertNull(mismatchedRetired?.blockRules)

        val invalidRetiredAction = BackupManager.parse(
            """
            {"_format":"callhs-backup","version":4,"sections":{"callBlockRules":{"rules":[
              {"type":"repeat_unanswered","rawValue":"5","matchValue":"5","enabled":true,"action":"future","scope":"not_saved"}
            ]}}}
            """.trimIndent()
        )
        assertNull(invalidRetiredAction?.blockRules)

        val unknownType = BackupManager.parse(
            """
            {"_format":"callhs-backup","version":4,"sections":{"callBlockRules":{"rules":[
              {"type":"future_rule","rawValue":"x","enabled":true,"action":"block","scope":"not_saved"}
            ]}}}
            """.trimIndent()
        )
        assertNull(unknownType?.blockRules)
    }

    @Test
    fun shippedV4BackupMovesUnknownContactOutOfSpecialRules() {
        val parsed = requireNotNull(
            BackupManager.parse(
                """
                {"_format":"callhs-backup","version":4,"sections":{"callBlockRules":{"rules":[
                  {"type":"special","rawValue":"private,unknown_contact","enabled":true,"action":"block","scope":"all_visible","userOrder":2},
                  {"type":"special","rawValue":"voip","enabled":true,"action":"block","scope":"saved_contact","userOrder":3}
                ]}}}
                """.trimIndent()
            )?.blockRules
        )

        assertEquals(4, parsed.rules.size)
        val group = parsed.rules.single { it.type == CallBlockRuleType.ANY.storageKey }
        assertEquals(CallBlockScope.NOT_SAVED.storageKey, group.scope)
        assertEquals(CallBlockAction.BLOCK.storageKey, group.action)
        val specialRules = parsed.rules.filter { it.type == CallBlockRuleType.SPECIAL.storageKey }
        val privateRule = specialRules.single { it.rawValue == "private" }
        val sipPhoneRule = specialRules.single { it.rawValue == "sip_phone" }
        val sipTextRule = specialRules.single { it.rawValue == "sip_text" }
        assertEquals(CallBlockScope.ALL_VISIBLE_NUMBERS.storageKey, privateRule.scope)
        assertEquals(CallBlockScope.SAVED_CONTACT.storageKey, sipPhoneRule.scope)
        assertEquals(CallBlockScope.ALL_VISIBLE_NUMBERS.storageKey, sipTextRule.scope)
        assertTrue(parsed.rules.none { it.rawValue.contains("unknown_contact") || it.rawValue == "voip" })
    }

    @Test
    fun legacyRepeatHistoryStillRoundTripsButCannotBecomeAnActiveRule() = runBlocking {
        val original = BackupBlockedCall(
            rawNumber = "0901234567",
            phoneKey = "901234567",
            blockedAt = 987654321L,
            ruleType = "repeat_unanswered",
            ruleValue = "5",
            consecutiveUnanswered = 5,
            ruleScope = CallBlockScope.NOT_SAVED.storageKey,
        )

        val repository = CallBlockRepository(context)
        val restored = repository.restoreHistory(listOf(original), MergeMode.REPLACE)
        assertEquals(1, restored.added)
        assertEquals(0, restored.skipped)

        val root = JSONObject(
            BackupManager.buildJson(
                context,
                setOf(BackupSection.BLOCK_HISTORY, BackupSection.BLOCK_RULES),
            )
        )
        val sections = root.getJSONObject("sections")
        val exportedHistory = sections
            .getJSONArray(BackupSection.BLOCK_HISTORY.jsonKey)
            .getJSONObject(0)
        assertEquals(original.ruleType, exportedHistory.getString("ruleType"))
        assertEquals(original.ruleValue, exportedHistory.getString("ruleValue"))
        assertEquals(original.consecutiveUnanswered, exportedHistory.getInt("consecutiveUnanswered"))
        assertEquals(original.ruleScope, exportedHistory.getString("ruleScope"))

        val exportedRules = sections
            .getJSONObject(BackupSection.BLOCK_RULES.jsonKey)
            .getJSONArray("rules")
        assertTrue((0 until exportedRules.length()).none { index ->
            exportedRules.getJSONObject(index).getString("type") == original.ruleType
        })

        val reparsed = requireNotNull(BackupManager.parse(root.toString()))
        val roundTripped = requireNotNull(reparsed.blockedCalls).single()
        assertEquals(original.ruleType, roundTripped.ruleType)
        assertEquals(original.ruleValue, roundTripped.ruleValue)
        assertEquals(original.consecutiveUnanswered, roundTripped.consecutiveUnanswered)
        assertEquals(original.ruleScope, roundTripped.ruleScope)
    }

    @Test
    fun spamRiskRuleRoundTripsWithStableProfileAndRejectsUnsafeShapes() = runBlocking {
        val parsed = requireNotNull(
            BackupManager.parse(
                """
                {"_format":"callhs-backup","version":4,"sections":{"callBlockRules":{"rules":[
                  {"type":"spam_risk","rawValue":"app_default","matchValue":"app_default","enabled":true,
                   "createdAt":123,"action":"block","scope":"not_saved","userOrder":4}
                ]}}}
                """.trimIndent()
            )?.blockRules
        )
        val parsedRule = parsed.rules.single()
        assertEquals(CallBlockRuleType.SPAM_RISK.storageKey, parsedRule.type)
        assertEquals("app_default", parsedRule.rawValue)
        assertEquals(CallBlockAction.BLOCK.storageKey, parsedRule.action)
        assertEquals(CallBlockScope.NOT_SAVED.storageKey, parsedRule.scope)

        val repository = CallBlockRepository(context)
        val restore = repository.restoreBlockingData(parsed.numberEntries, parsed.rules, MergeMode.REPLACE)
        assertEquals(1, restore.added)
        val exported = JSONObject(
            BackupManager.buildJson(context, setOf(BackupSection.BLOCK_RULES))
        ).getJSONObject("sections")
            .getJSONObject(BackupSection.BLOCK_RULES.jsonKey)
            .getJSONArray("rules")
            .getJSONObject(0)
        assertEquals(CallBlockRuleType.SPAM_RISK.storageKey, exported.getString("type"))
        assertEquals("app_default", exported.getString("rawValue"))
        assertEquals("app_default", exported.getString("matchValue"))
        assertEquals(CallBlockAction.BLOCK.storageKey, exported.getString("action"))
        assertEquals(CallBlockScope.NOT_SAVED.storageKey, exported.getString("scope"))

        val allowShape = BackupManager.parse(
            """
            {"_format":"callhs-backup","version":4,"sections":{"callBlockRules":{"rules":[
              {"type":"spam_risk","rawValue":"app_default","matchValue":"app_default","enabled":true,
               "action":"allow","scope":"not_saved"}
            ]}}}
            """.trimIndent()
        )
        assertNull(allowShape?.blockRules)

        val unknownProfile = BackupManager.parse(
            """
            {"_format":"callhs-backup","version":4,"sections":{"callBlockRules":{"rules":[
              {"type":"spam_risk","rawValue":"future_profile","matchValue":"future_profile","enabled":true,
               "action":"block","scope":"not_saved"}
            ]}}}
            """.trimIndent()
        )
        assertNull(unknownProfile?.blockRules)
    }

    @Test
    fun futureBackupVersionAndMalformedV4EnabledAreRejected() {
        assertNull(
            BackupManager.parse(
                """{"_format":"callhs-backup","version":7,"sections":{}}"""
            )
        )

        val missingEnabled = BackupManager.parse(
            """
            {"_format":"callhs-backup","version":4,"sections":{"callBlockRules":{"rules":[
              {"type":"prefix","rawValue":"028","action":"block","scope":"not_saved"}
            ]}}}
            """.trimIndent()
        )
        assertNull(missingEnabled?.blockRules)

        val stringEnabled = BackupManager.parse(
            """
            {"_format":"callhs-backup","version":4,"sections":{"callBlockRules":{"numberEntries":[
              {"action":"block","rawNumber":"0912345678","origin":"manual","enabled":"false"}
            ],"rules":[]}}}
            """.trimIndent()
        )
        assertNull(stringEnabled?.blockRules)
    }

    @Test
    fun v3SavedContactBypassBecomesGroupAllowWithoutNarrowingBroadRules() {
        val rules = requireNotNull(
            BackupManager.parse(
                """
                {"_format":"callhs-backup","version":3,"sections":{"callBlockRules":{
                  "allowSavedContactsEnabled":true,
                  "rules":[{"type":"prefix","rawValue":"028","enabled":true,"createdAt":5}]
                }}}
                """.trimIndent()
            )?.blockRules
        ).rules

        val broad = rules.single { it.type == CallBlockRuleType.PREFIX.storageKey }
        val savedAllow = rules.single { it.type == CallBlockRuleType.ANY.storageKey }
        assertEquals(CallBlockScope.ALL_VISIBLE_NUMBERS.storageKey, broad.scope)
        assertEquals(CallBlockAction.BLOCK.storageKey, broad.action)
        assertEquals(CallBlockScope.SAVED_CONTACT.storageKey, savedAllow.scope)
        assertEquals(CallBlockAction.ALLOW.storageKey, savedAllow.action)
    }
}

package com.antimobile.callhs

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.antimobile.callhs.data.backup.BackupNumberEntry
import com.antimobile.callhs.data.backup.MergeMode
import com.antimobile.callhs.data.blocking.CallBlockAction
import com.antimobile.callhs.data.blocking.CallBlockDecisionTier
import com.antimobile.callhs.data.blocking.CallBlockMethod
import com.antimobile.callhs.data.blocking.CallBlockRepository
import com.antimobile.callhs.data.blocking.CallBlockRuleType
import com.antimobile.callhs.data.blocking.CallBlockScope
import com.antimobile.callhs.data.blocking.CallBlockSettings
import com.antimobile.callhs.data.blocking.CallerNumberVerificationStatus
import com.antimobile.callhs.data.blocking.NumberEntryOrigin
import com.antimobile.callhs.data.blocking.SaveBlockRuleResult
import com.antimobile.callhs.data.blocking.SpamRiskReasonCodec
import com.antimobile.callhs.data.blocking.SpamRiskReasonKind
import com.antimobile.callhs.util.PhoneKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CallBlockArchitectureInstrumentedTest {
    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val repo
        get() = CallBlockRepository(context)

    @Before
    @After
    fun reset() = runBlocking {
        repo.restoreBlockingData(emptyList(), emptyList(), MergeMode.REPLACE)
        context.getSharedPreferences("call_block_settings", 0).edit().clear().commit()
        context.getSharedPreferences("call_block_runtime_state", 0).edit().clear().commit()
        context.getSharedPreferences("call_block_repeat_unknown_guard_attempts", 0).edit().clear().commit()
        CallBlockSettings.init(context)
        CallBlockSettings.setEnabled(context, true)
        CallBlockSettings.setBlockMethod(context, CallBlockMethod.BLOCK_AND_REJECT)
        CallBlockSettings.setRepeatUnknownCallerGuardEnabled(context, false)
    }

    @Test
    fun pickerSourceBecomesOneExactEntryAndMovingListsIsAtomic() = runBlocking {
        val number = "090 123 4567"
        repo.upsertNumberEntry(
            action = CallBlockAction.BLOCK,
            rawNumber = number,
            displayName = "From call log",
            origin = NumberEntryOrigin.CALL_LOG_PICKER,
        )
        repo.upsertNumberEntry(
            action = CallBlockAction.ALLOW,
            rawNumber = "+84 90 123 4567",
            displayName = "From contacts",
            origin = NumberEntryOrigin.CONTACT_PICKER,
        )

        val entries = repo.observeNumberEntries().first()
        assertEquals(1, entries.size)
        assertEquals(CallBlockAction.ALLOW, entries.single().action)
        assertEquals(NumberEntryOrigin.CONTACT_PICKER, entries.single().origin)
        val decision = repo.findMatch(number)
        assertEquals(CallBlockAction.ALLOW, decision?.action)
        assertEquals(CallBlockDecisionTier.EXACT_ALLOWLIST, decision?.decisionTier)
    }

    @Test
    fun restoreNeverDemotesExistingAllowWithImportedBlock() = runBlocking {
        val number = "0912345678"
        repo.upsertNumberEntry(CallBlockAction.ALLOW, number)
        val importedBlock = BackupNumberEntry(
            action = CallBlockAction.BLOCK.storageKey,
            rawNumber = number,
            phoneKey = PhoneKey.of(number),
            displayName = "Imported block",
            origin = NumberEntryOrigin.MANUAL.storageKey,
            enabled = true,
            createdAt = 1L,
        )

        repo.restoreBlockingData(listOf(importedBlock), emptyList(), MergeMode.UPDATE)

        val entries = repo.observeNumberEntries().first()
        assertEquals(1, entries.size)
        assertEquals(CallBlockAction.ALLOW, entries.single().action)
        assertEquals(CallBlockAction.ALLOW, repo.findMatch(number)?.action)
    }

    @Test
    fun firstAdvancedRuleWinsAndUserCanReorderIt() = runBlocking {
        repo.saveRule(
            id = null,
            type = CallBlockRuleType.CONTAINS,
            rawValue = "123",
            enabled = true,
            action = CallBlockAction.ALLOW,
            scope = CallBlockScope.ALL_VISIBLE_NUMBERS,
        )
        repo.saveRule(
            id = null,
            type = CallBlockRuleType.PREFIX,
            rawValue = "09",
            enabled = true,
            action = CallBlockAction.BLOCK,
            scope = CallBlockScope.ALL_VISIBLE_NUMBERS,
        )
        val ordered = repo.observeRules().first()
        val allowRule = ordered.first { it.action == CallBlockAction.ALLOW }
        val blockRule = ordered.first { it.action == CallBlockAction.BLOCK }

        assertEquals(CallBlockAction.ALLOW, repo.findMatch("0912345678")?.action)
        assertTrue(repo.moveAdvancedRule(blockRule.id, -1))
        assertEquals(CallBlockAction.BLOCK, repo.findMatch("0912345678")?.action)
        assertTrue(repo.moveAdvancedRule(allowRule.id, -1))
        assertEquals(CallBlockAction.ALLOW, repo.findMatch("0912345678")?.action)
    }

    @Test
    fun noEntryAndNoRuleAllowsCall() = runBlocking {
        assertNull(repo.findMatch("0987654321"))
    }

    @Test
    fun spamRiskIsBlockOnlyRecordsItsSignalAndStillLosesToExactAllowlist() = runBlocking {
        assertEquals(
            SaveBlockRuleResult.INVALID,
            repo.saveRule(
                id = null,
                type = CallBlockRuleType.SPAM_RISK,
                rawValue = "app_default",
                enabled = true,
                action = CallBlockAction.ALLOW,
                scope = CallBlockScope.ALL_VISIBLE_NUMBERS,
            ),
        )
        assertEquals(
            SaveBlockRuleResult.SAVED,
            repo.saveRule(
                id = null,
                type = CallBlockRuleType.SPAM_RISK,
                rawValue = "app_default",
                enabled = true,
                action = CallBlockAction.BLOCK,
                scope = CallBlockScope.ALL_VISIBLE_NUMBERS,
            ),
        )

        repo.upsertNumberEntry(CallBlockAction.ALLOW, "02412345678")
        assertEquals(CallBlockDecisionTier.EXACT_ALLOWLIST, repo.findMatch("02412345678")?.decisionTier)
        repo.observeNumberEntries(CallBlockAction.ALLOW).first().single().let { repo.deleteNumberEntry(it.id) }

        val prefixMatch = requireNotNull(repo.findMatch("+842412345678"))
        assertEquals(CallBlockRuleType.SPAM_RISK, prefixMatch.rule.type)
        assertEquals(SpamRiskReasonKind.PREFIX, SpamRiskReasonCodec.decode(prefixMatch.historyReasonValue)?.kind)

        val verificationMatch = requireNotNull(
            repo.findMatch(
                number = "0901234567",
                callerNumberVerificationStatus = CallerNumberVerificationStatus.FAILED,
            )
        )
        assertEquals(
            SpamRiskReasonKind.VERIFICATION_FAILED,
            SpamRiskReasonCodec.decode(verificationMatch.historyReasonValue)?.kind,
        )
    }
}

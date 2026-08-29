package com.antimobile.mcas

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.antimobile.mcas.data.backup.BackupBlockedCall
import com.antimobile.mcas.data.backup.BackupNumberEntry
import com.antimobile.mcas.data.backup.MergeMode
import com.antimobile.mcas.data.local.AppDatabase
import com.antimobile.mcas.data.blocking.CallBlockAction
import com.antimobile.mcas.data.blocking.CallBlockDecisionTier
import com.antimobile.mcas.data.blocking.CallBlockMethod
import com.antimobile.mcas.data.blocking.CallBlockHistoryEntity
import com.antimobile.mcas.data.blocking.CallBlockRepository
import com.antimobile.mcas.data.blocking.CallBlockRuleType
import com.antimobile.mcas.data.blocking.CallBlockScope
import com.antimobile.mcas.data.blocking.CallBlockSettings
import com.antimobile.mcas.data.blocking.CallerNumberVerificationStatus
import com.antimobile.mcas.data.blocking.IncomingCallAddressParser
import com.antimobile.mcas.data.blocking.NumberEntryOrigin
import com.antimobile.mcas.data.blocking.SaveBlockRuleResult
import com.antimobile.mcas.data.blocking.SpecialCallCondition
import com.antimobile.mcas.data.blocking.SipCallerIdKind
import com.antimobile.mcas.data.blocking.SipCallerIdentityParser
import com.antimobile.mcas.data.blocking.SpamRiskReasonCodec
import com.antimobile.mcas.data.blocking.SpamRiskReasonKind
import com.antimobile.mcas.util.PhoneKey
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
        repo.restoreHistory(emptyList(), MergeMode.REPLACE)
        context.getSharedPreferences("call_block_settings", 0).edit().clear().commit()
        context.getSharedPreferences("call_block_runtime_state", 0).edit().clear().commit()
        context.getSharedPreferences("call_block_repeat_unknown_guard_attempts", 0).edit().clear().commit()
        context.getSharedPreferences("call_block_data_migrations", 0).edit().clear().commit()
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
        val rejectedAlphaTel = IncomingCallAddressParser.parse("tel", "SERVICE123")
        assertNull(
            repo.findMatch(
                number = rejectedAlphaTel.screeningAddress,
                callerNumberVerificationStatus = CallerNumberVerificationStatus.FAILED,
            )
        )
    }

    @Test
    fun sipRulesSurviveRepositorySnapshotAndRespectPrecedence() = runBlocking {
        assertEquals(
            SaveBlockRuleResult.SAVED,
            repo.saveRule(
                id = null,
                type = CallBlockRuleType.SPECIAL,
                rawValue = SpecialCallCondition.encode(setOf(SpecialCallCondition.SIP_PHONE_NUMBER)),
                enabled = true,
                action = CallBlockAction.BLOCK,
                scope = CallBlockScope.ALL_VISIBLE_NUMBERS,
            ),
        )
        assertEquals(
            SaveBlockRuleResult.SAVED,
            repo.saveRule(
                id = null,
                type = CallBlockRuleType.SPECIAL,
                rawValue = SpecialCallCondition.encode(setOf(SpecialCallCondition.SIP_TEXT_ID)),
                enabled = true,
                action = CallBlockAction.BLOCK,
                scope = CallBlockScope.ALL_VISIBLE_NUMBERS,
            ),
        )
        val sipPhone = SipCallerIdentityParser.parse("sip", "+84987654321@provider.vn")
        val sipText = SipCallerIdentityParser.parse("sips", "support@company.vn")

        repo.upsertNumberEntry(CallBlockAction.ALLOW, "+84987654321")
        assertEquals(
            CallBlockDecisionTier.EXACT_ALLOWLIST,
            repo.findMatch(
                number = "+84987654321",
                isVoip = true,
                sipCallerIdentity = sipPhone,
            )?.decisionTier,
        )
        repo.observeNumberEntries(CallBlockAction.ALLOW).first().single().let {
            repo.deleteNumberEntry(it.id)
        }

        assertEquals(
            SpecialCallCondition.SIP_PHONE_NUMBER,
            repo.findMatch(
                number = "+84987654321",
                isVoip = true,
                sipCallerIdentity = sipPhone,
            )?.rule?.rawValue?.let(SpecialCallCondition::activeSelection),
        )
        assertEquals(
            SpecialCallCondition.SIP_TEXT_ID,
            repo.findMatch(
                number = "support@company.vn",
                isVoip = true,
                sipCallerIdentity = sipText,
            )?.rule?.rawValue?.let(SpecialCallCondition::activeSelection),
        )
    }

    @Test
    fun androidUriEncodedHandleIsDecodedExactlyOnceByClassifier() {
        val telPhoneUri = Uri.parse("tel:%2B84912345678")
        val sipPhoneUri = Uri.parse("sip:%2B84912345678@provider.vn")
        val encodedLiteralUri = Uri.parse("sip:%252B84912345678@provider.vn")
        val opaqueSipPhoneUri = Uri.fromParts("sip", "+84987654321@provider.vn", null)
        val opaqueTelUri = Uri.fromParts("tel", "912345678;phone-context=+84", null)
        val opaqueEncodedLiteralUri = Uri.fromParts("sip", "%2B84912345678@provider.vn", null)
        val opaqueTextUserUri = Uri.fromParts("sip", "alice?dept@example.com", null)
        val headerOnlyUri = Uri.parse("sip:example.com?to=alice%40example.net")
        val opaqueHeaderOnlyUri = Uri.fromParts(
            "sip",
            "example.com?to=alice@example.net",
            null,
        )
        val fragmentedSipUri = Uri.parse("sip:alice@example.com#x")
        val fragmentedTelUri = Uri.parse("tel:+84912345678#")

        val telPhone = IncomingCallAddressParser.parse(
            telPhoneUri.scheme,
            telPhoneUri.encodedSchemeSpecificPart,
            telPhoneUri.schemeSpecificPart,
        )
        val sipPhone = IncomingCallAddressParser.parse(
            sipPhoneUri.scheme,
            sipPhoneUri.encodedSchemeSpecificPart,
            sipPhoneUri.schemeSpecificPart,
        )
        val encodedLiteral = IncomingCallAddressParser.parse(
            encodedLiteralUri.scheme,
            encodedLiteralUri.encodedSchemeSpecificPart,
            encodedLiteralUri.schemeSpecificPart,
        )
        val opaqueSipPhone = IncomingCallAddressParser.parse(
            opaqueSipPhoneUri.scheme,
            opaqueSipPhoneUri.encodedSchemeSpecificPart,
            opaqueSipPhoneUri.schemeSpecificPart,
        )
        val opaqueTel = IncomingCallAddressParser.parse(
            opaqueTelUri.scheme,
            opaqueTelUri.encodedSchemeSpecificPart,
            opaqueTelUri.schemeSpecificPart,
        )
        val opaqueEncodedLiteral = IncomingCallAddressParser.parse(
            opaqueEncodedLiteralUri.scheme,
            opaqueEncodedLiteralUri.encodedSchemeSpecificPart,
            opaqueEncodedLiteralUri.schemeSpecificPart,
        )
        val opaqueTextUser = IncomingCallAddressParser.parse(
            opaqueTextUserUri.scheme,
            opaqueTextUserUri.encodedSchemeSpecificPart,
            opaqueTextUserUri.schemeSpecificPart,
        )
        val headerOnly = IncomingCallAddressParser.parse(
            headerOnlyUri.scheme,
            headerOnlyUri.encodedSchemeSpecificPart,
            headerOnlyUri.schemeSpecificPart,
        )
        val opaqueHeaderOnly = IncomingCallAddressParser.parse(
            opaqueHeaderOnlyUri.scheme,
            opaqueHeaderOnlyUri.encodedSchemeSpecificPart,
            opaqueHeaderOnlyUri.schemeSpecificPart,
        )
        val fragmentedSip = IncomingCallAddressParser.parse(
            scheme = fragmentedSipUri.scheme,
            encodedSchemeSpecificPart = fragmentedSipUri.encodedSchemeSpecificPart,
            decodedSchemeSpecificPart = fragmentedSipUri.schemeSpecificPart,
            encodedFragment = fragmentedSipUri.encodedFragment,
        )
        val fragmentedTel = IncomingCallAddressParser.parse(
            scheme = fragmentedTelUri.scheme,
            encodedSchemeSpecificPart = fragmentedTelUri.encodedSchemeSpecificPart,
            decodedSchemeSpecificPart = fragmentedTelUri.schemeSpecificPart,
            encodedFragment = fragmentedTelUri.encodedFragment,
        )

        assertEquals("+84912345678", telPhone.telephoneNumber)
        assertEquals("+84912345678", sipPhone.telephoneNumber)
        assertEquals(null, encodedLiteral.telephoneNumber)
        assertEquals("%2B84912345678", encodedLiteral.sipCallerIdentity.user)
        assertEquals(null, opaqueSipPhone.telephoneNumber)
        assertEquals(SipCallerIdKind.UNKNOWN, opaqueSipPhone.sipCallerIdentity.kind)
        assertEquals(null, opaqueTel.telephoneNumber)
        assertEquals(null, opaqueEncodedLiteral.telephoneNumber)
        assertEquals(SipCallerIdKind.UNKNOWN, opaqueEncodedLiteral.sipCallerIdentity.kind)
        assertEquals(SipCallerIdKind.UNKNOWN, opaqueTextUser.sipCallerIdentity.kind)
        assertEquals(SipCallerIdKind.UNKNOWN, headerOnly.sipCallerIdentity.kind)
        assertEquals(SipCallerIdKind.UNKNOWN, opaqueHeaderOnly.sipCallerIdentity.kind)
        assertEquals(SipCallerIdKind.UNKNOWN, fragmentedSip.sipCallerIdentity.kind)
        assertEquals(null, fragmentedTel.telephoneNumber)
        assertEquals("", fragmentedTel.screeningAddress)
    }

    @Test
    fun sipHistoryIsCanonicalAtRestoreExportAndLegacyMigrationBoundaries() = runBlocking {
        val dao = AppDatabase.get(context).callBlockDao()
        val secretUri = "sips:alice:secret@example.com?subject=private"
        val safeUri = "sips:alice@example.com"
        val reason = SpecialCallCondition.SIP_TEXT_ID.storageKey
        val backup = BackupBlockedCall(
            rawNumber = secretUri,
            phoneKey = "untrusted-backup-key",
            blockedAt = 123L,
            ruleType = CallBlockRuleType.SPECIAL.storageKey,
            ruleValue = reason,
            consecutiveUnanswered = 0,
        )

        assertEquals(1, repo.restoreHistory(listOf(backup), MergeMode.REPLACE).added)
        assertEquals(safeUri, repo.observeHistory().first().single().rawNumber)
        val exported = repo.exportHistoryForBackup().single()
        assertEquals(safeUri, exported.rawNumber)
        assertTrue("secret" !in exported.rawNumber && "subject" !in exported.rawNumber)
        assertTrue(exported.phoneKey != backup.phoneKey)

        dao.deleteAllHistory()
        context.getSharedPreferences("call_block_data_migrations", 0).edit().clear().commit()
        val malformedLegacySecret = "sips:alice:secret@-bad.example/path?subject=private"
        dao.insertHistory(
            CallBlockHistoryEntity(
                rawNumber = malformedLegacySecret,
                phoneKey = "uri:legacy-reversible-value",
                blockedAt = 456L,
                ruleType = CallBlockRuleType.SPECIAL.storageKey,
                ruleValue = reason,
            )
        )
        repo.warmScreeningRuleSnapshot()
        val migrated = dao.getHistory().single()
        assertTrue(migrated.rawNumber.startsWith("sips:redacted-"))
        assertTrue(migrated.rawNumber.endsWith("@invalid"))
        assertTrue("secret" !in migrated.rawNumber && "subject" !in migrated.rawNumber)
        assertTrue(migrated.phoneKey != "uri:legacy-reversible-value")

        dao.deleteAllHistory()
        context.getSharedPreferences("call_block_data_migrations", 0).edit().clear().commit()
        listOf(
            "sips:alice:first@example.com?subject=private" to "uri:legacy-one",
            "sips:alice:second@example.com?subject=private" to "uri:legacy-two",
        ).forEach { (raw, key) ->
            dao.insertHistory(
                CallBlockHistoryEntity(
                    rawNumber = raw,
                    phoneKey = key,
                    blockedAt = 789L,
                    ruleType = CallBlockRuleType.SPECIAL.storageKey,
                    ruleValue = reason,
                )
            )
        }
        repo.warmScreeningRuleSnapshot()
        val coalesced = dao.getHistory().single()
        assertEquals(safeUri, coalesced.rawNumber)

        dao.deleteAllHistory()
        context.getSharedPreferences("call_block_data_migrations", 0).edit().clear().commit()
        listOf(
            "sips:alice:first@-bad.example/path?subject=private",
            "sips:bob:second@-bad.example/path?subject=private",
        ).forEachIndexed { index, raw ->
            dao.insertHistory(
                CallBlockHistoryEntity(
                    rawNumber = raw,
                    phoneKey = "uri:invalid-$index",
                    blockedAt = 999L,
                    ruleType = CallBlockRuleType.SPECIAL.storageKey,
                    ruleValue = reason,
                )
            )
        }
        repo.warmScreeningRuleSnapshot()
        val independentlyRedacted = dao.getHistory()
        assertEquals(2, independentlyRedacted.size)
        assertEquals(2, independentlyRedacted.map { it.phoneKey }.toSet().size)
        assertTrue(independentlyRedacted.all { it.rawNumber.startsWith("sips:redacted-") })
        assertTrue(independentlyRedacted.all { it.rawNumber.endsWith("@invalid") })
    }
}

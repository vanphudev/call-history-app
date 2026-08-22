package com.antimobile.callhs

import com.antimobile.callhs.data.blocking.CallBlockRule
import com.antimobile.callhs.data.blocking.CallBlockContactSelection
import com.antimobile.callhs.data.blocking.CallBlockCallHistorySelection
import com.antimobile.callhs.data.blocking.CallBlockAction
import com.antimobile.callhs.data.blocking.CallBlockRuleMatcher
import com.antimobile.callhs.data.blocking.CallBlockRuleType
import com.antimobile.callhs.data.blocking.CallBlockHistoryReasonCodec
import com.antimobile.callhs.data.blocking.CallBlockScope
import com.antimobile.callhs.data.blocking.CallScreeningContext
import com.antimobile.callhs.data.blocking.CallerNumberVerificationStatus
import com.antimobile.callhs.data.blocking.BrandNamePresetCatalog
import com.antimobile.callhs.data.blocking.BrandNameRuleCodec
import com.antimobile.callhs.data.blocking.BlockedCallerIdentity
import com.antimobile.callhs.data.blocking.CallHistoryRuleCodec
import com.antimobile.callhs.data.blocking.ContactLookupStatus
import com.antimobile.callhs.data.blocking.ContactRuleCodec
import com.antimobile.callhs.data.blocking.GeographicBlockKind
import com.antimobile.callhs.data.blocking.GeographicBlockOption
import com.antimobile.callhs.data.blocking.LEGACY_REPEAT_UNANSWERED_REASON_TYPE
import com.antimobile.callhs.data.blocking.SpecialCallCondition
import com.antimobile.callhs.data.blocking.SipCallerIdKind
import com.antimobile.callhs.data.blocking.SipCallerIdentityParser
import com.antimobile.callhs.data.blocking.SpamRiskReason
import com.antimobile.callhs.data.blocking.SpamRiskReasonCodec
import com.antimobile.callhs.data.blocking.SpamRiskReasonKind
import com.antimobile.callhs.util.PhoneKey
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallBlockRuleMatcherTest {

    @Test
    fun exactNumberMatchesVietnameseDomesticAndInternationalForms() {
        val rule = rule(CallBlockRuleType.EXACT_NUMBER, "0912 345 678")

        assertTrue(CallBlockRuleMatcher.matches(rule, "+84 912 345 678"))
        assertTrue(CallBlockRuleMatcher.matches(rule, "0084912345678"))
        assertFalse(CallBlockRuleMatcher.matches(rule, "0912345679"))
    }

    @Test
    fun prefixPatternNormalizesTrunkAndCountryCodeForPartialInput() {
        val rule = rule(CallBlockRuleType.PREFIX, "098")

        assertEquals("98", rule.matchValue)
        assertTrue(CallBlockRuleMatcher.matches(rule, "+84 987 123 456"))
        assertFalse(CallBlockRuleMatcher.matches(rule, "0912345678"))
    }

    @Test
    fun twoDigitDomesticPrefixRemainsValidAfterTrunkNormalization() {
        val rule = rule(CallBlockRuleType.PREFIX, "09")

        assertTrue(CallBlockRuleMatcher.isValid(CallBlockRuleType.PREFIX, "09"))
        assertEquals("9", rule.matchValue)
        assertTrue(CallBlockRuleMatcher.matches(rule, "+84 912 345 678"))
        assertFalse(CallBlockRuleMatcher.matches(rule, "028 1234 5678"))
    }

    @Test
    fun suffixAndContainsOnlyMatchTheirOwnCriteria() {
        val suffix = rule(CallBlockRuleType.SUFFIX, "678")
        val contains = rule(CallBlockRuleType.CONTAINS, "345")

        assertTrue(CallBlockRuleMatcher.matches(suffix, "0912345678"))
        assertFalse(CallBlockRuleMatcher.matches(suffix, "0912345670"))
        assertTrue(CallBlockRuleMatcher.matches(contains, "0912345678"))
        assertFalse(CallBlockRuleMatcher.matches(contains, "0987654321"))
    }

    @Test
    fun suffixAndContainsKeepALeadingZeroLiteral() {
        val suffix = rule(CallBlockRuleType.SUFFIX, "09")
        val contains = rule(CallBlockRuleType.CONTAINS, "045")

        assertEquals("09", suffix.matchValue)
        assertEquals("045", contains.matchValue)
        assertTrue(CallBlockRuleMatcher.matches(suffix, "0912304509"))
        assertFalse(CallBlockRuleMatcher.matches(suffix, "0912304519"))
        assertTrue(CallBlockRuleMatcher.matches(contains, "0912304567"))
    }

    @Test
    fun carrierUsesSharedCarrierDataset() {
        val rule = rule(CallBlockRuleType.CARRIER, "Viettel")

        assertTrue(CallBlockRuleMatcher.matches(rule, "0981234567"))
        assertFalse(CallBlockRuleMatcher.matches(rule, "0912345678"))
    }

    @Test
    fun geographicPayloadIsCanonicalAndAllPresetOnlyRemovesRedundantCountries() {
        val raw = GeographicBlockOption.encode(
            setOf(
                GeographicBlockOption.VIETNAM_PREFIX_028,
                GeographicBlockOption.CHINA,
                GeographicBlockOption.ALL_INTERNATIONAL_EXCEPT_VIETNAM,
                GeographicBlockOption.VIETNAM_PREFIX_024,
                GeographicBlockOption.LAOS,
            )
        )

        assertEquals("international_except_vietnam,024,028", raw)
        assertEquals(
            setOf(
                GeographicBlockOption.ALL_INTERNATIONAL_EXCEPT_VIETNAM,
                GeographicBlockOption.VIETNAM_PREFIX_024,
                GeographicBlockOption.VIETNAM_PREFIX_028,
            ),
            GeographicBlockOption.decode(raw),
        )
        assertEquals(raw, CallBlockRuleMatcher.canonicalRawValue(CallBlockRuleType.GEOGRAPHIC, raw))
        assertTrue(CallBlockRuleMatcher.isValid(CallBlockRuleType.GEOGRAPHIC, raw))
        assertFalse(CallBlockRuleMatcher.isValid(CallBlockRuleType.GEOGRAPHIC, "cn,future"))
        assertFalse(CallBlockRuleMatcher.isValid(CallBlockRuleType.GEOGRAPHIC, ""))
    }

    @Test
    fun everyConfiguredCountryCallingCodeMatchesPlusAndDoubleZeroForms() {
        val samples = linkedMapOf(
            GeographicBlockOption.CHINA to "861012345678",
            GeographicBlockOption.CAMBODIA to "85512123456",
            GeographicBlockOption.MYANMAR to "9512345678",
            GeographicBlockOption.NANP_SHARED to "12025550123",
            GeographicBlockOption.GERMANY to "49301234567",
            GeographicBlockOption.LAOS to "8562012345678",
            GeographicBlockOption.THAILAND to "6621234567",
            GeographicBlockOption.MALAYSIA to "60312345678",
            GeographicBlockOption.SINGAPORE to "6561234567",
            GeographicBlockOption.INDONESIA to "622112345678",
            GeographicBlockOption.PHILIPPINES to "63281234567",
            GeographicBlockOption.INDIA to "911123456789",
        )

        samples.forEach { (option, digits) ->
            assertEquals(GeographicBlockKind.COUNTRY_CALLING_CODE, option.kind)
            val countryRule = geographicRule(option)
            assertTrue(option.storageKey, CallBlockRuleMatcher.matches(countryRule, "+$digits"))
            assertTrue(option.storageKey, CallBlockRuleMatcher.matches(countryRule, "00$digits"))
        }
    }

    @Test
    fun countryMatchingRequiresExplicitInternationalNotationAndKeepsCodesDistinct() {
        val china = geographicRule(GeographicBlockOption.CHINA)
        val cambodia = geographicRule(GeographicBlockOption.CAMBODIA)
        val laos = geographicRule(GeographicBlockOption.LAOS)

        assertTrue(CallBlockRuleMatcher.matches(china, "+86 (10) 1234-5678"))
        assertTrue(CallBlockRuleMatcher.matches(cambodia, "+855 12 123 456"))
        assertTrue(CallBlockRuleMatcher.matches(laos, "+856 20 1234 5678"))
        assertFalse(CallBlockRuleMatcher.matches(china, "+855 12 123 456"))
        assertFalse(CallBlockRuleMatcher.matches(cambodia, "+856 20 1234 5678"))
        assertFalse(CallBlockRuleMatcher.matches(china, "861012345678"))
        assertFalse(CallBlockRuleMatcher.matches(china, "0861012345"))
    }

    @Test
    fun allInternationalPresetExcludesVietnamAndAmbiguousOrMalformedNumbers() {
        val all = geographicRule(GeographicBlockOption.ALL_INTERNATIONAL_EXCEPT_VIETNAM)

        assertTrue(CallBlockRuleMatcher.matches(all, "+86 10 1234 5678"))
        assertTrue(CallBlockRuleMatcher.matches(all, "0049 30 1234567"))
        assertTrue(CallBlockRuleMatcher.matches(all, "+33 1 23 45 67 89"))
        assertFalse(CallBlockRuleMatcher.matches(all, "+84 912 345 678"))
        assertFalse(CallBlockRuleMatcher.matches(all, "0084 24 1234 5678"))
        assertFalse(CallBlockRuleMatcher.matches(all, "0861012345"))
        assertFalse(CallBlockRuleMatcher.matches(all, "861012345678"))
        assertFalse(CallBlockRuleMatcher.matches(all, "+12"))
        assertFalse(CallBlockRuleMatcher.matches(all, "+0123456789"))
        assertFalse(CallBlockRuleMatcher.matches(all, "++861012345678"))
        assertFalse(CallBlockRuleMatcher.matches(all, "+86-CALL-NOW"))
        assertFalse(CallBlockRuleMatcher.matches(all, "+8612345678901234"))
    }

    @Test
    fun vietnamPrefixesMatchDomesticPlus84And0084WithoutBecomingInternational() {
        val samples = linkedMapOf(
            GeographicBlockOption.VIETNAM_PREFIX_024 to Triple("02412345678", "+842412345678", "00842412345678"),
            GeographicBlockOption.VIETNAM_PREFIX_022 to Triple("02212345678", "+842212345678", "00842212345678"),
            GeographicBlockOption.VIETNAM_PREFIX_028 to Triple("02812345678", "+842812345678", "00842812345678"),
            GeographicBlockOption.VIETNAM_PREFIX_059 to Triple("0591234567", "+84591234567", "0084591234567"),
            GeographicBlockOption.VIETNAM_PREFIX_099 to Triple("0991234567", "+84991234567", "0084991234567"),
        )

        samples.forEach { (option, numbers) ->
            val prefixRule = geographicRule(option)
            assertTrue(option.storageKey, CallBlockRuleMatcher.matches(prefixRule, numbers.first))
            assertTrue(option.storageKey, CallBlockRuleMatcher.matches(prefixRule, numbers.second))
            assertTrue(option.storageKey, CallBlockRuleMatcher.matches(prefixRule, numbers.third))
        }
        assertFalse(
            CallBlockRuleMatcher.matches(
                geographicRule(GeographicBlockOption.VIETNAM_PREFIX_024),
                "+8402412345678",
            )
        )
        assertFalse(
            CallBlockRuleMatcher.matches(
                geographicRule(GeographicBlockOption.VIETNAM_PREFIX_024),
                "+862412345678",
            )
        )
    }

    @Test
    fun spamRiskProfileMatchesRequestedPrefixesInStrictCompleteVietnameseForms() {
        val rule = rule(CallBlockRuleType.SPAM_RISK, CallBlockRuleMatcher.SPAM_RISK_PROFILE)
        val samples = linkedMapOf(
            "023" to Triple("02312345678", "+842312345678", "00842312345678"),
            "024" to Triple("02412345678", "+842412345678", "00842412345678"),
            "028" to Triple("02812345678", "+842812345678", "00842812345678"),
            "022" to Triple("02212345678", "+842212345678", "00842212345678"),
            "059" to Triple("0591234567", "+84591234567", "0084591234567"),
            "099" to Triple("0991234567", "+84991234567", "0084991234567"),
        )

        samples.forEach { (prefix, numbers) ->
            listOf(numbers.first, numbers.second, numbers.third).forEach { number ->
                val reason = CallBlockRuleMatcher.spamRiskReason(CallScreeningContext(number))
                assertEquals(number, SpamRiskReason(SpamRiskReasonKind.PREFIX, prefix), reason)
                assertTrue(number, CallBlockRuleMatcher.matches(rule, number))
            }
        }

        listOf(
            "0231234567", "023123456789",
            "0241234567", "024123456789",
            "0281234567", "028123456789",
            "0221234567", "022123456789",
            "059123456", "05912345678",
            "099123456", "09912345678",
            "+84241234567", "+8424123456789",
            "008459123456", "00845912345678",
        ).forEach { malformed ->
            assertEquals(malformed, null, CallBlockRuleMatcher.spamRiskReason(CallScreeningContext(malformed)))
        }
    }

    @Test
    fun spamRiskProfileMatchesOnlyStrictUnrecognizedVietnameseMobileNumbers() {
        listOf("0541234567", "+84541234567", "0084541234567").forEach { number ->
            assertEquals(
                number,
                SpamRiskReason(SpamRiskReasonKind.UNKNOWN_MOBILE_PREFIX, "054"),
                CallBlockRuleMatcher.spamRiskReason(CallScreeningContext(number)),
            )
        }
        listOf(
            "0901234567", // recognized mobile prefix
            "02091234567", // Vietnamese fixed line outside the requested broad prefixes
            "1900123456", // service number
            "+861012345678", // international
            "541234567", // ambiguous bare national number
            "054123456", "05412345678", // malformed lengths
        ).forEach { number ->
            assertEquals(number, null, CallBlockRuleMatcher.spamRiskReason(CallScreeningContext(number)))
        }
    }

    @Test
    fun failedVerificationIsAnIndependentSpamSignalButPassedDoesNotExemptPrefixes() {
        assertEquals(
            SpamRiskReason(SpamRiskReasonKind.VERIFICATION_FAILED),
            CallBlockRuleMatcher.spamRiskReason(
                CallScreeningContext(
                    number = "0901234567",
                    callerNumberVerificationStatus = CallerNumberVerificationStatus.FAILED,
                )
            ),
        )
        assertEquals(
            SpamRiskReason(SpamRiskReasonKind.PREFIX, "024"),
            CallBlockRuleMatcher.spamRiskReason(
                CallScreeningContext(
                    number = "02412345678",
                    callerNumberVerificationStatus = CallerNumberVerificationStatus.PASSED,
                )
            ),
        )
        assertEquals(
            null,
            CallBlockRuleMatcher.spamRiskReason(
                CallScreeningContext(
                    number = "0901234567",
                    callerNumberVerificationStatus = CallerNumberVerificationStatus.NOT_VERIFIED,
                )
            ),
        )
        assertEquals(
            null,
            CallBlockRuleMatcher.spamRiskReason(
                CallScreeningContext(
                    number = "0901234567",
                    isVoip = true,
                    callerNumberVerificationStatus = CallerNumberVerificationStatus.FAILED,
                )
            ),
        )
    }

    @Test
    fun spamRiskPayloadActionAndReasonCodecAreStrict() {
        assertTrue(CallBlockRuleMatcher.isValid(CallBlockRuleType.SPAM_RISK, "app_default"))
        assertFalse(CallBlockRuleMatcher.isValid(CallBlockRuleType.SPAM_RISK, ""))
        assertFalse(CallBlockRuleMatcher.isValid(CallBlockRuleType.SPAM_RISK, "future"))
        assertTrue(CallBlockRuleType.SPAM_RISK.supportsAction(CallBlockAction.BLOCK))
        assertFalse(CallBlockRuleType.SPAM_RISK.supportsAction(CallBlockAction.ALLOW))

        val reasons = listOf(
            SpamRiskReason(SpamRiskReasonKind.PREFIX, "024"),
            SpamRiskReason(SpamRiskReasonKind.UNKNOWN_MOBILE_PREFIX, "054"),
            SpamRiskReason(SpamRiskReasonKind.VERIFICATION_FAILED),
        )
        reasons.forEach { reason ->
            assertEquals(reason, SpamRiskReasonCodec.decode(SpamRiskReasonCodec.encode(reason)))
        }
        assertEquals(null, SpamRiskReasonCodec.decode("v1|prefix|24"))
        assertEquals(null, SpamRiskReasonCodec.decode("v1|prefix|090"))
        // Decoding history is intentionally independent from the mutable carrier catalog: a prefix that
        // was unknown when recorded must retain its original reason after a future catalog update.
        assertEquals(
            SpamRiskReason(SpamRiskReasonKind.UNKNOWN_MOBILE_PREFIX, "090"),
            SpamRiskReasonCodec.decode("v1|unknown_mobile_prefix|090"),
        )
        assertEquals(null, SpamRiskReasonCodec.decode("v1|unknown_mobile_prefix|020"))
        assertEquals(null, SpamRiskReasonCodec.decode("v1|verification_failed|024"))
        assertEquals(null, SpamRiskReasonCodec.decode("v2|prefix|024"))

        val malformedHistory = requireNotNull(
            CallBlockHistoryReasonCodec.display(CallBlockRuleType.SPAM_RISK.storageKey, "corrupt")
        )
        assertEquals(CallBlockRuleMatcher.SPAM_RISK_PROFILE, malformedHistory.ruleValue)
        assertEquals(
            LEGACY_REPEAT_UNANSWERED_REASON_TYPE,
            CallBlockHistoryReasonCodec.display(LEGACY_REPEAT_UNANSWERED_REASON_TYPE, "5")?.ruleType,
        )
        assertEquals(null, CallBlockHistoryReasonCodec.display(LEGACY_REPEAT_UNANSWERED_REASON_TYPE, "4"))
    }

    @Test
    fun geographicOptionsUseOrSemanticsButHiddenAndVoipContextsFailOpen() {
        val raw = GeographicBlockOption.encode(
            setOf(GeographicBlockOption.CHINA, GeographicBlockOption.VIETNAM_PREFIX_024)
        )
        val mixed = rule(CallBlockRuleType.GEOGRAPHIC, raw)

        assertTrue(CallBlockRuleMatcher.matches(mixed, "+861012345678"))
        assertTrue(CallBlockRuleMatcher.matches(mixed, "02412345678"))
        assertFalse(CallBlockRuleMatcher.matches(mixed, "+49301234567"))
        assertFalse(
            CallBlockRuleMatcher.matches(
                mixed,
                CallScreeningContext(number = "+861012345678", isPrivateNumber = true),
            )
        )
        assertFalse(
            CallBlockRuleMatcher.matches(
                mixed,
                CallScreeningContext(number = "+861012345678", isVoip = true),
            )
        )
    }

    @Test
    fun exactRuleWinsBeforeBroadPattern() {
        val broad = rule(CallBlockRuleType.CONTAINS, "12", id = 2L)
        val exact = rule(CallBlockRuleType.EXACT_NUMBER, "0912345678", id = 1L)

        assertEquals(listOf(exact, broad), CallBlockRuleMatcher.ordered(listOf(broad, exact)))
    }

    @Test
    fun specialRuleAcceptsExactlyOneActiveCondition() {
        val privateRaw = SpecialCallCondition.encode(setOf(SpecialCallCondition.PRIVATE_NUMBER))
        val sipPhoneRaw = SpecialCallCondition.encode(setOf(SpecialCallCondition.SIP_PHONE_NUMBER))
        val sipTextRaw = SpecialCallCondition.encode(setOf(SpecialCallCondition.SIP_TEXT_ID))
        val legacyVoipRaw = SpecialCallCondition.encode(setOf(SpecialCallCondition.VOIP))
        val combinedRaw = SpecialCallCondition.encode(
            setOf(SpecialCallCondition.SIP_PHONE_NUMBER, SpecialCallCondition.SIP_TEXT_ID),
        )
        val privateRule = rule(CallBlockRuleType.SPECIAL, privateRaw)
        val sipPhoneRule = rule(CallBlockRuleType.SPECIAL, sipPhoneRaw)
        val sipTextRule = rule(CallBlockRuleType.SPECIAL, sipTextRaw)
        val phoneIdentity = SipCallerIdentityParser.parse("sip", "+84912345678@provider.vn")
        val textIdentity = SipCallerIdentityParser.parse("sip", "support@company.vn")

        assertEquals(
            listOf(
                SpecialCallCondition.PRIVATE_NUMBER,
                SpecialCallCondition.SIP_PHONE_NUMBER,
                SpecialCallCondition.SIP_TEXT_ID,
            ),
            SpecialCallCondition.activeEntries,
        )
        assertTrue(CallBlockRuleMatcher.isValid(CallBlockRuleType.SPECIAL, privateRaw))
        assertTrue(CallBlockRuleMatcher.isValid(CallBlockRuleType.SPECIAL, sipPhoneRaw))
        assertTrue(CallBlockRuleMatcher.isValid(CallBlockRuleType.SPECIAL, sipTextRaw))
        assertFalse(CallBlockRuleMatcher.isValid(CallBlockRuleType.SPECIAL, legacyVoipRaw))
        assertFalse(CallBlockRuleMatcher.isValid(CallBlockRuleType.SPECIAL, combinedRaw))
        assertTrue(
            CallBlockRuleMatcher.matches(
                privateRule,
                CallScreeningContext(number = "", isPrivateNumber = true),
            )
        )
        assertTrue(
            CallBlockRuleMatcher.matches(
                sipPhoneRule,
                CallScreeningContext(number = "+84912345678@provider.vn", isVoip = true, sipCallerIdentity = phoneIdentity),
            )
        )
        assertTrue(
            CallBlockRuleMatcher.matches(
                sipTextRule,
                CallScreeningContext(number = "support@company.vn", isVoip = true, sipCallerIdentity = textIdentity),
            )
        )
        assertFalse(
            CallBlockRuleMatcher.matches(
                sipPhoneRule,
                CallScreeningContext(number = "support@company.vn", isVoip = true, sipCallerIdentity = textIdentity),
            )
        )
        assertFalse(CallBlockRuleMatcher.matches(privateRule, "0912345678"))
    }

    @Test
    fun sipParserSeparatesPhoneTextAndUnknownWithoutExtractingEmbeddedDigits() {
        val globalPhone = SipCallerIdentityParser.parse("sip", "+84912345678@provider.vn")
        val encodedPhone = SipCallerIdentityParser.parse("sips", "%2B84912345678@secure.vn")
        val parameterizedPhone = SipCallerIdentityParser.parse(
            "sip",
            "+358-555-1234567;postd=pp22@foo.com;user=phone",
        )
        val text = SipCallerIdentityParser.parse("sip", "agent123@company.vn")
        val explicitText = SipCallerIdentityParser.parse("sip", "12345@company.vn;user=ip")
        val invalidDeclaredPhone = SipCallerIdentityParser.parse("sip", "support@company.vn;user=phone")
        val missingUser = SipCallerIdentityParser.parse("sip", "company.vn")
        val missingHost = SipCallerIdentityParser.parse("sip", "support@")
        val ordinaryCli = SipCallerIdentityParser.parse("tel", "+84912345678")

        assertEquals(SipCallerIdKind.PHONE_NUMBER, globalPhone.kind)
        assertEquals("+84912345678", globalPhone.phoneNumber)
        assertEquals("+84912345678", encodedPhone.phoneNumber)
        assertEquals("+358-555-1234567", parameterizedPhone.phoneNumber)
        assertEquals(SipCallerIdKind.TEXT_ID, text.kind)
        assertEquals("agent123", text.user)
        assertEquals(SipCallerIdKind.TEXT_ID, explicitText.kind)
        assertEquals(SipCallerIdKind.UNKNOWN, invalidDeclaredPhone.kind)
        assertEquals(SipCallerIdKind.UNKNOWN, missingUser.kind)
        assertEquals(SipCallerIdKind.UNKNOWN, missingHost.kind)
        assertEquals(SipCallerIdKind.UNKNOWN, ordinaryCli.kind)
        assertFalse(CallHistoryRuleCodec.isSelectableNumber(text.user))
    }

    @Test
    fun sipPhoneUsesNumberRulesButSipTextCannotLeakEmbeddedDigits() {
        val exact = rule(CallBlockRuleType.EXACT_NUMBER, "+84912345678")
        val embeddedDigits = rule(CallBlockRuleType.CONTAINS, "123", id = 2L)
        val phoneIdentity = SipCallerIdentityParser.parse("sip", "+84912345678@provider.vn")
        val textIdentity = SipCallerIdentityParser.parse("sip", "agent123@company.vn")

        assertTrue(
            CallBlockRuleMatcher.matches(
                exact,
                CallScreeningContext("+84912345678@provider.vn", isVoip = true, sipCallerIdentity = phoneIdentity),
            )
        )
        assertFalse(
            CallBlockRuleMatcher.matches(
                embeddedDigits,
                CallScreeningContext("agent123@company.vn", isVoip = true, sipCallerIdentity = textIdentity),
            )
        )
        assertTrue(BlockedCallerIdentity.key("sip:agent123@company.vn")?.startsWith("uri:") == true)
        assertEquals(PhoneKey.of("+84912345678"), BlockedCallerIdentity.key("+84912345678"))
    }

    @Test
    fun brandNameRuleRoundTripsAndMatchesExactCaseOnly() {
        val names = listOf("Vietcombank", "VIETCOMBANK", "MoMo")
        val raw = BrandNameRuleCodec.encode(names)
        val rule = rule(CallBlockRuleType.BRAND_NAME, raw)

        assertEquals(names.sorted(), BrandNameRuleCodec.decode(raw))
        assertTrue(CallBlockRuleMatcher.isValid(CallBlockRuleType.BRAND_NAME, raw))
        assertTrue(
            CallBlockRuleMatcher.matches(
                rule,
                CallScreeningContext(number = "1900", callerDisplayName = "Vietcombank"),
            )
        )
        assertFalse(
            CallBlockRuleMatcher.matches(
                rule,
                CallScreeningContext(number = "1900", callerDisplayName = "vietcombank"),
            )
        )
        assertFalse(CallBlockRuleMatcher.matches(rule, CallScreeningContext(number = "1900")))
    }

    @Test
    fun brandNameRuleRejectsMissingNameAndMoreThanFiveNames() {
        val noNames = BrandNameRuleCodec.encode(emptyList())
        val sixNames = BrandNameRuleCodec.encode((1..6).map { "Brand$it" })

        assertFalse(CallBlockRuleMatcher.isValid(CallBlockRuleType.BRAND_NAME, noNames))
        assertFalse(CallBlockRuleMatcher.isValid(CallBlockRuleType.BRAND_NAME, sixNames))
    }

    @Test
    fun sipPhoneHonorsContactScopeWhileBrandNameIsIndependentFromContacts() {
        val special = rule(
            CallBlockRuleType.SPECIAL,
            SpecialCallCondition.encode(setOf(SpecialCallCondition.SIP_PHONE_NUMBER)),
        ).copy(scope = CallBlockScope.NOT_SAVED)
        val brand = rule(
            CallBlockRuleType.BRAND_NAME,
            BrandNameRuleCodec.encode(listOf("MoMo")),
            id = 2L,
        )
        val invalidScopedBrand = brand.copy(scope = CallBlockScope.SAVED_CONTACT)

        assertTrue(
            CallBlockRuleMatcher.matches(
                special,
                CallScreeningContext(
                    "+84912345678@provider.vn",
                    ContactLookupStatus.NOT_IN_CONTACTS,
                    isVoip = true,
                    sipCallerIdentity = SipCallerIdentityParser.parse("sip", "+84912345678@provider.vn"),
                ),
            )
        )
        assertFalse(
            CallBlockRuleMatcher.matches(
                special,
                CallScreeningContext(
                    "+84912345678@provider.vn",
                    ContactLookupStatus.IN_CONTACTS,
                    isVoip = true,
                    sipCallerIdentity = SipCallerIdentityParser.parse("sip", "+84912345678@provider.vn"),
                ),
            )
        )
        assertTrue(
            CallBlockRuleMatcher.matches(
                brand,
                CallScreeningContext("0912345678", ContactLookupStatus.IN_CONTACTS, callerDisplayName = "MoMo"),
            )
        )
        assertTrue(
            CallBlockRuleMatcher.matches(
                brand,
                CallScreeningContext("0912345678", ContactLookupStatus.NOT_IN_CONTACTS, callerDisplayName = "MoMo"),
            )
        )
        assertFalse(
            CallBlockRuleMatcher.matches(
                invalidScopedBrand,
                CallScreeningContext("0912345678", ContactLookupStatus.IN_CONTACTS, callerDisplayName = "MoMo"),
            )
        )
    }

    @Test
    fun brandNamePresetGroupsHaveAtMostFiveValidNames() {
        assertTrue(BrandNamePresetCatalog.groups.isNotEmpty())
        BrandNamePresetCatalog.groups.forEach { group ->
            assertTrue(group.names.size <= BrandNameRuleCodec.MAX_NAMES)
            assertEquals(group.names.size, group.names.distinct().size)
            assertTrue(group.names.all(BrandNameRuleCodec::isAllowedName))
        }
    }

    @Test
    fun unknownContactTokenIsCompatibilityOnlyAndCannotBecomeAnActiveSpecialRule() {
        val raw = SpecialCallCondition.encode(setOf(SpecialCallCondition.UNKNOWN_CONTACT))

        assertEquals(setOf(SpecialCallCondition.UNKNOWN_CONTACT), SpecialCallCondition.decode(raw))
        assertFalse(SpecialCallCondition.UNKNOWN_CONTACT in SpecialCallCondition.activeEntries)
        assertFalse(CallBlockRuleMatcher.isValid(CallBlockRuleType.SPECIAL, raw))
    }

    @Test
    fun contactCodecRoundTripsPortableSnapshotAndMatchesEverySelectedNumber() {
        val payload = ContactRuleCodec.encode(
            listOf(
                CallBlockContactSelection("An | Sales", listOf("+84 912 345 678", "0912 345 678")),
                CallBlockContactSelection("Binh: Home", listOf("0987 654 321")),
            )
        )
        val rule = rule(CallBlockRuleType.CONTACTS, payload)

        assertEquals(2, ContactRuleCodec.selectedCount(payload))
        assertEquals(setOf("912345678", "987654321"), ContactRuleCodec.matchKeys(payload))
        assertEquals("912345678,987654321", rule.matchValue)
        assertTrue(CallBlockRuleMatcher.matches(rule, "0912345678"))
        assertTrue(CallBlockRuleMatcher.matches(rule, "+84 987 654 321"))
        assertFalse(CallBlockRuleMatcher.matches(rule, "0900000000"))
        assertEquals("An | Sales", ContactRuleCodec.decode(payload).first().displayName)
    }

    @Test
    fun callHistoryCodecCanonicalizesPickerOrderAndDeduplicatesByPhoneKey() {
        val selections = listOf(
            CallBlockCallHistorySelection("Binh | Work", "0987 654 321"),
            CallBlockCallHistorySelection("", "+84 912 345 678"),
            CallBlockCallHistorySelection("An: Mobile", "0912 345 678"),
            CallBlockCallHistorySelection("Zed", "0084 912 345 678"),
        )

        val payload = CallHistoryRuleCodec.encode(selections)
        val reversedPayload = CallHistoryRuleCodec.encode(selections.reversed())
        val decoded = CallHistoryRuleCodec.decode(payload)
        val rule = rule(CallBlockRuleType.CALL_HISTORY, payload)

        assertEquals(payload, reversedPayload)
        assertEquals(2, CallHistoryRuleCodec.selectedCount(payload))
        assertEquals(setOf("912345678", "987654321"), CallHistoryRuleCodec.matchKeys(payload))
        assertEquals(listOf("912345678", "987654321"), decoded.map { PhoneKey.of(it.rawNumber) })
        assertEquals("An: Mobile", decoded.first().displayName)
        assertEquals("912345678,987654321", rule.matchValue)
        assertEquals(payload, CallBlockRuleMatcher.canonicalRawValue(CallBlockRuleType.CALL_HISTORY, payload))
        assertTrue(CallBlockRuleMatcher.isValid(CallBlockRuleType.CALL_HISTORY, payload))
        assertTrue(CallBlockRuleMatcher.matches(rule, "+84 912 345 678"))
        assertTrue(CallBlockRuleMatcher.matches(rule, "0987654321"))
        assertFalse(CallBlockRuleMatcher.matches(rule, "0900000000"))
    }

    @Test
    fun malformedOrHiddenCallHistorySnapshotsFailValidationAndContextsFailOpen() {
        val privatePayload = CallHistoryRuleCodec.encode(
            listOf(
                CallBlockCallHistorySelection("Private", "-1"),
                CallBlockCallHistorySelection("Unknown", "-2"),
            )
        )
        val validPayload = CallHistoryRuleCodec.encode(
            listOf(CallBlockCallHistorySelection("An", "0912345678"))
        )
        val rule = rule(CallBlockRuleType.CALL_HISTORY, validPayload)

        assertEquals("v1", privatePayload)
        assertFalse(CallBlockRuleMatcher.isValid(CallBlockRuleType.CALL_HISTORY, privatePayload))
        assertFalse(CallBlockRuleMatcher.isValid(CallBlockRuleType.CALL_HISTORY, "v2|broken"))
        assertFalse(CallBlockRuleMatcher.isValid(CallBlockRuleType.CALL_HISTORY, "v1|broken"))
        assertFalse(CallBlockRuleMatcher.isValid(CallBlockRuleType.CALL_HISTORY, "$validPayload|broken"))
        assertFalse(
            CallBlockRuleMatcher.matches(
                rule,
                CallScreeningContext(number = "0912345678", isPrivateNumber = true),
            )
        )
        assertFalse(
            CallBlockRuleMatcher.matches(
                rule,
                CallScreeningContext(number = "0912345678", isVoip = true),
            )
        )
    }

    @Test
    fun callHistoryRejectsSipAlphanumericAndMixedBackupPayloads() {
        val sip = "sip:alice0912345678@example.com"
        val alphanumeric = "HOTLINE0912345678"
        val unsupportedDialChar = "0912#345678"
        val validNumber = "+84\u2003912\u00A0345 678"

        assertFalse(CallHistoryRuleCodec.isSelectableNumber(sip))
        assertFalse(CallHistoryRuleCodec.isSelectableNumber(alphanumeric))
        assertFalse(CallHistoryRuleCodec.isSelectableNumber(unsupportedDialChar))
        assertTrue(CallHistoryRuleCodec.isSelectableNumber(validNumber))
        assertTrue(CallHistoryRuleCodec.isSelectableNumber("+84 (912).345-678"))

        val rejected = CallHistoryRuleCodec.encode(
            listOf(
                CallBlockCallHistorySelection("SIP", sip),
                CallBlockCallHistorySelection("Alpha", alphanumeric),
            )
        )
        assertEquals("v1", rejected)
        assertFalse(CallBlockRuleMatcher.isValid(CallBlockRuleType.CALL_HISTORY, rejected))

        val validPayload = CallHistoryRuleCodec.encode(
            listOf(CallBlockCallHistorySelection("An", "0912345678"))
        )
        fun payloadPart(value: String): String =
            Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))
        val mixedPayload = "$validPayload|${payloadPart("SIP")}:${payloadPart(sip)}"

        assertFalse(CallBlockRuleMatcher.isValid(CallBlockRuleType.CALL_HISTORY, mixedPayload))
    }

    @Test
    fun callHistorySnapshotRemainsDecodableWhenTheSourceCallLogRowIsAbsent() {
        val selection = CallBlockCallHistorySelection(
            displayName = "Saved snapshot only",
            rawNumber = "+84 912 345 678",
        )
        val payload = CallHistoryRuleCodec.encode(listOf(selection))

        // Decode is intentionally self-contained and never consults the current CallLog provider.
        assertEquals(listOf(selection), CallHistoryRuleCodec.decode(payload))
        assertEquals(setOf("912345678"), CallHistoryRuleCodec.matchKeys(payload))
        assertTrue(CallBlockRuleMatcher.isValid(CallBlockRuleType.CALL_HISTORY, payload))
    }

    @Test
    fun malformedSpecialAndContactPayloadsAreRejected() {
        assertFalse(CallBlockRuleMatcher.isValid(CallBlockRuleType.SPECIAL, "private,future"))
        assertFalse(CallBlockRuleMatcher.isValid(CallBlockRuleType.CONTACTS, "v2|broken"))
    }

    @Test
    fun privateAndVoipContextsCannotFallThroughToNumberRules() {
        val exact = rule(CallBlockRuleType.EXACT_NUMBER, "0912345678")
        val prefix = rule(CallBlockRuleType.PREFIX, "091", id = 2L)

        assertFalse(
            CallBlockRuleMatcher.matches(
                exact,
                CallScreeningContext(number = "0912345678", isPrivateNumber = true),
            )
        )
        assertFalse(
            CallBlockRuleMatcher.matches(
                prefix,
                CallScreeningContext(number = "0912345678", isVoip = true),
            )
        )
    }

    @Test
    fun anyAndLengthMatchersRespectContactScope() {
        val unknownAny = rule(CallBlockRuleType.ANY, "").copy(scope = CallBlockScope.NOT_SAVED)
        val savedLength = rule(CallBlockRuleType.LENGTH, "9", id = 2L)
            .copy(scope = CallBlockScope.SAVED_CONTACT)

        assertTrue(
            CallBlockRuleMatcher.matches(
                unknownAny,
                CallScreeningContext("0912345678", contactStatus = ContactLookupStatus.NOT_IN_CONTACTS),
            )
        )
        assertFalse(
            CallBlockRuleMatcher.matches(
                unknownAny,
                CallScreeningContext("0912345678", contactStatus = ContactLookupStatus.UNKNOWN),
            )
        )
        assertTrue(
            CallBlockRuleMatcher.matches(
                savedLength,
                CallScreeningContext("0912345678", contactStatus = ContactLookupStatus.IN_CONTACTS),
            )
        )
        assertFalse(
            CallBlockRuleMatcher.matches(
                savedLength,
                CallScreeningContext("0912345678", contactStatus = ContactLookupStatus.NOT_IN_CONTACTS),
            )
        )
    }

    @Test
    fun lengthValidationIsBoundedAndCanonical() {
        assertTrue(CallBlockRuleMatcher.isValid(CallBlockRuleType.LENGTH, " 9 "))
        assertEquals("9", CallBlockRuleMatcher.normalizedValue(CallBlockRuleType.LENGTH, "09"))
        assertFalse(CallBlockRuleMatcher.isValid(CallBlockRuleType.LENGTH, "0"))
        assertFalse(CallBlockRuleMatcher.isValid(CallBlockRuleType.LENGTH, "33"))
        assertFalse(CallBlockRuleMatcher.isValid(CallBlockRuleType.LENGTH, "nine"))
    }

    @Test
    fun hiddenSipTextAndBrandNameDisableContactScopeWhileSipPhoneKeepsIt() {
        val privateRaw = SpecialCallCondition.encode(setOf(SpecialCallCondition.PRIVATE_NUMBER))
        val sipPhoneRaw = SpecialCallCondition.encode(setOf(SpecialCallCondition.SIP_PHONE_NUMBER))
        val sipTextRaw = SpecialCallCondition.encode(setOf(SpecialCallCondition.SIP_TEXT_ID))

        assertTrue(CallBlockRuleType.SPECIAL.supportsScope(CallBlockScope.ALL_VISIBLE_NUMBERS, privateRaw))
        assertFalse(CallBlockRuleType.SPECIAL.supportsScope(CallBlockScope.NOT_SAVED, privateRaw))
        assertFalse(CallBlockRuleType.SPECIAL.supportsScope(CallBlockScope.SAVED_CONTACT, privateRaw))
        assertTrue(CallBlockRuleType.SPECIAL.supportsScope(CallBlockScope.NOT_SAVED, sipPhoneRaw))
        assertTrue(CallBlockRuleType.SPECIAL.supportsScope(CallBlockScope.SAVED_CONTACT, sipPhoneRaw))
        assertFalse(CallBlockRuleType.SPECIAL.supportsScope(CallBlockScope.NOT_SAVED, sipTextRaw))
        assertFalse(CallBlockRuleType.SPECIAL.supportsScope(CallBlockScope.SAVED_CONTACT, sipTextRaw))
        assertTrue(CallBlockRuleType.BRAND_NAME.supportsScope(CallBlockScope.ALL_VISIBLE_NUMBERS))
        assertFalse(CallBlockRuleType.BRAND_NAME.supportsScope(CallBlockScope.NOT_SAVED))
        assertFalse(CallBlockRuleType.BRAND_NAME.supportsScope(CallBlockScope.SAVED_CONTACT))
    }

    private fun rule(type: CallBlockRuleType, raw: String, id: Long = 1L): CallBlockRule =
        CallBlockRule(
            id = id,
            type = type,
            rawValue = raw,
            matchValue = CallBlockRuleMatcher.normalizedValue(type, raw),
            enabled = true,
            createdAt = id,
        )

    private fun geographicRule(vararg options: GeographicBlockOption): CallBlockRule =
        rule(CallBlockRuleType.GEOGRAPHIC, GeographicBlockOption.encode(options.toSet()))
}

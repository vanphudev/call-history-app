package com.antimobile.callhs

import com.antimobile.callhs.data.blocking.CallBlockRule
import com.antimobile.callhs.data.blocking.CallBlockAction
import com.antimobile.callhs.data.blocking.CallBlockScope
import com.antimobile.callhs.data.blocking.CallBlockRuleMatcher
import com.antimobile.callhs.data.blocking.CallBlockRuleSnapshotCodec
import com.antimobile.callhs.data.blocking.CallBlockRuleSnapshotStore
import com.antimobile.callhs.data.blocking.CallBlockRuleType
import com.antimobile.callhs.data.blocking.SpecialCallCondition
import java.util.Base64
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CallBlockRuleSnapshotCodecTest {

    @Test
    fun roundTripKeepsOnlyEnabledRulesAndRestoresMatcherOrder() {
        val broad = rule(CallBlockRuleType.CONTAINS, "345", id = 3L, createdAt = 1L, userOrder = 2)
        val exact = rule(CallBlockRuleType.EXACT_NUMBER, "0912 345 678", id = 2L, createdAt = 9L, userOrder = 1)
        val disabled = rule(CallBlockRuleType.PREFIX, "098", id = 1L, enabled = false)

        val decoded = CallBlockRuleSnapshotCodec.decode(
            CallBlockRuleSnapshotCodec.encode(listOf(broad, disabled, exact))
        )

        assertEquals(listOf(exact, broad), decoded)
    }

    @Test
    fun validEmptySnapshotIsDifferentFromCorruption() {
        assertEquals(emptyList<CallBlockRule>(), CallBlockRuleSnapshotCodec.decode("v3"))
        assertNull(CallBlockRuleSnapshotCodec.decode(""))
        assertNull(CallBlockRuleSnapshotCodec.decode("v1"))
        assertNull(CallBlockRuleSnapshotCodec.decode("v2"))
        assertNull(CallBlockRuleSnapshotCodec.decode("v3|partial"))
        assertNull(CallBlockRuleSnapshotCodec.decode("v3|1:exact:1:block:all_visible:0:not_base64:not_base64"))
    }

    @Test
    fun rejectsTamperedMatchValueAndDuplicateRows() {
        val raw = base64("0912345678")
        val wrongMatch = base64("987654321")
        val validMatch = base64("912345678")

        assertNull(CallBlockRuleSnapshotCodec.decode("v3|1:exact:1:block:all_visible:0:$raw:$wrongMatch"))
        assertNull(
            CallBlockRuleSnapshotCodec.decode(
                "v3|1:exact:1:block:all_visible:0:$raw:$validMatch|1:exact:2:block:all_visible:1:$raw:$validMatch"
            )
        )
    }

    @Test
    fun canonicalPayloadStillMatchesDomesticAndInternationalForms() {
        val exact = rule(CallBlockRuleType.EXACT_NUMBER, "0912 345 678", id = 7L)
        val decoded = CallBlockRuleSnapshotCodec.decode(CallBlockRuleSnapshotCodec.encode(listOf(exact)))!!

        assertEquals(1, decoded.size)
        assertEquals("912345678", decoded.single().matchValue)
        assertEquals(true, CallBlockRuleMatcher.matches(decoded.single(), "+84 912 345 678"))
    }

    @Test
    fun versionTwoPreservesAllowScopeAndUserOrder() {
        val savedAllow = rule(
            type = CallBlockRuleType.ANY,
            raw = "any",
            id = 8L,
            userOrder = 4,
            action = CallBlockAction.ALLOW,
            scope = CallBlockScope.SAVED_CONTACT,
        )

        val decoded = requireNotNull(
            CallBlockRuleSnapshotCodec.decode(CallBlockRuleSnapshotCodec.encode(listOf(savedAllow)))
        ).single()

        assertEquals(CallBlockAction.ALLOW, decoded.action)
        assertEquals(CallBlockScope.SAVED_CONTACT, decoded.scope)
        assertEquals(4, decoded.userOrder)
    }

    @Test
    fun spamRiskSnapshotRoundTripsBlockButRejectsAllow() {
        val block = rule(
            CallBlockRuleType.SPAM_RISK,
            CallBlockRuleMatcher.SPAM_RISK_PROFILE,
            id = 21L,
        )
        assertEquals(
            listOf(block),
            CallBlockRuleSnapshotCodec.decode(CallBlockRuleSnapshotCodec.encode(listOf(block))),
        )

        val invalidAllow = block.copy(action = CallBlockAction.ALLOW)
        assertNull(CallBlockRuleSnapshotCodec.decode(CallBlockRuleSnapshotCodec.encode(listOf(invalidAllow))))
    }

    @Test
    fun versionTwoKeepsSameMatcherInDifferentScopes() {
        val saved = rule(
            CallBlockRuleType.PREFIX,
            "028",
            id = 10L,
            scope = CallBlockScope.SAVED_CONTACT,
        )
        val unknown = rule(
            CallBlockRuleType.PREFIX,
            "028",
            id = 11L,
            scope = CallBlockScope.NOT_SAVED,
            userOrder = 1,
        )

        val decoded = CallBlockRuleSnapshotCodec.decode(
            CallBlockRuleSnapshotCodec.encode(listOf(saved, unknown))
        )
        assertEquals(listOf(saved, unknown), decoded)
    }

    @Test
    fun sipTextRejectsContactScopeWhileSipPhoneKeepsIt() {
        val textRaw = SpecialCallCondition.encode(setOf(SpecialCallCondition.SIP_TEXT_ID))
        val phoneRaw = SpecialCallCondition.encode(setOf(SpecialCallCondition.SIP_PHONE_NUMBER))
        val invalidText = rule(
            CallBlockRuleType.SPECIAL,
            textRaw,
            id = 31L,
            scope = CallBlockScope.NOT_SAVED,
        )
        val validPhone = rule(
            CallBlockRuleType.SPECIAL,
            phoneRaw,
            id = 32L,
            scope = CallBlockScope.SAVED_CONTACT,
        )

        assertNull(CallBlockRuleSnapshotCodec.decode(CallBlockRuleSnapshotCodec.encode(listOf(invalidText))))
        assertEquals(
            listOf(validPhone),
            CallBlockRuleSnapshotCodec.decode(CallBlockRuleSnapshotCodec.encode(listOf(validPhone))),
        )
    }

    @Test
    fun ruleMutationCoordinatorSerializesTheWholeCriticalSection() = runBlocking {
        val events = mutableListOf<String>()
        val firstEntered = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val secondAttempting = CompletableDeferred<Unit>()

        val first = launch {
            CallBlockRuleSnapshotStore.withRuleMutation {
                events += "first-start"
                firstEntered.complete(Unit)
                releaseFirst.await()
                events += "first-end"
            }
        }
        firstEntered.await()
        val second = launch {
            secondAttempting.complete(Unit)
            CallBlockRuleSnapshotStore.withRuleMutation { events += "second" }
        }
        secondAttempting.await()
        yield()
        assertEquals(listOf("first-start"), events)

        releaseFirst.complete(Unit)
        first.join()
        second.join()
        assertEquals(listOf("first-start", "first-end", "second"), events)
    }

    @Test
    fun consistentScreeningReadWaitsUntilTheActiveMutationFinishes() = runBlocking {
        val mutationEntered = CompletableDeferred<Unit>()
        val releaseMutation = CompletableDeferred<Unit>()
        val barrierReturned = CompletableDeferred<Unit>()

        val mutation = launch {
            CallBlockRuleSnapshotStore.withRuleMutation {
                mutationEntered.complete(Unit)
                releaseMutation.await()
            }
        }
        mutationEntered.await()
        val reader = launch {
            CallBlockRuleSnapshotStore.withConsistentRuleRead {
                barrierReturned.complete(Unit)
            }
        }
        yield()
        assertEquals(false, barrierReturned.isCompleted)

        releaseMutation.complete(Unit)
        mutation.join()
        reader.join()
        assertEquals(true, barrierReturned.isCompleted)
    }

    @Test
    fun readerDuringMutationGetsPreviousSnapshotAndReaderAfterPublishGetsReplacement() = runBlocking {
        val previous = listOf(rule(CallBlockRuleType.EXACT_NUMBER, "0912345678", id = 41L))
        val replacement = listOf(rule(CallBlockRuleType.EXACT_NUMBER, "0987654321", id = 42L))
        val dirtyEntered = CompletableDeferred<Unit>()
        val releaseMutation = CompletableDeferred<Unit>()

        CallBlockRuleSnapshotStore.installMemoryForTest(previous)
        try {
            val mutation = launch {
                CallBlockRuleSnapshotStore.withRuleMutation {
                    val generation = CallBlockRuleSnapshotStore.beginDirtyForTest()
                    dirtyEntered.complete(Unit)
                    releaseMutation.await()
                    assertEquals(
                        true,
                        CallBlockRuleSnapshotStore.publishMemoryForTest(generation, replacement),
                    )
                }
            }
            dirtyEntered.await()

            val during = CallBlockRuleSnapshotStore.inMemoryRulesOrNull()
            assertEquals(previous, during)
            assertEquals(true, CallBlockRuleMatcher.matches(during!!.single(), "+84 912 345 678"))

            releaseMutation.complete(Unit)
            mutation.join()

            val after = CallBlockRuleSnapshotStore.inMemoryRulesOrNull()
            assertEquals(replacement, after)
            assertEquals(true, CallBlockRuleMatcher.matches(after!!.single(), "+84 987 654 321"))
        } finally {
            CallBlockRuleSnapshotStore.resetMemoryForTest()
        }
    }

    private fun rule(
        type: CallBlockRuleType,
        raw: String,
        id: Long,
        createdAt: Long = id,
        enabled: Boolean = true,
        userOrder: Int = 0,
        action: CallBlockAction = CallBlockAction.BLOCK,
        scope: CallBlockScope = CallBlockScope.ALL_VISIBLE_NUMBERS,
    ) = CallBlockRule(
        id = id,
        type = type,
        rawValue = raw,
        matchValue = CallBlockRuleMatcher.normalizedValue(type, raw),
        enabled = enabled,
        createdAt = createdAt,
        userOrder = userOrder,
        action = action,
        scope = scope,
    )

    private fun base64(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(Charsets.UTF_8))
}

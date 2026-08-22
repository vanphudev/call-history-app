package com.antimobile.callhs.data.blocking

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.CancellationSignal
import android.os.OperationCanceledException
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.room.withTransaction
import com.antimobile.callhs.data.backup.BackupBlockedCall
import com.antimobile.callhs.data.backup.BackupBlockRule
import com.antimobile.callhs.data.backup.BackupNumberEntry
import com.antimobile.callhs.data.backup.MergeMode
import com.antimobile.callhs.data.backup.SectionResult
import com.antimobile.callhs.data.local.AppDatabase
import com.antimobile.callhs.util.PhoneKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger

/**
 * Cổng duy nhất tới quy tắc chặn/lịch sử chặn app-owned.
 *
 * Engine chạy hoàn toàn offline: service chỉ đọc Room + Call Log cục bộ, nên có thể phản hồi
 * [android.telecom.CallScreeningService] đủ nhanh trước thời hạn 5 giây của Android.
 */
class CallBlockRepository(context: Context) {

    private val appContext = context.applicationContext
    private val db = AppDatabase.get(appContext)
    private val dao = db.callBlockDao()

    fun observeRules(): Flow<List<CallBlockRule>> = dao.observeRules().map { rows ->
        CallBlockRuleMatcher.ordered(rows.mapNotNull { it.toModel() })
    }

    fun observeNumberEntries(
        action: CallBlockAction? = null,
    ): Flow<List<CallBlockNumberEntry>> = dao.observeNumberEntries().map { rows ->
        rows.mapNotNull { it.toModel() }
            .filter { action == null || it.action == action }
    }

    fun observeHistory(): Flow<List<BlockedCallHistory>> = dao.observeHistory().map { rows ->
        rows.mapNotNull { row ->
            CallBlockHistoryReasonCodec.display(row.ruleType, row.ruleValue)?.let { reason ->
                BlockedCallHistory(
                    id = row.id,
                    rawNumber = row.rawNumber,
                    phoneKey = row.phoneKey,
                    blockedAt = row.blockedAt,
                    ruleType = reason.ruleType,
                    ruleValue = reason.ruleValue,
                    consecutiveUnanswered = row.consecutiveUnanswered,
                    blockedCountForNumber = row.blockedCountForNumber,
                    historyReasonType = row.ruleType,
                    historyReasonValue = row.ruleValue,
                    ruleScope = CallBlockScope.fromStorage(row.ruleScope)
                        ?: CallBlockScope.ALL_VISIBLE_NUMBERS,
                )
            }
        }
    }

    suspend fun getRule(id: Long): CallBlockRule? = dao.getRule(id)?.toModel()

    suspend fun getNumberEntry(id: Long): CallBlockNumberEntry? = dao.getNumberEntry(id)?.toModel()

    suspend fun ruleCount(): Int = dao.ruleCount()

    suspend fun historyCount(): Int = dao.historyCount()

    /**
     * Adds or edits one exact list entry. A number cannot silently live in both lists: callers must
     * explicitly use [moveNumberEntry] with replacement when resolving the opposite-list conflict.
     */
    suspend fun saveNumberEntry(
        id: Long? = null,
        action: CallBlockAction,
        rawNumber: String,
        displayName: String = "",
        origin: NumberEntryOrigin = NumberEntryOrigin.MANUAL,
        enabled: Boolean = true,
    ): SaveNumberEntryResult = CallBlockRuleSnapshotStore.withRuleMutation {
        val raw = rawNumber.trim()
        val phoneKey = PhoneKey.of(raw)
        if (!CallHistoryRuleCodec.isSelectableNumber(raw) || phoneKey.length < 3) {
            return@withRuleMutation SaveNumberEntryResult.INVALID
        }
        val existing = id?.let { dao.getNumberEntry(it) }
        if (id != null && existing == null) return@withRuleMutation SaveNumberEntryResult.NOT_FOUND
        if (existing == null && dao.numberEntryCount() >= MAX_NUMBER_ENTRIES) {
            return@withRuleMutation SaveNumberEntryResult.FULL
        }
        val opposite = if (action == CallBlockAction.ALLOW) CallBlockAction.BLOCK else CallBlockAction.ALLOW
        if (dao.numberEntrySignatureExists(opposite.storageKey, phoneKey) > 0) {
            return@withRuleMutation SaveNumberEntryResult.OPPOSITE_LIST_CONFLICT
        }
        if (dao.numberEntrySignatureExists(action.storageKey, phoneKey, id ?: -1L) > 0) {
            return@withRuleMutation SaveNumberEntryResult.DUPLICATE
        }

        val generation = CallBlockRuleSnapshotStore.markDirty(appContext)
        try {
            val entity = CallBlockNumberEntryEntity(
                id = existing?.id ?: 0L,
                action = action.storageKey,
                rawNumber = raw,
                phoneKey = phoneKey,
                displayName = displayName.trim(),
                origin = origin.storageKey,
                enabled = enabled,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            )
            if (existing == null) {
                if (dao.insertNumberEntry(entity) == -1L) {
                    refreshRuleSnapshotBestEffort(generation)
                    return@withRuleMutation SaveNumberEntryResult.DUPLICATE
                }
            } else {
                dao.updateNumberEntry(entity)
            }
        } catch (error: Throwable) {
            refreshRuleSnapshotBestEffort(generation)
            throw error
        }
        refreshRuleSnapshotBestEffort(generation)
        SaveNumberEntryResult.SAVED
    }

    /**
     * Picker/manual UX uses an upsert instead of silently ignoring a number already present in the
     * other list. The target entry is written first and the opposite entry is removed in the same
     * Room transaction, so screening can never observe a half-moved number.
     */
    suspend fun upsertNumberEntry(
        action: CallBlockAction,
        rawNumber: String,
        displayName: String = "",
        origin: NumberEntryOrigin = NumberEntryOrigin.MANUAL,
    ): SaveNumberEntryResult = CallBlockRuleSnapshotStore.withRuleMutation {
        val raw = rawNumber.trim()
        val phoneKey = PhoneKey.of(raw)
        if (!CallHistoryRuleCodec.isSelectableNumber(raw) || phoneKey.length < 3) {
            return@withRuleMutation SaveNumberEntryResult.INVALID
        }
        val oppositeAction = if (action == CallBlockAction.ALLOW) {
            CallBlockAction.BLOCK
        } else {
            CallBlockAction.ALLOW
        }
        val generation = CallBlockRuleSnapshotStore.markDirty(appContext)
        try {
            db.withTransaction {
                val target = dao.getNumberEntry(action.storageKey, phoneKey)
                val opposite = dao.getNumberEntry(oppositeAction.storageKey, phoneKey)
                if (target == null && opposite == null && dao.numberEntryCount() >= MAX_NUMBER_ENTRIES) {
                    return@withTransaction SaveNumberEntryResult.FULL
                }
                if (target == null) {
                    val inserted = dao.insertNumberEntry(
                        CallBlockNumberEntryEntity(
                            action = action.storageKey,
                            rawNumber = raw,
                            phoneKey = phoneKey,
                            displayName = displayName.trim(),
                            origin = origin.storageKey,
                            enabled = true,
                            createdAt = System.currentTimeMillis(),
                        )
                    )
                    if (inserted == -1L) return@withTransaction SaveNumberEntryResult.DUPLICATE
                } else {
                    dao.updateNumberEntry(
                        target.copy(
                            rawNumber = raw,
                            displayName = displayName.trim().ifBlank { target.displayName },
                            origin = origin.storageKey,
                            enabled = true,
                        )
                    )
                }
                opposite?.let { dao.deleteNumberEntry(it.id) }
                SaveNumberEntryResult.SAVED
            }
        } finally {
            refreshRuleSnapshotBestEffort(generation)
        }
    }

    /** Moves an exact number between allow/block lists as one Room transaction. */
    suspend fun moveNumberEntry(
        id: Long,
        targetAction: CallBlockAction,
        replaceOpposite: Boolean = false,
    ): SaveNumberEntryResult = CallBlockRuleSnapshotStore.withRuleMutation {
        val source = dao.getNumberEntry(id) ?: return@withRuleMutation SaveNumberEntryResult.NOT_FOUND
        val sourceModel = source.toModel() ?: return@withRuleMutation SaveNumberEntryResult.INVALID
        if (sourceModel.action == targetAction) return@withRuleMutation SaveNumberEntryResult.SAVED
        val conflict = dao.numberEntrySignatureExists(targetAction.storageKey, source.phoneKey) > 0
        if (conflict && !replaceOpposite) {
            return@withRuleMutation SaveNumberEntryResult.OPPOSITE_LIST_CONFLICT
        }
        val generation = CallBlockRuleSnapshotStore.markDirty(appContext)
        try {
            db.withTransaction {
                if (conflict) dao.deleteNumberEntry(targetAction.storageKey, source.phoneKey)
                dao.updateNumberEntry(
                    source.copy(
                        action = targetAction.storageKey,
                        createdAt = System.currentTimeMillis(),
                    )
                )
            }
        } finally {
            refreshRuleSnapshotBestEffort(generation)
        }
        SaveNumberEntryResult.SAVED
    }

    suspend fun deleteNumberEntry(id: Long) {
        CallBlockRuleSnapshotStore.withRuleMutation {
            val generation = CallBlockRuleSnapshotStore.markDirty(appContext)
            try {
                dao.deleteNumberEntry(id)
            } finally {
                refreshRuleSnapshotBestEffort(generation)
            }
        }
    }

    suspend fun setNumberEntryEnabled(id: Long, enabled: Boolean): Boolean =
        CallBlockRuleSnapshotStore.withRuleMutation {
            val current = dao.getNumberEntry(id) ?: return@withRuleMutation false
            if (current.enabled == enabled) return@withRuleMutation true
            val generation = CallBlockRuleSnapshotStore.markDirty(appContext)
            try {
                dao.updateNumberEntry(current.copy(enabled = enabled))
            } finally {
                refreshRuleSnapshotBestEffort(generation)
            }
            true
        }

    /** Tạo/sửa quy tắc; cùng kiểu + cùng khoá so khớp chỉ được tồn tại một lần. */
    suspend fun saveRule(
        id: Long?,
        type: CallBlockRuleType,
        rawValue: String,
        enabled: Boolean,
        action: CallBlockAction = CallBlockAction.BLOCK,
        scope: CallBlockScope = CallBlockScope.ALL_VISIBLE_NUMBERS,
        userOrder: Int? = null,
    ): SaveBlockRuleResult = CallBlockRuleSnapshotStore.withRuleMutation {
        saveRuleLocked(id, type, rawValue, enabled, action, scope, userOrder)
    }

    private suspend fun saveRuleLocked(
        id: Long?,
        type: CallBlockRuleType,
        rawValue: String,
        enabled: Boolean,
        action: CallBlockAction,
        scope: CallBlockScope,
        userOrder: Int?,
    ): SaveBlockRuleResult {
        val cleaned = rawValue.trim()
        if (!type.supportsAction(action)) return SaveBlockRuleResult.INVALID
        if (!type.supportsScope(scope, cleaned)) return SaveBlockRuleResult.INVALID
        if (!CallBlockRuleMatcher.isValid(type, cleaned)) return SaveBlockRuleResult.INVALID
        val canonicalRaw = CallBlockRuleMatcher.canonicalRawValue(type, cleaned)
        val matchValue = CallBlockRuleMatcher.normalizedValue(type, canonicalRaw)
        val existing = id?.let { dao.getRule(it) }
        if (id != null && existing == null) return SaveBlockRuleResult.NOT_FOUND
        if (dao.ruleSignatureExists(type.storageKey, matchValue, id ?: -1L, action.storageKey, scope.storageKey) > 0) {
            return SaveBlockRuleResult.DUPLICATE
        }
        if (existing == null && dao.ruleCount() >= MAX_RULES) return SaveBlockRuleResult.FULL

        val resolvedOrder = userOrder?.coerceAtLeast(0)
            ?: existing?.userOrder
            ?: if (type.isAdvancedConditionalType()) {
                dao.getRules().mapNotNull { it.toModel() }
                    .filter { it.type.isAdvancedConditionalType() }
                    .maxOfOrNull { it.userOrder }
                    ?.plus(1)
                    ?: 0
            } else 0
        val entity = CallBlockRuleEntity(
            id = existing?.id ?: 0L,
            type = type.storageKey,
            rawValue = canonicalRaw,
            matchValue = matchValue,
            enabled = enabled,
            createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            action = action.storageKey,
            scope = scope.storageKey,
            userOrder = resolvedOrder,
        )
        val snapshotGeneration = CallBlockRuleSnapshotStore.markDirty(appContext)
        try {
            if (existing == null) {
                if (dao.insertRule(entity) == -1L) {
                    refreshRuleSnapshotBestEffort(snapshotGeneration)
                    return SaveBlockRuleResult.DUPLICATE
                }
            } else {
                dao.updateRule(entity)
            }
        } catch (error: Throwable) {
            refreshRuleSnapshotBestEffort(snapshotGeneration)
            throw error
        }
        refreshRuleSnapshotBestEffort(snapshotGeneration)
        return SaveBlockRuleResult.SAVED
    }

    suspend fun deleteRule(id: Long) {
        CallBlockRuleSnapshotStore.withRuleMutation {
            val snapshotGeneration = CallBlockRuleSnapshotStore.markDirty(appContext)
            try {
                dao.deleteRule(id)
            } finally {
                refreshRuleSnapshotBestEffort(snapshotGeneration)
            }
        }
    }

    suspend fun setRuleEnabled(id: Long, enabled: Boolean): Boolean =
        CallBlockRuleSnapshotStore.withRuleMutation {
            val current = dao.getRule(id) ?: return@withRuleMutation false
            if (current.enabled == enabled) return@withRuleMutation true
            val generation = CallBlockRuleSnapshotStore.markDirty(appContext)
            try {
                dao.updateRule(current.copy(enabled = enabled))
            } finally {
                refreshRuleSnapshotBestEffort(generation)
            }
            true
        }

    /** Enables or disables every advanced conditional rule without touching fixed list/group policies. */
    suspend fun setAllAdvancedRulesEnabled(enabled: Boolean) {
        CallBlockRuleSnapshotStore.withRuleMutation {
            val advancedRows = dao.getRules().filter { row ->
                CallBlockRuleType.fromStorage(row.type)?.isAdvancedConditionalType() == true
            }
            if (advancedRows.none { it.enabled != enabled }) return@withRuleMutation
            val generation = CallBlockRuleSnapshotStore.markDirty(appContext)
            try {
                db.withTransaction {
                    advancedRows.forEach { row ->
                        if (row.enabled != enabled) dao.updateRule(row.copy(enabled = enabled))
                    }
                }
            } finally {
                refreshRuleSnapshotBestEffort(generation)
            }
        }
    }

    /** Permanently deletes only advanced conditional rules; exact lists and group policies are retained. */
    suspend fun deleteAllAdvancedRules() {
        CallBlockRuleSnapshotStore.withRuleMutation {
            val advancedIds = dao.getRules().mapNotNull { row ->
                row.id.takeIf {
                    CallBlockRuleType.fromStorage(row.type)?.isAdvancedConditionalType() == true
                }
            }
            if (advancedIds.isEmpty()) return@withRuleMutation
            val generation = CallBlockRuleSnapshotStore.markDirty(appContext)
            try {
                db.withTransaction {
                    advancedIds.forEach { dao.deleteRule(it) }
                }
            } finally {
                refreshRuleSnapshotBestEffort(generation)
            }
        }
    }

    /** Reorders only the advanced tier; exact lists and group policies keep their fixed priority. */
    suspend fun moveAdvancedRule(id: Long, offset: Int): Boolean =
        CallBlockRuleSnapshotStore.withRuleMutation {
            if (offset == 0) return@withRuleMutation true
            val generation = CallBlockRuleSnapshotStore.markDirty(appContext)
            try {
                db.withTransaction {
                    val ordered = CallBlockRuleMatcher.ordered(
                        dao.getRules().mapNotNull { it.toModel() }
                            .filter { it.type.isAdvancedConditionalType() }
                    )
                    val from = ordered.indexOfFirst { it.id == id }
                    if (from < 0) return@withTransaction false
                    val to = (from + offset).coerceIn(0, ordered.lastIndex)
                    if (to == from) return@withTransaction true
                    val reordered = ordered.toMutableList().apply {
                        add(to, removeAt(from))
                    }
                    reordered.forEachIndexed { index, rule ->
                        val row = dao.getRule(rule.id) ?: return@withTransaction false
                        if (row.userOrder != index) dao.updateRule(row.copy(userOrder = index))
                    }
                    true
                }
            } finally {
                refreshRuleSnapshotBestEffort(generation)
            }
        }

    suspend fun deleteHistory(id: Long) = dao.deleteHistory(id)

    suspend fun savedContactGroupPolicy(): SavedContactGroupPolicy {
        val rule = CallBlockRuleMatcher.ordered(dao.getEnabledRules().mapNotNull { it.toModel() })
            .firstOrNull { it.type == CallBlockRuleType.ANY && it.scope == CallBlockScope.SAVED_CONTACT }
            ?: return SavedContactGroupPolicy.FOLLOW_ADVANCED
        return if (rule.action == CallBlockAction.ALLOW) {
            SavedContactGroupPolicy.ALLOW
        } else {
            SavedContactGroupPolicy.BLOCK
        }
    }

    suspend fun setSavedContactGroupPolicy(policy: SavedContactGroupPolicy): Boolean =
        groupPolicyMutex.withLock {
            val action = when (policy) {
                SavedContactGroupPolicy.FOLLOW_ADVANCED -> null
                SavedContactGroupPolicy.ALLOW -> CallBlockAction.ALLOW
                SavedContactGroupPolicy.BLOCK -> CallBlockAction.BLOCK
            }
            replaceAnyGroupRule(CallBlockScope.SAVED_CONTACT, action)
        }

    suspend fun unknownNumberPolicy(): UnknownNumberPolicy {
        val groupRule = CallBlockRuleMatcher.ordered(dao.getEnabledRules().mapNotNull { it.toModel() })
            .firstOrNull { it.type == CallBlockRuleType.ANY && it.scope == CallBlockScope.NOT_SAVED }
        if (groupRule?.action == CallBlockAction.BLOCK) return UnknownNumberPolicy.BLOCK_ALWAYS
        return if (CallBlockSettings.repeatUnknownCallerGuardConfig(appContext).enabled) {
            UnknownNumberPolicy.BLOCK_UNTIL_REPEAT
        } else {
            UnknownNumberPolicy.PASS
        }
    }

    /**
     * Cross-store transition deliberately disables the old restriction before enabling the new
     * one. Room and SharedPreferences cannot share a transaction; a process death can therefore
     * temporarily yield PASS, but can never leave two overlapping blockers or over-block callers.
     */
    suspend fun setUnknownNumberPolicy(
        policy: UnknownNumberPolicy,
        threshold: Int = CallBlockSettings.repeatUnknownCallerGuardConfig(appContext).threshold,
        windowMinutes: Int = CallBlockSettings.repeatUnknownCallerGuardConfig(appContext).windowMinutes,
    ): Boolean = groupPolicyMutex.withLock {
        when (policy) {
            UnknownNumberPolicy.PASS -> {
                CallBlockSettings.setRepeatUnknownCallerGuardEnabled(appContext, false)
                replaceAnyGroupRule(CallBlockScope.NOT_SAVED, null)
            }
            UnknownNumberPolicy.BLOCK_ALWAYS -> {
                CallBlockSettings.setRepeatUnknownCallerGuardEnabled(appContext, false)
                replaceAnyGroupRule(CallBlockScope.NOT_SAVED, CallBlockAction.BLOCK)
            }
            UnknownNumberPolicy.BLOCK_UNTIL_REPEAT -> {
                if (!CallBlockSettings.isValidRepeatUnknownCallerGuardThreshold(threshold) ||
                    !CallBlockSettings.isValidRepeatUnknownCallerGuardWindowMinutes(windowMinutes)
                ) return@withLock false
                if (!replaceAnyGroupRule(CallBlockScope.NOT_SAVED, null)) return@withLock false
                if (!CallBlockSettings.setRepeatUnknownCallerGuardThreshold(appContext, threshold)) {
                    return@withLock false
                }
                if (!CallBlockSettings.setRepeatUnknownCallerGuardWindowMinutes(appContext, windowMinutes)) {
                    return@withLock false
                }
                CallBlockSettings.setRepeatUnknownCallerGuardEnabled(appContext, true)
                CallBlockSettings.repeatUnknownCallerGuardConfig(appContext).enabled
            }
        }
    }

    private suspend fun replaceAnyGroupRule(
        scope: CallBlockScope,
        action: CallBlockAction?,
    ): Boolean = CallBlockRuleSnapshotStore.withRuleMutation {
        val generation = CallBlockRuleSnapshotStore.markDirty(appContext)
        try {
            db.withTransaction {
                val groupRows = dao.getRules().filter { row ->
                    row.type == CallBlockRuleType.ANY.storageKey && row.scope == scope.storageKey
                }
                groupRows.forEach { dao.deleteRule(it.id) }
                if (action != null) {
                    if (dao.ruleCount() >= MAX_RULES) return@withTransaction false
                    val raw = CallBlockRuleMatcher.canonicalRawValue(CallBlockRuleType.ANY, "")
                    val inserted = dao.insertRule(
                        CallBlockRuleEntity(
                            type = CallBlockRuleType.ANY.storageKey,
                            rawValue = raw,
                            matchValue = CallBlockRuleMatcher.normalizedValue(CallBlockRuleType.ANY, raw),
                            enabled = true,
                            createdAt = System.currentTimeMillis(),
                            action = action.storageKey,
                            scope = scope.storageKey,
                            userOrder = 0,
                        )
                    )
                    if (inserted == -1L) return@withTransaction false
                }
                true
            }
        } finally {
            refreshRuleSnapshotBestEffort(generation)
        }
    }

    /** Evaluates one incoming call without consulting the system Call Log. */
    suspend fun findMatch(
        number: String,
        isPrivateNumber: Boolean = false,
        isVoip: Boolean = false,
        callerDisplayName: String? = null,
        sipCallerIdentity: SipCallerIdentity = SipCallerIdentity.UNKNOWN,
        callCreatedAt: Long = 0L,
        callerNumberVerificationStatus: CallerNumberVerificationStatus =
            CallerNumberVerificationStatus.UNKNOWN,
    ): CallBlockMatch? {
        if (!isScreeningRuntimeActive()) return null
        val telephoneNumber = when {
            isPrivateNumber -> ""
            sipCallerIdentity.kind == SipCallerIdKind.PHONE_NUMBER -> sipCallerIdentity.phoneNumber.orEmpty()
            isVoip -> ""
            else -> number
        }
        val canUseTelephoneRules = telephoneNumber.isNotEmpty()
        val phoneKey = PhoneKey.of(telephoneNumber)
        val screeningData = blockingDataForScreening(phoneKey)

        // Tier 1/2: exact sources have already become durable entries. Provenance never affects the
        // decision. ALLOW deliberately wins even if corrupt/legacy storage contains both actions.
        screeningData.numberEntries.firstOrNull { it.action == CallBlockAction.ALLOW }?.let { entry ->
            return entry.asExactMatch(
                tier = CallBlockDecisionTier.EXACT_ALLOWLIST,
                snapshotGeneration = screeningData.snapshotGeneration,
            )
        }
        screeningData.numberEntries.firstOrNull { it.action == CallBlockAction.BLOCK }?.let { entry ->
            return entry.asExactMatch(
                tier = CallBlockDecisionTier.EXACT_BLOCKLIST,
                snapshotGeneration = screeningData.snapshotGeneration,
            )
        }

        // Contact and Call Log providers are lazy. Lookup happens only after exact tiers and only
        // when the first otherwise-matching scoped rule needs membership. UNKNOWN never means
        // NOT_SAVED, preventing missing permission/provider timeout from blocking a real contact.
        var contactStatus: ContactLookupStatus? = null
        suspend fun resolvedContactStatus(): ContactLookupStatus = contactStatus
            ?: lookupContactStatus(telephoneNumber).also { contactStatus = it }

        suspend fun firstMatch(
            candidates: List<CallBlockRule>,
            tier: CallBlockDecisionTier,
        ): CallBlockMatch? {
            for (rule in candidates) {
                var matchedHistoryReasonValue: String? = null
                val patternMatches = when (rule.type) {
                    CallBlockRuleType.SPECIAL,
                    CallBlockRuleType.BRAND_NAME,
                    -> {
                        CallBlockRuleMatcher.matches(
                            // Pattern matching and Contacts scope are evaluated separately. Only a
                            // SIP-phone SPECIAL rule can use Contacts scope; Brandname is name-only.
                            rule.copy(scope = CallBlockScope.ALL_VISIBLE_NUMBERS),
                            CallScreeningContext(
                                number = number,
                                contactStatus = ContactLookupStatus.UNKNOWN,
                                isPrivateNumber = isPrivateNumber,
                                isVoip = isVoip,
                                callerDisplayName = callerDisplayName,
                                sipCallerIdentity = sipCallerIdentity,
                                callerNumberVerificationStatus = callerNumberVerificationStatus,
                            ),
                        )
                    }
                    CallBlockRuleType.SPAM_RISK -> CallBlockRuleMatcher.spamRiskReason(
                        CallScreeningContext(
                            number = telephoneNumber,
                            isPrivateNumber = isPrivateNumber,
                            isVoip = isVoip,
                            callerNumberVerificationStatus = callerNumberVerificationStatus,
                        ),
                    )?.let { reason ->
                        matchedHistoryReasonValue = SpamRiskReasonCodec.encode(reason)
                        true
                    } ?: false
                    // Legacy picker/source rule types are intentionally not runtime rule kinds in v4.
                    CallBlockRuleType.EXACT_NUMBER,
                    CallBlockRuleType.CONTACTS,
                    CallBlockRuleType.CALL_HISTORY,
                    -> false
                    else -> canUseTelephoneRules &&
                        CallBlockRuleMatcher.matches(rule, telephoneNumber)
                }
                if (!patternMatches) continue

                val scopeMatches = when {
                    rule.scope == CallBlockScope.ALL_VISIBLE_NUMBERS -> true
                    else -> rule.scope.matches(resolvedContactStatus())
                }
                if (!scopeMatches) continue
                return CallBlockMatch(
                    rule = rule,
                    historyReasonValue = matchedHistoryReasonValue ?: rule.rawValue,
                    action = rule.action,
                    decisionTier = tier,
                    ruleSnapshotGeneration = screeningData.snapshotGeneration,
                )
            }
            return null
        }

        val orderedRules = CallBlockRuleMatcher.ordered(screeningData.rules)
        val groupRules = orderedRules.filter { it.type == CallBlockRuleType.ANY }
        firstMatch(groupRules, CallBlockDecisionTier.GROUP_RULE)?.let { return it }

        val advancedRules = orderedRules.filterNot {
            it.type == CallBlockRuleType.ANY ||
                it.type == CallBlockRuleType.EXACT_NUMBER ||
                it.type == CallBlockRuleType.CONTACTS ||
                it.type == CallBlockRuleType.CALL_HISTORY
        }
        firstMatch(advancedRules, CallBlockDecisionTier.CONDITIONAL_RULE)?.let { return it }

        // Final tier: no exact/group/advanced rule matched. Disabled guard is PASS; enabled guard is
        // BLOCK_UNTIL_REPEAT. BLOCK_ALWAYS is represented canonically by BLOCK+ANY+NOT_SAVED above.
        val guardConfig = runCatching {
            CallBlockSettings.repeatUnknownCallerGuardConfig(appContext)
        }.onFailure { error ->
            Log.e(LOG_TAG, "Unable to read repeated-unknown-caller guard settings; allowing call", error)
        }.getOrNull() ?: return null
        if (
            !RepeatUnknownCallerGuardPolicy.shouldEvaluate(
                config = guardConfig,
                isPrivateNumber = isPrivateNumber,
                isVoip = isVoip,
            )
        ) return null
        // The no-rule guard is intentionally stricter than PhoneKey digit extraction. OEM call
        // logs/callbacks may expose SIP URIs or alphanumeric caller IDs containing digits; treating
        // those embedded digits as a PSTN number could block an unrelated caller.
        if (!CallHistoryRuleCodec.isSelectableNumber(number)) return null

        val unknownContactStatus = resolvedContactStatus()
        if (unknownContactStatus != ContactLookupStatus.NOT_IN_CONTACTS) return null
        if (!isScreeningRuntimeActive()) return null

        val now = System.currentTimeMillis()
        val attempt = runCatching {
            RepeatUnknownCallerGuardTracker.recordUnknownAttempt(
                context = appContext,
                number = number,
                eventId = callCreatedAt,
                eventAtMillis = callCreatedAt,
                nowMillis = now,
                config = guardConfig,
            )
        }.onFailure { error ->
            Log.e(LOG_TAG, "Unable to record repeated unknown caller attempt; allowing call", error)
        }.getOrNull() ?: return null
        if (!attempt.recorded) return null

        val latestGuardConfig = runCatching {
            CallBlockSettings.repeatUnknownCallerGuardConfig(appContext)
        }.onFailure { error ->
            Log.e(LOG_TAG, "Unable to re-read repeated caller guard settings; allowing call", error)
        }.getOrNull() ?: return null
        if (latestGuardConfig != guardConfig || !isScreeningRuntimeActive()) {
            Log.i(LOG_TAG, "Repeated caller guard changed during evaluation; allowing call")
            return null
        }
        if (attempt.reachedThreshold) return null
        return if (
            RepeatUnknownCallerGuardPolicy.shouldBlock(
                config = latestGuardConfig,
                attempt = attempt,
                contactStatus = unknownContactStatus,
                isPrivateNumber = isPrivateNumber,
                isVoip = isVoip,
            )
        ) RepeatUnknownCallerGuardPolicy.syntheticMatch(
            latestGuardConfig,
            attempt.attemptCount,
            screeningData.snapshotGeneration,
        )
        else null
    }

    private fun isScreeningRuntimeActive(): Boolean = runCatching {
        CallBlockSettings.isBlockingEnabled(appContext) &&
            CallBlockSettings.blockMethod(appContext) != CallBlockMethod.ALLOW
    }.onFailure { error ->
        Log.e(LOG_TAG, "Unable to read active screening state; allowing call", error)
    }.getOrDefault(false)

    private suspend fun lookupContactStatus(number: String): ContactLookupStatus {
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.READ_CONTACTS) !=
            PackageManager.PERMISSION_GRANTED
        ) return ContactLookupStatus.UNKNOWN

        val clean = number.trim()
        // PhoneLookup is a telephone-number provider. Treating SIP URIs/alphanumeric OEM caller
        // IDs as a lookup miss would incorrectly turn UNKNOWN into NOT_IN_CONTACTS and could make a
        // scoped SIP-phone/Brandname rule block the wrong call.
        if (!CallHistoryRuleCodec.isSelectableNumber(clean)) return ContactLookupStatus.UNKNOWN
        val cancellationSignal = CancellationSignal()
        val future = try {
            contactLookupExecutor.submit<ContactLookupStatus> {
                queryContactStatus(clean, cancellationSignal)
            }
        } catch (error: RejectedExecutionException) {
            Log.w(LOG_TAG, "Contacts lookup executor is saturated; using UNKNOWN", error)
            return ContactLookupStatus.UNKNOWN
        }
        return try {
            runInterruptible {
                future.get(CONTACT_LOOKUP_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            cancelContactLookup(cancellationSignal, future)
            throw cancelled
        } catch (error: TimeoutException) {
            cancelContactLookup(cancellationSignal, future)
            Log.w(LOG_TAG, "Contacts lookup timed out; using UNKNOWN")
            ContactLookupStatus.UNKNOWN
        } catch (error: ExecutionException) {
            Log.w(LOG_TAG, "Contacts lookup task failed; using UNKNOWN", error.cause ?: error)
            ContactLookupStatus.UNKNOWN
        } catch (error: Exception) {
            cancelContactLookup(cancellationSignal, future)
            Log.w(LOG_TAG, "Contacts lookup wait failed; using UNKNOWN", error)
            ContactLookupStatus.UNKNOWN
        }
    }

    private fun cancelContactLookup(
        cancellationSignal: CancellationSignal,
        future: Future<*>,
    ) {
        // Interrupting Future.cancel is non-blocking. CancellationSignal may call a remote provider,
        // so dispatch it separately: a broken provider must never retain the screening coroutine.
        future.cancel(true)
        try {
            contactLookupCancellationExecutor.execute {
                runCatching { cancellationSignal.cancel() }
                    .onFailure { error -> Log.w(LOG_TAG, "Contacts lookup cancellation failed", error) }
            }
        } catch (error: RejectedExecutionException) {
            Log.w(LOG_TAG, "Contacts cancellation executor is saturated", error)
        }
    }

    private fun queryContactStatus(
        cleanNumber: String,
        cancellationSignal: CancellationSignal,
    ): ContactLookupStatus = try {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(cleanNumber),
        )
        appContext.contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup._ID),
            null,
            null,
            null,
            cancellationSignal,
        )?.use { cursor ->
            if (cursor.moveToFirst()) ContactLookupStatus.IN_CONTACTS
            else ContactLookupStatus.NOT_IN_CONTACTS
        } ?: ContactLookupStatus.UNKNOWN
    } catch (_: OperationCanceledException) {
        ContactLookupStatus.UNKNOWN
    } catch (error: SecurityException) {
        Log.w(LOG_TAG, "Contacts lookup unavailable; contact-dependent policy will use UNKNOWN", error)
        ContactLookupStatus.UNKNOWN
    } catch (error: Exception) {
        Log.w(LOG_TAG, "Contacts provider lookup failed; contact-dependent policy will use UNKNOWN", error)
        ContactLookupStatus.UNKNOWN
    }

    /**
     * Ghi nhật ký SAU khi Telecom đã nhận phản hồi chặn. Lịch sử riêng này tồn tại độc lập với
     * Call Log hệ thống và giữ snapshot lý do để người dùng vẫn xem được sau khi xoá quy tắc.
     */
    suspend fun recordBlockedCall(
        number: String,
        match: CallBlockMatch,
        blockedAt: Long = System.currentTimeMillis(),
    ): BlockRecordResult? {
        val raw = number.trim()
        val phoneKey = BlockedCallerIdentity.key(raw) ?: return null
        val displayReason = CallBlockHistoryReasonCodec.display(
            match.historyReasonType,
            match.historyReasonValue,
        ) ?: return null
        return db.withTransaction {
            // Call creation time is stable when an OEM retries the same screening callback. Using
            // it in the existing unique signature suppresses duplicate history/notifications.
            val eventTime = blockedAt.takeIf { it > 0L } ?: System.currentTimeMillis()
            val inserted = dao.insertHistory(
                CallBlockHistoryEntity(
                    rawNumber = raw,
                    phoneKey = phoneKey,
                    blockedAt = eventTime,
                    ruleType = match.historyReasonType,
                    ruleValue = match.historyReasonValue,
                    // Retained in the schema only so legacy history remains portable.
                    consecutiveUnanswered = 0,
                    ruleScope = match.rule.scope.storageKey,
                )
            )
            if (inserted != -1L) dao.trimHistory(MAX_HISTORY)
            BlockRecordResult(
                historyId = if (inserted != -1L) inserted else 0L,
                rawNumber = raw,
                ruleType = displayReason.ruleType,
                ruleValue = displayReason.ruleValue,
                totalForNumber = dao.blockCountForNumber(phoneKey),
                isNew = inserted != -1L,
                historyReasonType = match.historyReasonType,
                historyReasonValue = match.historyReasonValue,
                ruleScope = match.rule.scope,
            )
        }
    }

    /**
     * Builds an immediate notification payload without waiting for Room. Android owns the posted
     * notification even if Telecom unbinds and the app process is reclaimed before history I/O.
     * The durable result later updates this same event ID silently with the exact count.
     */
    fun previewBlockedCall(
        number: String,
        match: CallBlockMatch,
        notificationEventId: Long,
    ): BlockRecordResult? {
        val raw = number.trim()
        if (BlockedCallerIdentity.key(raw) == null) return null
        val displayReason = CallBlockHistoryReasonCodec.display(
            match.historyReasonType,
            match.historyReasonValue,
        ) ?: return null
        return BlockRecordResult(
            historyId = notificationEventId,
            rawNumber = raw,
            ruleType = displayReason.ruleType,
            ruleValue = displayReason.ruleValue,
            totalForNumber = 1,
            isNew = true,
            historyReasonType = match.historyReasonType,
            historyReasonValue = match.historyReasonValue,
            ruleScope = match.rule.scope,
        )
    }

    // ---- Sao lưu / khôi phục ----

    suspend fun exportRulesForBackup(): List<BackupBlockRule> = dao.getRules().mapNotNull { row ->
        row.toModel()?.let {
            BackupBlockRule(
                type = it.type.storageKey,
                rawValue = it.rawValue,
                matchValue = it.matchValue,
                enabled = it.enabled,
                createdAt = it.createdAt,
                action = it.action.storageKey,
                scope = it.scope.storageKey,
                userOrder = it.userOrder,
            )
        }
    }

    suspend fun exportNumberEntriesForBackup(): List<BackupNumberEntry> =
        dao.getNumberEntries().mapNotNull { row ->
            row.toModel()?.let { entry ->
                BackupNumberEntry(
                    action = entry.action.storageKey,
                    rawNumber = entry.rawNumber,
                    phoneKey = entry.phoneKey,
                    displayName = entry.displayName,
                    origin = entry.origin.storageKey,
                    enabled = entry.enabled,
                    createdAt = entry.createdAt,
                )
            }
        }

    suspend fun exportHistoryForBackup(): List<BackupBlockedCall> = dao.getHistory().mapNotNull { row ->
        row.takeIf { CallBlockHistoryReasonCodec.isSupported(it.ruleType, it.ruleValue) }?.let {
            BackupBlockedCall(
                rawNumber = row.rawNumber,
                phoneKey = row.phoneKey,
                blockedAt = row.blockedAt,
                ruleType = row.ruleType,
                ruleValue = row.ruleValue,
                consecutiveUnanswered = row.consecutiveUnanswered,
                ruleScope = row.ruleScope,
            )
        }
    }

    /** Khôi phục quy tắc theo khoá (type + normalized match value). */
    suspend fun restoreRules(
        incoming: List<BackupBlockRule>,
        mode: MergeMode,
    ): SectionResult = restoreBlockingData(emptyList(), incoming, mode)

    /** Restores both exact lists and conditional rules under one snapshot lease/Room transaction. */
    suspend fun restoreBlockingData(
        numberEntries: List<BackupNumberEntry>,
        rules: List<BackupBlockRule>,
        mode: MergeMode,
    ): SectionResult = CallBlockRuleSnapshotStore.withRuleMutation {
        val generation = CallBlockRuleSnapshotStore.markDirty(appContext)
        try {
            db.withTransaction {
                val validEntries = numberEntries.mapNotNull { backup ->
                    val action = CallBlockAction.fromStorage(backup.action) ?: return@mapNotNull null
                    val origin = NumberEntryOrigin.fromStorage(backup.origin) ?: return@mapNotNull null
                    val raw = backup.rawNumber.trim()
                    val key = PhoneKey.of(raw)
                    if (!CallHistoryRuleCodec.isSelectableNumber(raw) || key.length < 3) return@mapNotNull null
                    Triple(backup, action, origin)
                }
                val validRules = rules.mapNotNull { backup ->
                    val type = CallBlockRuleType.fromStorage(backup.type) ?: return@mapNotNull null
                    val action = CallBlockAction.fromStorage(backup.action) ?: return@mapNotNull null
                    val scope = CallBlockScope.fromStorage(backup.scope) ?: return@mapNotNull null
                    if (!type.supportsAction(action)) return@mapNotNull null
                    if (!type.supportsScope(scope, backup.rawValue)) return@mapNotNull null
                    if (!CallBlockRuleMatcher.isValid(type, backup.rawValue)) return@mapNotNull null
                    Triple(backup, action, scope)
                }
                if (mode == MergeMode.REPLACE &&
                    (numberEntries.isNotEmpty() || rules.isNotEmpty()) &&
                    validEntries.isEmpty() && validRules.isEmpty()
                ) {
                    return@withTransaction SectionResult(skipped = numberEntries.size + rules.size)
                }

                if (mode == MergeMode.REPLACE) {
                    dao.deleteAllNumberEntries()
                    dao.deleteAllRules()
                }
                var added = 0
                var updated = 0
                var skipped = (numberEntries.size - validEntries.size) + (rules.size - validRules.size)
                var truncated = false

                // Canonicalize corrupted input overlap before writes. ALLOW always wins.
                val canonicalEntries = validEntries
                    .groupBy { PhoneKey.of(it.first.rawNumber) }
                    .mapNotNull { (_, duplicates) ->
                        duplicates.firstOrNull { it.second == CallBlockAction.ALLOW } ?: duplicates.firstOrNull()
                    }
                for ((backup, action, origin) in canonicalEntries) {
                    val raw = backup.rawNumber.trim()
                    val key = PhoneKey.of(raw)
                    val opposite = if (action == CallBlockAction.ALLOW) CallBlockAction.BLOCK else CallBlockAction.ALLOW
                    val current = dao.getNumberEntry(action.storageKey, key)
                    val oppositeEntry = dao.getNumberEntry(opposite.storageKey, key)
                    // Existing ALLOW is never silently demoted by an imported BLOCK. Conversely an
                    // imported ALLOW canonicalizes an existing BLOCK away in every merge mode.
                    if (action == CallBlockAction.BLOCK && oppositeEntry != null) {
                        skipped++
                        continue
                    }
                    if (current == null) {
                        if (dao.numberEntryCount() >= MAX_NUMBER_ENTRIES && oppositeEntry == null) {
                            truncated = true; skipped++; continue
                        }
                        val inserted = dao.insertNumberEntry(
                            CallBlockNumberEntryEntity(
                                action = action.storageKey,
                                rawNumber = raw,
                                phoneKey = key,
                                displayName = backup.displayName.trim(),
                                origin = origin.storageKey,
                                enabled = backup.enabled,
                                createdAt = backup.createdAt.takeIf { it > 0L } ?: System.currentTimeMillis(),
                            )
                        )
                        if (inserted != -1L) {
                            oppositeEntry?.let { dao.deleteNumberEntry(it.id) }
                            added++
                        } else skipped++
                    } else if (mode == MergeMode.UPDATE) {
                        dao.updateNumberEntry(
                            current.copy(
                                rawNumber = raw,
                                displayName = backup.displayName.trim(),
                                origin = origin.storageKey,
                                enabled = backup.enabled,
                            )
                        )
                        oppositeEntry?.let { dao.deleteNumberEntry(it.id) }
                        updated++
                    } else {
                        // Even ADD must repair an impossible dual-list row when the retained target
                        // is ALLOW; the imported payload itself remains counted as skipped.
                        if (action == CallBlockAction.ALLOW) oppositeEntry?.let { dao.deleteNumberEntry(it.id) }
                        skipped++
                    }
                }

                val existingRules = dao.getRules().associateBy {
                    listOf(it.action, it.type, it.matchValue, it.scope)
                }.toMutableMap()
                for ((backup, action, scope) in validRules) {
                    val type = CallBlockRuleType.fromStorage(backup.type) ?: continue
                    val raw = CallBlockRuleMatcher.canonicalRawValue(type, backup.rawValue)
                    val match = CallBlockRuleMatcher.normalizedValue(type, raw)
                    val key = listOf(action.storageKey, type.storageKey, match, scope.storageKey)
                    if (
                        type == CallBlockRuleType.ANY &&
                        scope in setOf(CallBlockScope.SAVED_CONTACT, CallBlockScope.NOT_SAVED)
                    ) {
                        val sameScope = existingRules.filterValues { row ->
                            row.type == CallBlockRuleType.ANY.storageKey &&
                                row.scope == scope.storageKey
                        }
                        // ADD preserves the user's current group decision. UPDATE replaces an
                        // opposite decision so the typed policy can never contain ALLOW and BLOCK
                        // for the same contact scope at once.
                        if (mode == MergeMode.ADD && sameScope.isNotEmpty()) {
                            skipped++
                            continue
                        }
                        sameScope.filterValues { it.action != action.storageKey }.forEach { (oldKey, row) ->
                            dao.deleteRule(row.id)
                            existingRules.remove(oldKey)
                        }
                    }
                    val current = existingRules[key]
                    if (current == null) {
                        if (existingRules.size >= MAX_RULES) {
                            truncated = true; skipped++; continue
                        }
                        val inserted = dao.insertRule(
                            CallBlockRuleEntity(
                                type = type.storageKey,
                                rawValue = raw,
                                matchValue = match,
                                enabled = backup.enabled,
                                createdAt = backup.createdAt.takeIf { it > 0L } ?: System.currentTimeMillis(),
                                action = action.storageKey,
                                scope = scope.storageKey,
                                userOrder = backup.userOrder.coerceAtLeast(0),
                            )
                        )
                        if (inserted != -1L) {
                            existingRules[key] = dao.getRule(inserted)!!
                            added++
                        } else skipped++
                    } else if (mode == MergeMode.UPDATE) {
                        dao.updateRule(
                            current.copy(
                                rawValue = raw,
                                enabled = backup.enabled,
                                userOrder = backup.userOrder.coerceAtLeast(0),
                            )
                        )
                        updated++
                    } else skipped++
                }
                SectionResult(added, updated, skipped, truncated)
            }
        } finally {
            refreshRuleSnapshotBestEffort(generation)
        }
    }

    private suspend fun restoreRulesLocked(
        incoming: List<BackupBlockRule>,
        mode: MergeMode,
    ): SectionResult {
        // Mark first and commit the complete replacement only after Room commits. A process death at
        // any point in between therefore causes a safe Room reload instead of screening with stale data.
        val snapshotGeneration = CallBlockRuleSnapshotStore.markDirty(appContext)
        return try {
            db.withTransaction {
                val validIncoming = incoming.mapNotNull { backup ->
                    val type = CallBlockRuleType.fromStorage(backup.type) ?: return@mapNotNull null
                    if (!CallBlockRuleMatcher.isValid(type, backup.rawValue)) return@mapNotNull null
                    backup to type
                }

                // Một file có khai báo quy tắc nhưng toàn bộ payload đều hỏng không được phép xoá sạch dữ liệu
                // hiện tại trong chế độ REPLACE. Danh sách rỗng hợp lệ vẫn có nghĩa là chủ động thay bằng 0 quy tắc.
                if (mode == MergeMode.REPLACE && incoming.isNotEmpty() && validIncoming.isEmpty()) {
                    return@withTransaction SectionResult(skipped = incoming.size)
                }

                var added = 0
                var updated = 0
                var skipped = incoming.size - validIncoming.size
                var truncated = false

                if (mode == MergeMode.REPLACE) dao.deleteAllRules()
                val existing = dao.getRules().associateBy { it.type to it.matchValue }.toMutableMap()

                for ((backup, type) in validIncoming) {
                    val canonicalRaw = CallBlockRuleMatcher.canonicalRawValue(type, backup.rawValue)
                    val match = CallBlockRuleMatcher.normalizedValue(type, canonicalRaw)
                    val key = type.storageKey to match
                    val current = existing[key]
                    if (current == null) {
                        if (existing.size >= MAX_RULES) {
                            truncated = true
                            skipped++
                            continue
                        }
                        val inserted = dao.insertRule(
                            CallBlockRuleEntity(
                                type = type.storageKey,
                                rawValue = canonicalRaw,
                                matchValue = match,
                                enabled = backup.enabled,
                                createdAt = backup.createdAt.takeIf { it > 0L } ?: System.currentTimeMillis(),
                            )
                        )
                        if (inserted != -1L) {
                            existing[key] = dao.getRule(inserted)!!
                            added++
                        } else skipped++
                    } else if (mode == MergeMode.UPDATE) {
                        dao.updateRule(
                            current.copy(
                                rawValue = canonicalRaw,
                                enabled = backup.enabled,
                            )
                        )
                        updated++
                    } else {
                        skipped++
                    }
                }
                SectionResult(added = added, updated = updated, skipped = skipped, truncated = truncated)
            }
        } finally {
            refreshRuleSnapshotBestEffort(snapshotGeneration)
        }
    }

    /**
     * Sự kiện chặn là immutable: ADD/UPDATE đều chỉ thêm sự kiện chưa có; REPLACE thay toàn bộ.
     * Không lưu ruleId vì id sau restore có thể khác; snapshot loại/giá trị vẫn đủ diễn giải lý do.
     */
    suspend fun restoreHistory(incoming: List<BackupBlockedCall>, mode: MergeMode): SectionResult = db.withTransaction {
        val validIncoming = incoming.mapNotNull { backup ->
            if (!CallBlockHistoryReasonCodec.isSupported(backup.ruleType, backup.ruleValue)) {
                return@mapNotNull null
            }
            val key = BlockedCallerIdentity.key(backup.rawNumber) ?: return@mapNotNull null
            backup to key
        }
        if (mode == MergeMode.REPLACE && incoming.isNotEmpty() && validIncoming.isEmpty()) {
            return@withTransaction SectionResult(skipped = incoming.size)
        }

        var added = 0
        var skipped = incoming.size - validIncoming.size
        var truncated = false
        if (mode == MergeMode.REPLACE) dao.deleteAllHistory()

        // Nạp bản mới trước để khi vượt giới hạn, người dùng giữ được thông tin hữu ích nhất.
        for ((backup, key) in validIncoming.sortedByDescending { it.first.blockedAt }) {
            val blockedAt = backup.blockedAt.takeIf { it > 0L } ?: System.currentTimeMillis()
            if (dao.historySignatureExists(key, blockedAt, backup.ruleType, backup.ruleValue) > 0) {
                skipped++
                continue
            }
            if (dao.historyCount() >= MAX_HISTORY) {
                truncated = true
                skipped++
                continue
            }
            val inserted = dao.insertHistory(
                CallBlockHistoryEntity(
                    rawNumber = backup.rawNumber,
                    phoneKey = key,
                    blockedAt = blockedAt,
                    ruleType = backup.ruleType,
                    ruleValue = backup.ruleValue,
                    consecutiveUnanswered = backup.consecutiveUnanswered.coerceAtLeast(0),
                    ruleScope = CallBlockScope.fromStorage(backup.ruleScope)?.storageKey
                        ?: CallBlockScope.ALL_VISIBLE_NUMBERS.storageKey,
                )
            )
            if (inserted != -1L) added++ else skipped++
        }
        SectionResult(added = added, skipped = skipped, truncated = truncated)
    }

    companion object {
        /** Đủ cho nhiều năm sử dụng thông thường, nhưng vẫn giới hạn query của service/UI. */
        const val MAX_HISTORY = 1_000
        const val MAX_RULES = 200
        const val MAX_NUMBER_ENTRIES = 2_000
        private const val LOG_TAG = "CallBlockRepository"
        private const val CONTACT_LOOKUP_TIMEOUT_MS = 450L
        /** Serializes cross-store group policy transitions across all repository instances. */
        private val groupPolicyMutex = Mutex()
        private val contactLookupThreadId = AtomicInteger()
        private val contactLookupExecutor = ThreadPoolExecutor(
            0,
            2,
            30L,
            TimeUnit.SECONDS,
            SynchronousQueue<Runnable>(),
            { task ->
                Thread(task, "CallHS-contact-lookup-${contactLookupThreadId.incrementAndGet()}").apply {
                    isDaemon = true
                }
            },
            ThreadPoolExecutor.AbortPolicy(),
        )
        private val contactLookupCancellationExecutor = ThreadPoolExecutor(
            0,
            1,
            30L,
            TimeUnit.SECONDS,
            SynchronousQueue<Runnable>(),
            { task ->
                Thread(task, "CallHS-contact-cancel").apply { isDaemon = true }
            },
            ThreadPoolExecutor.AbortPolicy(),
        )
    }

    private data class ScreeningBlockingData(
        val numberEntries: List<CallBlockNumberEntry>,
        val rules: List<CallBlockRule>,
        val snapshotGeneration: Long,
    )

    private fun CallBlockNumberEntry.asExactMatch(
        tier: CallBlockDecisionTier,
        snapshotGeneration: Long,
    ): CallBlockMatch {
        val exactRule = CallBlockRule(
            id = id,
            type = CallBlockRuleType.EXACT_NUMBER,
            rawValue = rawNumber,
            matchValue = phoneKey,
            enabled = enabled,
            createdAt = createdAt,
            action = action,
            scope = CallBlockScope.ALL_VISIBLE_NUMBERS,
        )
        return CallBlockMatch(
            rule = exactRule,
            action = action,
            decisionTier = tier,
            ruleSnapshotGeneration = snapshotGeneration,
        )
    }

    /**
     * Exact entries and the immutable rule snapshot are captured behind the same mutation barrier.
     * The indexed exact lookup is intentionally kept in Room for now; it is tiny and avoids placing
     * thousands of allow/block numbers in one SharedPreferences payload.
     */
    private suspend fun blockingDataForScreening(phoneKey: String): ScreeningBlockingData =
        CallBlockRuleSnapshotStore.withConsistentRuleRead {
            val generation = CallBlockRuleSnapshotStore.generationForConsistentRead()
            val entries = if (phoneKey.isBlank()) emptyList() else {
                dao.getEnabledNumberEntries(phoneKey).mapNotNull { it.toModel() }
            }
            val cached = CallBlockRuleSnapshotStore.rulesOrNull(appContext)
            val rules = cached ?: CallBlockRuleMatcher.ordered(
                dao.getEnabledRules().mapNotNull { it.toModel() }
            ).also { loaded ->
                CallBlockRuleSnapshotStore.publishBootstrapAsync(appContext, generation, loaded)
            }
            ScreeningBlockingData(entries, rules, generation)
        }

    /** Fast path for screening; a missing/corrupt snapshot bootstraps itself from Room once. */
    private suspend fun enabledRulesForScreening(): List<CallBlockRule> {
        // During save/delete/restore, the immutable pre-mutation snapshot stays effective until the
        // replacement is published. This keeps exact/pattern calls off the mutation/Room slow path.
        CallBlockRuleSnapshotStore.inMemoryRulesOrNull()?.let { return it }
        return CallBlockRuleSnapshotStore.withConsistentRuleRead {
            val cached = CallBlockRuleSnapshotStore.rulesOrNull(appContext)
            if (cached != null) {
                cached
            } else {
                val generation = CallBlockRuleSnapshotStore.generationForConsistentRead()
                val rules = CallBlockRuleMatcher.ordered(dao.getEnabledRules().mapNotNull { it.toModel() })
                CallBlockRuleSnapshotStore.publishBootstrapAsync(appContext, generation, rules)
                rules
            }
        }
    }

    /** Used by the service to bootstrap existing installs before the first callback where possible. */
    suspend fun warmScreeningRuleSnapshot() {
        if (CallBlockRuleSnapshotStore.rulesOrNull(appContext) == null) {
            enabledRulesForScreening()
        }
    }

    private suspend fun refreshRuleSnapshotBestEffort(expectedGeneration: Long) {
        runCatching {
            val rules = CallBlockRuleMatcher.ordered(dao.getEnabledRules().mapNotNull { it.toModel() })
            CallBlockRuleSnapshotStore.publishIfUnchanged(appContext, expectedGeneration, rules)
        }.onFailure { error ->
            // The dirty marker remains in place, so the next screening call safely falls back to Room.
            CallBlockRuleSnapshotStore.abandonMutationIfOwned(expectedGeneration)
            Log.e(LOG_TAG, "Unable to refresh screening rule snapshot", error)
        }
    }
}

private fun CallBlockRuleType.isAdvancedConditionalType(): Boolean = this !in setOf(
    CallBlockRuleType.ANY,
    CallBlockRuleType.EXACT_NUMBER,
    CallBlockRuleType.CONTACTS,
    CallBlockRuleType.CALL_HISTORY,
)

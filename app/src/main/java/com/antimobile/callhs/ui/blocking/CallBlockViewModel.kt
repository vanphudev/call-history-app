package com.antimobile.callhs.ui.blocking

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antimobile.callhs.data.blocking.BlockedCallHistory
import com.antimobile.callhs.data.blocking.CallBlockRepository
import com.antimobile.callhs.data.blocking.CallBlockRule
import com.antimobile.callhs.data.blocking.CallBlockAction
import com.antimobile.callhs.data.blocking.CallBlockNumberEntry
import com.antimobile.callhs.data.blocking.CallBlockRuleType
import com.antimobile.callhs.data.blocking.CallBlockScope
import com.antimobile.callhs.data.blocking.NumberEntryOrigin
import com.antimobile.callhs.data.blocking.CallBlockContactSelection
import com.antimobile.callhs.data.blocking.CallBlockCallHistorySelection
import com.antimobile.callhs.data.blocking.CallBlockCategorySelection
import com.antimobile.callhs.data.blocking.CallBlockSettings
import com.antimobile.callhs.data.blocking.SavedContactGroupPolicy
import com.antimobile.callhs.data.blocking.UnknownNumberPolicy
import kotlinx.coroutines.launch

/** State phản ứng cho hai tab Quy tắc chặn / Lịch sử chặn. */
class CallBlockViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = CallBlockRepository(app)

    var rules by mutableStateOf<List<CallBlockRule>>(emptyList())
        private set
    var history by mutableStateOf<List<BlockedCallHistory>>(emptyList())
        private set
    var numberEntries by mutableStateOf<List<CallBlockNumberEntry>>(emptyList())
        private set
    val allowlist: List<CallBlockNumberEntry> get() = numberEntries.filter { it.action == CallBlockAction.ALLOW }
    val blocklist: List<CallBlockNumberEntry> get() = numberEntries.filter { it.action == CallBlockAction.BLOCK }
    val advancedRules: List<CallBlockRule> get() = rules.filter { it.type !in setOf(
        CallBlockRuleType.ANY,
        CallBlockRuleType.EXACT_NUMBER,
        CallBlockRuleType.CONTACTS,
        CallBlockRuleType.CALL_HISTORY,
    ) }
    val savedGroupPolicy: SavedGroupPolicyUi get() {
        val enabled = rules.firstOrNull { it.enabled && it.type == CallBlockRuleType.ANY && it.scope == CallBlockScope.SAVED_CONTACT }
        return when (enabled?.action) {
            CallBlockAction.ALLOW -> SavedGroupPolicyUi.ALLOW
            CallBlockAction.BLOCK -> SavedGroupPolicyUi.BLOCK
            null -> SavedGroupPolicyUi.FOLLOW_ADVANCED
        }
    }
    val unknownGroupPolicy: UnknownGroupPolicyUi get() = when {
        rules.any { it.enabled && it.type == CallBlockRuleType.ANY && it.scope == CallBlockScope.NOT_SAVED && it.action == CallBlockAction.BLOCK } -> UnknownGroupPolicyUi.BLOCK_ALWAYS
        CallBlockSettings.repeatUnknownCallerGuardEnabled -> UnknownGroupPolicyUi.BLOCK_UNTIL_REPEAT
        else -> UnknownGroupPolicyUi.PASS
    }
    val repeatUnknownThreshold: Int get() = CallBlockSettings.repeatUnknownCallerGuardThreshold
    val repeatUnknownWindowMinutes: Int get() = CallBlockSettings.repeatUnknownCallerGuardWindowMinutes

    init {
        viewModelScope.launch { repo.observeRules().collect { rules = it } }
        viewModelScope.launch { repo.observeHistory().collect { history = it } }
        viewModelScope.launch { repo.observeNumberEntries().collect { numberEntries = it } }
    }

    fun deleteRule(id: Long) {
        viewModelScope.launch { repo.deleteRule(id) }
    }

    fun deleteHistory(id: Long) {
        viewModelScope.launch { repo.deleteHistory(id) }
    }

    fun deleteNumberEntry(id: Long) {
        viewModelScope.launch { repo.deleteNumberEntry(id) }
    }

    fun moveNumberEntry(id: Long, targetAction: CallBlockAction) {
        viewModelScope.launch { repo.moveNumberEntry(id, targetAction, replaceOpposite = true) }
    }

    fun setNumberEntryEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch { repo.setNumberEntryEnabled(id, enabled) }
    }

    fun setRuleEnabled(id: Long, enabled: Boolean) {
        viewModelScope.launch { repo.setRuleEnabled(id, enabled) }
    }

    fun setAllAdvancedRulesEnabled(enabled: Boolean) {
        viewModelScope.launch { repo.setAllAdvancedRulesEnabled(enabled) }
    }

    fun deleteAllAdvancedRules() {
        viewModelScope.launch { repo.deleteAllAdvancedRules() }
    }

    fun moveAdvancedRule(id: Long, offset: Int) {
        viewModelScope.launch { repo.moveAdvancedRule(id, offset) }
    }

    fun saveNumberEntry(
        action: CallBlockAction,
        rawNumber: String,
        displayName: String,
        origin: NumberEntryOrigin,
    ) {
        viewModelScope.launch {
            repo.upsertNumberEntry(action = action, rawNumber = rawNumber, displayName = displayName, origin = origin)
        }
    }

    fun saveContactSelections(action: CallBlockAction, selections: List<CallBlockContactSelection>) {
        viewModelScope.launch {
            selections.forEach { contact ->
                contact.numbers.forEach { number ->
                    repo.upsertNumberEntry(action, number, contact.displayName, NumberEntryOrigin.CONTACT_PICKER)
                }
            }
        }
    }

    fun saveHistorySelections(action: CallBlockAction, selections: List<CallBlockCallHistorySelection>) {
        viewModelScope.launch {
            selections.forEach { selected ->
                repo.upsertNumberEntry(action, selected.rawNumber, selected.displayName, NumberEntryOrigin.CALL_LOG_PICKER)
            }
        }
    }

    fun saveCategorySelections(action: CallBlockAction, selections: List<CallBlockCategorySelection>) {
        viewModelScope.launch {
            selections.forEach { selected ->
                repo.upsertNumberEntry(
                    action,
                    selected.rawNumber,
                    selected.displayName,
                    NumberEntryOrigin.CATEGORY_PICKER,
                )
            }
        }
    }

    fun setSavedGroupPolicy(policy: SavedGroupPolicyUi) {
        viewModelScope.launch {
            repo.setSavedContactGroupPolicy(
                when (policy) {
                    SavedGroupPolicyUi.FOLLOW_ADVANCED -> SavedContactGroupPolicy.FOLLOW_ADVANCED
                    SavedGroupPolicyUi.ALLOW -> SavedContactGroupPolicy.ALLOW
                    SavedGroupPolicyUi.BLOCK -> SavedContactGroupPolicy.BLOCK
                }
            )
        }
    }

    fun setUnknownGroupPolicy(policy: UnknownGroupPolicyUi) {
        viewModelScope.launch {
            repo.setUnknownNumberPolicy(
                when (policy) {
                    UnknownGroupPolicyUi.PASS -> UnknownNumberPolicy.PASS
                    UnknownGroupPolicyUi.BLOCK_ALWAYS -> UnknownNumberPolicy.BLOCK_ALWAYS
                    UnknownGroupPolicyUi.BLOCK_UNTIL_REPEAT -> UnknownNumberPolicy.BLOCK_UNTIL_REPEAT
                }
            )
        }
    }

    fun setRepeatUnknownThreshold(value: Int) {
        CallBlockSettings.setRepeatUnknownCallerGuardThreshold(getApplication(), value)
    }

    fun setRepeatUnknownWindow(value: Int): Boolean =
        CallBlockSettings.setRepeatUnknownCallerGuardWindowMinutes(getApplication(), value)
}

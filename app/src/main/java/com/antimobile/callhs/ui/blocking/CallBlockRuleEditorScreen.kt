package com.antimobile.callhs.ui.blocking

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.antimobile.callhs.data.blocking.CallBlockRuleMatcher
import com.antimobile.callhs.data.blocking.CallBlockScope
import com.antimobile.callhs.data.blocking.CallBlockAction
import com.antimobile.callhs.data.blocking.CallBlockRuleType
import com.antimobile.callhs.data.blocking.CallHistoryRuleCodec
import com.antimobile.callhs.data.blocking.ContactRuleCodec
import com.antimobile.callhs.data.blocking.GeographicBlockKind
import com.antimobile.callhs.data.blocking.GeographicBlockOption
import com.antimobile.callhs.data.blocking.SaveBlockRuleResult
import com.antimobile.callhs.data.blocking.SpecialCallCondition
import com.antimobile.callhs.i18n.CallBlockStrings
import com.antimobile.callhs.i18n.appStrings
import com.antimobile.callhs.ui.components.AppBottomSheet
import com.antimobile.callhs.ui.components.AppToastType
import com.antimobile.callhs.ui.components.AppMessageDialog
import com.antimobile.callhs.ui.components.DialogButton
import com.antimobile.callhs.ui.components.FilterOptionRow
import com.antimobile.callhs.ui.components.PanelCard
import com.antimobile.callhs.ui.components.Segmented
import com.antimobile.callhs.ui.calllist.CallListViewModel
import com.antimobile.callhs.ui.theme.AccentBlue
import com.antimobile.callhs.ui.theme.AccentRed
import com.antimobile.callhs.ui.theme.AppBackground
import com.antimobile.callhs.ui.theme.BrandSoft
import com.antimobile.callhs.ui.theme.FieldSurface
import com.antimobile.callhs.ui.theme.Primary
import com.antimobile.callhs.ui.theme.TextPrimary
import com.antimobile.callhs.ui.theme.TextSecondary
import com.antimobile.callhs.util.CallActions
import com.antimobile.callhs.util.Carrier

private enum class CallerIdentityTerm {
    VOIP,
    SIP,
    URI,
    CLI,
}

/**
 * Màn tạo/sửa quy tắc chặn, dùng cùng bố cục input + save bar của CategoryEditorScreen.
 * `ruleId == null` là tạo mới; màn danh sách chịu trách nhiệm xoá qua ContextMenuOverlay.
 */
@Composable
fun CallBlockRuleEditorScreen(
    vm: CallBlockRuleEditorViewModel,
    callListVm: CallListViewModel,
    ruleId: Long?,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val s = appStrings().blocker
    val isEdit = ruleId != null
    LaunchedEffect(ruleId) { vm.bind(ruleId) }

    var typeKey by rememberSaveable { mutableStateOf(CallBlockRuleType.PREFIX.storageKey) }
    var value by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var scopeKey by rememberSaveable { mutableStateOf(CallBlockScope.NOT_SAVED.storageKey) }
    var actionKey by rememberSaveable { mutableStateOf(CallBlockAction.BLOCK.storageKey) }
    var enabled by rememberSaveable { mutableStateOf(true) }
    var initialized by rememberSaveable { mutableStateOf(false) }
    var initialType by rememberSaveable { mutableStateOf(typeKey) }
    var initialValue by rememberSaveable { mutableStateOf("") }
    var initialScope by rememberSaveable { mutableStateOf(scopeKey) }
    var initialAction by rememberSaveable { mutableStateOf(actionKey) }
    var initialEnabled by rememberSaveable { mutableStateOf(true) }
    var showTypePicker by remember { mutableStateOf(false) }
    var showCarrierPicker by remember { mutableStateOf(false) }
    var showSpecialPicker by remember { mutableStateOf(false) }
    var showGeographicPicker by remember { mutableStateOf(false) }
    var showContactPicker by remember { mutableStateOf(false) }
    var showCallHistoryPicker by remember { mutableStateOf(false) }
    var showDiscard by remember { mutableStateOf(false) }
    var identityTerm by remember { mutableStateOf<CallerIdentityTerm?>(null) }

    LaunchedEffect(vm.loaded) {
        if (vm.loaded && !initialized) {
            vm.rule?.let { rule ->
                typeKey = rule.type.storageKey
                value = TextFieldValue(rule.rawValue)
                scopeKey = rule.scope.storageKey
                actionKey = rule.action.storageKey
                enabled = rule.enabled
            }
            initialType = typeKey
            initialValue = value.text
            initialScope = scopeKey
            initialAction = actionKey
            initialEnabled = enabled
            initialized = true
        }
    }

    val type = CallBlockRuleType.fromStorage(typeKey) ?: CallBlockRuleType.PREFIX
    val storedScope = CallBlockScope.fromStorage(scopeKey) ?: CallBlockScope.NOT_SAVED
    val scope = storedScope.takeIf { type.supportsScope(it, value.text) }
        ?: CallBlockScope.ALL_VISIBLE_NUMBERS
    val showsScope = type.supportsScope(CallBlockScope.NOT_SAVED, value.text) ||
        type.supportsScope(CallBlockScope.SAVED_CONTACT, value.text)
    val action = if (type == CallBlockRuleType.SPAM_RISK) {
        CallBlockAction.BLOCK
    } else {
        CallBlockAction.fromStorage(actionKey) ?: CallBlockAction.BLOCK
    }
    val hasChanges = initialized && (typeKey != initialType || value.text != initialValue || scopeKey != initialScope || actionKey != initialAction || enabled != initialEnabled)
    val valid = CallBlockRuleMatcher.isValid(type, value.text)

    fun attemptExit() {
        if (hasChanges) showDiscard = true else onExit()
    }
    BackHandler(enabled = !showContactPicker && !showCallHistoryPicker) { attemptExit() }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { EditorTopBar(if (isEdit) s.editRuleTitle else s.createRuleTitle, onBack = ::attemptExit) },
        bottomBar = {
            EditorSaveBar(
                label = if (isEdit) s.update else s.save,
                enabled = initialized && valid,
                onSave = {
                    vm.save(type, value.text, enabled, scope, action) { result ->
                        when (result) {
                            SaveBlockRuleResult.SAVED -> onExit()
                            SaveBlockRuleResult.INVALID -> CallActions.toast(context, s.invalidRule, AppToastType.Error)
                            SaveBlockRuleResult.DUPLICATE -> CallActions.toast(context, s.duplicateRule, AppToastType.Warning)
                            SaveBlockRuleResult.FULL -> CallActions.toast(context, s.maxRules, AppToastType.Warning)
                            SaveBlockRuleResult.NOT_FOUND -> onExit()
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .pointerInput(Unit) { detectTapGestures { focus.clearFocus() } }
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                EditorLabel(s.ruleTypeLabel)
                Spacer(Modifier.height(8.dp))
                PickerRow(
                    icon = Icons.Rounded.Block,
                    value = ruleTypeLabel(type, s),
                    onClick = { focus.clearFocus(); showTypePicker = true },
                )

                Spacer(Modifier.height(20.dp))
                when (type) {
                    CallBlockRuleType.SPAM_RISK -> SpamRiskInfoCard()
                    CallBlockRuleType.SPECIAL -> {
                        EditorLabel(s.specialTitle)
                        Spacer(Modifier.height(8.dp))
                        PickerRow(
                            icon = Icons.Rounded.PrivacyTip,
                            value = if (value.text.isBlank()) s.specialTitle else s.specialSummary(value.text),
                            placeholder = value.text.isBlank(),
                            onClick = { focus.clearFocus(); showSpecialPicker = true },
                        )
                        if (value.text.isBlank()) {
                            Spacer(Modifier.height(7.dp))
                            Text(s.validationSelectSpecial, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                    CallBlockRuleType.CONTACTS -> {
                        EditorLabel(s.typeContacts)
                        Spacer(Modifier.height(8.dp))
                        val selectedCount = ContactRuleCodec.selectedCount(value.text)
                        PickerRow(
                            icon = Icons.Rounded.Contacts,
                            value = if (selectedCount == 0) s.contactPickerOpen else s.contactPickerSelectedCount(selectedCount),
                            placeholder = selectedCount == 0,
                            onClick = { focus.clearFocus(); showContactPicker = true },
                        )
                        if (selectedCount == 0) {
                            Spacer(Modifier.height(7.dp))
                            Text(s.validationSelectContact, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                    CallBlockRuleType.CALL_HISTORY -> {
                        EditorLabel(s.typeCallHistory)
                        Spacer(Modifier.height(8.dp))
                        val selectedCount = CallHistoryRuleCodec.selectedCount(value.text)
                        PickerRow(
                            icon = Icons.Rounded.History,
                            value = if (selectedCount == 0) {
                                s.callHistoryPickerOpen
                            } else {
                                s.callHistoryPickerSelectedCount(selectedCount)
                            },
                            placeholder = selectedCount == 0,
                            onClick = { focus.clearFocus(); showCallHistoryPicker = true },
                        )
                        if (selectedCount == 0) {
                            Spacer(Modifier.height(7.dp))
                            Text(s.validationSelectCallHistory, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                    CallBlockRuleType.GEOGRAPHIC -> {
                        EditorLabel(s.typeCountryAndAreaCode)
                        Spacer(Modifier.height(8.dp))
                        val selected = GeographicBlockOption.decode(value.text)
                        PickerRow(
                            icon = Icons.Rounded.Public,
                            value = if (selected.isEmpty()) s.regionPickerTitle else s.regionSummary(value.text),
                            placeholder = selected.isEmpty(),
                            onClick = { focus.clearFocus(); showGeographicPicker = true },
                        )
                        if (selected.isEmpty()) {
                            Spacer(Modifier.height(7.dp))
                            Text(s.validationSelectRegion, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                    CallBlockRuleType.CARRIER -> {
                        EditorLabel(s.chooseCarrier)
                        Spacer(Modifier.height(8.dp))
                        PickerRow(
                            icon = Icons.Rounded.Phone,
                            value = value.text.ifBlank { s.carrierHint },
                            placeholder = value.text.isBlank(),
                            onClick = { focus.clearFocus(); showCarrierPicker = true },
                        )
                    }
                    else -> {
                        EditorLabel(ruleValueInputLabel(type, s))
                        Spacer(Modifier.height(8.dp))
                        NumberEditorField(
                            value = value,
                            onValueChange = { next -> value = next.copy(text = next.text.filter { it.isDigit() || it in "+ -()." }) },
                            hint = if (type == CallBlockRuleType.LENGTH) s.lengthHint else s.numberHint,
                            onDone = { focus.clearFocus() },
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                EditorLabel(s.ruleActionLabel)
                Spacer(Modifier.height(8.dp))
                if (type == CallBlockRuleType.SPAM_RISK) {
                    FixedBlockActionRow(s.actionBlock)
                } else {
                    val actions = listOf(CallBlockAction.BLOCK, CallBlockAction.ALLOW)
                    Segmented(
                        labels = listOf(s.actionBlock, s.actionAllow),
                        selected = actions.indexOf(action).coerceAtLeast(0),
                        onSelect = { index -> actions.getOrNull(index)?.let { actionKey = it.storageKey } },
                    )
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    if (action == CallBlockAction.BLOCK) s.actionBlockDesc else s.actionAllowDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )

                if (showsScope) {
                    Spacer(Modifier.height(20.dp))
                    EditorLabel(s.ruleScopeLabel)
                    Spacer(Modifier.height(8.dp))
                    val scopes = listOf(CallBlockScope.NOT_SAVED, CallBlockScope.SAVED_CONTACT, CallBlockScope.ALL_VISIBLE_NUMBERS)
                    Segmented(
                        labels = listOf(s.scopeUnknown, s.scopeContacts, s.scopeAll),
                        selected = scopes.indexOf(scope).coerceAtLeast(0),
                        onSelect = { index -> scopes.getOrNull(index)?.let { scopeKey = it.storageKey } },
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        when (scope) {
                            CallBlockScope.NOT_SAVED -> s.scopeUnknownDesc
                            CallBlockScope.SAVED_CONTACT -> s.scopeContactsDesc
                            CallBlockScope.ALL_VISIBLE_NUMBERS -> s.scopeAllDesc
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    if (!showsScope) {
                        "${if (action == CallBlockAction.BLOCK) s.actionBlock else s.actionAllow} · ${s.ruleSummary(type.storageKey, value.text)}"
                    } else if (action == CallBlockAction.BLOCK) {
                        s.rulePreview(s.ruleSummary(type.storageKey, value.text), s.ruleScopeSummary(scope.storageKey))
                    } else {
                        "${s.actionAllow} · ${s.ruleScopeSummary(scope.storageKey)} · ${s.ruleSummary(type.storageKey, value.text)}"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Primary,
                    fontWeight = FontWeight.SemiBold,
                )

                Spacer(Modifier.height(20.dp))
                PanelCard(modifier = Modifier.fillMaxWidth(), radius = 18.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { enabled = !enabled }.padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(if (enabled) BrandSoft else FieldSurface),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (enabled) Icons.Rounded.Check else Icons.Rounded.PowerSettingsNew,
                                contentDescription = null,
                                tint = if (enabled) Primary else TextSecondary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (enabled) s.ruleEnabledStatus else s.ruleDisabledStatus,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (enabled) Primary else TextSecondary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = enabled,
                            onCheckedChange = { enabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Primary,
                                checkedBorderColor = Primary,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                CallerIdentityTermLinks(
                    s = s,
                    onOpen = { identityTerm = it },
                )
                Spacer(Modifier.height(28.dp))
            }
        }
    }

        if (showContactPicker) {
            CallBlockContactPickerScreen(
                vm = vm,
                initialSelection = ContactRuleCodec.decode(value.text),
                onBack = { showContactPicker = false },
                onDone = { contacts ->
                    value = TextFieldValue(ContactRuleCodec.encode(contacts))
                    showContactPicker = false
                },
            )
        }
        if (showCallHistoryPicker) {
            CallBlockCallHistoryPickerScreen(
                vm = callListVm,
                initialSelection = CallHistoryRuleCodec.decode(value.text),
                onBack = { showCallHistoryPicker = false },
                onDone = { selections ->
                    value = TextFieldValue(CallHistoryRuleCodec.encode(selections))
                    showCallHistoryPicker = false
                },
            )
        }
    }

    if (showTypePicker) {
        RuleTypeSheet(
            selected = type,
            onDismiss = { showTypePicker = false },
            onPick = { picked ->
                val previous = type
                typeKey = picked.storageKey
                if (picked != previous) {
                    value = TextFieldValue(
                        when (picked) {
                            CallBlockRuleType.SPAM_RISK -> CallBlockRuleMatcher.SPAM_RISK_PROFILE
                            CallBlockRuleType.CARRIER,
                            CallBlockRuleType.SPECIAL,
                            CallBlockRuleType.CONTACTS,
                            CallBlockRuleType.CALL_HISTORY,
                            CallBlockRuleType.GEOGRAPHIC -> ""
                            CallBlockRuleType.ANY -> "*"
                            CallBlockRuleType.EXACT_NUMBER,
                            CallBlockRuleType.PREFIX,
                            CallBlockRuleType.SUFFIX,
                            CallBlockRuleType.CONTAINS,
                            CallBlockRuleType.LENGTH -> if (previous in numberRuleTypes) value.text else ""
                        }
                    )
                    if (picked == CallBlockRuleType.SPAM_RISK) {
                        actionKey = CallBlockAction.BLOCK.storageKey
                    }
                }
            },
        )
    }
    if (showCarrierPicker) {
        CarrierSheet(
            selected = value.text,
            onDismiss = { showCarrierPicker = false },
            onPick = { carrier -> value = TextFieldValue(carrier) },
        )
    }
    if (showSpecialPicker) {
        SpecialConditionSheet(
            selected = SpecialCallCondition.activeSelection(value.text),
            onDismiss = { showSpecialPicker = false },
            onConfirm = { selected ->
                value = TextFieldValue(SpecialCallCondition.encode(setOf(selected)))
                if (selected in setOf(
                        SpecialCallCondition.PRIVATE_NUMBER,
                        SpecialCallCondition.SIP_TEXT_ID,
                    )
                ) {
                    scopeKey = CallBlockScope.ALL_VISIBLE_NUMBERS.storageKey
                }
            },
        )
    }
    if (showGeographicPicker) {
        GeographicOptionsSheet(
            selected = GeographicBlockOption.decode(value.text),
            onDismiss = { showGeographicPicker = false },
            onConfirm = { selected -> value = TextFieldValue(GeographicBlockOption.encode(selected)) },
        )
    }
    identityTerm?.let { term ->
        CallerIdentityTermSheet(
            term = term,
            s = s,
            onDismiss = { identityTerm = null },
        )
    }
    if (showDiscard) {
        AppMessageDialog(
            onDismissRequest = { showDiscard = false },
            title = s.discardTitle,
            message = s.discardMessage,
            buttons = listOf(
                DialogButton(s.discardStay, TextSecondary) { showDiscard = false },
                DialogButton(s.discardExit, AccentRed, bold = true) { showDiscard = false; onExit() },
            ),
        )
    }
}

@Composable
private fun CallerIdentityTermLinks(
    s: CallBlockStrings,
    onOpen: (CallerIdentityTerm) -> Unit,
) {
    Text(
        text = s.identityTermsTitle,
        style = MaterialTheme.typography.labelLarge,
        color = TextSecondary,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(4.dp))
    listOf(
        CallerIdentityTerm.VOIP,
        CallerIdentityTerm.SIP,
        CallerIdentityTerm.URI,
        CallerIdentityTerm.CLI,
    ).forEach { term ->
        val interactionSource = remember(term) { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        Text(
            text = term.title(s),
            style = MaterialTheme.typography.bodyMedium,
            color = Primary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isPressed) FieldSurface else Color.Transparent)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { onOpen(term) },
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun CallerIdentityTermSheet(
    term: CallerIdentityTerm,
    s: CallBlockStrings,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(
        onDismiss = onDismiss,
        title = term.title(s),
        maxHeightFraction = 0.82f,
        sheetGesturesEnabled = false,
        showCloseButton = true,
    ) {
        Text(
            text = term.explanation(s),
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        )
        Spacer(Modifier.height(12.dp))
    }
}

private fun CallerIdentityTerm.title(s: CallBlockStrings): String = when (this) {
    CallerIdentityTerm.VOIP -> s.learnVoip
    CallerIdentityTerm.SIP -> s.learnSip
    CallerIdentityTerm.URI -> s.learnUri
    CallerIdentityTerm.CLI -> s.learnCli
}

private fun CallerIdentityTerm.explanation(s: CallBlockStrings): String = when (this) {
    CallerIdentityTerm.VOIP -> s.voipExplanation
    CallerIdentityTerm.SIP -> s.sipExplanation
    CallerIdentityTerm.URI -> s.uriExplanation
    CallerIdentityTerm.CLI -> s.cliExplanation
}

@Composable
internal fun EditorTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppBackground)
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = appStrings().common.back, tint = TextPrimary, modifier = Modifier.size(23.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun EditorSaveBar(label: String, enabled: Boolean, onSave: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppBackground)
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 12.dp),
    ) {
        val bg = if (enabled) Primary else Primary.copy(alpha = 0.5f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bg)
                .clickable(enabled = enabled, onClick = onSave),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

@Composable
private fun EditorLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = TextSecondary, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun PickerRow(icon: ImageVector, value: String, placeholder: Boolean = false, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(FieldSurface).clickable(onClick = onClick).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(BrandSoft), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(13.dp))
        Text(value, style = MaterialTheme.typography.bodyLarge, color = if (placeholder) TextSecondary else TextPrimary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun NumberEditorField(value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit, hint: String, onDone: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(12.dp)).background(FieldSurface).padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (value.text.isEmpty()) Text(hint, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
            cursorBrush = SolidColor(Primary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { onDone() },
                onGo = { onDone() },
                onSend = { onDone() },
            ),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SpamRiskInfoCard() {
    val s = appStrings().blocker
    PanelCard(modifier = Modifier.fillMaxWidth(), radius = 18.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Rounded.Policy, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(21.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    s.spamRiskDetailsTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
            SpamRiskDetail(s.spamRiskPrefixDetail)
            SpamRiskDetail(s.spamRiskUnknownPrefixDetail)
            SpamRiskDetail(s.spamRiskVerificationDetail)
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AccentRed.copy(alpha = 0.08f))
                    .padding(10.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = AccentRed, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(9.dp))
                Text(
                    s.spamRiskWarning,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SpamRiskDetail(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 31.dp, end = 2.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier.padding(top = 6.dp).size(6.dp).clip(CircleShape).background(AccentBlue),
        )
        Spacer(Modifier.width(9.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = TextPrimary, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun FixedBlockActionRow(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(FieldSurface).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(BrandSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Block, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(13.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.Rounded.Check, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun RuleTypeSheet(selected: CallBlockRuleType, onDismiss: () -> Unit, onPick: (CallBlockRuleType) -> Unit) {
    val s = appStrings().blocker
    val types = listOf(CallBlockRuleType.SPAM_RISK) + CallBlockRuleType.entries.filter { type ->
        type !in setOf(
            CallBlockRuleType.EXACT_NUMBER,
            CallBlockRuleType.CONTACTS,
            CallBlockRuleType.CALL_HISTORY,
            CallBlockRuleType.ANY,
            CallBlockRuleType.SPAM_RISK,
        )
    }
    AppBottomSheet(onDismiss = onDismiss, title = s.chooseRuleType, sheetGesturesEnabled = false, showCloseButton = true) { close ->
        types.forEach { type ->
            FilterOptionRow(
                icon = ruleTypeIcon(type),
                label = ruleTypeLabel(type, s),
                supportingText = s.spamRiskPickerDescription.takeIf { type == CallBlockRuleType.SPAM_RISK },
                selected = selected == type,
                onClick = { onPick(type); close() },
            )
        }
    }
}

@Composable
private fun CarrierSheet(selected: String, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val s = appStrings().blocker
    AppBottomSheet(onDismiss = onDismiss, title = s.chooseCarrier, sheetGesturesEnabled = false, showCloseButton = true) { close ->
        Carrier.names.forEach { carrier ->
            FilterOptionRow(
                icon = Icons.Rounded.SimCard,
                label = carrier,
                selected = selected == carrier,
                onClick = { onPick(carrier); close() },
            )
        }
    }
}

@Composable
private fun SpecialConditionSheet(
    selected: SpecialCallCondition?,
    onDismiss: () -> Unit,
    onConfirm: (SpecialCallCondition) -> Unit,
) {
    val s = appStrings().blocker
    var temporary by remember(selected) { mutableStateOf(selected) }
    AppBottomSheet(
        onDismiss = onDismiss,
        title = s.specialTitle,
        maxHeightFraction = 0.9f,
        sheetGesturesEnabled = false,
        showCloseButton = true,
    ) { close ->
        SpecialCallCondition.activeEntries.forEach { condition ->
            FilterOptionRow(
                icon = specialConditionIcon(condition),
                label = specialConditionLabel(condition, s),
                selected = condition == temporary,
                onClick = { temporary = condition },
            )
            Text(
                text = specialConditionDescription(condition, s),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(start = 72.dp, end = 20.dp, bottom = 8.dp),
            )
        }
        SheetWarningNotice(s.specialAndroidLimit)
        SheetConfirmButton(
            text = s.contactPickerDone,
            enabled = temporary != null,
            onClick = {
                temporary?.let(onConfirm)
                close()
            },
        )
    }
}

@Composable
private fun GeographicOptionsSheet(
    selected: Set<GeographicBlockOption>,
    onDismiss: () -> Unit,
    onConfirm: (Set<GeographicBlockOption>) -> Unit,
) {
    val s = appStrings().blocker
    var temporary by remember(selected) { mutableStateOf(selected) }
    val internationalOptions = GeographicBlockOption.entries.filter { it.kind != GeographicBlockKind.VIETNAM_PREFIX }
    val vietnamPrefixes = GeographicBlockOption.entries.filter { it.kind == GeographicBlockKind.VIETNAM_PREFIX }

    AppBottomSheet(
        onDismiss = onDismiss,
        title = s.regionPickerTitle,
        sheetGesturesEnabled = false,
        showCloseButton = true,
    ) { close ->
        SheetSectionLabel(s.regionInternationalSection)
        internationalOptions.forEach { option ->
            FilterOptionRow(
                icon = Icons.Rounded.Public,
                label = geographicOptionLabel(option, s),
                selected = option in temporary,
                onClick = {
                    temporary = when {
                        option == GeographicBlockOption.ALL_INTERNATIONAL_EXCEPT_VIETNAM -> {
                            if (option in temporary) {
                                temporary - option
                            } else {
                                temporary.filterTo(linkedSetOf()) { it.kind == GeographicBlockKind.VIETNAM_PREFIX } + option
                            }
                        }
                        option.kind == GeographicBlockKind.COUNTRY_CALLING_CODE -> {
                            val withoutPreset = temporary - GeographicBlockOption.ALL_INTERNATIONAL_EXCEPT_VIETNAM
                            if (option in temporary) withoutPreset - option else withoutPreset + option
                        }
                        else -> if (option in temporary) temporary - option else temporary + option
                    }
                },
            )
            if (option == GeographicBlockOption.ALL_INTERNATIONAL_EXCEPT_VIETNAM) {
                Text(
                    text = s.regionAllInternationalExceptVietnamDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 72.dp, end = 20.dp, bottom = 8.dp),
                )
            }
        }

        SheetSectionLabel(s.regionVietnamPrefixSection, topPadding = 14.dp)
        vietnamPrefixes.forEach { option ->
            FilterOptionRow(
                icon = Icons.Rounded.LocationOn,
                label = geographicOptionLabel(option, s),
                selected = option in temporary,
                onClick = {
                    temporary = if (option in temporary) temporary - option else temporary + option
                },
            )
        }

        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(9.dp))
            Text(s.regionCallerIdWarning, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.weight(1f))
        }
        SheetConfirmButton(
            text = s.contactPickerDone,
            enabled = temporary.isNotEmpty(),
            onClick = {
                onConfirm(temporary)
                close()
            },
        )
    }
}

@Composable
private fun SheetSectionLabel(text: String, topPadding: androidx.compose.ui.unit.Dp = 4.dp) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = TextSecondary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = topPadding, bottom = 5.dp),
    )
}

@Composable
private fun SheetWarningNotice(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AccentRed.copy(alpha = 0.07f))
            .padding(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            Icons.Rounded.WarningAmber,
            contentDescription = null,
            tint = AccentRed,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SheetConfirmButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    val background = if (enabled) Primary else Primary.copy(alpha = 0.5f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(background)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun ruleTypeIcon(type: CallBlockRuleType): ImageVector = when (type) {
    CallBlockRuleType.EXACT_NUMBER -> Icons.Rounded.Phone
    CallBlockRuleType.PREFIX -> Icons.AutoMirrored.Rounded.CallMade
    CallBlockRuleType.SUFFIX -> Icons.AutoMirrored.Rounded.CallReceived
    CallBlockRuleType.CONTAINS -> Icons.Rounded.Search
    CallBlockRuleType.LENGTH -> Icons.Rounded.Phone
    CallBlockRuleType.ANY -> Icons.Rounded.Block
    CallBlockRuleType.CARRIER -> Icons.Rounded.SimCard
    CallBlockRuleType.GEOGRAPHIC -> Icons.Rounded.Public
    CallBlockRuleType.SPAM_RISK -> Icons.Rounded.Policy
    CallBlockRuleType.SPECIAL -> Icons.Rounded.PrivacyTip
    CallBlockRuleType.CONTACTS -> Icons.Rounded.Contacts
    CallBlockRuleType.CALL_HISTORY -> Icons.Rounded.History
}

private fun ruleTypeLabel(type: CallBlockRuleType, s: CallBlockStrings): String = when (type) {
    CallBlockRuleType.EXACT_NUMBER -> s.typeExact
    CallBlockRuleType.PREFIX -> s.typePrefix
    CallBlockRuleType.SUFFIX -> s.typeSuffix
    CallBlockRuleType.CONTAINS -> s.typeContains
    CallBlockRuleType.LENGTH -> s.typeLength
    CallBlockRuleType.ANY -> s.groupBlockingTitle
    CallBlockRuleType.CARRIER -> s.typeCarrier
    CallBlockRuleType.GEOGRAPHIC -> s.typeCountryAndAreaCode
    CallBlockRuleType.SPAM_RISK -> s.typeSpamRisk
    CallBlockRuleType.SPECIAL -> s.typeSpecial
    CallBlockRuleType.CONTACTS -> s.typeContacts
    CallBlockRuleType.CALL_HISTORY -> s.typeCallHistory
}

private fun ruleValueInputLabel(type: CallBlockRuleType, s: CallBlockStrings): String = when (type) {
    CallBlockRuleType.EXACT_NUMBER -> s.exactValueLabel
    CallBlockRuleType.PREFIX -> s.prefixValueLabel
    CallBlockRuleType.SUFFIX -> s.suffixValueLabel
    CallBlockRuleType.CONTAINS -> s.containsValueLabel
    CallBlockRuleType.LENGTH -> s.lengthValueLabel
    else -> s.ruleValueLabel
}

private fun specialConditionIcon(condition: SpecialCallCondition): ImageVector = when (condition) {
    SpecialCallCondition.PRIVATE_NUMBER -> Icons.Rounded.PrivacyTip
    SpecialCallCondition.UNKNOWN_CONTACT -> Icons.Rounded.Contacts
    SpecialCallCondition.VOIP -> Icons.Rounded.Phone
    SpecialCallCondition.SIP_PHONE_NUMBER -> Icons.Rounded.Phone
    SpecialCallCondition.SIP_TEXT_ID -> Icons.Rounded.Public
}

private fun specialConditionLabel(condition: SpecialCallCondition, s: CallBlockStrings): String = when (condition) {
    SpecialCallCondition.PRIVATE_NUMBER -> s.specialPrivate
    SpecialCallCondition.UNKNOWN_CONTACT -> s.specialUnknownContact
    SpecialCallCondition.VOIP -> s.specialVoip
    SpecialCallCondition.SIP_PHONE_NUMBER -> s.specialSipPhone
    SpecialCallCondition.SIP_TEXT_ID -> s.specialSipText
}

private fun specialConditionDescription(condition: SpecialCallCondition, s: CallBlockStrings): String = when (condition) {
    SpecialCallCondition.PRIVATE_NUMBER -> s.specialPrivateDesc
    SpecialCallCondition.UNKNOWN_CONTACT -> s.specialUnknownContactDesc
    SpecialCallCondition.VOIP -> s.specialVoipDesc
    SpecialCallCondition.SIP_PHONE_NUMBER -> s.specialSipPhoneDesc
    SpecialCallCondition.SIP_TEXT_ID -> s.specialSipTextDesc
}

private fun geographicOptionLabel(option: GeographicBlockOption, s: CallBlockStrings): String = when (option) {
    GeographicBlockOption.ALL_INTERNATIONAL_EXCEPT_VIETNAM -> s.regionAllInternationalExceptVietnam
    GeographicBlockOption.CHINA -> s.regionChina
    GeographicBlockOption.CAMBODIA -> s.regionCambodia
    GeographicBlockOption.MYANMAR -> s.regionMyanmar
    GeographicBlockOption.NANP_SHARED -> s.regionNanpShared
    GeographicBlockOption.GERMANY -> s.regionGermany
    GeographicBlockOption.LAOS -> s.regionLaos
    GeographicBlockOption.THAILAND -> s.regionThailand
    GeographicBlockOption.MALAYSIA -> s.regionMalaysia
    GeographicBlockOption.SINGAPORE -> s.regionSingapore
    GeographicBlockOption.INDONESIA -> s.regionIndonesia
    GeographicBlockOption.PHILIPPINES -> s.regionPhilippines
    GeographicBlockOption.INDIA -> s.regionIndia
    GeographicBlockOption.VIETNAM_PREFIX_024 -> s.regionPrefix024
    GeographicBlockOption.VIETNAM_PREFIX_022 -> s.regionPrefix022
    GeographicBlockOption.VIETNAM_PREFIX_028 -> s.regionPrefix028
    GeographicBlockOption.VIETNAM_PREFIX_059 -> s.regionPrefix059
    GeographicBlockOption.VIETNAM_PREFIX_099 -> s.regionPrefix099
}

private val numberRuleTypes = setOf(
    CallBlockRuleType.EXACT_NUMBER,
    CallBlockRuleType.PREFIX,
    CallBlockRuleType.SUFFIX,
    CallBlockRuleType.CONTAINS,
    CallBlockRuleType.LENGTH,
)

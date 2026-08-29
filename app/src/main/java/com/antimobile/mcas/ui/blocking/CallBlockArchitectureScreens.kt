package com.antimobile.mcas.ui.blocking

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Rule
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PowerOff
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.antimobile.mcas.data.blocking.CallBlockAction
import com.antimobile.mcas.data.blocking.CallBlockCallHistorySelection
import com.antimobile.mcas.data.blocking.CallBlockContactSelection
import com.antimobile.mcas.data.blocking.CallBlockNumberEntry
import com.antimobile.mcas.data.blocking.CallBlockRule
import com.antimobile.mcas.data.blocking.CallBlockRuleType
import com.antimobile.mcas.data.blocking.CallBlockScope
import com.antimobile.mcas.data.blocking.CallBlockSettings
import com.antimobile.mcas.data.blocking.NumberEntryOrigin
import com.antimobile.mcas.i18n.appStrings
import com.antimobile.mcas.ui.calllist.CallListViewModel
import com.antimobile.mcas.ui.components.ActionGlyph
import com.antimobile.mcas.ui.components.AppBottomSheet
import com.antimobile.mcas.ui.components.AppDialog
import com.antimobile.mcas.ui.components.ContextAction
import com.antimobile.mcas.ui.components.ContextMenuOverlay
import com.antimobile.mcas.ui.components.DialogButton
import com.antimobile.mcas.ui.components.FilterOptionRow
import com.antimobile.mcas.ui.components.FrostedScrollButton
import com.antimobile.mcas.ui.components.PanelCard
import com.antimobile.mcas.ui.components.Segmented
import com.antimobile.mcas.ui.components.SectionHeader
import com.antimobile.mcas.ui.components.rememberPressHighlight
import com.antimobile.mcas.ui.theme.AccentGreen
import com.antimobile.mcas.ui.theme.AccentGreenBg
import com.antimobile.mcas.ui.theme.AccentRed
import com.antimobile.mcas.ui.theme.AccentRedBg
import com.antimobile.mcas.ui.theme.AppBackground
import com.antimobile.mcas.ui.theme.BrandSoft
import com.antimobile.mcas.ui.theme.FieldSurface
import com.antimobile.mcas.ui.theme.Primary
import com.antimobile.mcas.ui.theme.TextPrimary
import com.antimobile.mcas.ui.theme.TextSecondary
import com.antimobile.mcas.util.PhoneKey
import com.antimobile.mcas.util.TimeFormat
import com.antimobile.mcas.util.formatPhone
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

private data class NumberMenuTarget(val entry: CallBlockNumberEntry, val bounds: Rect)
private data class AdvancedRuleMenuTarget(val rule: CallBlockRule, val bounds: Rect)
private enum class GroupPolicyPicker { SAVED, UNKNOWN }
private enum class AdvancedRulesBulkAction { ENABLE_ALL, DISABLE_ALL, DELETE_ALL }

enum class SavedGroupPolicyUi { FOLLOW_ADVANCED, ALLOW, BLOCK }
enum class UnknownGroupPolicyUi { PASS, BLOCK_ALWAYS, BLOCK_UNTIL_REPEAT }

/** Exact-number list. Contacts, Call Log and categories are picker sources only. */
@Composable
fun CallBlockNumberListScreen(
    vm: CallBlockViewModel,
    contactVm: CallBlockRuleEditorViewModel,
    callListVm: CallListViewModel,
    action: CallBlockAction,
    onBack: () -> Unit,
) {
    val strings = appStrings()
    val s = strings.blocker
    val entries = if (action == CallBlockAction.ALLOW) vm.allowlist else vm.blocklist
    var target by remember { mutableStateOf<NumberMenuTarget?>(null) }
    var showSource by remember { mutableStateOf(false) }
    var showManual by remember { mutableStateOf(false) }
    var showContacts by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showCategories by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val topInset = WindowInsets.statusBars.getTop(density)
    val bottomInset = WindowInsets.navigationBars.getBottom(density)
    val zone = remember { ZoneId.systemDefault() }
    val entriesByDate = remember(entries, zone) {
        entries.groupBy { entry ->
            Instant.ofEpochMilli(entry.createdAt).atZone(zone).toLocalDate()
        }
    }
    val listState = rememberLazyListState()
    val scrollScope = rememberCoroutineScope()
    val backdropLayer = rememberGraphicsLayer()
    var listContentCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val canScrollUp by remember {
        derivedStateOf { listState.canScrollBackward }
    }
    val canScrollDown by remember {
        derivedStateOf { listState.canScrollForward }
    }
    val navigationBottom = with(density) { bottomInset.toDp() }

    BackHandler(enabled = showContacts || showHistory || showCategories) {
        showContacts = false
        showHistory = false
        showCategories = false
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            BlockTopBar(
                title = if (action == CallBlockAction.ALLOW) s.allowlistScreenTitle else s.blocklistScreenTitle,
                onBack = onBack,
            )
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { listContentCoords = it }
                    .drawWithContent {
                        // Hai nút cuộn luôn cố định như tab Lịch sử, nên luôn ghi lớp nội dung
                        // phía dưới để nền kính mờ của cả nút bật lẫn nút đang vô hiệu hoạt động.
                        backdropLayer.record { this@drawWithContent.drawContent() }
                        drawLayer(backdropLayer)
                    },
                // Hai nút cuộn nằm giữa cạnh phải, không còn FAB ở đáy cần né.
                contentPadding = PaddingValues(bottom = navigationBottom + 24.dp),
            ) {
                item { AddNumberCard(onClick = { showSource = true }) }
                if (entries.isEmpty()) {
                    item {
                        ArchitectureEmptyCard(
                            if (action == CallBlockAction.ALLOW) s.allowlistEmpty else s.blocklistEmpty
                        )
                    }
                } else {
                    entriesByDate.forEach { (date, dateEntries) ->
                        item(key = "number-date-$date") {
                            val dayStartMillis = date.atStartOfDay(zone).toInstant().toEpochMilli()
                            SectionHeader(TimeFormat.sectionLabel(dayStartMillis))
                        }
                        items(dateEntries, key = { it.id }) { entry ->
                            NumberEntryItem(
                                entry = entry,
                                activeInMenu = target?.entry?.id == entry.id,
                                onLongPress = { bounds -> target = NumberMenuTarget(entry, bounds) },
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FrostedScrollButton(
                icon = Icons.Rounded.KeyboardArrowUp,
                contentDescription = strings.callList.scrollToTop,
                backdropLayer = backdropLayer,
                contentCoords = listContentCoords,
                enabled = canScrollUp,
                buttonSize = 40.dp,
                iconSize = 24.dp,
                onClick = {
                    scrollScope.launch { listState.animateScrollToItem(0) }
                },
            )
            FrostedScrollButton(
                icon = Icons.Rounded.KeyboardArrowDown,
                contentDescription = strings.callList.scrollToBottom,
                backdropLayer = backdropLayer,
                contentCoords = listContentCoords,
                enabled = canScrollDown,
                buttonSize = 40.dp,
                iconSize = 24.dp,
                onClick = {
                    scrollScope.launch {
                        val lastIndex =
                            (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
                        listState.animateScrollToItem(lastIndex)
                    }
                },
            )
        }

        target?.let { selected ->
            ContextMenuOverlay(
                bounds = selected.bounds,
                actions = listOf(
                    ContextAction(
                        glyph = ActionGlyph.Vector(Icons.Rounded.SwapHoriz, Primary),
                        desc = if (selected.entry.action == CallBlockAction.ALLOW) {
                            s.menuMoveToBlocklist
                        } else {
                            s.menuMoveToAllowlist
                        },
                        onClick = {
                            vm.moveNumberEntry(
                                selected.entry.id,
                                if (selected.entry.action == CallBlockAction.ALLOW) {
                                    CallBlockAction.BLOCK
                                } else {
                                    CallBlockAction.ALLOW
                                },
                            )
                        },
                    ),
                    ContextAction(
                        glyph = ActionGlyph.Vector(
                            Icons.Rounded.PowerSettingsNew,
                            if (selected.entry.enabled) TextSecondary else AccentGreen,
                        ),
                        desc = if (selected.entry.enabled) s.menuDisableNumber else s.menuEnableNumber,
                        onClick = { vm.setNumberEntryEnabled(selected.entry.id, !selected.entry.enabled) },
                    ),
                    ContextAction(
                        glyph = ActionGlyph.Vector(Icons.Rounded.Delete, AccentRed),
                        desc = s.menuDeleteNumber,
                        onClick = { vm.deleteNumberEntry(selected.entry.id) },
                    ),
                ),
                topInsetPx = topInset,
                bottomInsetPx = bottomInset,
                onClosed = { target = null },
                lifted = { NumberEntryCard(selected.entry) },
            )
        }
    }

    if (showSource) {
        NumberSourceSheet(
            onDismiss = { showSource = false },
            onManual = { showSource = false; showManual = true },
            onContacts = { showSource = false; showContacts = true },
            onHistory = { showSource = false; showHistory = true },
            onCategories = { showSource = false; showCategories = true },
        )
    }
    if (showManual) {
        ManualNumberSheet(
            action = action,
            onDismiss = { showManual = false },
            onSave = { number, name ->
                vm.saveNumberEntry(action, number, name, NumberEntryOrigin.MANUAL)
            },
        )
    }
    if (showContacts) {
        CallBlockContactPickerScreen(
            vm = contactVm,
            initialSelection = emptyList(),
            onBack = { showContacts = false },
            onDone = { selected ->
                vm.saveContactSelections(action, selected)
                showContacts = false
            },
        )
    }
    if (showHistory) {
        CallBlockCallHistoryPickerScreen(
            vm = callListVm,
            initialSelection = emptyList(),
            onBack = { showHistory = false },
            onDone = { selected ->
                vm.saveHistorySelections(action, selected)
                showHistory = false
            },
        )
    }
    if (showCategories) {
        CallBlockCategoryPickerScreen(
            onBack = { showCategories = false },
            onDone = { selected ->
                vm.saveCategorySelections(action, selected)
                showCategories = false
            },
        )
    }
}

@Composable
fun CallBlockGroupScreen(vm: CallBlockViewModel, onBack: () -> Unit) {
    val s = appStrings().blocker
    val context = LocalContext.current
    var scheduleClockMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val controlsEnabled = CallBlockSettings.isEffectivelyEnabledAt(scheduleClockMillis)
    val savedOptions = SavedGroupPolicyUi.entries
    val unknownOptions = UnknownGroupPolicyUi.entries
    var showRepeatWindow by remember { mutableStateOf(false) }
    var policyPicker by remember { mutableStateOf<GroupPolicyPicker?>(null) }

    // Keep disabled state aligned with a pause deadline while this child destination is open.
    LaunchedEffect(
        CallBlockSettings.baseEnabled,
        CallBlockSettings.pauseUntilMillis,
        CallBlockSettings.dailySchedule,
    ) {
        while (true) {
            val current = System.currentTimeMillis()
            scheduleClockMillis = current
            val remaining = CallBlockSettings.refresh(context, current).remainingPauseMillisAt(current)
            delay(if (remaining > 0L) minOf(1_000L, remaining) else 60_000L - current % 60_000L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .statusBarsPadding(),
    ) {
        BlockTopBar(title = s.groupScreenTitle, onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(s.groupPriorityNote, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp))
            Spacer(Modifier.height(8.dp))
            GroupPolicyCard(
                icon = Icons.Rounded.Contacts,
                title = s.savedPolicyTitle,
                selectedLabel = listOf(s.savedPolicyFollowRules, s.savedPolicyAllow, s.savedPolicyBlock)
                    .getOrElse(savedOptions.indexOf(vm.savedGroupPolicy)) { s.savedPolicyFollowRules },
                selectedDescription = listOf(s.savedPolicyFollowRulesDesc, s.savedPolicyAllowDesc, s.savedPolicyBlockDesc)
                    .getOrElse(savedOptions.indexOf(vm.savedGroupPolicy)) { s.savedPolicyFollowRulesDesc },
                enabled = controlsEnabled,
                onClick = { policyPicker = GroupPolicyPicker.SAVED },
            )
            Spacer(Modifier.height(12.dp))
            GroupPolicyCard(
                icon = Icons.Rounded.Phone,
                title = s.unknownPolicyTitle,
                selectedLabel = listOf(s.unknownPolicyPass, s.unknownPolicyBlockAlways, s.unknownPolicyBlockUntilRepeat)
                    .getOrElse(unknownOptions.indexOf(vm.unknownGroupPolicy)) { s.unknownPolicyPass },
                selectedDescription = listOf(s.unknownPolicyPassDesc, s.unknownPolicyBlockAlwaysDesc, s.unknownPolicyBlockUntilRepeatDesc)
                    .getOrElse(unknownOptions.indexOf(vm.unknownGroupPolicy)) { s.unknownPolicyPassDesc },
                enabled = controlsEnabled,
                onClick = { policyPicker = GroupPolicyPicker.UNKNOWN },
            )
            if (vm.unknownGroupPolicy == UnknownGroupPolicyUi.BLOCK_UNTIL_REPEAT) {
                Spacer(Modifier.height(12.dp))
                RepeatUnknownConfigurationCard(
                    threshold = vm.repeatUnknownThreshold,
                    minutes = vm.repeatUnknownWindowMinutes,
                    enabled = controlsEnabled,
                    onThreshold = vm::setRepeatUnknownThreshold,
                    onWindow = { showRepeatWindow = true },
                )
            }
            Spacer(Modifier.height(28.dp))
        }
    }

    if (showRepeatWindow) {
        RepeatCallerWindowSheet(
            selectedMinutes = vm.repeatUnknownWindowMinutes,
            onDismiss = { showRepeatWindow = false },
            onConfirm = { minutes -> vm.setRepeatUnknownWindow(minutes) },
        )
    }
    when (policyPicker) {
        GroupPolicyPicker.SAVED -> GroupPolicySheet(
            title = s.savedPolicyTitle,
            labels = listOf(s.savedPolicyFollowRules, s.savedPolicyAllow, s.savedPolicyBlock),
            descriptions = listOf(s.savedPolicyFollowRulesDesc, s.savedPolicyAllowDesc, s.savedPolicyBlockDesc),
            icons = listOf(Icons.AutoMirrored.Rounded.Rule, Icons.Rounded.Phone, Icons.Rounded.Block),
            selected = savedOptions.indexOf(vm.savedGroupPolicy).coerceAtLeast(0),
            onDismiss = { policyPicker = null },
            onSelect = { index -> savedOptions.getOrNull(index)?.let(vm::setSavedGroupPolicy) },
        )
        GroupPolicyPicker.UNKNOWN -> GroupPolicySheet(
            title = s.unknownPolicyTitle,
            labels = listOf(s.unknownPolicyPass, s.unknownPolicyBlockAlways, s.unknownPolicyBlockUntilRepeat),
            descriptions = listOf(s.unknownPolicyPassDesc, s.unknownPolicyBlockAlwaysDesc, s.unknownPolicyBlockUntilRepeatDesc),
            icons = listOf(Icons.Rounded.Phone, Icons.Rounded.Block, Icons.Rounded.Repeat),
            selected = unknownOptions.indexOf(vm.unknownGroupPolicy).coerceAtLeast(0),
            onDismiss = { policyPicker = null },
            onSelect = { index -> unknownOptions.getOrNull(index)?.let(vm::setUnknownGroupPolicy) },
        )
        null -> Unit
    }
}

@Composable
fun CallBlockAdvancedRulesScreen(
    vm: CallBlockViewModel,
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (Long) -> Unit,
) {
    val s = appStrings().blocker
    val rules = vm.advancedRules
    var target by remember { mutableStateOf<AdvancedRuleMenuTarget?>(null) }
    var bulkAction by remember { mutableStateOf<AdvancedRulesBulkAction?>(null) }
    val density = LocalDensity.current
    val topInset = WindowInsets.statusBars.getTop(density)
    val bottomInset = WindowInsets.navigationBars.getBottom(density)
    val navigationBottom = with(density) { bottomInset.toDp() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            BlockTopBar(title = s.advancedRulesScreenTitle, onBack = onBack)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                // Chỉ chừa vùng FAB khi cụm thao tác hàng loạt thực sự xuất hiện.
                contentPadding = PaddingValues(
                    bottom = navigationBottom + if (rules.isNotEmpty()) 96.dp else 24.dp,
                ),
            ) {
                item { AddAdvancedRuleCard(onCreate) }
                item {
                    Text(
                        s.advancedOrderNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    )
                }
                if (rules.isEmpty()) {
                    item { ArchitectureEmptyCard(s.advancedRulesEmpty) }
                } else {
                    items(rules, key = { it.id }) { rule ->
                        AdvancedRuleItem(
                            rule = rule,
                            activeInMenu = target?.rule?.id == rule.id,
                            onClick = { onEdit(rule.id) },
                            onLongPress = { bounds -> target = AdvancedRuleMenuTarget(rule, bounds) },
                        )
                    }
                }
            }
        }
        if (rules.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = navigationBottom + 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AdvancedRulesBulkFab(
                    icon = Icons.Rounded.DoneAll,
                    contentDescription = s.enableAllAdvancedRules,
                    background = AccentGreen,
                    enabled = rules.any { !it.enabled },
                    onClick = { bulkAction = AdvancedRulesBulkAction.ENABLE_ALL },
                )
                AdvancedRulesBulkFab(
                    icon = Icons.Rounded.PowerOff,
                    contentDescription = s.disableAllAdvancedRules,
                    background = Primary,
                    enabled = rules.any { it.enabled },
                    onClick = { bulkAction = AdvancedRulesBulkAction.DISABLE_ALL },
                )
                AdvancedRulesBulkFab(
                    icon = Icons.Rounded.DeleteSweep,
                    contentDescription = s.deleteAllAdvancedRules,
                    background = AccentRed,
                    enabled = true,
                    onClick = { bulkAction = AdvancedRulesBulkAction.DELETE_ALL },
                )
            }
        }
        target?.let { selected ->
            ContextMenuOverlay(
                bounds = selected.bounds,
                actions = listOf(
                    ContextAction(
                        ActionGlyph.Vector(Icons.Rounded.KeyboardArrowUp, Primary),
                        s.menuMoveRuleUp,
                    ) { vm.moveAdvancedRule(selected.rule.id, -1) },
                    ContextAction(
                        ActionGlyph.Vector(Icons.Rounded.KeyboardArrowDown, Primary),
                        s.menuMoveRuleDown,
                    ) { vm.moveAdvancedRule(selected.rule.id, 1) },
                    ContextAction(
                        ActionGlyph.Vector(
                            Icons.Rounded.PowerSettingsNew,
                            if (selected.rule.enabled) TextSecondary else AccentGreen,
                        ),
                        if (selected.rule.enabled) s.menuDisableRule else s.menuEnableRule,
                    ) { vm.setRuleEnabled(selected.rule.id, !selected.rule.enabled) },
                    ContextAction(
                        ActionGlyph.Vector(Icons.Rounded.Delete, AccentRed),
                        s.menuDeleteRule,
                    ) { vm.deleteRule(selected.rule.id) },
                ),
                topInsetPx = topInset,
                bottomInsetPx = bottomInset,
                onClosed = { target = null },
                lifted = { AdvancedRuleCard(selected.rule) },
            )
        }
    }

    bulkAction?.let { action ->
        val title = when (action) {
            AdvancedRulesBulkAction.ENABLE_ALL -> s.enableAllAdvancedRules
            AdvancedRulesBulkAction.DISABLE_ALL -> s.disableAllAdvancedRules
            AdvancedRulesBulkAction.DELETE_ALL -> s.deleteAllAdvancedRules
        }
        val message = when (action) {
            AdvancedRulesBulkAction.ENABLE_ALL -> s.enableAllAdvancedRulesMessage(rules.size)
            AdvancedRulesBulkAction.DISABLE_ALL -> s.disableAllAdvancedRulesMessage(rules.size)
            AdvancedRulesBulkAction.DELETE_ALL -> s.deleteAllAdvancedRulesMessage(rules.size)
        }
        val confirmColor = when (action) {
            AdvancedRulesBulkAction.ENABLE_ALL -> AccentGreen
            AdvancedRulesBulkAction.DISABLE_ALL -> Primary
            AdvancedRulesBulkAction.DELETE_ALL -> AccentRed
        }
        AppDialog(
            onDismissRequest = { bulkAction = null },
            title = title,
            buttons = listOf(
                DialogButton(appStrings().common.cancel, TextSecondary) { bulkAction = null },
                DialogButton(title, confirmColor, bold = true) {
                    when (action) {
                        AdvancedRulesBulkAction.ENABLE_ALL -> vm.setAllAdvancedRulesEnabled(true)
                        AdvancedRulesBulkAction.DISABLE_ALL -> vm.setAllAdvancedRulesEnabled(false)
                        AdvancedRulesBulkAction.DELETE_ALL -> vm.deleteAllAdvancedRules()
                    }
                    bulkAction = null
                },
            ),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun GroupPolicyCard(
    icon: ImageVector,
    title: String,
    selectedLabel: String,
    selectedDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    PanelCard(modifier = Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.48f), radius = 22.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    role = Role.Button
                    stateDescription = selectedLabel
                }
                .clickable(enabled = enabled, onClick = onClick)
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(BrandSoft), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.height(11.dp))
            Text(selectedLabel, style = MaterialTheme.typography.bodyLarge, color = Primary, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(selectedDescription, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Composable
private fun GroupPolicySheet(
    title: String,
    labels: List<String>,
    descriptions: List<String>,
    icons: List<ImageVector>,
    selected: Int,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    AppBottomSheet(
        onDismiss = onDismiss,
        title = title,
        sheetGesturesEnabled = false,
        showCloseButton = true,
    ) { close ->
        labels.forEachIndexed { index, label ->
            FilterOptionRow(
                icon = icons.getOrElse(index) { Icons.AutoMirrored.Rounded.Rule },
                label = label,
                supportingText = descriptions.getOrNull(index),
                selected = index == selected,
                onClick = {
                    onSelect(index)
                    close()
                },
            )
        }
    }
}

@Composable
private fun RepeatUnknownConfigurationCard(
    threshold: Int,
    minutes: Int,
    enabled: Boolean,
    onThreshold: (Int) -> Unit,
    onWindow: () -> Unit,
) {
    val s = appStrings().blocker
    val thresholds = listOf(2, 3, 4)
    PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(BrandSoft), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Repeat, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(s.repeatCallerExceptionTitle, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
            Text(s.repeatCallerThresholdTitle, style = MaterialTheme.typography.labelLarge, color = TextSecondary)
            Spacer(Modifier.height(7.dp))
            Segmented(
                labels = thresholds.map(Int::toString),
                semanticLabels = thresholds.map(s::repeatCallerThresholdOption),
                selected = thresholds.indexOf(threshold).coerceAtLeast(0),
                enabled = enabled,
                onSelect = { index -> thresholds.getOrNull(index)?.let(onThreshold) },
            )
            Spacer(Modifier.height(10.dp))
            FilterOptionRow(
                icon = Icons.Rounded.History,
                label = "${s.repeatCallerWindowTitle}: ${s.repeatCallerWindowValue(minutes)}",
                selected = false,
                modifier = Modifier.clip(RoundedCornerShape(14.dp)),
                enabled = enabled,
                onClick = onWindow,
            )
        }
    }
}

@Composable
private fun NumberSourceSheet(
    onDismiss: () -> Unit,
    onManual: () -> Unit,
    onContacts: () -> Unit,
    onHistory: () -> Unit,
    onCategories: () -> Unit,
) {
    val s = appStrings().blocker
    AppBottomSheet(onDismiss = onDismiss, title = s.addNumberSourceTitle, sheetGesturesEnabled = false, showCloseButton = true) { close ->
        FilterOptionRow(Icons.Rounded.Phone, s.sourceEnterManually, false, onClick = { close(); onManual() })
        FilterOptionRow(Icons.Rounded.Contacts, s.sourceFromContacts, false, onClick = { close(); onContacts() })
        FilterOptionRow(Icons.Rounded.History, s.sourceFromCallHistory, false, onClick = { close(); onHistory() })
        FilterOptionRow(Icons.Rounded.Category, s.sourceFromCategories, false, onClick = { close(); onCategories() })
    }
}

@Composable
private fun ManualNumberSheet(
    action: CallBlockAction,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit,
) {
    val s = appStrings().blocker
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var number by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    val valid = PhoneKey.of(number).length >= 3
    AppBottomSheet(onDismiss = onDismiss, title = s.enterNumberTitle, sheetGesturesEnabled = false, showCloseButton = true) { close ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) { detectTapGestures { focus.clearFocus(); keyboard?.hide() } }
                .padding(horizontal = 20.dp, vertical = 6.dp),
        ) {
            ManualField(
                value = number,
                hint = s.enterNumberHint,
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Next,
                onValueChange = { number = it.filter { char -> char.isDigit() || char in "+ -()." } },
                onDone = {},
            )
            Spacer(Modifier.height(10.dp))
            ManualField(
                value = name,
                hint = s.enterNumberNameHint,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
                onValueChange = { name = it.take(80) },
                onDone = { focus.clearFocus(force = true); keyboard?.hide() },
            )
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (valid) Primary else Primary.copy(alpha = 0.45f))
                    .clickable(enabled = valid) {
                        focus.clearFocus(force = true)
                        keyboard?.hide()
                        onSave(number.trim(), name.trim())
                        close()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (action == CallBlockAction.ALLOW) s.addToAllowlist else s.addToBlocklist,
                    style = MaterialTheme.typography.titleSmall,
                    color = androidx.compose.ui.graphics.Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ManualField(
    value: String,
    hint: String,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(FieldSurface).padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        if (value.isEmpty()) Text(hint, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
            cursorBrush = SolidColor(Primary),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = hint },
        )
    }
}

@Composable
private fun AddNumberCard(onClick: () -> Unit) = AddArchitectureCard(appStrings().blocker.addNumber, Icons.Rounded.Add, onClick)

@Composable
private fun AddAdvancedRuleCard(onClick: () -> Unit) = AddArchitectureCard(appStrings().blocker.addAdvancedRule, Icons.Rounded.Add, onClick)

/** FAB thao tác hàng loạt; trạng thái không còn gì để đổi vẫn được giữ lại nhưng làm mờ và khoá chạm. */
@Composable
private fun AdvancedRulesBulkFab(
    icon: ImageVector,
    contentDescription: String,
    background: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .alpha(if (enabled) 1f else 0.42f)
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(27.dp),
        )
    }
}

@Composable
private fun AddArchitectureCard(label: String, icon: ImageVector, onClick: () -> Unit) {
    PanelCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), radius = 18.dp) {
        Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(BrandSoft), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(25.dp))
            }
            Spacer(Modifier.width(13.dp))
            Text(label, style = MaterialTheme.typography.titleMedium, color = Primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ArchitectureEmptyCard(text: String) {
    PanelCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), radius = 18.dp) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.padding(20.dp))
    }
}

@Composable
private fun NumberEntryItem(entry: CallBlockNumberEntry, activeInMenu: Boolean, onLongPress: (Rect) -> Unit) {
    val coords = remember { ArchitectureCoords() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 7.dp)
            .onGloballyPositioned { coords.value = it }
            .graphicsLayer { alpha = if (activeInMenu) 0f else if (entry.enabled) 1f else 0.48f },
    ) {
        NumberEntryCard(
            entry = entry,
            onLongClick = { coords.value?.takeIf { it.isAttached }?.boundsInWindow()?.let(onLongPress) },
        )
    }
}

@Composable
private fun NumberEntryCard(entry: CallBlockNumberEntry, onLongClick: (() -> Unit)? = null) {
    val allow = entry.action == CallBlockAction.ALLOW
    val s = appStrings().blocker
    PanelCard(modifier = Modifier.fillMaxWidth(), radius = 18.dp) {
        val interaction = remember { MutableInteractionSource() }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = interaction,
                    indication = rememberPressHighlight(),
                    onClick = {},
                    onLongClick = onLongClick,
                )
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(if (allow) AccentGreenBg else AccentRedBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (allow) Icons.Rounded.Check else Icons.Rounded.Block, contentDescription = null, tint = if (allow) AccentGreen else AccentRed, modifier = Modifier.size(23.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.displayName.ifBlank { formatPhone(entry.rawNumber) }, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (entry.displayName.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(formatPhone(entry.rawNumber), style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1)
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = s.numberAddedAt(TimeFormat.fullDateTimeWithSeconds(entry.createdAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun AdvancedRuleItem(rule: CallBlockRule, activeInMenu: Boolean, onClick: () -> Unit, onLongPress: (Rect) -> Unit) {
    val coords = remember { ArchitectureCoords() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 7.dp)
            .onGloballyPositioned { coords.value = it }
            .graphicsLayer { alpha = if (activeInMenu) 0f else if (rule.enabled) 1f else 0.48f },
    ) {
        AdvancedRuleCard(
            rule = rule,
            onClick = onClick,
            onLongClick = { coords.value?.takeIf { it.isAttached }?.boundsInWindow()?.let(onLongPress) },
        )
    }
}

@Composable
private fun AdvancedRuleCard(rule: CallBlockRule, onClick: (() -> Unit)? = null, onLongClick: (() -> Unit)? = null) {
    val s = appStrings().blocker
    val allow = rule.action == CallBlockAction.ALLOW
    PanelCard(modifier = Modifier.fillMaxWidth(), radius = 18.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberPressHighlight(),
                    onClick = onClick ?: {},
                    onLongClick = onLongClick,
                )
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(if (allow) AccentGreenBg else AccentRedBg), contentAlignment = Alignment.Center) {
                Icon(if (allow) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.Rule, contentDescription = null, tint = if (allow) AccentGreen else AccentRed, modifier = Modifier.size(23.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(s.ruleSummary(rule.type.storageKey, rule.rawValue), style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                Text("${if (allow) s.actionAllow else s.actionBlock} · ${s.ruleScopeSummary(rule.scope.storageKey)}", style = MaterialTheme.typography.bodySmall, color = if (allow) AccentGreen else AccentRed, maxLines = 1)
            }
        }
    }
}

private class ArchitectureCoords(var value: LayoutCoordinates? = null)

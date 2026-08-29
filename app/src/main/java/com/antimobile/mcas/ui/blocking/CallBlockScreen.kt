package com.antimobile.mcas.ui.blocking

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.PhoneDisabled
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.antimobile.mcas.data.blocking.BlockNotificationMode
import com.antimobile.mcas.data.blocking.BlockedCallHistory
import com.antimobile.mcas.data.blocking.CallBlockAction
import com.antimobile.mcas.data.blocking.CallBlockMethod
import com.antimobile.mcas.data.blocking.CallBlockNotifier
import com.antimobile.mcas.data.blocking.CallBlockPauseDuration
import com.antimobile.mcas.data.blocking.CallBlockDailySchedule
import com.antimobile.mcas.data.blocking.CallBlockScheduleAction
import com.antimobile.mcas.data.blocking.CallBlockScheduleUpdate
import com.antimobile.mcas.data.blocking.CallBlockProtectionState
import com.antimobile.mcas.data.blocking.CallBlockRule
import com.antimobile.mcas.data.blocking.CallBlockRuleType
import com.antimobile.mcas.data.blocking.CallBlockSettings
import com.antimobile.mcas.data.blocking.CallBlockTimeWindow
import com.antimobile.mcas.data.blocking.CallScreeningRole
import com.antimobile.mcas.data.blocking.CallHistoryRuleCodec
import com.antimobile.mcas.data.blocking.NumberEntryOrigin
import com.antimobile.mcas.data.blocking.REPEAT_UNKNOWN_CALLER_GUARD_REASON_TYPE
import com.antimobile.mcas.data.blocking.RepeatUnknownCallerGuardReasonCodec
import com.antimobile.mcas.data.blocking.SpamRiskReasonCodec
import com.antimobile.mcas.data.blocking.SpamRiskReasonKind
import com.antimobile.mcas.i18n.CallBlockStrings
import com.antimobile.mcas.i18n.appStrings
import com.antimobile.mcas.ui.calllist.DayFilterSheet
import com.antimobile.mcas.ui.components.ActionGlyph
import com.antimobile.mcas.ui.components.AppBottomSheet
import com.antimobile.mcas.ui.components.ContextAction
import com.antimobile.mcas.ui.components.ContextCardAction
import com.antimobile.mcas.ui.components.ContextMenuOverlay
import com.antimobile.mcas.ui.components.FilterOptionRow
import com.antimobile.mcas.ui.components.FrostedScrollButton
import com.antimobile.mcas.ui.components.GearIndexBar
import com.antimobile.mcas.ui.components.GearIndexItem
import com.antimobile.mcas.ui.components.PanelCard
import com.antimobile.mcas.ui.components.Segmented
import com.antimobile.mcas.ui.components.rememberPressHighlight
import com.antimobile.mcas.ui.theme.AccentGreen
import com.antimobile.mcas.ui.theme.AccentGreenBg
import com.antimobile.mcas.ui.theme.AccentBlue
import com.antimobile.mcas.ui.theme.AccentRed
import com.antimobile.mcas.ui.theme.AccentRedBg
import com.antimobile.mcas.ui.theme.AppBackground
import com.antimobile.mcas.ui.theme.BrandSoft
import com.antimobile.mcas.ui.theme.CardFill
import com.antimobile.mcas.ui.theme.FieldSurface
import com.antimobile.mcas.ui.theme.Primary
import com.antimobile.mcas.ui.theme.TextPrimary
import com.antimobile.mcas.ui.theme.TextSecondary
import com.antimobile.mcas.util.TimeFormat
import com.antimobile.mcas.util.DayPart
import com.antimobile.mcas.util.StatsPeriod
import com.antimobile.mcas.util.formatPhone
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private data class RuleMenuTarget(val rule: CallBlockRule, val bounds: Rect)
private data class HistoryMenuTarget(val row: BlockedCallHistory, val bounds: Rect)
private data class HistoryDateScrollTarget(val date: LocalDate, val itemIndex: Int)
private data class FeatureMenuTarget(val feature: MainBlockFeature, val bounds: Rect)
private data class ScheduleMenuTarget(
    val window: CallBlockTimeWindow,
    val active: Boolean,
    val bounds: Rect,
)

private enum class MainBlockFeature {
    ALWAYS_ALLOW,
    BLOCKED_NUMBERS,
    GROUP_BLOCKING,
    ADVANCED_RULES,
}

/**
 * Màn chặn cuộc gọi. Role của Android là cổng đầu tiên: chưa được chọn làm Call Screening app
 * thì chỉ hiển thị màn kích hoạt, tránh tạo cảm giác các quy tắc đã có hiệu lực khi thực tế chưa có.
 */
@Composable
fun CallBlockScreen(
    vm: CallBlockViewModel,
    notificationUiState: CallBlockNotificationUiState,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAllowlist: () -> Unit,
    onOpenBlocklist: () -> Unit,
    onOpenGroups: () -> Unit,
    onOpenAdvancedRules: () -> Unit,
    onOpenCommonIssues: () -> Unit,
    onOpenNumber: (String) -> Unit,
) {
    val context = LocalContext.current
    val s = appStrings().blocker
    var roleHeld by remember { mutableStateOf(CallScreeningRole.isHeld(context)) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        roleHeld = CallScreeningRole.isHeld(context)
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        roleHeld = CallScreeningRole.isHeld(context)
        nowMillis = System.currentTimeMillis()
        CallBlockSettings.refresh(context, nowMillis)
        CallBlockSettings.refreshDailySchedule(context)
    }
    LaunchedEffect(Unit) { CallBlockSettings.init(context) }
    LaunchedEffect(
        CallBlockSettings.baseEnabled,
        CallBlockSettings.pauseUntilMillis,
        CallBlockSettings.dailySchedule,
    ) {
        while (true) {
            val current = System.currentTimeMillis()
            nowMillis = current
            val refreshedState = CallBlockSettings.refresh(context, current)
            val remainingMillis = refreshedState.remainingPauseMillisAt(current)
            delay(
                if (remainingMillis > 0L) minOf(1_000L, remainingMillis)
                else millisUntilNextClockMinute(current)
            )
        }
    }
    LaunchedEffect(
        roleHeld,
        notificationUiState.readiness,
        notificationUiState.permissionAskedThisSession,
        CallBlockSettings.notificationMode,
    ) {
        if (roleHeld && CallBlockSettings.notificationMode != BlockNotificationMode.OFF) {
            notificationUiState.requestFirstPermission()
        }
    }

    if (!roleHeld) {
        CallBlockRoleGate(
            onBack = onBack,
            onEnable = {
                CallScreeningRole.requestIntent(context)?.let(roleLauncher::launch)
            },
        )
        return
    }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var ruleTarget by remember { mutableStateOf<RuleMenuTarget?>(null) }
    var historyTarget by remember { mutableStateOf<HistoryMenuTarget?>(null) }
    var featureMenuTarget by remember { mutableStateOf<FeatureMenuTarget?>(null) }
    var pendingFeatureInfo by remember { mutableStateOf<MainBlockFeature?>(null) }
    var featureInfo by remember { mutableStateOf<MainBlockFeature?>(null) }
    var showProcessingGuide by remember { mutableStateOf(false) }
    val protectionState = CallBlockSettings.protectionState
    val protectionBaseEnabled = protectionState.baseEnabled
    val activePauseWindow = activePauseWindow(protectionState, nowMillis)
    val scheduledAction = if (activePauseWindow == null) {
        CallBlockDailySchedule.actionAt(CallBlockSettings.dailySchedule, nowMillis)
    } else {
        null
    }
    val scheduledPauseWindow = if (scheduledAction == CallBlockScheduleAction.PAUSE) {
        val minute = CallBlockDailySchedule.minuteOfDay(nowMillis)
        val day = CallBlockDailySchedule.dayOfWeek(nowMillis)
        CallBlockSettings.dailySchedule.firstOrNull { it.isActiveAt(day, minute) }
    } else {
        null
    }
    val protectionEffective = CallBlockSettings.isEffectivelyEnabledAt(nowMillis)
    val density = LocalDensity.current
    val topInset = WindowInsets.statusBars.getTop(density)
    val bottomInset = WindowInsets.navigationBars.getBottom(density)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
            BlockTopBar(
                title = s.screenTitle,
                onBack = onBack,
                statusActionIcon = Icons.Rounded.PowerSettingsNew,
                statusActionContentDescription = if (scheduledAction != null) {
                    s.openSettings
                } else if (protectionEffective) {
                    s.disableProtectionAction
                } else {
                    s.enableProtectionAction
                },
                statusActionTint = if (protectionEffective) Primary else AccentRed,
                onStatusAction = {
                    if (scheduledAction != null) {
                        onOpenSettings()
                    } else {
                        val clickedAt = System.currentTimeMillis()
                        nowMillis = clickedAt
                        // setEnabled(true) cũng huỷ một phiên tạm dừng, nên màu xanh luôn đồng nghĩa đang bảo vệ thật.
                        CallBlockSettings.setEnabled(context, !protectionEffective, clickedAt)
                    }
                },
                settingsContentDescription = s.openSettings,
                onOpenSettings = onOpenSettings,
            )
            Segmented(
                labels = listOf(s.tabRules, s.tabHistory(vm.history.size)),
                selected = tab,
                onSelect = { tab = it },
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 6.dp),
            )
            if (!protectionEffective) {
                ProtectionStatusBanner(
                    permanentOff = !protectionBaseEnabled && scheduledPauseWindow == null,
                    activePauseWindow = activePauseWindow,
                    scheduledPauseWindow = scheduledPauseWindow,
                    nowMillis = nowMillis,
                )
            }
            if (tab == 0) {
                RulesTab(
                    rules = vm.rules,
                    allowCount = vm.allowlist.size,
                    blockCount = vm.blocklist.size,
                    rulesDimmed = !protectionEffective,
                    activeMenuFeature = featureMenuTarget?.feature,
                    onOpenAllowlist = onOpenAllowlist,
                    onOpenBlocklist = onOpenBlocklist,
                    onOpenGroups = onOpenGroups,
                    onOpenAdvancedRules = onOpenAdvancedRules,
                    onOpenCommonIssues = onOpenCommonIssues,
                    onOpenProcessingGuide = { showProcessingGuide = true },
                    onFeatureLongPress = { feature, bounds ->
                        featureMenuTarget = FeatureMenuTarget(feature, bounds)
                    },
                    modifier = Modifier.weight(1f),
                )
            } else {
                HistoryTab(
                    rows = vm.history,
                    activeMenuHistoryId = historyTarget?.row?.id,
                    onOpenNumber = onOpenNumber,
                    onLongPress = { row, bounds -> historyTarget = HistoryMenuTarget(row, bounds) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        featureMenuTarget?.let { target ->
            ContextMenuOverlay(
                bounds = target.bounds,
                actions = listOf(
                    ContextAction(
                        glyph = ActionGlyph.Vector(Icons.Outlined.Info, Primary),
                        desc = s.featureDetailsAction,
                        onClick = { pendingFeatureInfo = target.feature },
                    )
                ),
                topInsetPx = topInset,
                bottomInsetPx = bottomInset,
                onClosed = {
                    featureMenuTarget = null
                    featureInfo = pendingFeatureInfo
                    pendingFeatureInfo = null
                },
                lifted = {
                    BlockingArchitectureCard(
                        feature = target.feature,
                        count = blockFeatureCount(
                            feature = target.feature,
                            allowCount = vm.allowlist.size,
                            blockCount = vm.blocklist.size,
                            rules = vm.rules,
                            s = s,
                        ),
                        onClick = null,
                        onLongClick = null,
                    )
                },
            )
        }
        ruleTarget?.let { target ->
            ContextMenuOverlay(
                bounds = target.bounds,
                actions = listOf(
                    ContextAction(
                        ActionGlyph.Vector(Icons.Rounded.Delete, AccentRed),
                        s.menuDeleteRule,
                    ) { vm.deleteRule(target.rule.id) }
                ),
                topInsetPx = topInset,
                bottomInsetPx = bottomInset,
                onClosed = { ruleTarget = null },
                lifted = {
                    CallBlockRuleCard(
                        rule = target.rule,
                        onClick = null,
                        onLongClick = null,
                    )
                },
            )
        }
        historyTarget?.let { target ->
            ContextMenuOverlay(
                bounds = target.bounds,
                actions = emptyList(),
                cardActions = buildList {
                    add(
                        ContextCardAction(
                            icon = Icons.Rounded.Delete,
                            label = s.menuDeleteHistory,
                            onClick = { vm.deleteHistory(target.row.id) },
                        )
                    )
                    if (CallHistoryRuleCodec.isSelectableNumber(target.row.rawNumber)) {
                        add(
                            ContextCardAction(
                                icon = Icons.Rounded.Phone,
                                label = s.addToAllowlist,
                                onClick = {
                                    vm.saveNumberEntry(
                                        action = CallBlockAction.ALLOW,
                                        rawNumber = target.row.rawNumber,
                                        displayName = "",
                                        origin = NumberEntryOrigin.CALL_LOG_PICKER,
                                    )
                                },
                            )
                        )
                        add(
                            ContextCardAction(
                                icon = Icons.Rounded.Block,
                                label = s.addToBlocklist,
                                onClick = {
                                    vm.saveNumberEntry(
                                        action = CallBlockAction.BLOCK,
                                        rawNumber = target.row.rawNumber,
                                        displayName = "",
                                        origin = NumberEntryOrigin.CALL_LOG_PICKER,
                                    )
                                },
                            )
                        )
                    }
                },
                topInsetPx = topInset,
                bottomInsetPx = bottomInset,
                onClosed = { historyTarget = null },
                lifted = {
                    BlockedCallCard(
                        row = target.row,
                        onClick = null,
                        onLongClick = null,
                    )
                },
            )
        }
    }

    featureInfo?.let { feature ->
        MainBlockFeatureInfoSheet(
            feature = feature,
            onDismiss = { featureInfo = null },
        )
    }
    if (showProcessingGuide) {
        CallBlockProcessingGuideSheet(onDismiss = { showProcessingGuide = false })
    }
}

/**
 * Màn cấu hình riêng của mô-đun chặn cuộc gọi. Màn danh sách chỉ điều hướng tới đây;
 * toàn bộ quyền notification, sheet chọn và trạng thái cấu hình được giữ cùng nhau tại màn này.
 */
@Composable
fun CallBlockSettingsScreen(
    notificationUiState: CallBlockNotificationUiState,
    onBack: () -> Unit,
    onOpenAdvancedNotifications: () -> Unit,
) {
    val context = LocalContext.current
    val s = appStrings().blocker
    var roleHeld by remember { mutableStateOf(CallScreeningRole.isHeld(context)) }
    var showBlockMethods by remember { mutableStateOf(false) }
    var showNotificationModes by remember { mutableStateOf(false) }
    var editingScheduleWindow by remember { mutableStateOf<CallBlockTimeWindow?>(null) }
    var showScheduleEditor by remember { mutableStateOf(false) }
    var scheduleMenuTarget by remember { mutableStateOf<ScheduleMenuTarget?>(null) }
    var pendingCreatedScheduleId by remember { mutableStateOf<String?>(null) }
    var highlightedScheduleId by remember { mutableStateOf<String?>(null) }
    var highlightedScheduleBounds by remember { mutableStateOf<Rect?>(null) }
    var settingsViewportBounds by remember { mutableStateOf<Rect?>(null) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val settingsScrollState = rememberScrollState()
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        roleHeld = CallScreeningRole.isHeld(context)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        roleHeld = CallScreeningRole.isHeld(context)
        nowMillis = System.currentTimeMillis()
        CallBlockSettings.refresh(context, nowMillis)
        CallBlockSettings.refreshDailySchedule(context)
    }
    LaunchedEffect(Unit) { CallBlockSettings.init(context) }
    LaunchedEffect(
        CallBlockSettings.baseEnabled,
        CallBlockSettings.pauseUntilMillis,
        CallBlockSettings.dailySchedule,
    ) {
        while (true) {
            val current = System.currentTimeMillis()
            nowMillis = current
            val refreshedState = CallBlockSettings.refresh(context, current)
            val remainingMillis = refreshedState.remainingPauseMillisAt(current)
            delay(
                if (remainingMillis > 0L) minOf(1_000L, remainingMillis)
                else millisUntilNextClockMinute(current)
            )
        }
    }
    LaunchedEffect(
        roleHeld,
        notificationUiState.readiness,
        notificationUiState.permissionAskedThisSession,
        CallBlockSettings.notificationMode,
    ) {
        if (roleHeld && CallBlockSettings.notificationMode != BlockNotificationMode.OFF) {
            notificationUiState.requestFirstPermission()
        }
    }

    if (!roleHeld) {
        CallBlockRoleGate(
            onBack = onBack,
            onEnable = {
                CallScreeningRole.requestIntent(context)?.let(roleLauncher::launch)
            },
        )
        return
    }

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val density = LocalDensity.current
    val topInset = WindowInsets.statusBars.getTop(density)
    val bottomInset = WindowInsets.navigationBars.getBottom(density)
    LaunchedEffect(highlightedScheduleId) {
        val targetId = highlightedScheduleId ?: return@LaunchedEffect
        val positioned = snapshotFlow {
            highlightedScheduleBounds to settingsViewportBounds
        }.first { (item, viewport) -> item != null && viewport != null }
        val itemBounds = requireNotNull(positioned.first)
        val viewportBounds = requireNotNull(positioned.second)
        val scrollDelta = itemBounds.center.y - viewportBounds.center.y
        if (scrollDelta > 1f || scrollDelta < -1f) {
            settingsScrollState.animateScrollBy(scrollDelta)
        }
        delay(2_000L)
        if (highlightedScheduleId == targetId) {
            highlightedScheduleId = null
            highlightedScheduleBounds = null
        }
    }
    Box(
        modifier = Modifier.fillMaxSize().background(AppBackground),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .statusBarsPadding(),
        ) {
            BlockTopBar(
                title = s.settingsScreenTitle,
                onBack = onBack,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .onGloballyPositioned { settingsViewportBounds = it.boundsInWindow() },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(settingsScrollState)
                        .padding(top = 8.dp, bottom = navBottom + 24.dp),
                ) {
                    CallBlockOverview(
                        blockMethod = CallBlockSettings.blockMethod,
                        onBlockMethodClick = { showBlockMethods = true },
                        notificationStatus = notificationUiState.readiness,
                        onNotificationClick = { showNotificationModes = true },
                        onFixNotifications = notificationUiState::repair,
                        onOpenAdvancedNotifications = onOpenAdvancedNotifications,
                        nowMillis = nowMillis,
                        onPauseTimerSelect = { index ->
                            val current = System.currentTimeMillis()
                            when (index) {
                                0 -> CallBlockSettings.clearPause(context, current)
                                1 -> CallBlockSettings.pause(context, CallBlockPauseDuration.MINUTES_10, current)
                                2 -> CallBlockSettings.pause(context, CallBlockPauseDuration.MINUTES_30, current)
                                3 -> CallBlockSettings.pause(context, CallBlockPauseDuration.MINUTES_60, current)
                            }
                            nowMillis = current
                        },
                        activeMenuScheduleId = scheduleMenuTarget?.window?.id,
                        highlightedScheduleId = highlightedScheduleId,
                        onAddSchedule = {
                            pendingCreatedScheduleId = null
                            editingScheduleWindow = null
                            showScheduleEditor = true
                        },
                        onEditSchedule = { window ->
                            pendingCreatedScheduleId = null
                            editingScheduleWindow = window
                            showScheduleEditor = true
                        },
                        onScheduleLongPress = { window, active, bounds ->
                            scheduleMenuTarget = ScheduleMenuTarget(window, active, bounds)
                        },
                        onHighlightedSchedulePositioned = { id, bounds ->
                            if (id == highlightedScheduleId && highlightedScheduleBounds != bounds) {
                                highlightedScheduleBounds = bounds
                            }
                        },
                    )
                }
            }
        }

        scheduleMenuTarget?.let { target ->
            ContextMenuOverlay(
                bounds = target.bounds,
                actions = listOf(
                    ContextAction(
                        ActionGlyph.Vector(Icons.Rounded.Delete, AccentRed),
                        s.dailyScheduleDelete,
                    ) {
                        CallBlockSettings.removeDailyWindow(context, target.window.id)
                    }
                ),
                topInsetPx = topInset,
                bottomInsetPx = bottomInset,
                onClosed = { scheduleMenuTarget = null },
                lifted = {
                    CallBlockDailyScheduleLiftedRow(
                        window = target.window,
                        active = target.active,
                    )
                },
            )
        }
    }

    if (showBlockMethods) {
        BlockMethodSheet(
            selected = CallBlockSettings.blockMethod,
            onDismiss = { showBlockMethods = false },
            onPick = { method -> CallBlockSettings.setBlockMethod(context, method) },
        )
    }
    if (showNotificationModes) {
        NotificationModeSheet(
            notificationStatus = notificationUiState.readiness,
            onDismiss = { showNotificationModes = false },
            onPick = { mode ->
                CallBlockSettings.setNotificationMode(context, mode)
                if (
                    mode != BlockNotificationMode.OFF &&
                    notificationUiState.readiness != CallBlockNotifier.Readiness.READY
                ) {
                    notificationUiState.repair()
                }
            },
        )
    }
    if (showScheduleEditor) {
        CallBlockScheduleEditorSheet(
            existing = editingScheduleWindow,
            onDismiss = {
                showScheduleEditor = false
                pendingCreatedScheduleId?.let { createdId ->
                    highlightedScheduleBounds = null
                    highlightedScheduleId = createdId
                    pendingCreatedScheduleId = null
                }
            },
            onSave = { window ->
                CallBlockSettings.upsertDailyWindow(context, window).also { result ->
                    if (
                        editingScheduleWindow == null &&
                        result is CallBlockScheduleUpdate.Success
                    ) {
                        pendingCreatedScheduleId = window.id
                    }
                }
            },
        )
    }
}

@Composable
private fun CallBlockRoleGate(onBack: () -> Unit, onEnable: () -> Unit) {
    val s = appStrings().blocker
    val context = LocalContext.current
    val available = CallScreeningRole.isAvailable(context)
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .statusBarsPadding(),
    ) {
        BlockTopBar(title = s.screenTitle, onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = navBottom + 24.dp),
        ) {
            PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Box(
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(17.dp)).background(BrandSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Block, contentDescription = null, tint = Primary, modifier = Modifier.size(30.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (available) s.roleTitle else s.roleUnavailableTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (available) s.roleBody else s.roleUnavailableBody,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                    if (available) {
                        Spacer(Modifier.height(20.dp))
                        PrimaryWideButton(text = s.roleAction, onClick = onEnable)
                    }
                }
            }
        }
    }
}

@Composable
internal fun BlockTopBar(
    title: String,
    onBack: () -> Unit,
    statusActionIcon: ImageVector? = null,
    statusActionContentDescription: String? = null,
    statusActionTint: Color = TextSecondary,
    onStatusAction: (() -> Unit)? = null,
    settingsContentDescription: String? = null,
    onOpenSettings: (() -> Unit)? = null,
) {
    val common = appStrings().common
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, common.back, tint = TextPrimary, modifier = Modifier.size(23.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (statusActionIcon != null && onStatusAction != null) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onStatusAction),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = statusActionIcon,
                    contentDescription = statusActionContentDescription,
                    tint = statusActionTint,
                    modifier = Modifier.size(25.dp),
                )
            }
        }
        if (onOpenSettings != null) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onOpenSettings),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = settingsContentDescription,
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

@Composable
private fun CallBlockOverview(
    blockMethod: CallBlockMethod,
    onBlockMethodClick: () -> Unit,
    notificationStatus: CallBlockNotifier.Readiness,
    onNotificationClick: () -> Unit,
    onFixNotifications: () -> Unit,
    onOpenAdvancedNotifications: () -> Unit,
    nowMillis: Long,
    onPauseTimerSelect: (Int) -> Unit,
    activeMenuScheduleId: String?,
    highlightedScheduleId: String?,
    onAddSchedule: () -> Unit,
    onEditSchedule: (CallBlockTimeWindow) -> Unit,
    onScheduleLongPress: (CallBlockTimeWindow, Boolean, Rect) -> Unit,
    onHighlightedSchedulePositioned: (String, Rect) -> Unit,
) {
    val s = appStrings().blocker
    val context = LocalContext.current
    val protectionState = CallBlockSettings.protectionState
    val protectionEnabled = protectionState.baseEnabled
    val pauseStartedAtMillis = protectionState.pauseStartedAtMillis
    val pauseUntilMillis = protectionState.pauseUntilMillis
    val activePauseWindow = activePauseWindow(protectionState, nowMillis)
    val pauseActive = activePauseWindow != null
    val scheduledAction = if (pauseActive) null else {
        CallBlockDailySchedule.actionAt(CallBlockSettings.dailySchedule, nowMillis)
    }
    val pauseIndex = pauseSegmentIndex(
        active = pauseActive,
        startedAtMillis = pauseStartedAtMillis ?: 0L,
        untilMillis = pauseUntilMillis ?: 0L,
    )
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = protectionEnabled,
                            role = Role.Switch,
                            onValueChange = { CallBlockSettings.setEnabled(context, it) },
                        )
                        .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(BrandSoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Phone, contentDescription = null, tint = Primary, modifier = Modifier.size(25.dp))
                        }
                        Spacer(Modifier.width(13.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(s.protectionTitle, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                when {
                                    pauseActive -> s.pauseActive
                                    scheduledAction == CallBlockScheduleAction.BLOCK -> s.dailyScheduleBlockActive
                                    scheduledAction == CallBlockScheduleAction.PAUSE -> s.dailySchedulePauseActive
                                    !protectionEnabled -> s.protectionOff
                                    else -> s.protectionOn
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = when {
                                    pauseActive -> Primary
                                    scheduledAction == CallBlockScheduleAction.BLOCK -> AccentRed
                                    scheduledAction == CallBlockScheduleAction.PAUSE -> AccentBlue
                                    !protectionEnabled -> AccentRed
                                    else -> AccentGreen
                                },
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = protectionEnabled,
                            onCheckedChange = null,
                            modifier = Modifier.clearAndSetSemantics { },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Primary,
                                checkedBorderColor = Primary,
                            ),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = s.roleActive,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
                    Text(
                        s.pauseTimerTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(7.dp))
                    Segmented(
                        labels = listOf(
                            s.pauseTimerOff,
                            s.pauseTimer10Minutes,
                            s.pauseTimer30Minutes,
                            s.pauseTimer1Hour,
                        ),
                        selected = pauseIndex,
                        onSelect = onPauseTimerSelect,
                        enabled = protectionEnabled,
                    )
                    if (!protectionEnabled) {
                        Spacer(Modifier.height(7.dp))
                        Text(
                            s.pauseUnavailableWhileOff,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                    activePauseWindow?.let { (startedAtMillis, untilMillis) ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            s.pausePeriod(
                                TimeFormat.dayClock(startedAtMillis),
                                TimeFormat.dayClock(untilMillis),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            s.pauseRemaining(
                                countdownClock(untilMillis, nowMillis),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = Primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        CallBlockDailyScheduleCard(
            nowMillis = nowMillis,
            activeMenuWindowId = activeMenuScheduleId,
            highlightedWindowId = highlightedScheduleId,
            onAdd = onAddSchedule,
            onEdit = onEditSchedule,
            onLongPress = onScheduleLongPress,
            onHighlightedWindowPositioned = onHighlightedSchedulePositioned,
        )
        Spacer(Modifier.height(10.dp))
        PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onBlockMethodClick)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(BrandSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.PhoneDisabled, contentDescription = null, tint = Primary, modifier = Modifier.size(25.dp))
                }
                Spacer(Modifier.width(13.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(s.blockMethodTitle, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        blockMethodLabel(blockMethod, s),
                        style = MaterialTheme.typography.bodySmall,
                        color = Primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onNotificationClick)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(BrandSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Notifications, contentDescription = null, tint = Primary, modifier = Modifier.size(25.dp))
                    }
                    Spacer(Modifier.width(13.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(s.notificationTitleSetting, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(2.dp))
                        Text(notificationModeLabel(CallBlockSettings.notificationMode, s), style = MaterialTheme.typography.bodySmall, color = Primary, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (
                    notificationStatus != CallBlockNotifier.Readiness.READY &&
                    CallBlockSettings.notificationMode != BlockNotificationMode.OFF
                ) {
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val permissionMissing = notificationStatus ==
                            CallBlockNotifier.Readiness.RUNTIME_PERMISSION_REQUIRED
                        Text(
                            if (permissionMissing) s.notificationPermissionNeeded
                            else s.notificationChannelNeedsAttention,
                            style = MaterialTheme.typography.bodySmall,
                            color = AccentRed,
                            modifier = Modifier.weight(1f),
                        )
                        Box(
                            modifier = Modifier
                                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onFixNotifications),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                if (permissionMissing) s.notificationPermissionAction
                                else s.notificationChannelSettingsAction,
                                style = MaterialTheme.typography.labelLarge,
                                color = Primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp),
                            )
                        }
                    }
                }
            }
        }
        AnimatedVisibility(visible = CallBlockSettings.notificationMode == BlockNotificationMode.EVERY_BLOCK) {
            Column {
                Spacer(Modifier.height(10.dp))
                PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onOpenAdvancedNotifications)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(BrandSoft),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Tune, contentDescription = null, tint = Primary, modifier = Modifier.size(25.dp))
                        }
                        Spacer(Modifier.width(13.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                advancedNotificationItemTitle(),
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                advancedNotificationItemSubtitle(),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun pauseSegmentIndex(
    active: Boolean,
    startedAtMillis: Long,
    untilMillis: Long,
): Int {
    if (!active) return 0
    val durationMillis = (untilMillis - startedAtMillis).coerceAtLeast(0L)
    val presetIndex = CallBlockPauseDuration.entries.indexOfFirst { preset ->
        preset.durationMillis == durationMillis
    }
    return if (presetIndex >= 0) presetIndex + 1 else 0
}

private fun countdownClock(untilMillis: Long, nowMillis: Long): String {
    val totalSeconds = ((untilMillis - nowMillis).coerceAtLeast(0L) + 999L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = totalSeconds % 3_600L / 60L
    val seconds = totalSeconds % 60L
    return listOf(hours, minutes, seconds).joinToString(":") { value ->
        value.toString().padStart(2, '0')
    }
}

private fun millisUntilNextClockMinute(nowMillis: Long): Long {
    val remainder = nowMillis % 60_000L
    return if (remainder < 0L) 1_000L else (60_000L - remainder).coerceAtLeast(1_000L)
}

@Composable
internal fun RepeatCallerWindowSheet(
    selectedMinutes: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Boolean,
) {
    val s = appStrings().blocker
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var draft by rememberSaveable(selectedMinutes) { mutableStateOf(selectedMinutes.toString()) }
    var rejectedBySettings by remember { mutableStateOf(false) }
    var inputBounds by remember { mutableStateOf<Rect?>(null) }
    val value = draft.toIntOrNull()
    val valid = value != null && CallBlockSettings.isValidRepeatUnknownCallerGuardWindowMinutes(value)
    val showValidation = rejectedBySettings || (draft.isNotEmpty() && !valid)

    AppBottomSheet(
        onDismiss = onDismiss,
        title = s.repeatCallerWindowSheetTitle,
        sheetGesturesEnabled = false,
        showCloseButton = true,
    ) { close ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .pointerInput(inputBounds) {
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(
                                requireUnconsumed = false,
                                pass = PointerEventPass.Initial,
                            )
                            val bounds = inputBounds
                            if (bounds != null && !bounds.contains(down.position)) {
                                focus.clearFocus(force = true)
                                keyboard?.hide()
                            }
                        }
                    }
                }
                .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 10.dp),
        ) {
            Text(
                s.repeatCallerWindowTitle,
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(7.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .onGloballyPositioned { inputBounds = it.boundsInParent() }
                    .clip(RoundedCornerShape(14.dp))
                    .background(FieldSurface)
                    .padding(horizontal = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (draft.isEmpty()) {
                    Text(
                        s.repeatCallerWindowHint(
                            CallBlockSettings.MIN_REPEAT_UNKNOWN_CALLER_GUARD_WINDOW_MINUTES,
                            CallBlockSettings.MAX_REPEAT_UNKNOWN_CALLER_GUARD_WINDOW_MINUTES,
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                    )
                }
                BasicTextField(
                    value = draft,
                    onValueChange = { incoming ->
                        draft = incoming.filter(Char::isDigit).take(4)
                        rejectedBySettings = false
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                    cursorBrush = SolidColor(Primary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focus.clearFocus(force = true)
                            keyboard?.hide()
                        },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = s.repeatCallerWindowTitle },
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (showValidation) {
                    s.repeatCallerWindowInvalid(
                        CallBlockSettings.MIN_REPEAT_UNKNOWN_CALLER_GUARD_WINDOW_MINUTES,
                        CallBlockSettings.MAX_REPEAT_UNKNOWN_CALLER_GUARD_WINDOW_MINUTES,
                    )
                } else {
                    s.repeatCallerWindowHint(
                        CallBlockSettings.MIN_REPEAT_UNKNOWN_CALLER_GUARD_WINDOW_MINUTES,
                        CallBlockSettings.MAX_REPEAT_UNKNOWN_CALLER_GUARD_WINDOW_MINUTES,
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (showValidation) AccentRed else TextSecondary,
            )
            Spacer(Modifier.height(10.dp))
            PrimaryWideButton(
                text = s.repeatCallerApply,
                enabled = valid,
                onClick = {
                    focus.clearFocus(force = true)
                    keyboard?.hide()
                    if (value != null && onConfirm(value)) close() else rejectedBySettings = true
                },
            )
        }
    }
}

@Composable
private fun MainBlockFeatureInfoSheet(
    feature: MainBlockFeature,
    onDismiss: () -> Unit,
) {
    val s = appStrings().blocker
    AppBottomSheet(
        onDismiss = onDismiss,
        title = s.featureInfoSheetTitle,
        sheetGesturesEnabled = false,
        showCloseButton = true,
    ) { _ ->
        FeatureInfoSection(
            icon = feature.icon(),
            title = feature.title(s),
            description = feature.details(s),
        )
        Text(
            text = s.featureInfoAvailabilityNote,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
        )
        Spacer(Modifier.height(5.dp))
    }
}

@Composable
private fun CallBlockProcessingGuideSheet(onDismiss: () -> Unit) {
    val s = appStrings().blocker
    AppBottomSheet(
        onDismiss = onDismiss,
        title = s.processingGuideSheetTitle,
        maxHeightFraction = 0.82f,
        sheetGesturesEnabled = false,
        showCloseButton = true,
    ) { _ ->
        Text(
            text = s.processingGuideIntro,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(start = 22.dp, end = 22.dp, top = 2.dp, bottom = 8.dp),
        )
        (1..6).forEach { step ->
            PanelCard(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp),
                radius = 18.dp,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape).background(BrandSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = step.toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = Primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = s.processingGuideStepTitle(step),
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = s.processingGuideStepDescription(step),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(9.dp))
            Text(
                text = s.processingGuideConclusion,
                style = MaterialTheme.typography.bodySmall,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(5.dp))
    }
}

@Composable
private fun FeatureInfoSection(
    icon: ImageVector,
    title: String,
    description: String,
) {
    PanelCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp),
        radius = 18.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(BrandSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(23.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun BlockMethodSheet(
    selected: CallBlockMethod,
    onDismiss: () -> Unit,
    onPick: (CallBlockMethod) -> Unit,
) {
    val s = appStrings().blocker
    AppBottomSheet(
        onDismiss = onDismiss,
        title = s.chooseBlockMethod,
        sheetGesturesEnabled = false,
        showCloseButton = true,
    ) { close ->
        CallBlockMethod.entries.filterNot { it == CallBlockMethod.ALLOW }.forEach { method ->
            FilterOptionRow(
                icon = blockMethodIcon(method),
                label = blockMethodLabel(method, s),
                selected = method == selected,
                onClick = { onPick(method); close() },
            )
        }
        Text(
            text = blockMethodDescription(selected, s),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        )
    }
}

private fun activePauseWindow(
    protectionState: CallBlockProtectionState,
    nowMillis: Long,
): Pair<Long, Long>? {
    val startedAtMillis = protectionState.pauseStartedAtMillis
    val untilMillis = protectionState.pauseUntilMillis
    return if (
        startedAtMillis != null &&
        untilMillis != null &&
        protectionState.isPausedAt(nowMillis)
    ) {
        startedAtMillis to untilMillis
    } else {
        null
    }
}

@Composable
private fun ProtectionStatusBanner(
    permanentOff: Boolean,
    activePauseWindow: Pair<Long, Long>?,
    scheduledPauseWindow: CallBlockTimeWindow?,
    nowMillis: Long,
) {
    val s = appStrings().blocker
    val scheduledPause = scheduledPauseWindow != null
    val accent = if (scheduledPause) AccentBlue else AccentRed
    val background = if (scheduledPause) com.antimobile.mcas.ui.theme.AccentBlueBg else AccentRedBg
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 5.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = if (permanentOff) Icons.Rounded.Block else Icons.Rounded.Schedule,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        permanentOff -> s.protectionOff
                        scheduledPause -> s.dailySchedulePauseActive
                        else -> s.pauseActive
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = accent,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (permanentOff) {
                        s.protectionOffBannerBody
                    } else {
                        s.protectionPausedBannerBody
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                if (!permanentOff && activePauseWindow != null) {
                    val (startedAtMillis, untilMillis) = activePauseWindow
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = s.pausePeriod(
                            TimeFormat.dayClock(startedAtMillis),
                            TimeFormat.dayClock(untilMillis),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = s.pauseRemaining(countdownClock(untilMillis, nowMillis)),
                        style = MaterialTheme.typography.bodySmall,
                        color = accent,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (scheduledPauseWindow != null) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = buildString {
                            append(formatScheduleMinute(scheduledPauseWindow.startMinute))
                            append("–")
                            append(formatScheduleMinute(scheduledPauseWindow.endMinute))
                            if (scheduledPauseWindow.crossesMidnight) {
                                append(" · ")
                                append(s.dailyScheduleNextDay)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = accent,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ProcessingGuideCard(onClick: () -> Unit) {
    val s = appStrings().blocker
    PanelCard(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
        radius = 20.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberPressHighlight(),
                    onClick = onClick,
                )
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(BrandSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = Primary, modifier = Modifier.size(25.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = s.processingGuideItemTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = s.processingGuideItemSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun RulesTab(
    rules: List<CallBlockRule>,
    allowCount: Int,
    blockCount: Int,
    rulesDimmed: Boolean,
    activeMenuFeature: MainBlockFeature?,
    onOpenAllowlist: () -> Unit,
    onOpenBlocklist: () -> Unit,
    onOpenGroups: () -> Unit,
    onOpenAdvancedRules: () -> Unit,
    onOpenCommonIssues: () -> Unit,
    onOpenProcessingGuide: () -> Unit,
    onFeatureLongPress: (MainBlockFeature, Rect) -> Unit,
    modifier: Modifier,
) {
    val s = appStrings().blocker
    val advancedCount = rules.count { it.type !in setOf(
        com.antimobile.mcas.data.blocking.CallBlockRuleType.ANY,
        com.antimobile.mcas.data.blocking.CallBlockRuleType.EXACT_NUMBER,
        com.antimobile.mcas.data.blocking.CallBlockRuleType.CONTACTS,
        com.antimobile.mcas.data.blocking.CallBlockRuleType.CALL_HISTORY,
    ) }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        // FAB bật/tắt đã nằm trên top bar, nên tab Quy tắc không còn phải chừa đáy cho FAB.
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp,
        ),
    ) {
        item {
            ProcessingGuideCard(onClick = onOpenProcessingGuide)
            Text(
                s.manageSection,
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 5.dp),
            )
            BlockingArchitectureItem(
                feature = MainBlockFeature.ALWAYS_ALLOW,
                count = s.savedNumberCount(allowCount),
                activeInMenu = activeMenuFeature == MainBlockFeature.ALWAYS_ALLOW,
                onClick = onOpenAllowlist,
                onLongPress = { bounds ->
                    onFeatureLongPress(MainBlockFeature.ALWAYS_ALLOW, bounds)
                },
                dimmed = rulesDimmed,
            )
            BlockingArchitectureItem(
                feature = MainBlockFeature.BLOCKED_NUMBERS,
                count = s.savedNumberCount(blockCount),
                activeInMenu = activeMenuFeature == MainBlockFeature.BLOCKED_NUMBERS,
                onClick = onOpenBlocklist,
                onLongPress = { bounds ->
                    onFeatureLongPress(MainBlockFeature.BLOCKED_NUMBERS, bounds)
                },
                dimmed = rulesDimmed,
            )
            BlockingArchitectureItem(
                feature = MainBlockFeature.GROUP_BLOCKING,
                count = "",
                activeInMenu = activeMenuFeature == MainBlockFeature.GROUP_BLOCKING,
                onClick = onOpenGroups,
                onLongPress = { bounds ->
                    onFeatureLongPress(MainBlockFeature.GROUP_BLOCKING, bounds)
                },
                dimmed = rulesDimmed,
            )
            BlockingArchitectureItem(
                feature = MainBlockFeature.ADVANCED_RULES,
                count = s.ruleCount(advancedCount),
                activeInMenu = activeMenuFeature == MainBlockFeature.ADVANCED_RULES,
                onClick = onOpenAdvancedRules,
                onLongPress = { bounds ->
                    onFeatureLongPress(MainBlockFeature.ADVANCED_RULES, bounds)
                },
                dimmed = rulesDimmed,
            )
            CommonIssuesCard(onClick = onOpenCommonIssues)
        }
    }
}

@Composable
private fun CommonIssuesCard(onClick: () -> Unit) {
    val s = appStrings().blocker
    PanelCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        radius = 20.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberPressHighlight(),
                    onClick = onClick,
                )
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(BrandSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(25.dp),
                )
            }
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = s.commonIssuesTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = s.commonIssuesSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun BlockingArchitectureItem(
    feature: MainBlockFeature,
    count: String,
    activeInMenu: Boolean,
    onClick: () -> Unit,
    onLongPress: (Rect) -> Unit,
    dimmed: Boolean = false,
) {
    val coords = remember { CoordsHolder() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .onGloballyPositioned { coords.value = it }
            .graphicsLayer {
                alpha = when {
                    activeInMenu -> 0f
                    dimmed -> 0.48f
                    else -> 1f
                }
            },
    ) {
        BlockingArchitectureCard(
            feature = feature,
            count = count,
            onClick = onClick,
            onLongClick = {
                coords.value?.takeIf { it.isAttached }?.boundsInWindow()?.let(onLongPress)
            },
        )
    }
}

@Composable
private fun BlockingArchitectureCard(
    feature: MainBlockFeature,
    count: String,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
) {
    val s = appStrings().blocker
    PanelCard(
        modifier = Modifier.fillMaxWidth(),
        radius = 20.dp,
    ) {
        val interactionModifier = if (onClick != null) {
            Modifier.combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberPressHighlight(),
                onLongClickLabel = s.featureDetailsAction,
                onLongClick = onLongClick,
                onClick = onClick,
            )
        } else {
            Modifier
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(interactionModifier)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(BrandSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(feature.icon(), contentDescription = null, tint = Primary, modifier = Modifier.size(25.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(feature.title(s), style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text(feature.subtitle(s), style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (count.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(count, style = MaterialTheme.typography.labelMedium, color = Primary, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

private fun MainBlockFeature.icon(): ImageVector = when (this) {
    MainBlockFeature.ALWAYS_ALLOW -> Icons.Rounded.Phone
    MainBlockFeature.BLOCKED_NUMBERS -> Icons.Rounded.Block
    MainBlockFeature.GROUP_BLOCKING -> Icons.Rounded.Contacts
    MainBlockFeature.ADVANCED_RULES -> Icons.Rounded.Settings
}

private fun MainBlockFeature.title(s: CallBlockStrings): String = when (this) {
    MainBlockFeature.ALWAYS_ALLOW -> s.alwaysAllowTitle
    MainBlockFeature.BLOCKED_NUMBERS -> s.blockedNumbersTitle
    MainBlockFeature.GROUP_BLOCKING -> s.groupBlockingTitle
    MainBlockFeature.ADVANCED_RULES -> s.advancedRulesTitle
}

private fun MainBlockFeature.subtitle(s: CallBlockStrings): String = when (this) {
    MainBlockFeature.ALWAYS_ALLOW -> s.alwaysAllowSubtitle
    MainBlockFeature.BLOCKED_NUMBERS -> s.blockedNumbersSubtitle
    MainBlockFeature.GROUP_BLOCKING -> s.groupBlockingSubtitle
    MainBlockFeature.ADVANCED_RULES -> s.advancedRulesSubtitle
}

private fun MainBlockFeature.details(s: CallBlockStrings): String = when (this) {
    MainBlockFeature.ALWAYS_ALLOW -> s.alwaysAllowDetails
    MainBlockFeature.BLOCKED_NUMBERS -> s.blockedNumbersDetails
    MainBlockFeature.GROUP_BLOCKING -> s.groupBlockingDetails
    MainBlockFeature.ADVANCED_RULES -> s.advancedRulesDetails
}

private fun blockFeatureCount(
    feature: MainBlockFeature,
    allowCount: Int,
    blockCount: Int,
    rules: List<CallBlockRule>,
    s: CallBlockStrings,
): String {
    val count = when (feature) {
        MainBlockFeature.ALWAYS_ALLOW -> allowCount
        MainBlockFeature.BLOCKED_NUMBERS -> blockCount
        MainBlockFeature.GROUP_BLOCKING -> 0
        MainBlockFeature.ADVANCED_RULES -> rules.count {
            it.type !in setOf(
                com.antimobile.mcas.data.blocking.CallBlockRuleType.ANY,
                com.antimobile.mcas.data.blocking.CallBlockRuleType.EXACT_NUMBER,
                com.antimobile.mcas.data.blocking.CallBlockRuleType.CONTACTS,
                com.antimobile.mcas.data.blocking.CallBlockRuleType.CALL_HISTORY,
            )
        }
    }
    return when (feature) {
        MainBlockFeature.ALWAYS_ALLOW,
        MainBlockFeature.BLOCKED_NUMBERS,
        -> s.savedNumberCount(count)
        MainBlockFeature.GROUP_BLOCKING -> ""
        MainBlockFeature.ADVANCED_RULES -> s.ruleCount(count)
    }
}

@Composable
private fun HistoryTab(
    rows: List<BlockedCallHistory>,
    activeMenuHistoryId: Long?,
    onOpenNumber: (String) -> Unit,
    onLongPress: (BlockedCallHistory, Rect) -> Unit,
    modifier: Modifier,
) {
    val strings = appStrings()
    val s = strings.blocker
    if (rows.isEmpty()) {
        Column(modifier = modifier.fillMaxSize()) { EmptyBlockCard(s.emptyHistory) }
        return
    }

    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }
    var periodIndex by rememberSaveable { mutableIntStateOf(StatsPeriod.WEEK.ordinal) }
    var selectedEpochDay by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val period = StatsPeriod.entries.getOrElse(periodIndex) { StatsPeriod.WEEK }
    val selectedDay = remember(selectedEpochDay, today) {
        runCatching { LocalDate.ofEpochDay(selectedEpochDay) }
            .getOrDefault(today)
            .coerceIn(today.minusDays(29), today)
    }
    val range = remember(period, selectedDay, today) { blockHistoryRange(period, selectedDay, today) }
    val analytics = remember(rows, range, zone) { analyzeBlockHistory(rows, range, zone) }
    val reasonCounts = analytics.rows
        .groupingBy { row -> blockedHistoryReasonLabel(row, s) }
        .eachCount()
        .map { (label, count) -> HistoryReasonCount(label, count) }
        .sortedWith(compareByDescending<HistoryReasonCount> { it.count }.thenBy { it.label })
    val groupedRows = analytics.rows.groupBy { row ->
        Instant.ofEpochMilli(row.blockedAt).atZone(zone).toLocalDate()
    }
    val dateScrollTargets = remember(
        groupedRows,
        reasonCounts.isNotEmpty(),
        analytics.topNumbers.isNotEmpty(),
    ) {
        var itemIndex = 7 +
            (if (reasonCounts.isNotEmpty()) 2 else 0) +
            (if (analytics.topNumbers.isNotEmpty()) 2 else 0)
        groupedRows.map { (date, dayRows) ->
            HistoryDateScrollTarget(date = date, itemIndex = itemIndex).also {
                itemIndex += 1 + dayRows.size
            }
        }
    }
    val dateIndexItems = when (period) {
        StatsPeriod.DAY -> emptyList()
        StatsPeriod.WEEK -> dateScrollTargets.map { target ->
            val shortLabel = strings.callList.weekdayHeaders[target.date.dayOfWeek.value - 1]
            GearIndexItem(
                key = target.date.toString(),
                label = shortLabel,
                bubbleLabel = shortLabel,
            )
        }
        StatsPeriod.MONTH -> dateScrollTargets.map { target ->
            val dayNumber = "%02d".format(target.date.dayOfMonth)
            GearIndexItem(
                key = target.date.toString(),
                label = dayNumber,
                bubbleLabel = dayNumber,
            )
        }
    }
    val listState = rememberLazyListState()
    val scrollScope = rememberCoroutineScope()
    val backdropLayer = rememberGraphicsLayer()
    var historyContentCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val canScrollUp by remember {
        derivedStateOf { listState.canScrollBackward }
    }
    val canScrollDown by remember {
        derivedStateOf { listState.canScrollForward }
    }
    val showDayScrollControls = period == StatsPeriod.DAY
    val currentDateIndexKey by remember(dateScrollTargets) {
        derivedStateOf {
            if (dateScrollTargets.isEmpty()) {
                null
            } else {
                val info = listState.layoutInfo
                val visibleItems = info.visibleItemsInfo
                val referenceIndex = when {
                    visibleItems.isEmpty() -> listState.firstVisibleItemIndex
                    listState.canScrollBackward && !listState.canScrollForward ->
                        dateScrollTargets.last().itemIndex
                    listState.canScrollBackward &&
                        visibleItems.last().index >= info.totalItemsCount - 1 -> {
                        val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
                        visibleItems.minByOrNull { item ->
                            kotlin.math.abs((item.offset + item.size / 2) - center)
                        }?.index ?: listState.firstVisibleItemIndex
                    }
                    else -> listState.firstVisibleItemIndex
                }
                (dateScrollTargets.lastOrNull { it.itemIndex <= referenceIndex }
                    ?: dateScrollTargets.first()).date.toString()
            }
        }
    }
    val navigationBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { historyContentCoords = it }
                .drawWithContent {
                    if (showDayScrollControls) {
                        backdropLayer.record { this@drawWithContent.drawContent() }
                        drawLayer(backdropLayer)
                    } else {
                        drawContent()
                    }
                },
            // Giữ khoảng thở với thanh điều hướng; mục lục ngày nổi ở giữa mép phải.
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                bottom = navigationBottom + 24.dp,
            ),
        ) {
            item("history-period") {
                Segmented(
                    labels = listOf(s.historyPeriodDay, s.historyPeriodWeek, s.historyPeriodMonth),
                    selected = periodIndex,
                    onSelect = { periodIndex = it },
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                )
            }
            item("history-range") {
                HistoryRangeCard(
                    period = period,
                    range = range,
                    zone = zone,
                    onPickDate = { showDatePicker = true },
                )
            }
            item("history-overview-title") { HistorySectionHeader(s.historyOverviewTitle) }
            item("history-overview") { HistoryOverviewCard(analytics, period, zone) }
            item("history-activity-title") { HistorySectionHeader(s.historyActivityTitle) }
            item("history-activity") { HistoryActivityCard(analytics, period) }

            if (reasonCounts.isNotEmpty()) {
                item("history-reasons-title") { HistorySectionHeader(s.historyReasonsTitle) }
                item("history-reasons") { HistoryReasonsCard(reasonCounts.take(5)) }
            }
            if (analytics.topNumbers.isNotEmpty()) {
                item("history-numbers-title") { HistorySectionHeader(s.historyTopNumbersTitle) }
                item("history-numbers") { HistoryTopNumbersCard(analytics.topNumbers.take(5)) }
            }

            item("history-detail-title") { HistorySectionHeader(s.historyDetails(analytics.total)) }
            if (analytics.rows.isEmpty()) {
                item("history-detail-empty") { EmptyBlockCard(s.historyNoEventsInPeriod) }
            } else {
                groupedRows.forEach { (date, dayRows) ->
                    item("history-day-$date") { HistoryDayHeader(date, dayRows.size, zone) }
                    items(dayRows, key = { it.id }) { row ->
                        BlockedCallItem(
                            row = row,
                            activeInMenu = row.id == activeMenuHistoryId,
                            // SIP history is useful evidence, but it is not a dialable
                            // PSTN identity and must never open call/SMS/contact actions.
                            onClick = row.rawNumber
                                .takeIf(CallHistoryRuleCodec::isSelectableNumber)
                                ?.let { number -> { onOpenNumber(number) } },
                            onLongPress = { bounds -> onLongPress(row, bounds) },
                        )
                    }
                }
            }
        }

        if (showDayScrollControls) {
            // Luôn giữ nguyên hai vị trí để không bị nhảy khi chạm đầu/cuối danh sách.
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
                    contentCoords = historyContentCoords,
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
                    contentCoords = historyContentCoords,
                    enabled = canScrollDown,
                    buttonSize = 40.dp,
                    iconSize = 24.dp,
                    onClick = {
                        scrollScope.launch {
                            val lastIndex = (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
                            listState.animateScrollToItem(lastIndex)
                        }
                    },
                )
            }
        } else if (dateIndexItems.isNotEmpty()) {
            GearIndexBar(
                items = dateIndexItems,
                currentKey = currentDateIndexKey,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp, top = 6.dp, bottom = 6.dp),
                // Không phủ cụm chọn Ngày/Tuần/Tháng và thẻ khoảng thời gian ở đầu tab.
                contentTopPadding = 196.dp,
                onFocusKey = { key ->
                    dateScrollTargets.firstOrNull { it.date.toString() == key }?.let { target ->
                        scrollScope.launch { listState.scrollToItem(target.itemIndex) }
                    }
                },
            )
        }
    }

    if (showDatePicker) {
        DayFilterSheet(
            initialEpochDay = selectedEpochDay,
            isActive = period == StatsPeriod.DAY,
            onApply = { epochDay ->
                selectedEpochDay = epochDay
                periodIndex = StatsPeriod.DAY.ordinal
            },
            onClear = {
                selectedEpochDay = today.toEpochDay()
                periodIndex = StatsPeriod.WEEK.ordinal
            },
            onDismiss = { showDatePicker = false },
            minimumDate = today.minusDays(29),
            rangeNote = s.historyDateRangeNote,
        )
    }
}

private data class HistoryReasonCount(val label: String, val count: Int)

@Composable
private fun HistoryRangeCard(
    period: StatsPeriod,
    range: BlockHistoryDateRange,
    zone: ZoneId,
    onPickDate: () -> Unit,
) {
    val s = appStrings().blocker
    val startMillis = range.start.atStartOfDay(zone).toInstant().toEpochMilli()
    val endMillis = range.endInclusive.atStartOfDay(zone).toInstant().toEpochMilli()
    val periodLabel = when (period) {
        StatsPeriod.DAY -> s.historyPeriodDay
        StatsPeriod.WEEK -> s.historyPeriodWeek
        StatsPeriod.MONTH -> s.historyPeriodMonth
    }
    val dateLabel = if (range.start == range.endInclusive) {
        TimeFormat.sectionLabel(startMillis)
    } else {
        s.historyRange(TimeFormat.date(startMillis), TimeFormat.date(endMillis))
    }
    PanelCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        radius = 18.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(BrandSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.EditCalendar, null, tint = Primary, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(periodLabel, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        dateLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(BrandSoft)
                    .clickable(onClick = onPickDate),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Rounded.EditCalendar, null, tint = Primary, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(7.dp))
                Text(
                    s.historyPickDate,
                    style = MaterialTheme.typography.labelLarge,
                    color = Primary,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun HistorySectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = TextPrimary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 3.dp),
    )
}

@Composable
private fun HistoryOverviewCard(
    analytics: BlockHistoryAnalytics,
    period: StatsPeriod,
    zone: ZoneId,
) {
    val s = appStrings().blocker
    val peakLabel = when (period) {
        StatsPeriod.DAY -> analytics.peakHour?.let { "%02d:00".format(it) }
        StatsPeriod.WEEK,
        StatsPeriod.MONTH,
        -> analytics.peakDay?.let { day ->
            TimeFormat.date(day.date.atStartOfDay(zone).toInstant().toEpochMilli())
        }
    } ?: s.historyNoPeak
    PanelCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        radius = 20.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 15.dp)) {
            HistoryTrendPill(analytics.deltaVsPrevious)
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                HistoryMetricCell(s.historyTotalBlocks, analytics.total.toString(), AccentRed, Modifier.weight(1f))
                HistoryMetricCell(s.historyUniqueNumbers, analytics.distinctNumbers.toString(), AccentBlue, Modifier.weight(1f))
                HistoryMetricCell(
                    if (period == StatsPeriod.DAY) s.historyPeakHour else s.historyPeakDay,
                    peakLabel,
                    TextPrimary,
                    Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HistoryMetricCell(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = color,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Spacer(Modifier.height(3.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 2,
        )
    }
}

@Composable
private fun HistoryTrendPill(delta: Int) {
    val s = appStrings().blocker
    val (icon, color, background, label) = when {
        delta > 0 -> TrendUi(
            Icons.AutoMirrored.Rounded.TrendingUp,
            AccentRed,
            AccentRedBg,
            s.historyTrendUp(delta),
        )
        delta < 0 -> TrendUi(
            Icons.AutoMirrored.Rounded.TrendingDown,
            AccentGreen,
            AccentGreenBg,
            s.historyTrendDown(-delta),
        )
        else -> TrendUi(
            Icons.AutoMirrored.Rounded.TrendingFlat,
            TextSecondary,
            CardFill,
            s.historyTrendSame,
        )
    }
    Row(
        modifier = Modifier.clip(CircleShape).background(background).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.Medium)
    }
}

private data class TrendUi(
    val icon: ImageVector,
    val color: Color,
    val background: Color,
    val label: String,
)

@Composable
private fun HistoryActivityCard(analytics: BlockHistoryAnalytics, period: StatsPeriod) {
    val s = appStrings().blocker
    val chart = when (period) {
        StatsPeriod.DAY -> HistoryChartSpec(
            values = analytics.hourly.toList(),
            labels = analytics.hourly.indices.map { hour -> s.historyHourBucket(hour, hour + 1) },
            itemWidth = 66.dp,
        )
        StatsPeriod.WEEK -> HistoryChartSpec(
            values = analytics.daily.map { it.count },
            labels = analytics.daily.map { bucket -> s.historyWeekdayAxis(bucket.date.dayOfWeek) },
            itemWidth = 96.dp,
        )
        StatsPeriod.MONTH -> HistoryChartSpec(
            values = analytics.daily.map { it.count },
            labels = analytics.daily.map { bucket -> s.historyDayAxis(bucket.date.dayOfMonth) },
            itemWidth = 78.dp,
        )
    }
    PanelCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        radius = 20.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(AccentRedBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.BarChart, null, tint = AccentRed, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(11.dp))
                Text(
                    if (period == StatsPeriod.DAY) s.historyHourlySubtitle else s.historyDailySubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(14.dp))
            Box(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                HistoryBarChart(chart)
            }
            Spacer(Modifier.height(7.dp))
            Text(
                s.historySwipeChartHint,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(CardFill))
            Spacer(Modifier.height(14.dp))
            Text(
                s.historyDayPartsTitle,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                s.historyDayPartsSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Spacer(Modifier.height(11.dp))
            HistoryDayPartsGrid(analytics.dayParts)
        }
    }
}

private data class HistoryChartSpec(
    val values: List<Int>,
    val labels: List<String>,
    val itemWidth: Dp,
)

@Composable
private fun HistoryBarChart(chart: HistoryChartSpec) {
    val maxValue = (chart.values.maxOrNull() ?: 0).coerceAtLeast(1)
    val chartWidth = chart.itemWidth * chart.values.size.coerceAtLeast(1)
    Column(modifier = Modifier.width(chartWidth)) {
        Row(modifier = Modifier.width(chartWidth).height(98.dp), verticalAlignment = Alignment.Bottom) {
            chart.values.forEach { value ->
                Column(
                    modifier = Modifier.width(chart.itemWidth).fillMaxHeight(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        value.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (value > 0) TextPrimary else TextSecondary,
                        fontWeight = if (value > 0) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(72.dp), contentAlignment = Alignment.BottomCenter) {
                        Box(
                            modifier = Modifier
                                .width(26.dp)
                                .fillMaxHeight(
                                    if (value == 0) 0.04f
                                    else (value.toFloat() / maxValue).coerceAtLeast(0.12f)
                                )
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(if (value == 0) CardFill else AccentRed),
                        )
                    }
                }
            }
        }
        Box(modifier = Modifier.width(chartWidth).height(1.dp).background(TextSecondary.copy(alpha = 0.28f)))
        Row(modifier = Modifier.width(chartWidth).height(42.dp)) {
            chart.labels.forEach { label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.width(chart.itemWidth).padding(top = 7.dp, start = 3.dp, end = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun HistoryDayPartsGrid(dayParts: List<Pair<DayPart, Int>>) {
    dayParts.chunked(2).forEachIndexed { rowIndex, rowParts ->
        if (rowIndex > 0) Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            rowParts.forEach { (part, count) ->
                HistoryDayPartCell(part = part, count = count, modifier = Modifier.weight(1f))
            }
            if (rowParts.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun HistoryDayPartCell(part: DayPart, count: Int, modifier: Modifier = Modifier) {
    val allStrings = appStrings()
    val s = allStrings.blocker
    val label = when (part) {
        DayPart.MORNING -> allStrings.phoneStats.partMorning
        DayPart.AFTERNOON -> allStrings.phoneStats.partAfternoon
        DayPart.EVENING -> allStrings.phoneStats.partEvening
        DayPart.NIGHT -> allStrings.phoneStats.partNight
    }
    val range = when (part) {
        DayPart.MORNING -> s.historyDayPartRange(5, 10)
        DayPart.AFTERNOON -> s.historyDayPartRange(11, 17)
        DayPart.EVENING -> s.historyDayPartRange(18, 22)
        DayPart.NIGHT -> s.historyDayPartRange(23, 4)
    }
    val tint = when (part) {
        DayPart.MORNING -> AccentGreen
        DayPart.AFTERNOON -> AccentBlue
        DayPart.EVENING -> AccentRed
        DayPart.NIGHT -> TextSecondary
    }
    Row(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).background(CardFill).padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(tint))
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            Text(range, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
        Spacer(Modifier.width(6.dp))
        Text(
            count.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = tint,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun HistoryReasonsCard(reasons: List<HistoryReasonCount>) {
    val max = reasons.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    PanelCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        radius = 20.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            reasons.forEach { reason ->
                HistoryProgressRow(reason.label, reason.count, max)
            }
        }
    }
}

@Composable
private fun HistoryProgressRow(label: String, count: Int, max: Int) {
    val s = appStrings().blocker
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            Text(s.historyEvents(count), style = MaterialTheme.typography.labelMedium, color = AccentRed)
        }
        Spacer(Modifier.height(7.dp))
        Box(modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape).background(CardFill)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((count.toFloat() / max).coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(AccentRed),
            )
        }
    }
}

@Composable
private fun HistoryTopNumbersCard(numbers: List<BlockHistoryNumberCount>) {
    val s = appStrings().blocker
    PanelCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        radius = 20.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
            numbers.forEachIndexed { index, number ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(34.dp).clip(CircleShape).background(BrandSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            (index + 1).toString(),
                            style = MaterialTheme.typography.labelLarge,
                            color = Primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        formatPhone(number.rawNumber),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        s.historyEvents(number.count),
                        style = MaterialTheme.typography.labelMedium,
                        color = AccentRed,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(CircleShape).background(AccentRedBg).padding(horizontal = 9.dp, vertical = 5.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryDayHeader(date: LocalDate, count: Int, zone: ZoneId) {
    val s = appStrings().blocker
    val millis = date.atStartOfDay(zone).toInstant().toEpochMilli()
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 13.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            TimeFormat.sectionLabel(millis),
            style = MaterialTheme.typography.titleSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Text(
            s.historyEvents(count),
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            modifier = Modifier.clip(CircleShape).background(CardFill).padding(horizontal = 9.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun AddRuleCard(onClick: () -> Unit) {
    val s = appStrings().blocker
    PanelCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp), radius = 18.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(BrandSoft), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = Primary, modifier = Modifier.size(25.dp))
            }
            Spacer(Modifier.width(13.dp))
            Text(s.addRule, style = MaterialTheme.typography.titleMedium, color = Primary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun EmptyBlockCard(text: String) {
    PanelCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), radius = 18.dp) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.padding(20.dp))
    }
}

@Composable
private fun CallBlockRuleItem(
    rule: CallBlockRule,
    activeInMenu: Boolean,
    dimmed: Boolean,
    onClick: () -> Unit,
    onLongPress: (Rect) -> Unit,
) {
    val coords = remember { CoordsHolder() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 7.dp)
            .onGloballyPositioned { coords.value = it }
            .graphicsLayer {
                alpha = when {
                    activeInMenu -> 0f
                    dimmed -> 0.48f
                    else -> 1f
                }
            }
    ) {
        CallBlockRuleCard(
            rule = rule,
            onClick = onClick,
            onLongClick = {
                coords.value?.takeIf { it.isAttached }?.boundsInWindow()?.let(onLongPress)
            },
        )
    }
}

@Composable
private fun CallBlockRuleCard(
    rule: CallBlockRule,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val s = appStrings().blocker
    PanelCard(modifier = modifier.fillMaxWidth(), radius = 18.dp) {
        val clickable = if (onClick != null) {
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberPressHighlight(),
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
        } else Modifier.fillMaxWidth()
        Row(modifier = clickable.padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(if (rule.enabled) AccentGreenBg else CardFill),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Block, contentDescription = null, tint = if (rule.enabled) AccentGreen else TextSecondary, modifier = Modifier.size(23.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    s.ruleSummary(rule.type.storageKey, rule.rawValue),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    if (rule.enabled) s.ruleEnabledStatus else s.ruleDisabledStatus,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (rule.enabled) AccentGreen else TextSecondary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun BlockedCallItem(
    row: BlockedCallHistory,
    activeInMenu: Boolean,
    onClick: (() -> Unit)?,
    onLongPress: (Rect) -> Unit,
) {
    val coords = remember { CoordsHolder() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 7.dp)
            .onGloballyPositioned { coords.value = it }
            .graphicsLayer { alpha = if (activeInMenu) 0f else 1f }
    ) {
        BlockedCallCard(
            row = row,
            onClick = onClick,
            onLongClick = {
                coords.value?.takeIf { it.isAttached }?.boundsInWindow()?.let(onLongPress)
            },
        )
    }
}

@Composable
private fun BlockedCallCard(
    row: BlockedCallHistory,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val s = appStrings().blocker
    PanelCard(modifier = modifier.fillMaxWidth(), radius = 18.dp) {
        val clickable = if (onClick != null || onLongClick != null) {
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberPressHighlight(),
                    // Non-dialable SIP rows keep their delete menu on long-press while a
                    // normal tap deliberately performs no phone/detail action.
                    onClick = onClick ?: {},
                    onLongClick = onLongClick,
                )
        } else Modifier.fillMaxWidth()
        Row(modifier = clickable.padding(horizontal = 14.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(AccentRedBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Block, contentDescription = null, tint = AccentRed, modifier = Modifier.size(23.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    formatPhone(row.rawNumber),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AccentRed,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "${s.blockedAt(TimeFormat.fullDateTimeWithSeconds(row.blockedAt))}  ·  ${s.blockedCount(row.blockedCountForNumber)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    blockedHistoryReasonLabel(row, s),
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentRed,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (row.consecutiveUnanswered > 0) {
                    Spacer(Modifier.height(2.dp))
                    Text(s.consecutiveMissed(row.consecutiveUnanswered), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }
    }
}

private fun blockedHistoryReasonLabel(
    row: BlockedCallHistory,
    s: com.antimobile.mcas.i18n.CallBlockStrings,
): String {
    if (row.historyReasonType == REPEAT_UNKNOWN_CALLER_GUARD_REASON_TYPE) {
        RepeatUnknownCallerGuardReasonCodec.decode(row.historyReasonValue)?.let { reason ->
            return s.repeatCallerGuardReason(
                attempt = reason.attempt,
                threshold = reason.threshold,
                minutes = reason.windowMinutes,
            )
        }
    }
    if (row.historyReasonType == CallBlockRuleType.SPAM_RISK.storageKey) {
        SpamRiskReasonCodec.decode(row.historyReasonValue)?.let { reason ->
            return when (reason.kind) {
                SpamRiskReasonKind.PREFIX -> s.spamRiskReasonPrefix(reason.prefix)
                SpamRiskReasonKind.UNKNOWN_MOBILE_PREFIX ->
                    s.spamRiskReasonUnknownMobilePrefix(reason.prefix)
                SpamRiskReasonKind.VERIFICATION_FAILED -> s.spamRiskReasonVerificationFailed
            }
        }
    }
    return s.matchedRule(s.ruleSummary(row.ruleType, row.ruleValue))
}

@Composable
private fun NotificationModeSheet(
    notificationStatus: CallBlockNotifier.Readiness,
    onDismiss: () -> Unit,
    onPick: (BlockNotificationMode) -> Unit,
) {
    val s = appStrings().blocker
    AppBottomSheet(
        onDismiss = onDismiss,
        title = s.notificationTitleSetting,
        sheetGesturesEnabled = false,
        showCloseButton = true,
    ) { close ->
        BlockNotificationMode.entries.forEach { mode ->
            val selected = CallBlockSettings.notificationMode == mode
            FilterOptionRow(
                icon = notificationModeIcon(mode),
                label = notificationModeLabel(mode, s),
                selected = selected,
                onClick = { onPick(mode); close() },
            )
        }
        if (notificationStatus != CallBlockNotifier.Readiness.READY) {
            Text(
                if (notificationStatus == CallBlockNotifier.Readiness.RUNTIME_PERMISSION_REQUIRED) {
                    s.notificationPermissionNeeded
                } else {
                    s.notificationChannelNeedsAttention
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun PrimaryWideButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) Primary else Primary.copy(alpha = 0.5f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.titleSmall, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun notificationModeLabel(mode: BlockNotificationMode, s: com.antimobile.mcas.i18n.CallBlockStrings): String = when (mode) {
    BlockNotificationMode.OFF -> s.notificationOff
    BlockNotificationMode.EVERY_BLOCK -> s.notificationEvery
}

private fun notificationModeIcon(mode: BlockNotificationMode): ImageVector = when (mode) {
    BlockNotificationMode.OFF -> Icons.Rounded.NotificationsOff
    BlockNotificationMode.EVERY_BLOCK -> Icons.Rounded.Notifications
}

private fun blockMethodLabel(
    method: CallBlockMethod,
    s: com.antimobile.mcas.i18n.CallBlockStrings,
): String = when (method) {
    CallBlockMethod.BLOCK_AND_REJECT -> s.methodBlockAndReject
    CallBlockMethod.BLOCK_WITHOUT_REJECT -> s.methodBlockWithoutReject
    CallBlockMethod.SILENCE_ONLY -> s.methodSilenceOnly
    CallBlockMethod.ALLOW -> s.methodAllow
}

private fun blockMethodDescription(
    method: CallBlockMethod,
    s: com.antimobile.mcas.i18n.CallBlockStrings,
): String = when (method) {
    CallBlockMethod.BLOCK_AND_REJECT -> s.methodBlockAndRejectDesc
    CallBlockMethod.BLOCK_WITHOUT_REJECT -> s.methodBlockWithoutRejectDesc
    CallBlockMethod.SILENCE_ONLY -> s.methodSilenceOnlyDesc
    CallBlockMethod.ALLOW -> s.methodAllowDesc
}

private fun blockMethodIcon(method: CallBlockMethod): ImageVector = when (method) {
    CallBlockMethod.BLOCK_AND_REJECT -> Icons.Rounded.CallEnd
    CallBlockMethod.BLOCK_WITHOUT_REJECT -> Icons.Rounded.Block
    CallBlockMethod.SILENCE_ONLY -> Icons.AutoMirrored.Rounded.VolumeOff
    CallBlockMethod.ALLOW -> Icons.Rounded.Phone
}

private class CoordsHolder(var value: LayoutCoordinates? = null)

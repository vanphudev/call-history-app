package com.antimobile.callhs.ui.blocking

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.antimobile.callhs.data.blocking.ALL_WEEKDAYS_MASK
import com.antimobile.callhs.data.blocking.CallBlockDailySchedule
import com.antimobile.callhs.data.blocking.CallBlockScheduleAction
import com.antimobile.callhs.data.blocking.CallBlockSchedulePreset
import com.antimobile.callhs.data.blocking.CallBlockScheduleUpdate
import com.antimobile.callhs.data.blocking.CallBlockSettings
import com.antimobile.callhs.data.blocking.CallBlockTimeWindow
import com.antimobile.callhs.data.blocking.MAX_CALL_BLOCK_TIME_WINDOWS
import com.antimobile.callhs.data.blocking.MINUTES_PER_DAY
import com.antimobile.callhs.data.blocking.weekdayBit
import com.antimobile.callhs.i18n.CallBlockStrings
import com.antimobile.callhs.i18n.appStrings
import com.antimobile.callhs.ui.components.AppDialog
import com.antimobile.callhs.ui.components.AppBottomSheet
import com.antimobile.callhs.ui.components.AppToastHost
import com.antimobile.callhs.ui.components.DialogButton
import com.antimobile.callhs.ui.components.PanelCard
import com.antimobile.callhs.ui.components.Segmented
import com.antimobile.callhs.ui.components.rememberPressHighlight
import com.antimobile.callhs.ui.theme.AccentBlue
import com.antimobile.callhs.ui.theme.AccentBlueBg
import com.antimobile.callhs.ui.theme.AccentRed
import com.antimobile.callhs.ui.theme.AccentRedBg
import com.antimobile.callhs.ui.theme.BrandSoft
import com.antimobile.callhs.ui.theme.CardFill
import com.antimobile.callhs.ui.theme.CardSurface
import com.antimobile.callhs.ui.theme.FieldSurface
import com.antimobile.callhs.ui.theme.Primary
import com.antimobile.callhs.ui.theme.ProvideAppDensity
import com.antimobile.callhs.ui.theme.TextPrimary
import com.antimobile.callhs.ui.theme.TextSecondary
import java.time.DayOfWeek

@Composable
internal fun CallBlockDailyScheduleCard(
    nowMillis: Long,
    activeMenuWindowId: String?,
    highlightedWindowId: String?,
    onAdd: () -> Unit,
    onEdit: (CallBlockTimeWindow) -> Unit,
    onLongPress: (CallBlockTimeWindow, Boolean, Rect) -> Unit,
    onHighlightedWindowPositioned: (String, Rect) -> Unit,
) {
    val s = appStrings().blocker
    val context = LocalContext.current
    val windows = CallBlockDailySchedule.sort(CallBlockSettings.dailySchedule)
    val currentMinute = CallBlockDailySchedule.minuteOfDay(nowMillis)
    val currentDay = CallBlockDailySchedule.dayOfWeek(nowMillis)
    val oneShotPaused = CallBlockSettings.protectionState.isPausedAt(nowMillis)
    val activeWindow = if (oneShotPaused) null else {
        windows.firstOrNull { it.isActiveAt(currentDay, currentMinute) }
    }
    val limitReached = windows.size >= MAX_CALL_BLOCK_TIME_WINDOWS
    var updateError by remember { mutableStateOf<String?>(null) }
    var overlapDialogMessage by remember { mutableStateOf<String?>(null) }

    PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 15.dp)) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(BrandSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Schedule, contentDescription = null, tint = Primary, modifier = Modifier.size(25.dp))
                }
                Spacer(Modifier.width(13.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        s.dailyScheduleTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        activeWindow?.let { window ->
                            if (window.action == CallBlockScheduleAction.BLOCK) {
                                s.dailyScheduleBlockActive
                            } else {
                                s.dailySchedulePauseActive
                            }
                        } ?: s.dailyScheduleCount(windows.size, MAX_CALL_BLOCK_TIME_WINDOWS),
                        style = MaterialTheme.typography.bodySmall,
                        color = when (activeWindow?.action) {
                            CallBlockScheduleAction.BLOCK -> AccentRed
                            CallBlockScheduleAction.PAUSE -> AccentBlue
                            null -> Primary
                        },
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (limitReached) CardFill else BrandSoft)
                        .clickable(enabled = !limitReached, onClick = onAdd),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = s.dailyScheduleAdd,
                        tint = if (limitReached) TextSecondary else Primary,
                        modifier = Modifier.size(23.dp),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                s.dailyScheduleDescription,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(14.dp))
            DailyScheduleTimeline(
                windows = windows,
                currentMinute = currentMinute,
                dayOfWeek = currentDay,
            )
            Spacer(Modifier.height(9.dp))
            ScheduleLegend()

            if (windows.isEmpty()) {
                Text(
                    s.dailyScheduleEmpty,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 13.dp),
                )
            } else {
                Spacer(Modifier.height(8.dp))
                windows.forEach { window ->
                    key(window.id) {
                        val active = window.id == activeWindow?.id
                        DailyScheduleRow(
                            window = window,
                            active = active,
                            activeInMenu = window.id == activeMenuWindowId,
                            highlighted = window.id == highlightedWindowId,
                            onEdit = { onEdit(window) },
                            onLongPress = { bounds -> onLongPress(window, active, bounds) },
                            onPositioned = { bounds ->
                                onHighlightedWindowPositioned(window.id, bounds)
                            },
                            onToggle = { enabled ->
                                when (
                                    val result = CallBlockSettings.upsertDailyWindow(
                                        context,
                                        window.copy(enabled = enabled),
                                    )
                                ) {
                                    is CallBlockScheduleUpdate.Success -> {
                                        updateError = null
                                        overlapDialogMessage = null
                                    }
                                    is CallBlockScheduleUpdate.Overlap -> {
                                        updateError = null
                                        overlapDialogMessage = s.dailyScheduleOverlapError(
                                            formatScheduleMinute(result.conflicting.startMinute),
                                            formatScheduleMinute(result.conflicting.endMinute),
                                        )
                                    }
                                    CallBlockScheduleUpdate.InvalidWindow -> {
                                        updateError = s.dailyScheduleInvalidError
                                    }
                                    CallBlockScheduleUpdate.TooManyWindows -> {
                                        updateError = s.dailyScheduleLimitReached
                                    }
                                    CallBlockScheduleUpdate.StorageFailure -> {
                                        updateError = s.dailyScheduleStorageError
                                    }
                                }
                            },
                        )
                    }
                }
            }

            updateError?.let { error ->
                Text(
                    error,
                    style = MaterialTheme.typography.bodySmall,
                    color = AccentRed,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 9.dp),
                )
            }

            Text(
                if (limitReached) s.dailyScheduleLimitReached else s.dailyScheduleBaseState,
                style = MaterialTheme.typography.bodySmall,
                color = if (limitReached) AccentRed else TextSecondary,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 11.dp),
            )
        }
    }

    overlapDialogMessage?.let { message ->
        AppDialog(
            onDismissRequest = { overlapDialogMessage = null },
            title = s.dailyScheduleOverlapTitle,
            buttons = listOf(
                DialogButton(
                    text = s.dailyScheduleOverlapConfirm,
                    color = Primary,
                    bold = true,
                    onClick = { overlapDialogMessage = null },
                )
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
private fun DailyScheduleTimeline(
    windows: List<CallBlockTimeWindow>,
    currentMinute: Int,
    dayOfWeek: DayOfWeek,
) {
    val s = appStrings().blocker
    val blockColor = AccentRed
    val pauseColor = AccentBlue
    val baseColor = FieldSurface
    val markerColor = TextPrimary
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            s.dailyScheduleToday(s.dailyScheduleWeekdayShort(dayOfWeek)),
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .semantics { contentDescription = s.dailyScheduleTimelineDescription },
        ) {
            drawRect(baseColor)
            windows.filter(CallBlockTimeWindow::enabled).forEach { window ->
                val color = if (window.action == CallBlockScheduleAction.BLOCK) blockColor else pauseColor
                CallBlockDailySchedule.segmentsOnDay(window, dayOfWeek).forEach { (start, end) ->
                    val left = size.width * start / MINUTES_PER_DAY
                    val right = size.width * end / MINUTES_PER_DAY
                    drawRect(color = color, topLeft = Offset(left, 0f), size = size.copy(width = right - left))
                }
            }
            val markerX = size.width * currentMinute / MINUTES_PER_DAY
            drawLine(
                color = Color.White,
                start = Offset(markerX, 2.dp.toPx()),
                end = Offset(markerX, size.height - 2.dp.toPx()),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = markerColor,
                start = Offset(markerX, 0f),
                end = Offset(markerX, size.height),
                strokeWidth = 1.dp.toPx(),
            )
        }
        Spacer(Modifier.height(5.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("00", "06", "12", "18", "24").forEach { hour ->
                Text(hour, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun ScheduleLegend() {
    val s = appStrings().blocker
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScheduleLegendItem(color = AccentRed, label = s.dailyScheduleBlock)
        ScheduleLegendItem(color = AccentBlue, label = s.dailySchedulePause)
    }
}

@Composable
private fun ScheduleLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    }
}

@Composable
private fun DailyScheduleRow(
    window: CallBlockTimeWindow,
    active: Boolean,
    activeInMenu: Boolean,
    highlighted: Boolean,
    onEdit: () -> Unit,
    onLongPress: (Rect) -> Unit,
    onPositioned: (Rect) -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    val coords = remember { ScheduleRowCoords() }
    LaunchedEffect(highlighted) {
        if (highlighted) {
            withFrameNanos { }
            coords.value
                ?.takeIf(LayoutCoordinates::isAttached)
                ?.boundsInWindow()
                ?.let(onPositioned)
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp)
            .onGloballyPositioned { coordinates ->
                coords.value = coordinates
                if (highlighted && coordinates.isAttached) {
                    onPositioned(coordinates.boundsInWindow())
                }
            }
            .graphicsLayer {
                alpha = when {
                    activeInMenu -> 0f
                    window.enabled -> 1f
                    else -> 0.62f
                }
            },
    ) {
        DailyScheduleRowContent(
            window = window,
            active = active,
            highlighted = highlighted,
            onEdit = onEdit,
            onLongPress = {
                coords.value
                    ?.takeIf(LayoutCoordinates::isAttached)
                    ?.boundsInWindow()
                    ?.let(onLongPress)
            },
            onToggle = onToggle,
        )
    }
}

@Composable
internal fun CallBlockDailyScheduleLiftedRow(
    window: CallBlockTimeWindow,
    active: Boolean,
) {
    // Dòng gốc nằm trong PanelCard lớn nên nền trong suốt vẫn nhìn đúng. Bản sao được
    // ContextMenuOverlay "nhấc" ra khỏi card đó phải có surface riêng, giống mẫu
    // CallBlockRuleCard/BlockedCallCard; nếu không scrim sẽ xuyên qua toàn bộ item.
    PanelCard(modifier = Modifier.fillMaxWidth(), radius = 14.dp) {
        DailyScheduleRowContent(
            window = window,
            active = active,
            highlighted = false,
            onEdit = null,
            onLongPress = null,
            onToggle = null,
        )
    }
}

@Composable
private fun DailyScheduleRowContent(
    window: CallBlockTimeWindow,
    active: Boolean,
    highlighted: Boolean,
    onEdit: (() -> Unit)?,
    onLongPress: (() -> Unit)?,
    onToggle: ((Boolean) -> Unit)?,
) {
    val s = appStrings().blocker
    val isBlock = window.action == CallBlockScheduleAction.BLOCK
    val accent = if (isBlock) AccentRed else AccentBlue
    val activeBackground = if (isBlock) AccentRedBg else AccentBlueBg
    val backgroundColor by animateColorAsState(
        targetValue = when {
            highlighted -> TextSecondary.copy(alpha = 0.16f)
            active -> activeBackground
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = if (highlighted) 180 else 360),
        label = "scheduleItemBackground",
    )
    val interactionModifier = if (onEdit != null) {
        Modifier.combinedClickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = rememberPressHighlight(),
            onLongClickLabel = s.dailyScheduleDelete,
            onLongClick = onLongPress,
            onClick = onEdit,
        )
    } else {
        Modifier
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor)
            .then(interactionModifier)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(accent))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                scheduleRange(window, s),
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                buildString {
                    append(if (isBlock) s.dailyScheduleBlock else s.dailySchedulePause)
                    window.presetKey?.let { key ->
                        append(" · ")
                        append(schedulePresetLabel(CallBlockSchedulePreset.fromStorage(key), s))
                    }
                    append(" · ")
                    append(if (window.enabled) s.dailyScheduleEnabled else s.dailyScheduleDisabled)
                },
                style = MaterialTheme.typography.bodySmall,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                scheduleDaysLabel(window.weekdaysMask, s),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Switch(
            checked = window.enabled,
            onCheckedChange = onToggle,
            modifier = Modifier.width(48.dp),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Primary,
                checkedBorderColor = Primary,
            ),
        )
    }
}

private class ScheduleRowCoords {
    var value: LayoutCoordinates? = null
}

@Composable
internal fun CallBlockScheduleEditorSheet(
    existing: CallBlockTimeWindow?,
    onDismiss: () -> Unit,
    onSave: (CallBlockTimeWindow) -> CallBlockScheduleUpdate,
) {
    val s = appStrings().blocker
    val initialPreset = existing?.presetKey?.let(CallBlockSchedulePreset::fromStorage)
        ?: if (existing == null) CallBlockSchedulePreset.MORNING else null
    var actionKey by rememberSaveable(existing?.id) {
        mutableStateOf((existing?.action ?: CallBlockScheduleAction.BLOCK).storageKey)
    }
    var startMinute by rememberSaveable(existing?.id) {
        mutableIntStateOf(existing?.startMinute ?: CallBlockSchedulePreset.MORNING.startMinute)
    }
    var endMinute by rememberSaveable(existing?.id) {
        mutableIntStateOf(existing?.endMinute ?: CallBlockSchedulePreset.MORNING.endMinute)
    }
    var presetKey by rememberSaveable(existing?.id) { mutableStateOf(initialPreset?.storageKey) }
    var weekdaysMask by rememberSaveable(existing?.id) {
        mutableIntStateOf(existing?.weekdaysMask ?: ALL_WEEKDAYS_MASK)
    }
    var errorText by remember(existing?.id) { mutableStateOf<String?>(null) }
    var timePickerTarget by rememberSaveable(existing?.id) { mutableStateOf<String?>(null) }

    AppBottomSheet(
        onDismiss = onDismiss,
        title = if (existing == null) s.dailyScheduleEditorAddTitle else s.dailyScheduleEditorEditTitle,
        maxHeightFraction = 0.88f,
        sheetGesturesEnabled = false,
        showCloseButton = true,
    ) { close ->
        Text(
            s.dailyScheduleActionTitle,
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(8.dp))
        Segmented(
            labels = listOf(s.dailyScheduleBlock, s.dailySchedulePause),
            selected = if (actionKey == CallBlockScheduleAction.BLOCK.storageKey) 0 else 1,
            onSelect = { index ->
                actionKey = if (index == 0) {
                    CallBlockScheduleAction.BLOCK.storageKey
                } else {
                    CallBlockScheduleAction.PAUSE.storageKey
                }
                errorText = null
            },
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(Modifier.height(18.dp))
        Text(
            s.dailySchedulePresetTitle,
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CallBlockSchedulePreset.entries.forEach { preset ->
                SchedulePresetChip(
                    preset = preset,
                    selected = presetKey == preset.storageKey,
                    onClick = {
                        startMinute = preset.startMinute
                        endMinute = preset.endMinute
                        presetKey = preset.storageKey
                        errorText = null
                    },
                )
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(
            s.dailyScheduleCustom,
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ScheduleTimeButton(
                label = s.dailyScheduleStartTime,
                value = formatScheduleMinute(startMinute),
                onClick = { timePickerTarget = "start" },
                modifier = Modifier.weight(1f),
            )
            ScheduleTimeButton(
                label = s.dailyScheduleEndTime,
                value = formatScheduleMinute(endMinute),
                onClick = { timePickerTarget = "end" },
                modifier = Modifier.weight(1f),
            )
        }
        if (endMinute < startMinute) {
            Text(
                s.dailyScheduleNextDay,
                style = MaterialTheme.typography.bodySmall,
                color = Primary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 7.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                s.dailyScheduleDaysTitle,
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                scheduleDaysLabel(weekdaysMask, s),
                style = MaterialTheme.typography.bodySmall,
                color = Primary,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            DayOfWeek.entries.forEach { day ->
                val bit = weekdayBit(day)
                ScheduleWeekdayChip(
                    label = s.dailyScheduleWeekdayShort(day),
                    selected = weekdaysMask and bit != 0,
                    onClick = {
                        val updated = weekdaysMask xor bit
                        if (updated == 0) {
                            errorText = s.dailyScheduleNoDayError
                        } else {
                            weekdaysMask = updated
                            errorText = null
                        }
                    },
                )
            }
        }
        errorText?.let { error ->
            Text(
                error,
                style = MaterialTheme.typography.bodySmall,
                color = AccentRed,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp),
            )
        }

        Spacer(Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Primary)
                .clickable {
                    val action = CallBlockScheduleAction.fromStorage(actionKey)
                        ?: CallBlockScheduleAction.BLOCK
                    val candidate = existing?.copy(
                        action = action,
                        startMinute = startMinute,
                        endMinute = endMinute,
                        presetKey = presetKey,
                        weekdaysMask = weekdaysMask,
                    ) ?: CallBlockTimeWindow.create(
                        action = action,
                        startMinute = startMinute,
                        endMinute = endMinute,
                        preset = presetKey?.let(CallBlockSchedulePreset::fromStorage),
                    ).copy(weekdaysMask = weekdaysMask)
                    when (val result = onSave(candidate)) {
                        is CallBlockScheduleUpdate.Success -> close()
                        is CallBlockScheduleUpdate.Overlap -> {
                            errorText = s.dailyScheduleOverlapError(
                                formatScheduleMinute(result.conflicting.startMinute),
                                formatScheduleMinute(result.conflicting.endMinute),
                            )
                        }
                        CallBlockScheduleUpdate.InvalidWindow -> errorText = s.dailyScheduleInvalidError
                        CallBlockScheduleUpdate.TooManyWindows -> errorText = s.dailyScheduleLimitReached
                        CallBlockScheduleUpdate.StorageFailure -> errorText = s.dailyScheduleStorageError
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                s.dailyScheduleSave,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.height(8.dp))
    }

    timePickerTarget?.let { target ->
        val initialMinute = if (target == "start") startMinute else endMinute
        CallBlockTimePickerDialog(
            title = if (target == "start") s.dailyScheduleStartTime else s.dailyScheduleEndTime,
            initialMinute = initialMinute,
            onDismiss = { timePickerTarget = null },
            onConfirm = { selectedMinute ->
                if (target == "start") startMinute = selectedMinute else endMinute = selectedMinute
                presetKey = null
                errorText = null
                timePickerTarget = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CallBlockTimePickerDialog(
    title: String,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    val common = appStrings().common
    val state = rememberTimePickerState(
        initialHour = initialMinute / 60,
        initialMinute = initialMinute % 60,
        is24Hour = true,
    )
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        ProvideAppDensity {
            Box(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 380.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(CardSurface)
                        .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(18.dp))
                    TimePicker(
                        state = state,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = FieldSurface,
                            clockDialSelectedContentColor = Color.White,
                            clockDialUnselectedContentColor = TextPrimary,
                            selectorColor = Primary,
                            containerColor = CardSurface,
                            periodSelectorBorderColor = Primary,
                            periodSelectorSelectedContainerColor = Primary,
                            periodSelectorUnselectedContainerColor = FieldSurface,
                            periodSelectorSelectedContentColor = Color.White,
                            periodSelectorUnselectedContentColor = TextSecondary,
                            timeSelectorSelectedContainerColor = Primary,
                            timeSelectorUnselectedContainerColor = FieldSurface,
                            timeSelectorSelectedContentColor = Color.White,
                            timeSelectorUnselectedContentColor = TextPrimary,
                        ),
                    )
                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(onClick = onDismiss)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = common.cancel,
                                style = MaterialTheme.typography.labelLarge,
                                color = Primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .height(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Primary)
                                .clickable { onConfirm(state.hour * 60 + state.minute) }
                                .padding(horizontal = 18.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = appStrings().blocker.dailyScheduleTimeConfirm,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                AppToastHost(modifier = Modifier.matchParentSize())
            }
        }
    }
}

@Composable
private fun SchedulePresetChip(
    preset: CallBlockSchedulePreset,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val s = appStrings().blocker
    Column(
        modifier = Modifier
            .width(112.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) BrandSoft else CardFill)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Text(
            schedulePresetLabel(preset, s),
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) Primary else TextPrimary,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "${formatScheduleMinute(preset.startMinute)}–${formatScheduleMinute(preset.endMinute)}",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
    }
}

@Composable
private fun ScheduleTimeButton(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CardFill)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(Modifier.height(3.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ScheduleWeekdayChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (selected) Primary else CardFill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) Color.White else TextSecondary,
            fontWeight = FontWeight.Bold,
        )
    }
}

private fun scheduleRange(window: CallBlockTimeWindow, s: CallBlockStrings): String = buildString {
    append(formatScheduleMinute(window.startMinute))
    append("–")
    append(formatScheduleMinute(window.endMinute))
    if (window.crossesMidnight) {
        append(" · ")
        append(s.dailyScheduleNextDay)
    }
}

private fun schedulePresetLabel(preset: CallBlockSchedulePreset?, s: CallBlockStrings): String = when (preset) {
    CallBlockSchedulePreset.MORNING -> s.dailyScheduleMorning
    CallBlockSchedulePreset.AFTERNOON -> s.dailyScheduleAfternoon
    CallBlockSchedulePreset.EVENING -> s.dailyScheduleEvening
    CallBlockSchedulePreset.NIGHT -> s.dailyScheduleNight
    null -> s.dailyScheduleCustom
}

private fun scheduleDaysLabel(weekdaysMask: Int, s: CallBlockStrings): String =
    if (weekdaysMask == ALL_WEEKDAYS_MASK) {
        s.dailyScheduleEveryDay
    } else {
        DayOfWeek.entries
            .filter { day -> weekdaysMask and weekdayBit(day) != 0 }
            .joinToString(", ") { day -> s.dailyScheduleWeekdayShort(day) }
    }

internal fun formatScheduleMinute(minute: Int): String =
    "%02d:%02d".format(minute / 60, minute % 60)

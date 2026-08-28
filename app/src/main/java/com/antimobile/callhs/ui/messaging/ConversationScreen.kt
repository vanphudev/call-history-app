package com.antimobile.callhs.ui.messaging

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.antimobile.callhs.data.messaging.model.MessageDirection
import com.antimobile.callhs.data.messaging.model.MessageState
import com.antimobile.callhs.data.messaging.model.SendFailure
import com.antimobile.callhs.data.messaging.model.SmsMessageItem
import com.antimobile.callhs.data.messaging.notification.MessagingForegroundState
import com.antimobile.callhs.data.messaging.role.SmsRole
import com.antimobile.callhs.i18n.appStrings
import com.antimobile.callhs.ui.components.AppBottomSheet
import com.antimobile.callhs.ui.components.AppMessageDialog
import com.antimobile.callhs.ui.components.Avatar
import com.antimobile.callhs.ui.components.DialogButton
import com.antimobile.callhs.ui.components.FilterOptionRow
import com.antimobile.callhs.ui.components.LoadingState
import com.antimobile.callhs.ui.components.SimBadge
import com.antimobile.callhs.ui.theme.AccentRed
import com.antimobile.callhs.ui.theme.AppBackground
import com.antimobile.callhs.ui.theme.CardSurface
import com.antimobile.callhs.ui.theme.FieldSurface
import com.antimobile.callhs.ui.theme.MessageIncomingBubble
import com.antimobile.callhs.ui.theme.MessageIncomingText
import com.antimobile.callhs.ui.theme.MessageOutgoingBubble
import com.antimobile.callhs.ui.theme.MessageOutgoingText
import com.antimobile.callhs.ui.theme.Primary
import com.antimobile.callhs.ui.theme.TextPrimary
import com.antimobile.callhs.ui.theme.TextSecondary
import com.antimobile.callhs.util.CallActions
import com.antimobile.callhs.util.TimeFormat
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationScreen(vm: ConversationViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val s = appStrings().messaging
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    var showSimSheet by remember { mutableStateOf(false) }
    var menuTarget by remember { mutableStateOf<SmsMessageItem?>(null) }
    var deleteTarget by remember { mutableStateOf<SmsMessageItem?>(null) }
    var previousMessageCount by remember { mutableIntStateOf(0) }
    var roleHeld by remember { mutableStateOf(SmsRole.isHeld(context)) }
    var permissionsReady by remember { mutableStateOf(SmsRole.hasCorePermissions(context)) }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permissionsReady = SmsRole.hasCorePermissions(context)
        if (permissionsReady) {
            vm.refreshCapability()
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        roleHeld = SmsRole.isHeld(context)
        permissionsReady = SmsRole.hasCorePermissions(context)
        if (roleHeld) {
            val missing = SmsRole.missingPermissions(context)
            if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray()) else vm.refreshCapability()
        }
    }
    val nearBottom by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = listState.layoutInfo.totalItemsCount
            total == 0 || last >= total - 3
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        roleHeld = SmsRole.isHeld(context)
        permissionsReady = SmsRole.hasCorePermissions(context)
        vm.refreshCapability()
    }
    DisposableEffect(vm.threadId) {
        val id = vm.threadId
        MessagingForegroundState.visibleThreadId = id
        onDispose { if (MessagingForegroundState.visibleThreadId == id) MessagingForegroundState.visibleThreadId = null }
    }
    LaunchedEffect(vm.messages.size) {
        val shouldScroll = previousMessageCount == 0 || nearBottom
        previousMessageCount = vm.messages.size
        if (shouldScroll && vm.messages.isNotEmpty()) listState.animateScrollToItem(vm.messages.lastIndex)
    }

    Column(
        modifier = Modifier.fillMaxSize().background(AppBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .statusBarsPadding()
            .imePadding(),
    ) {
        ConversationTopBar(vm = vm, onBack = { focusManager.clearFocus(); onBack() })
        when {
            !roleHeld -> MessagingGateState(
                title = s.roleTitle,
                body = s.roleBody,
                action = s.setDefault to { SmsRole.requestIntent(context)?.let(roleLauncher::launch) },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            !permissionsReady -> MessagingGateState(
                title = s.permissionTitle,
                body = s.permissionBody,
                action = s.grantPermissions to {
                    permissionLauncher.launch(SmsRole.missingPermissions(context).toTypedArray())
                },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            vm.loading && vm.messages.isEmpty() -> LoadingState(Modifier.weight(1f).fillMaxWidth(), s.loading)
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            ) {
                itemsIndexed(vm.messages, key = { _, item -> item.id }) { index, item ->
                    val previous = vm.messages.getOrNull(index - 1)
                    if (previous == null || !sameDay(previous.timestampMillis, item.timestampMillis)) {
                        DaySeparator(item.timestampMillis)
                    }
                    MessageBubble(
                        item = item,
                        showSim = vm.sims.size > 1,
                        simLabel = item.subscriptionId?.let { subId ->
                            vm.sims.firstOrNull { it.subscriptionId == subId }?.label
                        },
                        onLongClick = { menuTarget = item },
                    )
                }
            }
        }
        if (roleHeld && permissionsReady) {
            MessageComposer(
                vm = vm,
                onChooseSim = { if (vm.sims.size > 1) showSimSheet = true },
            )
        }
    }

    if (showSimSheet) {
        AppBottomSheet(onDismiss = { showSimSheet = false }, title = s.chooseSim) { close ->
            if (vm.sims.isEmpty()) {
                Text(s.noActiveSim, color = TextSecondary, modifier = Modifier.padding(20.dp))
            } else vm.sims.forEach { sim ->
                val detail = listOfNotNull(sim.displayName, sim.carrier).distinct().joinToString(" · ")
                FilterOptionRow(
                    icon = Icons.Rounded.SimCard,
                    label = sim.label,
                    selected = sim.subscriptionId == vm.selectedSubId,
                    supportingText = detail,
                    onClick = { vm.selectSim(sim.subscriptionId); close() },
                )
            }
        }
    }
    menuTarget?.let { item ->
        AppBottomSheet(onDismiss = { menuTarget = null }) { close ->
            FilterOptionRow(Icons.Rounded.ContentCopy, s.copy, false, onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText(s.messageHint, item.body))
                close()
            })
            if (item.state == MessageState.FAILED) {
                FilterOptionRow(Icons.Rounded.Refresh, s.retry, false, onClick = { vm.retry(item); close() })
            }
            FilterOptionRow(Icons.Rounded.Delete, s.delete, false, onClick = { close(); deleteTarget = item })
        }
    }
    deleteTarget?.let { item ->
        AppMessageDialog(
            onDismissRequest = { deleteTarget = null },
            title = s.deleteMessageTitle,
            message = s.deleteMessageBody,
            buttons = listOf(
                DialogButton(appStrings().common.cancel, TextSecondary) { deleteTarget = null },
                DialogButton(s.delete, AccentRed, bold = true) { vm.deleteMessage(item.id); deleteTarget = null },
            ),
        )
    }
}

@Composable
private fun ConversationTopBar(vm: ConversationViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, appStrings().common.back, onBack)
        Avatar(vm.title, vm.photoUri, vm.title != vm.address, size = 40.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(vm.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (vm.title != vm.address) Text(vm.address, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1)
        }
        CircleIconButton(Icons.Rounded.Call, appStrings().callDetail.call, { CallActions.dial(context, vm.address) })
    }
}

@Composable
private fun CircleIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(46.dp).clip(CircleShape).combinedClickable(onClick = onClick, onLongClick = {}),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, description, tint = TextSecondary, modifier = Modifier.size(24.dp)) }
}

@Composable
private fun DaySeparator(timestamp: Long) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center) {
        Text(
            TimeFormat.sectionLabel(timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(FieldSurface).padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun MessageBubble(item: SmsMessageItem, showSim: Boolean, simLabel: String?, onLongClick: () -> Unit) {
    val outgoing = item.direction == MessageDirection.OUTGOING
    val s = appStrings().messaging
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = if (outgoing) Arrangement.End else Arrangement.Start,
    ) {
        Column(horizontalAlignment = if (outgoing) Alignment.End else Alignment.Start) {
            Box(
                modifier = Modifier.fillMaxWidth(0.82f)
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (outgoing) 18.dp else 5.dp,
                            bottomEnd = if (outgoing) 5.dp else 18.dp,
                        )
                    )
                    .background(if (outgoing) MessageOutgoingBubble else MessageIncomingBubble)
                    .combinedClickable(onClick = {}, onLongClick = onLongClick)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    item.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (outgoing) MessageOutgoingText else MessageIncomingText,
                )
            }
            Row(
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (showSim && simLabel != null) {
                    SimBadge(simLabel)
                    Spacer(Modifier.width(5.dp))
                }
                Text(TimeFormat.time(item.timestampMillis), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                if (outgoing) {
                    Spacer(Modifier.width(5.dp))
                    Text(
                        when (item.state) {
                            MessageState.QUEUED -> s.stateQueued
                            MessageState.SENDING -> s.stateSending
                            MessageState.SENT_TO_NETWORK -> s.stateSent
                            MessageState.DELIVERED -> s.stateDelivered
                            MessageState.FAILED -> s.stateFailed
                            MessageState.RECEIVED -> ""
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.state == MessageState.FAILED) AccentRed else TextSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageComposer(vm: ConversationViewModel, onChooseSim: () -> Unit) {
    val s = appStrings().messaging
    val segment = vm.segmentInfo()
    Column(
        modifier = Modifier.fillMaxWidth().background(CardSurface)
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
            .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
    ) {
        if (segment.parts > 1) {
            Text(
                s.segmentCount(segment.parts, segment.remainingInPart),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.padding(start = 50.dp, bottom = 4.dp),
            )
        }
        vm.sendFailure?.let {
            Text(sendFailureLabel(it), style = MaterialTheme.typography.bodySmall, color = AccentRed, modifier = Modifier.padding(horizontal = 50.dp, vertical = 3.dp))
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape)
                    .background(if (vm.sims.size > 1) FieldSurface else Color.Transparent)
                    .combinedClickable(onClick = onChooseSim, onLongClick = {}),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.SimCard, s.chooseSim, tint = if (vm.selectedSubId != null) Primary else TextSecondary)
            }
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier.weight(1f).clip(RoundedCornerShape(22.dp)).background(FieldSurface)
                    .padding(horizontal = 15.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (vm.draft.isEmpty()) Text(s.messageHint, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                BasicTextField(
                    value = vm.draft,
                    onValueChange = vm::updateDraft,
                    minLines = 1,
                    maxLines = 5,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                    cursorBrush = SolidColor(Primary),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        keyboardType = KeyboardType.Text,
                    ),
                    modifier = Modifier.fillMaxWidth().onFocusChanged { },
                )
            }
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape)
                    .background(if (vm.draft.isNotBlank() && !vm.sending) Primary else FieldSurface)
                    .combinedClickable(
                        enabled = vm.draft.isNotBlank() && !vm.sending,
                        onClick = vm::send,
                        onLongClick = {},
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, s.send, tint = if (vm.draft.isNotBlank() && !vm.sending) Color.White else TextSecondary)
            }
        }
    }
}

@Composable
private fun sendFailureLabel(failure: SendFailure): String {
    val s = appStrings().messaging
    return when (failure) {
        SendFailure.SIM_UNAVAILABLE -> s.simUnavailable
        SendFailure.NO_TELEPHONY -> s.unsupportedBody
        SendFailure.NOT_DEFAULT_APP -> s.roleRequired
        SendFailure.INVALID_RECIPIENT -> s.invalidRecipient
        else -> s.sendFailed
    }
}

private fun sameDay(first: Long, second: Long): Boolean {
    val zone = ZoneId.systemDefault()
    return Instant.ofEpochMilli(first).atZone(zone).toLocalDate() ==
        Instant.ofEpochMilli(second).atZone(zone).toLocalDate()
}

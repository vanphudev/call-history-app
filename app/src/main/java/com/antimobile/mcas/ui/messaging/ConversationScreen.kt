package com.antimobile.mcas.ui.messaging

import android.Manifest
import android.os.Build
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.Subject
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.antimobile.mcas.data.messaging.model.MessageDirection
import com.antimobile.mcas.data.messaging.model.MessageState
import com.antimobile.mcas.data.messaging.model.MessageAttachment
import com.antimobile.mcas.data.messaging.model.MessageTransport
import com.antimobile.mcas.data.messaging.model.MmsDownloadState
import com.antimobile.mcas.data.messaging.model.SendFailure
import com.antimobile.mcas.data.messaging.model.SmsMessageItem
import com.antimobile.mcas.data.messaging.notification.MessagingForegroundState
import com.antimobile.mcas.data.messaging.role.SmsRole
import com.antimobile.mcas.i18n.appStrings
import com.antimobile.mcas.ui.components.ActionGlyph
import com.antimobile.mcas.ui.components.AppBottomSheet
import com.antimobile.mcas.ui.components.AppMessageDialog
import com.antimobile.mcas.ui.components.Avatar
import com.antimobile.mcas.ui.components.ContextAction
import com.antimobile.mcas.ui.components.ContextMenuOverlay
import com.antimobile.mcas.ui.components.DialogButton
import com.antimobile.mcas.ui.components.FilterOptionRow
import com.antimobile.mcas.ui.components.FrostedScrollButton
import com.antimobile.mcas.ui.components.FrostedSurface
import com.antimobile.mcas.ui.components.LoadingState
import com.antimobile.mcas.ui.theme.AccentRed
import com.antimobile.mcas.ui.theme.AppBackground
import com.antimobile.mcas.ui.theme.CardSurface
import com.antimobile.mcas.ui.theme.FieldSurface
import com.antimobile.mcas.ui.theme.MessageIncomingBubble
import com.antimobile.mcas.ui.theme.MessageIncomingText
import com.antimobile.mcas.ui.theme.MessageOutgoingBubble
import com.antimobile.mcas.ui.theme.MessageOutgoingText
import com.antimobile.mcas.ui.theme.Primary
import com.antimobile.mcas.ui.theme.TextPrimary
import com.antimobile.mcas.ui.theme.TextSecondary
import com.antimobile.mcas.util.CallActions
import com.antimobile.mcas.util.TimeFormat
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationScreen(vm: ConversationViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val s = appStrings().messaging
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    val backdropLayer = rememberGraphicsLayer()
    var backdropCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var showSimSheet by remember { mutableStateOf(false) }
    var showConversationSettings by remember { mutableStateOf(false) }
    var menuTarget by remember { mutableStateOf<MessageContextTarget?>(null) }
    var deleteAfterMenuCloses by remember { mutableStateOf<SmsMessageItem?>(null) }
    var deleteTarget by remember { mutableStateOf<SmsMessageItem?>(null) }
    var deleteConversationRequested by remember { mutableStateOf(false) }
    var fullscreenAttachment by remember { mutableStateOf<MessageAttachment?>(null) }
    var showMmsConfirmation by remember { mutableStateOf(false) }
    var previousMessageCount by remember { mutableIntStateOf(0) }
    var positionedAddress by remember { mutableStateOf<String?>(null) }
    var roleHeld by remember { mutableStateOf(SmsRole.isHeld(context)) }
    var permissionsReady by remember { mutableStateOf(SmsRole.hasCorePermissions(context)) }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(vm::selectImage)
    }
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
            val missing = SmsRole.missingCorePermissions(context)
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
    val statusBarPx = WindowInsets.statusBars.getTop(density)
    val navigationBarPx = WindowInsets.navigationBars.getBottom(density)
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val leaveScreen: () -> Unit = { focusManager.clearFocus(); onBack() }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        roleHeld = SmsRole.isHeld(context)
        permissionsReady = SmsRole.hasCorePermissions(context)
        vm.refreshCapability()
    }
    DisposableEffect(vm.threadId) {
        val id = vm.threadId
        MessagingForegroundState.visibleThreadId = id
        onDispose { if (MessagingForegroundState.visibleThreadId == id) MessagingForegroundState.visibleThreadId = null }
    }
    LaunchedEffect(vm.address, vm.messages.size) {
        if (vm.messages.isEmpty()) {
            previousMessageCount = 0
            positionedAddress = null
            return@LaunchedEffect
        }
        val initialPosition = positionedAddress != vm.address
        val appendedWhileNearBottom = !initialPosition && vm.messages.size > previousMessageCount && nearBottom
        if (initialPosition) {
            // Lần đầu mở phải xuất hiện ngay ở tin mới nhất, không chạy xuyên cả lịch sử.
            listState.scrollToItem(vm.messages.lastIndex)
            positionedAddress = vm.address
        } else if (appendedWhileNearBottom) {
            listState.animateScrollToItem(vm.messages.lastIndex)
        }
        previousMessageCount = vm.messages.size
    }

    Box(Modifier.fillMaxSize().background(AppBackground)) {
        Column(
            modifier = Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .statusBarsPadding()
                .imePadding(),
        ) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .onGloballyPositioned { backdropCoords = it }
                        .drawWithContent {
                            backdropLayer.record { this@drawWithContent.drawContent() }
                            drawLayer(backdropLayer)
                        },
                ) {
                    when {
                        !roleHeld -> MessagingGateState(
                            title = s.roleTitle,
                            body = s.roleBody,
                            action = s.setDefault to { SmsRole.requestIntent(context)?.let(roleLauncher::launch) },
                            modifier = Modifier.fillMaxSize().padding(top = ConversationHeaderClearance),
                        )
                        !permissionsReady -> MessagingGateState(
                            title = s.permissionTitle,
                            body = s.permissionBody,
                            action = s.grantPermissions to {
                                permissionLauncher.launch(SmsRole.missingCorePermissions(context).toTypedArray())
                            },
                            modifier = Modifier.fillMaxSize().padding(top = ConversationHeaderClearance),
                        )
                        vm.loading && vm.messages.isEmpty() -> LoadingState(
                            Modifier.fillMaxSize().padding(top = ConversationHeaderClearance),
                            s.loading,
                        )
                        else -> LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize().graphicsLayer {
                                alpha = if (vm.messages.isEmpty() || positionedAddress == vm.address) 1f else 0f
                            },
                            contentPadding = PaddingValues(
                                start = 12.dp,
                                top = ConversationHeaderClearance,
                                end = 12.dp,
                                bottom = 10.dp,
                            ),
                        ) {
                            itemsIndexed(vm.messages, key = { _, item -> item.stableId }) { index, item ->
                                val previous = vm.messages.getOrNull(index - 1)
                                if (previous == null || !sameDay(previous.timestampMillis, item.timestampMillis)) {
                                    DaySeparator(item.timestampMillis)
                                }
                                val showSim = vm.sims.size > 1
                                val simLabel = item.subscriptionId?.let { subId ->
                                    vm.sims.firstOrNull { it.subscriptionId == subId }?.label
                                }
                                val downloading = item.id in vm.downloadingMmsIds
                                MessageBubble(
                                    item = item,
                                    showSim = showSim,
                                    simLabel = simLabel,
                                    activeInMenu = menuTarget?.message?.stableId == item.stableId,
                                    onLongClick = { bounds ->
                                        menuTarget = MessageContextTarget(item, bounds, showSim, simLabel, downloading)
                                    },
                                    downloading = downloading,
                                    onDownload = { vm.downloadMms(item.id) },
                                    onOpenAttachment = { fullscreenAttachment = it },
                                )
                            }
                        }
                    }
                }

                ConversationHeaderOverlay(
                    vm = vm,
                    backdropLayer = backdropLayer,
                    backdropCoords = backdropCoords,
                    onBack = leaveScreen,
                    onSettings = { showConversationSettings = true },
                )
            }

            if (roleHeld && permissionsReady) {
                MessageComposer(
                    vm = vm,
                    onChooseSim = { if (vm.sims.size > 1) showSimSheet = true },
                    onChooseImage = {
                        imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onSend = {
                        if (vm.preparedImage != null || vm.subject.isNotBlank()) showMmsConfirmation = true else vm.send()
                    },
                )
            }
        }

        menuTarget?.let { target ->
            val copyText = listOfNotNull(
                target.message.subject?.takeIf(String::isNotBlank),
                target.message.body.takeIf(String::isNotBlank),
            ).joinToString("\n")
            ContextMenuOverlay(
                bounds = target.bounds,
                actions = buildList {
                    if (copyText.isNotBlank()) {
                        add(
                            ContextAction(
                                glyph = ActionGlyph.Vector(Icons.Rounded.ContentCopy, TextSecondary),
                                desc = s.copy,
                                onClick = { CallActions.copyContent(context, copyText) },
                            )
                        )
                    }
                    add(
                        ContextAction(
                            glyph = ActionGlyph.Vector(Icons.Rounded.Delete, AccentRed),
                            desc = s.delete,
                            onClick = { deleteAfterMenuCloses = target.message },
                        )
                    )
                },
                topInsetPx = statusBarPx,
                bottomInsetPx = maxOf(navigationBarPx, imeBottomPx),
                onClosed = {
                    menuTarget = null
                    deleteAfterMenuCloses?.let { deleteTarget = it }
                    deleteAfterMenuCloses = null
                },
                lifted = {
                    MessageBubbleCard(
                        item = target.message,
                        showSim = target.showSim,
                        simLabel = target.simLabel,
                        downloading = target.downloading,
                        textSelectionEnabled = false,
                    )
                },
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
    if (showConversationSettings) {
        AppBottomSheet(
            onDismiss = { showConversationSettings = false },
            title = appStrings().common.settings,
            showCloseButton = true,
        ) { close ->
            FilterOptionRow(Icons.Rounded.Call, appStrings().callDetail.call, false, onClick = {
                CallActions.dial(context, vm.address)
                close()
            })
            FilterOptionRow(Icons.Rounded.ContentCopy, appStrings().callDetail.copyNumber, false, onClick = {
                CallActions.copy(context, vm.address)
                close()
            })
            if (vm.threadId != null) {
                FilterOptionRow(Icons.Rounded.Delete, s.deleteConversation, false, onClick = {
                    close()
                    deleteConversationRequested = true
                })
            }
        }
    }
    deleteTarget?.let { item ->
        AppMessageDialog(
            onDismissRequest = { deleteTarget = null },
            title = s.deleteMessageTitle,
            message = s.deleteMessageBody,
            buttons = listOf(
                DialogButton(appStrings().common.cancel, TextSecondary) { deleteTarget = null },
                DialogButton(s.delete, AccentRed, bold = true) { vm.deleteMessage(item); deleteTarget = null },
            ),
        )
    }
    if (deleteConversationRequested) {
        AppMessageDialog(
            onDismissRequest = { deleteConversationRequested = false },
            title = s.deleteConversationTitle,
            message = s.deleteConversationBody,
            buttons = listOf(
                DialogButton(appStrings().common.cancel, TextSecondary) { deleteConversationRequested = false },
                DialogButton(s.delete, AccentRed, bold = true) {
                    deleteConversationRequested = false
                    vm.deleteConversation(leaveScreen)
                },
            ),
        )
    }
    if (showMmsConfirmation) {
        AppMessageDialog(
            onDismissRequest = { showMmsConfirmation = false },
            title = s.mmsConfirmTitle,
            message = s.mmsConfirmBody,
            buttons = listOf(
                DialogButton(appStrings().common.cancel, TextSecondary) { showMmsConfirmation = false },
                DialogButton(s.sendMms, Primary, bold = true) {
                    showMmsConfirmation = false
                    vm.sendMms()
                },
            ),
        )
    }
    fullscreenAttachment?.let { attachment ->
        Dialog(
            onDismissRequest = { fullscreenAttachment = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                Modifier.fillMaxSize().background(Color.Black).combinedClickable(
                    onClick = { fullscreenAttachment = null },
                    onLongClick = {},
                ),
                contentAlignment = Alignment.Center,
            ) {
                MmsImage(attachment.contentUri, Modifier.fillMaxWidth(), ContentScale.Fit)
                Box(
                    Modifier.align(Alignment.TopEnd).padding(12.dp).size(44.dp).clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .combinedClickable(onClick = { fullscreenAttachment = null }, onLongClick = {}),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Close, appStrings().common.dismiss, tint = Color.White) }
            }
        }
    }
}

private val ConversationHeaderClearance = 132.dp

@Composable
private fun ConversationHeaderOverlay(
    vm: ConversationViewModel,
    backdropLayer: androidx.compose.ui.graphics.layer.GraphicsLayer,
    backdropCoords: LayoutCoordinates?,
    onBack: () -> Unit,
    onSettings: () -> Unit,
) {
    val context = LocalContext.current
    val displayTitle = vm.title.takeIf { it.isNotBlank() && it != vm.address }
        ?: appStrings().messaging.recipientTitle
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 10.dp, top = 6.dp, end = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FrostedScrollButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = appStrings().common.back,
                backdropLayer = backdropLayer,
                contentCoords = backdropCoords,
                onClick = onBack,
                buttonSize = 44.dp,
                iconSize = 23.dp,
            )
            Spacer(Modifier.width(8.dp))
            FrostedSurface(
                backdropLayer = backdropLayer,
                contentCoords = backdropCoords,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(28.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(vm.title, vm.photoUri, vm.title != vm.address, size = 40.dp)
                    Spacer(Modifier.width(9.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            displayTitle,
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            vm.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            FrostedScrollButton(
                icon = Icons.Rounded.Settings,
                contentDescription = appStrings().common.settings,
                backdropLayer = backdropLayer,
                contentCoords = backdropCoords,
                onClick = onSettings,
                buttonSize = 44.dp,
                iconSize = 22.dp,
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FrostedScrollButton(
                icon = Icons.Rounded.Call,
                contentDescription = appStrings().callDetail.call,
                backdropLayer = backdropLayer,
                contentCoords = backdropCoords,
                onClick = { CallActions.dial(context, vm.address) },
                buttonSize = 44.dp,
                iconSize = 21.dp,
            )
            FrostedScrollButton(
                icon = Icons.Rounded.ContentCopy,
                contentDescription = appStrings().callDetail.copyNumber,
                backdropLayer = backdropLayer,
                contentCoords = backdropCoords,
                onClick = { CallActions.copy(context, vm.address) },
                buttonSize = 44.dp,
                iconSize = 21.dp,
            )
        }
    }
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
private fun MessageBubble(
    item: SmsMessageItem,
    showSim: Boolean,
    simLabel: String?,
    activeInMenu: Boolean,
    onLongClick: (Rect) -> Unit,
    downloading: Boolean,
    onDownload: () -> Unit,
    onOpenAttachment: (MessageAttachment) -> Unit,
) {
    val outgoing = item.direction == MessageDirection.OUTGOING
    var coordinates by remember(item.stableId) { mutableStateOf<LayoutCoordinates?>(null) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = if (outgoing) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(0.82f)
                .onGloballyPositioned { coordinates = it }
                .graphicsLayer { alpha = if (activeInMenu) 0f else 1f },
        ) {
            MessageBubbleCard(
                item = item,
                showSim = showSim,
                simLabel = simLabel,
                downloading = downloading,
                onBubbleLongClick = {
                    coordinates?.takeIf(LayoutCoordinates::isAttached)?.boundsInWindow()?.let(onLongClick)
                },
                onDownload = onDownload,
                onOpenAttachment = onOpenAttachment,
                textSelectionEnabled = true,
            )
        }
    }
}

private data class MessageContextTarget(
    val message: SmsMessageItem,
    val bounds: Rect,
    val showSim: Boolean,
    val simLabel: String?,
    val downloading: Boolean,
)

@Composable
private fun MessageBubbleCard(
    item: SmsMessageItem,
    showSim: Boolean,
    simLabel: String?,
    downloading: Boolean,
    onBubbleLongClick: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null,
    onOpenAttachment: ((MessageAttachment) -> Unit)? = null,
    textSelectionEnabled: Boolean,
) {
    val outgoing = item.direction == MessageDirection.OUTGOING
    val s = appStrings().messaging
    val messageColor = if (outgoing) MessageOutgoingText else MessageIncomingText
    val bubbleShape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (outgoing) 18.dp else 5.dp,
        bottomEnd = if (outgoing) 5.dp else 18.dp,
    )
    val stateLabel = if (outgoing) {
        when (item.state) {
            MessageState.QUEUED -> s.stateQueued
            MessageState.SENDING -> s.stateSending
            MessageState.SENT_TO_NETWORK -> s.stateSent
            MessageState.DELIVERED -> s.stateDelivered
            MessageState.FAILED -> s.stateFailed
            MessageState.RECEIVED -> ""
        }
    } else ""

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (outgoing) Alignment.End else Alignment.Start,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().clip(bubbleShape)
                .background(if (outgoing) MessageOutgoingBubble else MessageIncomingBubble),
        ) {
            // Lớp gesture nằm SAU nội dung: vùng trống mở menu; vùng chữ ở trên vẫn nhận long-press để chọn text.
            if (onBubbleLongClick != null) {
                Box(
                    Modifier.matchParentSize().combinedClickable(
                        onClick = {},
                        onLongClick = onBubbleLongClick,
                    )
                )
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
                item.subject?.takeIf(String::isNotBlank)?.let { subject ->
                    SelectableMessageText(textSelectionEnabled) {
                        Text(
                            subject,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = messageColor,
                        )
                    }
                    if (item.body.isNotBlank() || item.attachments.isNotEmpty()) Spacer(Modifier.height(5.dp))
                }
                item.attachments.forEach { attachment ->
                    MmsImage(
                        attachment.contentUri,
                        Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(12.dp))
                            .then(
                                if (onOpenAttachment != null) {
                                    Modifier.combinedClickable(
                                        onClick = { onOpenAttachment(attachment) },
                                        onLongClick = { onBubbleLongClick?.invoke() },
                                    )
                                } else Modifier
                            ),
                        ContentScale.Crop,
                    )
                    Spacer(Modifier.height(7.dp))
                }
                if (item.body.isNotBlank()) {
                    SelectableMessageText(textSelectionEnabled) {
                        Text(
                            item.body,
                            style = MaterialTheme.typography.bodyLarge,
                            color = messageColor,
                        )
                    }
                }
                if (
                    item.transport == MessageTransport.MMS &&
                    item.mmsDownloadState in setOf(MmsDownloadState.PENDING, MmsDownloadState.FAILED)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = if (outgoing) 0.16f else 0.55f))
                            .then(
                                if (onDownload != null) {
                                    Modifier.combinedClickable(
                                        onClick = { if (!downloading) onDownload() },
                                        onLongClick = { onBubbleLongClick?.invoke() },
                                    )
                                } else Modifier
                            )
                            .padding(horizontal = 10.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            if (downloading) Icons.Rounded.Refresh else Icons.Rounded.Download,
                            null,
                            tint = if (outgoing) MessageOutgoingText else Primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            when {
                                downloading -> s.downloadingMms
                                item.mmsDownloadState == MmsDownloadState.FAILED -> s.mmsDownloadFailed
                                else -> s.downloadMms
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = messageColor,
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                .then(
                    if (onBubbleLongClick != null) {
                        Modifier.combinedClickable(onClick = {}, onLongClick = onBubbleLongClick)
                    } else Modifier
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (showSim && !simLabel.isNullOrBlank()) {
                MessageMetadataText(simLabel)
                MessageMetadataText("·")
            }
            MessageMetadataText(TimeFormat.time(item.timestampMillis))
            if (stateLabel.isNotBlank()) {
                MessageMetadataText("·")
                MessageMetadataText(
                    stateLabel,
                    color = if (item.state == MessageState.FAILED) AccentRed else TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun SelectableMessageText(enabled: Boolean, content: @Composable () -> Unit) {
    if (enabled) SelectionContainer(content = content) else content()
}

@Composable
private fun MessageMetadataText(text: String, color: Color = TextSecondary) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1)
}

@Composable
private fun MessageComposer(
    vm: ConversationViewModel,
    onChooseSim: () -> Unit,
    onChooseImage: () -> Unit,
    onSend: () -> Unit,
) {
    val s = appStrings().messaging
    val segment = vm.segmentInfo()
    val canSend = (vm.draft.isNotBlank() || vm.preparedImage != null) && !vm.sending && !vm.processingImage
    Column(
        modifier = Modifier.fillMaxWidth().background(CardSurface)
            .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
            .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
    ) {
        if (vm.preparedImage == null && segment.parts > 1) {
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
        if (vm.processingImage) {
            Text(s.imageProcessing, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.padding(horizontal = 50.dp, vertical = 4.dp))
        }
        vm.preparedImage?.let { image ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 50.dp, end = 50.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MmsImage(
                    image.sourceUri,
                    Modifier.size(76.dp).clip(RoundedCornerShape(14.dp)),
                    ContentScale.Crop,
                )
                Spacer(Modifier.width(9.dp))
                Text(s.mmsLabel, style = MaterialTheme.typography.labelLarge, color = Primary, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(5.dp))
                Box(
                    Modifier.size(38.dp).clip(CircleShape).combinedClickable(onClick = vm::removeImage, onLongClick = {}),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Close, s.removeAttachment, tint = TextSecondary) }
            }
        }
        if (vm.subjectVisible) {
            BasicTextField(
                value = vm.subject,
                onValueChange = vm::updateSubject,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                cursorBrush = SolidColor(Primary),
                modifier = Modifier.fillMaxWidth().padding(start = 50.dp, end = 50.dp, bottom = 7.dp)
                    .clip(RoundedCornerShape(16.dp)).background(FieldSurface)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                decorationBox = { inner ->
                    if (vm.subject.isEmpty()) Text(s.subjectHint, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    inner()
                },
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape)
                    .background(FieldSurface)
                    .combinedClickable(onClick = onChooseImage, onLongClick = {}),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.AddPhotoAlternate, s.attachPhoto, tint = if (vm.preparedImage != null) Primary else TextSecondary)
            }
            Spacer(Modifier.width(3.dp))
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape)
                    .combinedClickable(onClick = vm::toggleSubject, onLongClick = {}),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Rounded.Subject, s.subjectHint, tint = if (vm.subjectVisible) Primary else TextSecondary)
            }
            Spacer(Modifier.width(3.dp))
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
            if (vm.sims.size > 1) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).combinedClickable(onClick = onChooseSim, onLongClick = {}),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.SimCard, s.chooseSim, tint = if (vm.selectedSubId != null) Primary else TextSecondary) }
                Spacer(Modifier.width(4.dp))
            }
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape)
                    .background(if (canSend) Primary else FieldSurface)
                    .combinedClickable(
                        enabled = canSend,
                        onClick = onSend,
                        onLongClick = {},
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Rounded.Send, s.send, tint = if (canSend) Color.White else TextSecondary)
            }
        }
    }
}

@Composable
private fun MmsImage(uriString: String, modifier: Modifier, contentScale: ContentScale) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, uriString) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val uri = Uri.parse(uriString)
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                var sample = 1
                while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) sample *= 2
                val options = BitmapFactory.Options().apply { inSampleSize = sample.coerceAtLeast(1) }
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            }.getOrNull()
        }
    }
    Box(modifier.background(FieldSurface), contentAlignment = Alignment.Center) {
        bitmap?.let {
            Image(it.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = contentScale)
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
        SendFailure.IMAGE_UNREADABLE -> s.imageUnreadable
        SendFailure.IMAGE_TOO_LARGE -> s.imageTooLarge
        SendFailure.SUBJECT_TOO_LONG -> s.subjectTooLong
        SendFailure.MMS_DISABLED -> s.mmsDisabled
        SendFailure.MOBILE_DATA_DISABLED -> s.mobileDataDisabled
        else -> s.sendFailed
    }
}

private fun sameDay(first: Long, second: Long): Boolean {
    val zone = ZoneId.systemDefault()
    return Instant.ofEpochMilli(first).atZone(zone).toLocalDate() ==
        Instant.ofEpochMilli(second).atZone(zone).toLocalDate()
}

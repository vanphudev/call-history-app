package com.antimobile.callhs.ui.messaging

import android.Manifest
import android.os.Build
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.MarkChatUnread
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.platform.LocalContext
import com.antimobile.callhs.data.agency.normalizeForSearch
import com.antimobile.callhs.data.messaging.model.ConversationSummary
import com.antimobile.callhs.data.messaging.model.MessageDirection
import com.antimobile.callhs.data.messaging.model.MessageState
import com.antimobile.callhs.data.messaging.role.SmsRole
import com.antimobile.callhs.i18n.appStrings
import com.antimobile.callhs.ui.components.AppBottomSheet
import com.antimobile.callhs.ui.components.AppMessageDialog
import com.antimobile.callhs.ui.components.Avatar
import com.antimobile.callhs.ui.components.DialogButton
import com.antimobile.callhs.ui.components.EmptyState
import com.antimobile.callhs.ui.components.FilterOptionRow
import com.antimobile.callhs.ui.components.LoadingState
import com.antimobile.callhs.ui.components.SimBadge
import com.antimobile.callhs.ui.theme.AccentAmber
import com.antimobile.callhs.ui.theme.AccentAmberBg
import com.antimobile.callhs.ui.theme.AccentRed
import com.antimobile.callhs.ui.theme.CardSurface
import com.antimobile.callhs.ui.theme.DividerColor
import com.antimobile.callhs.ui.theme.Primary
import com.antimobile.callhs.ui.theme.TextPrimary
import com.antimobile.callhs.ui.theme.TextSecondary
import com.antimobile.callhs.util.PhoneKey
import com.antimobile.callhs.util.SimInfo
import com.antimobile.callhs.util.TimeFormat

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationListPage(
    vm: ConversationListViewModel,
    query: String,
    onOpenConversation: (ConversationSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val s = appStrings().messaging
    var roleHeld by remember { mutableStateOf(SmsRole.isHeld(context)) }
    var permissionsReady by remember { mutableStateOf(SmsRole.hasCorePermissions(context)) }
    var selectedForMenu by remember { mutableStateOf<ConversationSummary?>(null) }
    var deleteTarget by remember { mutableStateOf<ConversationSummary?>(null) }
    val simLabels = remember(roleHeld, permissionsReady) {
        SimInfo.activeSims(context).associate { it.subscriptionId to it.simLabel }
    }
    val listState = rememberLazyListState()

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permissionsReady = SmsRole.hasCorePermissions(context)
        if (permissionsReady) {
            vm.refreshCapability()
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
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

    val refreshCapability = {
        roleHeld = SmsRole.isHeld(context)
        permissionsReady = SmsRole.hasCorePermissions(context)
        vm.refreshCapability()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { refreshCapability() }
    LaunchedEffect(roleHeld, permissionsReady) {
        if (roleHeld && permissionsReady) vm.refreshCapability()
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            !SmsRole.isMessagingSupported(context) -> MessagingGateState(
                iconWarning = true,
                title = s.unsupportedTitle,
                body = s.unsupportedBody,
                action = null,
            )

            !SmsRole.isAvailable(context) -> MessagingGateState(
                iconWarning = true,
                title = s.unsupportedTitle,
                body = s.unsupportedBody,
                action = null,
            )

            !roleHeld -> MessagingGateState(
                title = s.roleTitle,
                body = s.roleBody,
                action = s.setDefault to {
                    SmsRole.requestIntent(context)?.let(roleLauncher::launch)
                },
            )

            !permissionsReady -> MessagingGateState(
                title = s.permissionTitle,
                body = s.permissionBody,
                action = s.grantPermissions to {
                    permissionLauncher.launch(SmsRole.missingPermissions(context).toTypedArray())
                },
            )

            vm.loading && !vm.loaded -> LoadingState(modifier = Modifier.fillMaxSize(), text = s.loading)
            vm.loadFailed && vm.conversations.isEmpty() -> EmptyState(s.loadFailed, Modifier.fillMaxSize())
            else -> {
                val normalized = normalizeForSearch(query)
                val digitQuery = query.filter(Char::isDigit)
                val filtered = remember(vm.conversations, query) {
                    vm.conversations.filter { item ->
                        normalized.isBlank() ||
                            normalizeForSearch(item.title).contains(normalized) ||
                            normalizeForSearch(item.snippet).contains(normalized) ||
                            (digitQuery.isNotEmpty() && PhoneKey.matchesQuery(item.address, query))
                    }
                }
                val pullState = rememberPullToRefreshState()
                PullToRefreshBox(
                    isRefreshing = vm.refreshing,
                    onRefresh = vm::refresh,
                    state = pullState,
                    modifier = Modifier.fillMaxSize(),
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            state = pullState,
                            isRefreshing = vm.refreshing,
                            modifier = Modifier.align(Alignment.TopCenter),
                            color = Primary,
                            containerColor = CardSurface,
                        )
                    },
                ) {
                    if (filtered.isEmpty()) {
                        EmptyState(if (query.isBlank()) s.emptyTitle else s.noSearchResults, Modifier.fillMaxSize())
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 104.dp),
                        ) {
                            items(filtered, key = { it.threadId }) { conversation ->
                                ConversationRow(
                                    conversation = conversation,
                                    simLabel = conversation.lastSubscriptionId?.let(simLabels::get),
                                    onClick = { onOpenConversation(conversation) },
                                    onLongClick = { selectedForMenu = conversation },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedForMenu?.let { item ->
        AppBottomSheet(onDismiss = { selectedForMenu = null }, title = item.title) { close ->
            FilterOptionRow(
                icon = if (item.unreadCount > 0) Icons.Rounded.DoneAll else Icons.Rounded.MarkChatUnread,
                label = if (item.unreadCount > 0) s.markRead else s.markUnread,
                selected = false,
                onClick = {
                    if (item.unreadCount > 0) vm.markRead(item.threadId) else vm.markUnread(item.threadId)
                    close()
                },
            )
            FilterOptionRow(
                icon = Icons.Rounded.Delete,
                label = s.deleteConversation,
                selected = false,
                onClick = { close(); deleteTarget = item },
            )
        }
    }
    deleteTarget?.let { item ->
        AppMessageDialog(
            onDismissRequest = { deleteTarget = null },
            title = s.deleteConversationTitle,
            message = s.deleteConversationBody,
            buttons = listOf(
                DialogButton(appStrings().common.cancel, TextSecondary) { deleteTarget = null },
                DialogButton(s.delete, AccentRed, bold = true) {
                    vm.deleteConversation(item.threadId)
                    deleteTarget = null
                },
            ),
        )
    }
}

@Composable
internal fun MessagingGateState(
    title: String,
    body: String,
    action: Pair<String, () -> Unit>?,
    modifier: Modifier = Modifier,
    iconWarning: Boolean = false,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier.size(72.dp).clip(CircleShape)
                .background(if (iconWarning) AccentAmberBg else com.antimobile.callhs.ui.theme.BrandSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (iconWarning) Icons.Rounded.WarningAmber else Icons.Rounded.Sms,
                contentDescription = null,
                tint = if (iconWarning) AccentAmber else Primary,
                modifier = Modifier.size(34.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        action?.let { (label, onClick) ->
            Spacer(Modifier.height(22.dp))
            Box(
                modifier = Modifier.clip(RoundedCornerShape(22.dp)).background(Primary)
                    .combinedClickable(onClick = onClick, onLongClick = {})
                    .padding(horizontal = 22.dp, vertical = 12.dp),
            ) {
                Text(label, color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ConversationRow(
    conversation: ConversationSummary,
    simLabel: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val s = appStrings().messaging
    Row(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(
            label = conversation.title,
            photoUri = conversation.photoUri,
            isNamed = conversation.isNamed,
            size = 50.dp,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    conversation.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (conversation.unreadCount > 0) FontWeight.Bold else FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(TimeFormat.dayClock(conversation.timestampMillis), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
            Spacer(Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val prefix = if (conversation.lastDirection == MessageDirection.OUTGOING) "${s.youPrefix} " else ""
                Text(
                    prefix + conversation.snippet,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (conversation.lastState == MessageState.FAILED) AccentRed else TextSecondary,
                    fontWeight = if (conversation.unreadCount > 0) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                simLabel?.let { label ->
                    Spacer(Modifier.width(6.dp))
                    SimBadge(label)
                }
                if (conversation.unreadCount > 0) {
                    Spacer(Modifier.width(7.dp))
                    Box(Modifier.size(9.dp).clip(CircleShape).background(Primary))
                }
            }
        }
    }
    Box(Modifier.fillMaxWidth().padding(start = 78.dp).height(1.dp).background(DividerColor))
}

package com.antimobile.mcas.ui.messaging

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.platform.LocalContext
import com.antimobile.mcas.data.agency.normalizeForSearch
import com.antimobile.mcas.data.messaging.model.ConversationSummary
import com.antimobile.mcas.data.messaging.role.SmsRole
import com.antimobile.mcas.i18n.appStrings
import com.antimobile.mcas.ui.components.EmptyState
import com.antimobile.mcas.ui.components.LoadingState
import com.antimobile.mcas.ui.theme.AccentAmber
import com.antimobile.mcas.ui.theme.AccentAmberBg
import com.antimobile.mcas.ui.theme.CardSurface
import com.antimobile.mcas.ui.theme.Primary
import com.antimobile.mcas.ui.theme.TextPrimary
import com.antimobile.mcas.ui.theme.TextSecondary
import com.antimobile.mcas.util.PhoneKey
import com.antimobile.mcas.util.SimInfo

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ConversationListPage(
    vm: ConversationListViewModel,
    query: String,
    onOpenConversation: (ConversationSummary) -> Unit,
    modifier: Modifier = Modifier,
    activeMenuThreadId: Long? = null,
    onLongPressConversation: ((ConversationSummary, String?, Rect) -> Unit)? = null,
) {
    val context = LocalContext.current
    val s = appStrings().messaging
    var roleHeld by remember { mutableStateOf(SmsRole.isHeld(context)) }
    var permissionsReady by remember { mutableStateOf(SmsRole.hasCorePermissions(context)) }
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
            val missing = SmsRole.missingCorePermissions(context)
            if (missing.isNotEmpty()) permissionLauncher.launch(missing.toTypedArray()) else vm.refreshCapability()
        }
    }

    val refreshCapability = {
        roleHeld = SmsRole.isHeld(context)
        permissionsReady = SmsRole.hasCorePermissions(context)
        vm.refreshCapability()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refreshCapability() }
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
                    permissionLauncher.launch(SmsRole.missingCorePermissions(context).toTypedArray())
                },
            )

            vm.loading && !vm.loaded -> LoadingState(modifier = Modifier.fillMaxSize(), text = s.loading)
            vm.loadFailed && vm.conversations.isEmpty() -> EmptyState(
                text = s.loadFailed,
                modifier = Modifier.fillMaxSize(),
                icon = Icons.Rounded.Sms,
            )
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
                        EmptyState(
                            text = if (query.isBlank()) s.emptyTitle else s.noSearchResults,
                            modifier = Modifier.fillMaxSize(),
                            icon = Icons.Rounded.Sms,
                        )
                    } else {
                        val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 4.dp, bottom = navBottom + 96.dp),
                        ) {
                            items(filtered, key = { it.threadId }) { conversation ->
                                val simLabel = conversation.lastSubscriptionId?.let(simLabels::get)
                                ConversationListItem(
                                    conversation = conversation,
                                    simLabel = simLabel,
                                    onOpen = { onOpenConversation(conversation) },
                                    onLongPress = onLongPressConversation?.let { callback ->
                                        { bounds -> callback(conversation, simLabel, bounds) }
                                    },
                                    activeInMenu = activeMenuThreadId == conversation.threadId,
                                )
                            }
                        }
                    }
                }
            }
        }
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
                .background(if (iconWarning) AccentAmberBg else com.antimobile.mcas.ui.theme.BrandSoft),
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

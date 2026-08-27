package com.antimobile.callhs.ui.outgoing

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.antimobile.callhs.data.outgoing.OutgoingCallNotifier
import com.antimobile.callhs.data.outgoing.OutgoingCallOverlay
import com.antimobile.callhs.data.outgoing.OutgoingCallPresentation
import com.antimobile.callhs.data.outgoing.OutgoingCallRole
import com.antimobile.callhs.data.outgoing.OutgoingCallSettings
import com.antimobile.callhs.i18n.appStrings
import com.antimobile.callhs.ui.components.PanelCard
import com.antimobile.callhs.ui.theme.AccentAmber
import com.antimobile.callhs.ui.theme.AccentAmberBg
import com.antimobile.callhs.ui.theme.AccentGreen
import com.antimobile.callhs.ui.theme.AccentGreenBg
import com.antimobile.callhs.ui.theme.AppBackground
import com.antimobile.callhs.ui.theme.DividerColor
import com.antimobile.callhs.ui.theme.Primary
import com.antimobile.callhs.ui.theme.TextPrimary
import com.antimobile.callhs.ui.theme.TextSecondary
import com.antimobile.callhs.util.hasPermission

/** Màn riêng của tính năng cảnh báo cuộc gọi đi; không thuộc navigation/cài đặt bộ chặn. */
@Composable
fun OutgoingCallSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val strings = appStrings()
    val s = strings.outgoingCall
    LaunchedEffect(context) { OutgoingCallSettings.init(context) }

    var roleHeld by remember { mutableStateOf(OutgoingCallRole.isHeld(context)) }
    val roleAvailable = remember(context) { OutgoingCallRole.isAvailable(context) }
    var overlayAllowed by remember { mutableStateOf(OutgoingCallOverlay.canDraw(context)) }
    var notificationReadiness by remember { mutableStateOf(OutgoingCallNotifier.readiness(context)) }
    var phoneStateGranted by remember {
        mutableStateOf(hasPermission(context, Manifest.permission.READ_PHONE_STATE))
    }
    var pendingEnable by rememberSaveable { mutableStateOf(false) }
    var pendingOffNetwork by rememberSaveable { mutableStateOf(false) }

    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        roleHeld = OutgoingCallRole.isHeld(context)
        if (pendingEnable && roleHeld) OutgoingCallSettings.setEnabled(context, true)
        pendingEnable = false
    }
    val overlayLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        overlayAllowed = OutgoingCallOverlay.canDraw(context)
        notificationReadiness = OutgoingCallNotifier.readiness(context)
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        notificationReadiness = OutgoingCallNotifier.readiness(context)
    }
    val phoneStatePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        phoneStateGranted = granted || hasPermission(context, Manifest.permission.READ_PHONE_STATE)
        if (pendingOffNetwork && phoneStateGranted) {
            OutgoingCallSettings.setNotifyOffNetwork(context, true)
        }
        pendingOffNetwork = false
    }

    fun requestRole(enableAfterGrant: Boolean) {
        val intent = OutgoingCallRole.requestIntent(context) ?: return
        pendingEnable = enableAfterGrant
        roleLauncher.launch(intent)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        roleHeld = OutgoingCallRole.isHeld(context)
        overlayAllowed = OutgoingCallOverlay.canDraw(context)
        notificationReadiness = OutgoingCallNotifier.readiness(context)
        phoneStateGranted = hasPermission(context, Manifest.permission.READ_PHONE_STATE)
    }

    // Giống bộ chặn cuộc gọi: vai trò hệ thống là cổng bắt buộc. Không hiển thị các tuỳ chọn như
    // thể chúng đã hoạt động khi Telecom chưa chuyển callback cuộc gọi đi cho CallHS.
    if (!roleHeld) {
        OutgoingCallRoleGate(
            onBack = onBack,
            available = roleAvailable,
            onEnable = { requestRole(enableAfterGrant = true) },
        )
        return
    }

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = strings.common.back,
                    tint = TextPrimary,
                    modifier = Modifier.size(23.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = s.screenTitle,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = navBottom + 24.dp),
        ) {
            SectionTitle(s.activationSection)
            PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ToggleRow(
                        icon = Icons.Rounded.PhoneInTalk,
                        title = s.enabledTitle,
                        subtitle = s.enabledSubtitle,
                        checked = OutgoingCallSettings.enabled,
                        enabled = roleAvailable || OutgoingCallSettings.enabled,
                        onCheckedChange = { enabled ->
                            if (!enabled) {
                                OutgoingCallSettings.setEnabled(context, false)
                            } else if (roleHeld) {
                                OutgoingCallSettings.setEnabled(context, true)
                            } else {
                                requestRole(enableAfterGrant = true)
                            }
                        },
                    )
                    RowDivider()
                    RoleStatusRow(
                        title = when {
                            !roleAvailable -> s.roleUnavailable
                            roleHeld -> s.roleActive
                            else -> s.roleRequired
                        },
                        active = roleHeld,
                        showAction = roleAvailable && !roleHeld,
                        action = strings.common.grantPermission,
                        onAction = { requestRole(enableAfterGrant = true) },
                    )
                }
            }
            Text(
                text = s.roleExplanation,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 10.dp),
            )

            Spacer(Modifier.height(10.dp))
            SectionTitle(s.conditionsSection)
            PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ToggleRow(
                        title = s.offNetworkTitle,
                        subtitle = s.offNetworkSubtitle,
                        checked = OutgoingCallSettings.notifyOffNetwork,
                        onCheckedChange = { enabled ->
                            if (!enabled) {
                                pendingOffNetwork = false
                                OutgoingCallSettings.setNotifyOffNetwork(context, false)
                            } else if (phoneStateGranted) {
                                OutgoingCallSettings.setNotifyOffNetwork(context, true)
                            } else {
                                pendingOffNetwork = true
                                phoneStatePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
                            }
                        },
                    )
                    RowDivider()
                    ToggleRow(
                        title = s.blocklistTitle,
                        subtitle = s.blocklistSubtitle,
                        checked = OutgoingCallSettings.notifyBlocklist,
                        onCheckedChange = { OutgoingCallSettings.setNotifyBlocklist(context, it) },
                    )
                    RowDivider()
                    ToggleRow(
                        title = s.allowlistTitle,
                        subtitle = s.allowlistSubtitle,
                        checked = OutgoingCallSettings.notifyAllowlist,
                        onCheckedChange = { OutgoingCallSettings.setNotifyAllowlist(context, it) },
                    )
                }
            }

            if (OutgoingCallSettings.notifyOffNetwork && !phoneStateGranted) {
                Spacer(Modifier.height(12.dp))
                RepairCard(
                    icon = Icons.Rounded.WarningAmber,
                    title = s.simPermissionTitle,
                    subtitle = s.simPermissionSubtitle,
                    action = s.grantSimPermission,
                    onAction = {
                        pendingOffNetwork = true
                        phoneStatePermissionLauncher.launch(Manifest.permission.READ_PHONE_STATE)
                    },
                )
            }

            Spacer(Modifier.height(22.dp))
            SectionTitle(s.presentationSection)
            PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    PresentationRow(
                        title = s.headsUpTitle,
                        subtitle = s.headsUpSubtitle,
                        selected = OutgoingCallSettings.presentation == OutgoingCallPresentation.HEADS_UP,
                        onClick = {
                            OutgoingCallSettings.setPresentation(context, OutgoingCallPresentation.HEADS_UP)
                            if (
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                notificationReadiness == OutgoingCallNotifier.Readiness.RUNTIME_PERMISSION_REQUIRED
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                    )
                    RowDivider()
                    PresentationRow(
                        title = s.overlayTitle,
                        subtitle = s.overlaySubtitle,
                        selected = OutgoingCallSettings.presentation == OutgoingCallPresentation.OVERLAY,
                        onClick = {
                            OutgoingCallSettings.setPresentation(context, OutgoingCallPresentation.OVERLAY)
                            if (!overlayAllowed) {
                                overlayLauncher.launch(OutgoingCallOverlay.permissionIntent(context))
                            }
                        },
                    )
                }
            }

            if (
                OutgoingCallSettings.presentation == OutgoingCallPresentation.OVERLAY &&
                !overlayAllowed
            ) {
                Spacer(Modifier.height(12.dp))
                RepairCard(
                    icon = Icons.Rounded.WarningAmber,
                    title = s.overlayPermissionTitle,
                    subtitle = s.overlayPermissionSubtitle,
                    action = s.grantOverlayPermission,
                    onAction = { overlayLauncher.launch(OutgoingCallOverlay.permissionIntent(context)) },
                )
            }

            val notificationNeeded =
                OutgoingCallSettings.presentation == OutgoingCallPresentation.HEADS_UP || !overlayAllowed
            if (notificationNeeded && notificationReadiness != OutgoingCallNotifier.Readiness.READY) {
                Spacer(Modifier.height(12.dp))
                RepairCard(
                    icon = Icons.Rounded.NotificationsActive,
                    title = s.notificationPermissionTitle,
                    subtitle = s.notificationPermissionSubtitle,
                    action = if (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        notificationReadiness == OutgoingCallNotifier.Readiness.RUNTIME_PERMISSION_REQUIRED
                    ) s.grantNotificationPermission else s.openNotificationSettings,
                    onAction = {
                        if (
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            notificationReadiness == OutgoingCallNotifier.Readiness.RUNTIME_PERMISSION_REQUIRED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            OutgoingCallNotifier.openSettings(context)
                        }
                    },
                )
            }

            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Icon(
                    Icons.Rounded.Security,
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = s.privacyNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun OutgoingCallRoleGate(
    onBack: () -> Unit,
    available: Boolean,
    onEnable: () -> Unit,
) {
    val strings = appStrings()
    val s = strings.outgoingCall
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = strings.common.back,
                    tint = TextPrimary,
                    modifier = Modifier.size(23.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = s.screenTitle,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = navBottom + 24.dp),
        ) {
            PanelCard(modifier = Modifier.fillMaxWidth(), radius = 22.dp) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(AccentGreenBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.PhoneInTalk,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = if (available) s.roleGateTitle else s.roleUnavailableTitle,
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (available) s.roleGateBody else s.roleUnavailableBody,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )
                    if (available) {
                        Spacer(Modifier.height(20.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Primary)
                                .clickable(onClick = onEnable),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = s.roleGateAction,
                                style = MaterialTheme.typography.titleSmall,
                                color = androidx.compose.ui.graphics.Color.White,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp),
    )
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(AccentGreenBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) TextPrimary else TextSecondary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Spacer(Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(
                checkedThumbColor = androidx.compose.ui.graphics.Color.White,
                checkedTrackColor = Primary,
            ),
        )
    }
}

@Composable
private fun PresentationRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = Primary, unselectedColor = TextSecondary),
        )
    }
}

@Composable
private fun RoleStatusRow(
    title: String,
    active: Boolean,
    showAction: Boolean,
    action: String,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (active) Icons.Rounded.Security else Icons.Rounded.Info,
            contentDescription = null,
            tint = if (active) AccentGreen else AccentAmber,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Text(title, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.weight(1f))
        if (showAction) {
            Spacer(Modifier.width(10.dp))
            Text(
                text = action,
                style = MaterialTheme.typography.labelLarge,
                color = Primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onAction)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun RepairCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    action: String,
    onAction: () -> Unit,
) {
    PanelCard(modifier = Modifier.fillMaxWidth(), radius = 18.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().background(AccentAmberBg).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                action,
                style = MaterialTheme.typography.labelLarge,
                color = Primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onAction)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun RowDivider() {
    Box(
        modifier = Modifier.padding(start = 16.dp).fillMaxWidth().height(1.dp).background(DividerColor),
    )
}

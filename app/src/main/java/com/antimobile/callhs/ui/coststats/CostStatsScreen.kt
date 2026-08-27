package com.antimobile.callhs.ui.coststats

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.antimobile.callhs.data.model.CallNumberDetail
import com.antimobile.callhs.ui.components.AppBottomSheet
import com.antimobile.callhs.ui.components.AppToastType
import com.antimobile.callhs.ui.components.Avatar
import com.antimobile.callhs.ui.components.EmptyState
import com.antimobile.callhs.ui.components.LoadingState
import com.antimobile.callhs.ui.components.PanelCard
import com.antimobile.callhs.ui.components.SimBadge
import com.antimobile.callhs.ui.components.collapsingHeaderSwap
import com.antimobile.callhs.ui.theme.AccentAmber
import com.antimobile.callhs.ui.theme.AccentAmberBg
import com.antimobile.callhs.ui.theme.AccentBlue
import com.antimobile.callhs.ui.theme.AccentBlueBg
import com.antimobile.callhs.ui.theme.AccentGreen
import com.antimobile.callhs.ui.theme.AccentGreenBg
import com.antimobile.callhs.ui.theme.AppBackground
import com.antimobile.callhs.ui.theme.BrandSoft
import com.antimobile.callhs.ui.theme.CardFill
import com.antimobile.callhs.ui.theme.Primary
import com.antimobile.callhs.ui.theme.TextPrimary
import com.antimobile.callhs.ui.theme.TextSecondary
import com.antimobile.callhs.i18n.appStrings
import com.antimobile.callhs.util.BillingBlock
import com.antimobile.callhs.util.CallActions
import com.antimobile.callhs.util.CallCharge
import com.antimobile.callhs.util.ChargeStatus
import com.antimobile.callhs.util.CostSummary
import com.antimobile.callhs.util.NetworkClass
import com.antimobile.callhs.util.SimInfo
import com.antimobile.callhs.util.SpecialNumbers
import com.antimobile.callhs.util.Tariffs
import com.antimobile.callhs.util.TimeFormat
import com.antimobile.callhs.util.formatDong
import com.antimobile.callhs.util.formatPhone

private val TOP_BAR_HEIGHT = 56.dp

@Composable
fun CostStatsScreen(
    vm: CostStatsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val state = vm.state
    val detail = state?.detail

    // ---- Quyền READ_PHONE_STATE để dò nhà mạng SIM (tính nội/ngoại mạng) ----
    var permanentlyDenied by rememberSaveable { mutableStateOf(false) }
    val phoneLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permanentlyDenied = false
        } else {
            val activity = context.findActivity()
            permanentlyDenied = activity != null &&
                !activity.shouldShowRationale(Manifest.permission.READ_PHONE_STATE)
        }
        vm.refresh()
    }
    val requestPhone: () -> Unit = {
        if (permanentlyDenied) context.openAppSettings()
        else phoneLauncher.launch(Manifest.permission.READ_PHONE_STATE)
    }

    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val collapseThresholdPx = with(density) { 92.dp.toPx() }
    val collapsed by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > collapseThresholdPx
        }
    }
    var expandedIds by remember(detail?.number) { mutableStateOf(emptySet<Long>()) }
    var showInfo by remember { mutableStateOf(false) }
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .statusBarsPadding()
    ) {
        CostTopBar(
            collapsed = collapsed && detail != null,
            detail = detail,
            showInfo = vm.hasPhonePermission,
            onBack = onBack,
            onInfo = { showInfo = true }
        )

        when {
            !vm.hasPhonePermission -> PhonePermissionGate(
                onGrant = requestPhone,
                buttonLabel = if (permanentlyDenied) appStrings().common.openSettings else appStrings().common.allowAccess
            )

            vm.loading && state == null -> Box(Modifier.fillMaxSize()) { LoadingState(text = appStrings().costStats.calculating) }

            state == null || state.charges.isEmpty() ->
                Box(Modifier.fillMaxSize()) { EmptyState(appStrings().callDetail.emptyNoCalls) }

            else -> {
                val outgoing = remember(state.charges) {
                    state.charges.filter { it.entry.type == com.antimobile.callhs.data.model.CallType.OUTGOING }
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 6.dp, bottom = navBottom + 24.dp)
                ) {
                    item { SummaryCard(state.summary) }
                    item { Spacer(Modifier.height(16.dp)) }
                    item { BreakdownCard(state.summary) }
                    item { Spacer(Modifier.height(16.dp)) }
                    item { SimCard(state.slots, state.summary.unknownSimCalls > 0) }
                    item { Spacer(Modifier.height(20.dp)) }
                    if (outgoing.isEmpty()) {
                        item { NoOutgoingNote() }
                    } else {
                        item {
                            Text(
                                appStrings().costStats.detailTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary,
                                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
                            )
                        }
                        item {
                            PanelCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), radius = 20.dp) {
                                Column(Modifier.fillMaxWidth()) {
                                    outgoing.forEach { charge ->
                                        CostCallItem(
                                            charge = charge,
                                            expanded = charge.entry.id in expandedIds,
                                            onToggle = {
                                                expandedIds = if (charge.entry.id in expandedIds)
                                                    expandedIds - charge.entry.id else expandedIds + charge.entry.id
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(18.dp)) }
                    item { DisclaimerCard() }
                }
            }
        }
    }

    if (showInfo) CostInfoSheet(onDismiss = { showInfo = false })
}

// ---------------------------------------------------------------------------------------------
// Top bar (thu gọn khi cuộn) — giống màn "Tất cả cuộc gọi".
// ---------------------------------------------------------------------------------------------

@Composable
private fun CostTopBar(
    collapsed: Boolean,
    detail: CallNumberDetail?,
    showInfo: Boolean,
    onBack: () -> Unit,
    onInfo: () -> Unit
) {
    val s = appStrings()
    Row(
        modifier = Modifier.fillMaxWidth().height(TOP_BAR_HEIGHT).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, s.common.back, TextPrimary, onBack)
        Spacer(Modifier.width(12.dp))
        // Đổi tiêu đề ↔ avatar+tên bằng cú TRƯỢT DỌC đục (không crossfade mờ chồng),
        // chuyển hẳn theo ngưỡng nhưng vẫn uyển chuyển — giống CallDetail/AllCalls.
        AnimatedContent(
            targetState = collapsed,
            transitionSpec = { collapsingHeaderSwap() },
            label = "cost-topbar",
            modifier = Modifier.weight(1f).fillMaxHeight()
        ) { isCollapsed ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                if (isCollapsed && detail != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar(label = detail.nameOrNumber, photoUri = detail.photoUri, isNamed = !detail.cachedName.isNullOrBlank(), size = 38.dp, specialIconRes = SpecialNumbers.of(detail.number)?.iconRes)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(detail.displayName, style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${formatPhone(detail.number)} · ${detail.region}",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    Column {
                        Text(s.callDetail.costStats, style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (detail != null) {
                            Text(formatPhone(detail.number), style = MaterialTheme.typography.labelMedium, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        if (showInfo) {
            RoundIconButton(Icons.Rounded.Info, s.costStats.infoDesc, TextSecondary, onInfo)
        }
    }
}

@Composable
private fun RoundIconButton(icon: ImageVector, desc: String, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = desc, tint = tint, modifier = Modifier.size(24.dp))
    }
}

// ---------------------------------------------------------------------------------------------
// Thẻ tổng cước (hero).
// ---------------------------------------------------------------------------------------------

@Composable
private fun SummaryCard(summary: CostSummary) {
    PanelCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), radius = 20.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(AccentGreenBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Payments, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    appStrings().detailedStats.costTotalLabel,
                    style = MaterialTheme.typography.titleSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                EstimateTag()
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = formatDong(summary.totalCost),
                style = MaterialTheme.typography.headlineLarge,
                color = AccentGreen,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = buildString {
                    append(appStrings().costStats.chargeableCallsLine(summary.chargeableCalls))
                    if (summary.billedSeconds > 0) append("  ·  ${appStrings().costStats.billedSuffix(TimeFormat.durationVerboseHours(summary.billedSeconds))}")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun EstimateTag() {
    Text(
        text = appStrings().costStats.estimateTag,
        color = AccentAmber,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(AccentAmberBg).padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

// ---------------------------------------------------------------------------------------------
// Thẻ phân tích nội/ngoại mạng + miễn phí.
// ---------------------------------------------------------------------------------------------

@Composable
private fun BreakdownCard(summary: CostSummary) {
    PanelCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), radius = 20.dp) {
        val cs = appStrings().costStats
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            BreakdownRow(networkClassLabel(NetworkClass.NOI_MANG), AccentGreen, summary.noiMangCalls, summary.noiMangCost)
            BreakdownRow(networkClassLabel(NetworkClass.NGOAI_MANG), AccentBlue, summary.ngoaiMangCalls, summary.ngoaiMangCost)
            if (summary.khacCalls > 0) {
                BreakdownRow(networkClassLabel(NetworkClass.KHAC), AccentAmber, summary.khacCalls, summary.khacCost)
            }
            InfoRow(cs.freeCallsLabel, cs.freeCallsValue(summary.freeCalls), TextSecondary)
            if (summary.unknownSimCalls > 0) {
                InfoRow(cs.unknownSimLabel, cs.unknownSimValue(summary.unknownSimCalls), AccentAmber)
            }
        }
    }
}

/** Dòng nội/ngoại mạng: chấm màu + nhãn + "N cuộc" (trái) và số tiền (phải). */
@Composable
private fun BreakdownRow(label: String, dot: Color, calls: Int, cost: Long) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, modifier = Modifier.weight(1f))
        Text(appStrings().detailedStats.calls(calls), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(Modifier.width(14.dp))
        Text(formatDong(cost), style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor, fontWeight = FontWeight.Medium)
    }
}

// ---------------------------------------------------------------------------------------------
// Thẻ SIM đã dò được.
// ---------------------------------------------------------------------------------------------

@Composable
private fun SimCard(slots: List<SimInfo.SlotInfo>, hasUnknown: Boolean) {
    // Khe DÙNG ĐƯỢC = đang lắp SIM HOẶC người dùng đã nhập số (suy ra nhà mạng). Nguồn CHUNG với màn "Số của tôi".
    val usable = slots.filter { it.present || it.manualNumber.isNotBlank() }
    val cs = appStrings().costStats
    Column(Modifier.fillMaxWidth()) {
        Text(
            cs.simCardTitle,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
        )
        PanelCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), radius = 20.dp) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                if (usable.isEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.SimCard, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            cs.simNoneNote,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    usable.forEach { slot ->
                        val carrier = slot.effectiveCarrier
                        // Nhà mạng SUY TỪ SỐ NHẬP (không có SIM đang lắp) → gắn nhãn để người dùng hiểu nguồn.
                        val fromEntered = !slot.present
                        val sub = buildList {
                            slot.resolved.takeIf { it.isNotBlank() }?.let { add(formatPhone(it)) }
                            if (fromEntered && carrier != null) add(cs.fromEnteredNumber)
                        }.joinToString(" · ")
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.SimCard,
                                contentDescription = null,
                                tint = if (slot.present) Primary else AccentAmber,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            // Chỉ gắn nhãn "SIM n" khi có TỪ 2 khe trở lên để phân biệt; 1 SIM thì thừa.
                            if (usable.size >= 2) {
                                SimBadge(slot.label)
                                Spacer(Modifier.width(10.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = carrier ?: cs.unknownCarrier,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = if (carrier != null) TextPrimary else TextSecondary,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (sub.isNotBlank()) {
                                    Text(
                                        text = sub,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
                if (hasUnknown) {
                    Text(
                        text = cs.unknownSimCallsNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 2.dp, bottom = 10.dp)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Item cuộc gọi đi (mở rộng xem cách tính).
// ---------------------------------------------------------------------------------------------

@Composable
private fun CostCallItem(charge: CallCharge, expanded: Boolean, onToggle: () -> Unit) {
    val entry = charge.entry
    val cs = appStrings().costStats
    Column(modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Rounded.CallMade, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            NetworkBadge(charge.status, charge.networkClass)
            Spacer(Modifier.width(8.dp))
            Text(TimeFormat.time(entry.dateMillis), style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            CostValue(charge)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = subline(charge),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp)) {
                Text(
                    text = TimeFormat.fullDateTimeWithSeconds(entry.dateMillis),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(8.dp))
                when (charge.status) {
                    ChargeStatus.CHARGED -> {
                        CalcLine(cs.calcSim, listOfNotNull(entry.displaySimLabel, charge.simCarrier).joinToString("  ·  ").ifBlank { "—" })
                        CalcLine(cs.calcCalled, "${charge.calledCarrier ?: cs.calledFallback} → ${networkClassLabel(charge.networkClass)}")
                        CalcLine(cs.calcDuration, "${TimeFormat.durationLabel(entry.durationSeconds)} (${entry.durationSeconds}s)")
                        CalcLine(cs.calcBilling, "${charge.billedSeconds}s · ${billingBlockLabel(charge.block)}")
                        CalcLine(cs.calcRate, "${formatDong(charge.ratePerMinute.toLong())}${cs.rateUnitSlash}")
                        Spacer(Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(cs.calcTotal, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text("≈ ${formatDong(charge.cost)}", style = MaterialTheme.typography.titleMedium, color = AccentGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                    ChargeStatus.UNKNOWN_SIM -> HintLine(cs.hintUnknownSim)
                    ChargeStatus.FREE_NO_CONNECT -> HintLine(cs.hintNoConnect)
                    ChargeStatus.FREE_INCOMING -> HintLine(cs.hintFreeIncoming)
                }
            }
        }
    }
}

// --- Nhãn 2 enum CallCost theo ngôn ngữ hiện hành (ánh xạ enum→chuỗi ở UI; nội/ngoại mạng tái dùng detailedStats) ---
private fun networkClassLabel(nc: NetworkClass): String = when (nc) {
    NetworkClass.NOI_MANG -> appStrings().detailedStats.networkOnNet
    NetworkClass.NGOAI_MANG -> appStrings().detailedStats.networkOffNet
    NetworkClass.KHAC -> appStrings().detailedStats.networkOther
    NetworkClass.NONE -> "—"
}

private fun billingBlockLabel(b: BillingBlock): String = with(appStrings().costStats) {
    when (b) {
        BillingBlock.SIX_PLUS_ONE -> block6
        BillingBlock.SIXTY_PLUS_ONE -> block60
        BillingBlock.PER_MINUTE -> blockPerMinute
    }
}

@Composable
private fun NetworkBadge(status: ChargeStatus, networkClass: NetworkClass) {
    val cs = appStrings().costStats
    val ds = appStrings().detailedStats
    val (text, fg, bg) = when (status) {
        ChargeStatus.CHARGED -> when (networkClass) {
            NetworkClass.NOI_MANG -> Triple(ds.networkOnNet, AccentGreen, AccentGreenBg)
            NetworkClass.NGOAI_MANG -> Triple(ds.networkOffNet, AccentBlue, AccentBlueBg)
            else -> Triple(cs.badgeOther, AccentAmber, AccentAmberBg)
        }
        ChargeStatus.UNKNOWN_SIM -> Triple(cs.badgeUnknownSim, AccentAmber, AccentAmberBg)
        ChargeStatus.FREE_NO_CONNECT -> Triple(cs.badgeNoConnect, TextSecondary, CardFill)
        ChargeStatus.FREE_INCOMING -> Triple(cs.badgeFree, TextSecondary, CardFill)
    }
    Text(
        text = text,
        color = fg,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(bg).padding(horizontal = 7.dp, vertical = 2.dp)
    )
}

@Composable
private fun CostValue(charge: CallCharge) {
    when (charge.status) {
        ChargeStatus.CHARGED -> Text(formatDong(charge.cost), style = MaterialTheme.typography.titleMedium, color = AccentGreen, fontWeight = FontWeight.Bold)
        ChargeStatus.UNKNOWN_SIM -> Text("?", style = MaterialTheme.typography.titleMedium, color = AccentAmber, fontWeight = FontWeight.Bold)
        else -> Text(appStrings().costStats.badgeFree, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, fontWeight = FontWeight.Medium)
    }
}

/** Dòng phụ dưới mỗi cuộc: SIM · nhà mạng đích · thời lượng. */
private fun subline(charge: CallCharge) = buildAnnotatedString {
    val entry = charge.entry
    val parts = listOfNotNull(
        TimeFormat.itemClock(entry.dateMillis),
        entry.displaySimLabel,
        charge.calledCarrier
    ).joinToString("  ·  ")
    withStyle(SpanStyle(color = TextSecondary)) {
        append(parts)
        if (entry.durationSeconds > 0) append("  ·  ${TimeFormat.durationLabel(entry.durationSeconds)}")
    }
}

@Composable
private fun CalcLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun HintLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.padding(vertical = 2.dp))
}

// ---------------------------------------------------------------------------------------------
// Ghi chú & màn xin quyền.
// ---------------------------------------------------------------------------------------------

@Composable
private fun NoOutgoingNote() {
    PanelCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), radius = 20.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.AutoMirrored.Rounded.CallMade, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(appStrings().costStats.noOutgoing, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun DisclaimerCard() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(16.dp)).background(CardFill).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.Info, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = appStrings().costStats.disclaimer,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun PhonePermissionGate(onGrant: () -> Unit, buttonLabel: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(104.dp).clip(CircleShape).background(AccentGreenBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Payments, contentDescription = null, tint = Primary, modifier = Modifier.size(48.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text(appStrings().costStats.permTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(10.dp))
        Text(
            text = appStrings().costStats.permBody,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(Primary)
                .clickable { onGrant() }
                .padding(vertical = 15.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(buttonLabel, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Bottom sheet: bảng giá tham khảo + lưu ý.
// ---------------------------------------------------------------------------------------------

@Composable
private fun CostInfoSheet(onDismiss: () -> Unit) {
    val cs = appStrings().costStats
    val ds = appStrings().detailedStats
    AppBottomSheet(
        onDismiss = onDismiss,
        title = cs.infoSheetTitle,
        maxHeightFraction = 0.7f,
        sheetGesturesEnabled = false,
        showCloseButton = true
    ) { _ ->
        Text(
            text = cs.infoSheetDesc,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp)
        )
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)) {
            Text(cs.tableCarrier, style = MaterialTheme.typography.labelMedium, color = TextSecondary, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Start, modifier = Modifier.weight(1.3f))
            Text(ds.networkOnNet, style = MaterialTheme.typography.labelMedium, color = AccentGreen, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
            Text(ds.networkOffNet, style = MaterialTheme.typography.labelMedium, color = AccentBlue, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        }
        Tariffs.table.forEach { (carrier, tariff) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1.3f)) {
                    Text(carrier, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Medium)
                    Text(billingBlockLabel(tariff.block), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
                Text(cs.tariffRate(tariff.noiMangPerMinute), style = MaterialTheme.typography.bodyMedium, color = TextPrimary, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                Text(cs.tariffRate(tariff.ngoaiMangPerMinute), style = MaterialTheme.typography.bodyMedium, color = TextPrimary, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).clip(RoundedCornerShape(14.dp)).background(BrandSoft).padding(14.dp)
        ) {
            Column {
                Text(
                    text = cs.infoNote1,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = cs.infoNote2,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Tiện ích quyền.
// ---------------------------------------------------------------------------------------------

private fun Context.findActivity(): Activity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

private fun Activity.shouldShowRationale(permission: String): Boolean =
    androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(this, permission)

private fun Context.openAppSettings() {
    runCatching {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }.onFailure { CallActions.toast(this, appStrings().common.appSettingsOpenFailed, AppToastType.Error) }
}

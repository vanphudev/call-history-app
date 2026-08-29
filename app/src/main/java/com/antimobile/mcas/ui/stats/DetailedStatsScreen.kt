package com.antimobile.mcas.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallMissed
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.antimobile.mcas.ui.components.Avatar
import com.antimobile.mcas.ui.components.EmptyState
import com.antimobile.mcas.ui.components.LoadingState
import com.antimobile.mcas.ui.components.PanelCard
import com.antimobile.mcas.ui.components.SectionHeader
import com.antimobile.mcas.ui.components.Segmented
import com.antimobile.mcas.ui.theme.AccentAmber
import com.antimobile.mcas.ui.theme.AccentBlue
import com.antimobile.mcas.ui.theme.AccentBlueBg
import com.antimobile.mcas.ui.theme.AccentGreen
import com.antimobile.mcas.ui.theme.AccentGreenBg
import com.antimobile.mcas.ui.theme.AccentRed
import com.antimobile.mcas.ui.theme.AccentRedBg
import com.antimobile.mcas.ui.theme.AppBackground
import com.antimobile.mcas.ui.theme.BrandSoft
import com.antimobile.mcas.ui.theme.ChipBg
import com.antimobile.mcas.ui.theme.CardFill
import com.antimobile.mcas.ui.theme.Primary
import com.antimobile.mcas.ui.theme.TextPrimary
import com.antimobile.mcas.ui.theme.TextSecondary
import com.antimobile.mcas.i18n.appStrings
import com.antimobile.mcas.util.DayActivity
import com.antimobile.mcas.util.PhoneStat
import com.antimobile.mcas.util.SpecialNumbers
import com.antimobile.mcas.util.StatsPeriod
import com.antimobile.mcas.util.TimeFormat
import com.antimobile.mcas.util.formatDong
import com.antimobile.mcas.util.formatPhone

/**
 * Màn "Thống kê chi tiết" — 3 hạng mục (tab): THEO SỐ, KHUNG GIỜ hoạt động, CƯỚC PHÍ.
 * Tab "Theo số" & "Cước phí" dùng chung bộ chọn khoảng Hôm nay / Tuần này / Tháng này.
 */
@Composable
fun DetailedStatsScreen(vm: DetailedStatsViewModel, onBack: () -> Unit) {
    // Nạp lần đầu khi mở màn; ON_START xử lý làm mới khi quay lại app / vừa cấp quyền.
    LaunchedEffect(Unit) { if (!vm.loaded) vm.load() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { vm.refresh() }

    var tab by rememberSaveable { mutableStateOf(0) }
    val s = appStrings()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .statusBarsPadding()
    ) {
        // ---- Thanh trên: quay lại + tiêu đề ----
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, s.common.back, tint = TextPrimary, modifier = Modifier.size(23.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text(s.detailedStats.title, style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
        }

        // ---- Tab hạng mục ----
        Segmented(
            labels = listOf(s.detailedStats.tabByNumber, s.detailedStats.tabHourly, s.detailedStats.tabCost),
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                !vm.hasCallLogPermission ->
                    EmptyState(s.repeatStats.permNeeded)

                vm.loading && !vm.loaded ->
                    LoadingState(text = s.repeatStats.analyzing)

                else -> when (tab) {
                    0 -> PhoneStatsTab(vm)
                    1 -> HourlyTab(vm)
                    else -> CostTab(vm)
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------
// Tab 1 — THEO SỐ
// ---------------------------------------------------------------------------------------------------

@Composable
private fun PhoneStatsTab(vm: DetailedStatsViewModel) {
    val s = appStrings().detailedStats
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = bottomInsetPadding()) {
        item("period") { PeriodSelector(vm) }
        item("overview") { OverviewCard(vm) }

        item("rank-title") { SectionHeader(s.rankTitle) }
        item("rank") { RankingsCard(vm) }

        item("detail-title") { SectionHeader(s.byNumberTitle(vm.phoneStats.size)) }
        if (vm.phoneStats.isEmpty()) {
            item("detail-empty") { EmptyBlock(s.emptyNoCallsInPeriod(periodLabel(vm.period).lowercase())) }
        } else {
            val maxTotal = vm.phoneStats.maxOf { it.total }
            items(vm.phoneStats, key = { "p_${it.number}" }) { stat ->
                PhoneStatRow(stat, maxTotal)
            }
        }
    }
}

@Composable
private fun OverviewCard(vm: DetailedStatsViewModel) {
    val o = vm.overview
    val s = appStrings().detailedStats
    PanelCard(modifier = cardModifier(), radius = 20.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                MetricCell(s.metricTotalCalls, o.totalCalls.toString(), TextPrimary, Modifier.weight(1f))
                MetricCell(s.metricOutgoing, o.outgoing.toString(), AccentGreen, Modifier.weight(1f))
                MetricCell(s.metricIncoming, o.incoming.toString(), AccentBlue, Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                MetricCell(s.metricMissed, o.missed.toString(), AccentRed, Modifier.weight(1f))
                MetricCell(s.metricDuration, TimeFormat.durationCompact(o.totalDurationSeconds), TextPrimary, Modifier.weight(1f))
                MetricCell(s.metricNumbers, o.distinctNumbers.toString(), TextPrimary, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricCell(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = valueColor, fontWeight = FontWeight.Bold, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
private fun RankingsCard(vm: DetailedStatsViewModel) {
    val t = vm.topNumbers
    val s = appStrings().detailedStats
    PanelCard(modifier = cardModifier(), radius = 20.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            RankingRow(Icons.AutoMirrored.Rounded.CallMade, AccentGreen, AccentGreenBg, s.rankTopOutgoing, t.topOutgoing?.let { it.number to it.outgoing }, t.topOutgoing)
            RankingRow(Icons.AutoMirrored.Rounded.CallReceived, AccentBlue, AccentBlueBg, s.rankTopIncoming, t.topIncoming?.let { it.number to it.incoming }, t.topIncoming)
            RankingRow(Icons.AutoMirrored.Rounded.CallMissed, AccentRed, AccentRedBg, s.rankTopMissed, t.topMissed?.let { it.number to it.missed }, t.topMissed)
        }
    }
}

@Composable
private fun RankingRow(
    icon: ImageVector,
    tint: Color,
    bg: Color,
    title: String,
    numberAndCount: Pair<String, Int>?,
    stat: PhoneStat?
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(38.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = TextSecondary, maxLines = 1)
            Spacer(Modifier.height(1.dp))
            Text(
                text = if (stat != null) statName(stat.number, stat.cachedName) else "—",
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (numberAndCount != null) {
            Spacer(Modifier.width(10.dp))
            CountPill(appStrings().detailedStats.calls(numberAndCount.second), tint, bg)
        }
    }
}

@Composable
private fun PhoneStatRow(stat: PhoneStat, maxTotal: Int) {
    PanelCard(modifier = cardModifier(vertical = 5.dp), radius = 16.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatAvatar(stat.number, stat.cachedName, stat.photoUri, 42.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = statName(stat.number, stat.cachedName),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                CountBar(
                    outgoing = stat.outgoing,
                    incoming = stat.incoming,
                    missed = stat.missed,
                    fraction = (stat.total.toFloat() / maxTotal.coerceAtLeast(1)).coerceIn(0.06f, 1f)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = statBreakdownLine(stat),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(stat.total.toString(), style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                Text(appStrings().detailedStats.callsUnit, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }
    }
}

private fun statBreakdownLine(stat: PhoneStat): String {
    val parts = ArrayList<String>(4)
    if (stat.outgoing > 0) parts.add("↗ ${stat.outgoing}")
    if (stat.incoming > 0) parts.add("↘ ${stat.incoming}")
    if (stat.missed > 0) parts.add("✕ ${stat.missed}")
    if (stat.connectedCount > 0) parts.add(TimeFormat.durationCompact(stat.totalDurationSeconds))
    return parts.joinToString("  ·  ")
}

/**
 * Thanh xếp hạng: NỀN xám full-width; phần TÔ dài [fraction] (tổng cuộc số này / tổng cuộc số nhiều nhất),
 * bên trong chia tỉ lệ gọi đi (xanh lá) / nhận (xanh dương) / nhỡ (đỏ) theo số cuộc.
 */
@Composable
private fun CountBar(outgoing: Int, incoming: Int, missed: Int, fraction: Float, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(CardFill)) {
        Row(modifier = Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxHeight().clip(CircleShape)) {
            if (outgoing > 0) Box(Modifier.weight(outgoing.toFloat()).fillMaxHeight().background(AccentGreen))
            if (incoming > 0) Box(Modifier.weight(incoming.toFloat()).fillMaxHeight().background(AccentBlue))
            if (missed > 0) Box(Modifier.weight(missed.toFloat()).fillMaxHeight().background(AccentRed))
        }
    }
}

// ---------------------------------------------------------------------------------------------------
// Tab 2 — KHUNG GIỜ HOẠT ĐỘNG (≤ 15 ngày)
// ---------------------------------------------------------------------------------------------------

@Composable
private fun HourlyTab(vm: DetailedStatsViewModel) {
    val s = appStrings().detailedStats
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = bottomInsetPadding()) {
        item("legend") {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 2.dp)) {
                Text(
                    s.hourlyIntro(vm.dayActivities.size),
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary
                )
                Spacer(Modifier.height(8.dp))
                Legend()
            }
        }
        if (vm.dayActivities.isEmpty()) {
            item("empty") { EmptyBlock(s.emptyNoActivity) }
        } else {
            items(vm.dayActivities, key = { "d_${it.startOfDayMillis}" }) { day -> DayActivityCard(day) }
        }
    }
}

@Composable
private fun Legend() {
    val s = appStrings().detailedStats
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        LegendDot(AccentGreen, s.legendOutgoing)
        LegendDot(AccentBlue, s.legendIncoming)
        LegendDot(AccentRed, s.legendMissed)
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    }
}

@Composable
private fun DayActivityCard(day: DayActivity) {
    val s = appStrings().detailedStats
    PanelCard(modifier = cardModifier(vertical = 5.dp), radius = 18.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(TimeFormat.sectionLabel(day.startOfDayMillis), style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(s.callsFull(day.total), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                }
                DeltaChip(day.deltaVsPrevDay)
            }
            Spacer(Modifier.height(12.dp))
            HourlyBars(day)
            Spacer(Modifier.height(10.dp))
            PeakLine(Icons.AutoMirrored.Rounded.CallMade, AccentGreen, s.rankTopOutgoing, day.peakOutgoingHour, day.peakOutgoingCount)
            Spacer(Modifier.height(6.dp))
            PeakLine(Icons.AutoMirrored.Rounded.CallReceived, AccentBlue, s.rankTopIncoming, day.peakIncomingHour, day.peakIncomingCount)
        }
    }
}

@Composable
private fun DeltaChip(delta: Int) {
    val (icon, color) = when {
        delta > 0 -> Icons.AutoMirrored.Rounded.TrendingUp to AccentGreen
        delta < 0 -> Icons.AutoMirrored.Rounded.TrendingDown to AccentRed
        else -> Icons.AutoMirrored.Rounded.TrendingFlat to TextSecondary
    }
    val ds = appStrings().detailedStats
    val text = when {
        delta > 0 -> ds.deltaVsPrev("+$delta")
        delta < 0 -> ds.deltaVsPrev("$delta")
        else -> ds.deltaSame
    }
    Row(
        modifier = Modifier.clip(CircleShape).background(CardFill).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

/** Cột 24 giờ, mỗi cột cao theo tổng cuộc/giờ (chuẩn hoá theo giờ đỉnh), xếp chồng gọi đi/nhận/nhỡ. */
@Composable
private fun HourlyBars(day: DayActivity) {
    val totals = IntArray(24) { day.hourlyOutgoing[it] + day.hourlyIncoming[it] + day.hourlyMissed[it] }
    val max = (totals.maxOrNull() ?: 0).coerceAtLeast(1)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().height(46.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            for (h in 0..23) {
                val total = totals[h]
                val frac = total.toFloat() / max
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(if (total > 0) 0.82f else 0f)
                            .fillMaxHeight(frac)
                            .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    ) {
                        if (day.hourlyMissed[h] > 0) Box(Modifier.weight(day.hourlyMissed[h].toFloat()).fillMaxWidth().background(AccentRed))
                        if (day.hourlyIncoming[h] > 0) Box(Modifier.weight(day.hourlyIncoming[h].toFloat()).fillMaxWidth().background(AccentBlue))
                        if (day.hourlyOutgoing[h] > 0) Box(Modifier.weight(day.hourlyOutgoing[h].toFloat()).fillMaxWidth().background(AccentGreen))
                    }
                }
            }
        }
        Spacer(Modifier.height(3.dp))
        // Nhãn mốc giờ 0 · 6 · 12 · 18 · 23 để định vị trục.
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("0h", "6h", "12h", "18h", "23h").forEachIndexed { i, label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    textAlign = if (i == 0) TextAlign.Start else if (i == 4) TextAlign.End else TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PeakLine(icon: ImageVector, tint: Color, label: String, hour: Int?, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.weight(1f))
        Text(
            text = if (hour != null) "${hourSlot(hour)} · ${appStrings().detailedStats.calls(count)}" else "—",
            style = MaterialTheme.typography.bodySmall,
            color = if (hour != null) TextPrimary else TextSecondary,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun hourSlot(hour: Int): String = "%02d:00–%02d:00".format(hour, (hour + 1) % 24)

// ---------------------------------------------------------------------------------------------------
// Tab 3 — CƯỚC PHÍ
// ---------------------------------------------------------------------------------------------------

@Composable
private fun CostTab(vm: DetailedStatsViewModel) {
    val cost = vm.cost
    val s = appStrings().detailedStats
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = bottomInsetPadding()) {
        item("period") { PeriodSelector(vm) }
        if (!vm.hasPhonePermission) {
            item("phone-perm") { NoteBanner(s.costPhonePermNote) }
        }
        item("total") { CostTotalCard(cost) }
        item("breakdown-title") { SectionHeader(s.costByNetworkTitle) }
        item("breakdown") { CostBreakdownCard(cost) }

        item("bynum-title") { SectionHeader(s.costTopTitle(cost.byNumber.size)) }
        if (cost.byNumber.isEmpty()) {
            item("bynum-empty") { EmptyBlock(s.emptyNoBilledInPeriod(periodLabel(vm.period).lowercase())) }
        } else {
            val maxCost = cost.byNumber.maxOf { it.cost }
            items(cost.byNumber, key = { "c_${it.number}" }) { nc -> NumberCostRow(nc, maxCost) }
        }
        item("disclaimer") {
            Text(
                s.costDisclaimer,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun CostTotalCard(cost: PeriodCost) {
    PanelCard(modifier = cardModifier(), radius = 20.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(BrandSoft), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Payments, null, tint = Primary, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(appStrings().detailedStats.costTotalLabel, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(Modifier.height(2.dp))
                Text(formatDong(cost.totalCost), style = MaterialTheme.typography.headlineSmall, color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(Modifier.height(2.dp))
                Text(appStrings().detailedStats.costChargeableLine(cost.summary.chargeableCalls, TimeFormat.durationCompact(cost.summary.billedSeconds)), style = MaterialTheme.typography.labelMedium, color = TextSecondary, maxLines = 1)
            }
        }
    }
}

@Composable
private fun CostBreakdownCard(cost: PeriodCost) {
    val sum = cost.summary
    val ds = appStrings().detailedStats
    PanelCard(modifier = cardModifier(), radius = 20.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            BreakdownRow(AccentGreen, ds.networkOnNet, sum.noiMangCalls, sum.noiMangCost)
            BreakdownRow(AccentBlue, ds.networkOffNet, sum.ngoaiMangCalls, sum.ngoaiMangCost)
            BreakdownRow(AccentAmber, ds.networkOther, sum.khacCalls, sum.khacCost)
        }
    }
}

@Composable
private fun BreakdownRow(color: Color, label: String, calls: Int, cost: Long) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f))
        Text(appStrings().detailedStats.calls(calls), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(Modifier.width(12.dp))
        Text(formatDong(cost), style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun NumberCostRow(nc: NumberCost, maxCost: Long) {
    PanelCard(modifier = cardModifier(vertical = 5.dp), radius = 16.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatAvatar(nc.number, nc.cachedName, nc.photoUri, 42.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(statName(nc.number, nc.cachedName), style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(CardFill)) {
                    val frac = (nc.cost.toFloat() / maxCost.coerceAtLeast(1)).coerceIn(0f, 1f)
                    if (frac > 0f) Box(Modifier.fillMaxWidth(frac).fillMaxHeight().background(Primary))
                }
                Spacer(Modifier.height(6.dp))
                Text(appStrings().detailedStats.billedOutgoing(nc.chargeableCalls), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Spacer(Modifier.width(10.dp))
            Text(formatDong(nc.cost), style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

// ---------------------------------------------------------------------------------------------------
// Dùng chung
// ---------------------------------------------------------------------------------------------------

@Composable
private fun PeriodSelector(vm: DetailedStatsViewModel) {
    val periods = StatsPeriod.entries
    Segmented(
        labels = periods.map { periodLabel(it) },
        selected = periods.indexOf(vm.period),
        onSelect = { vm.selectPeriod(periods[it]) },
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun CountPill(text: String, tint: Color, bg: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = tint,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = Modifier.clip(CircleShape).background(bg).padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

@Composable
private fun NoteBanner(text: String) {
    Row(
        modifier = cardModifier(vertical = 6.dp).clip(RoundedCornerShape(14.dp)).background(BrandSoft).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.AccessTime, null, tint = Primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = TextPrimary)
    }
}

@Composable
private fun EmptyBlock(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 40.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun StatAvatar(number: String, cachedName: String?, photoUri: String?, size: Dp) {
    Avatar(
        label = statName(number, cachedName),
        photoUri = photoUri,
        isNamed = !cachedName.isNullOrBlank(),
        size = size,
        specialIconRes = SpecialNumbers.of(number)?.iconRes
    )
}

/** Tên hiển thị: danh bạ → tên khẩn cấp (113/114/115) → số đã định dạng. */
private fun statName(number: String, cachedName: String?): String =
    cachedName?.takeIf { it.isNotBlank() } ?: SpecialNumbers.name(number) ?: formatPhone(number)

/** Nhãn khoảng thời gian theo ngôn ngữ hiện hành (ánh xạ [StatsPeriod]→chuỗi ở UI). */
private fun periodLabel(p: StatsPeriod): String = with(appStrings().detailedStats) {
    when (p) {
        StatsPeriod.DAY -> periodDay
        StatsPeriod.WEEK -> periodWeek
        StatsPeriod.MONTH -> periodMonth
    }
}

private fun cardModifier(vertical: Dp = 6.dp): Modifier =
    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = vertical)

@Composable
private fun bottomInsetPadding(): PaddingValues {
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return PaddingValues(top = 4.dp, bottom = navBottom + 24.dp)
}

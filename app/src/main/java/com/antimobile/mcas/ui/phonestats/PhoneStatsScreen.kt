package com.antimobile.mcas.ui.phonestats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.CallMissed
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.NetworkCell
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.Videocam
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.antimobile.mcas.data.model.CallNumberDetail
import com.antimobile.mcas.i18n.appStrings
import com.antimobile.mcas.ui.components.EmptyState
import com.antimobile.mcas.ui.components.LoadingState
import com.antimobile.mcas.ui.components.PanelCard
import com.antimobile.mcas.ui.components.SectionHeader
import com.antimobile.mcas.ui.components.SimBadge
import com.antimobile.mcas.ui.components.SimScopeControl
import com.antimobile.mcas.ui.theme.AccentAmber
import com.antimobile.mcas.ui.theme.AccentBlue
import com.antimobile.mcas.ui.theme.AccentGreen
import com.antimobile.mcas.ui.theme.AccentRed
import com.antimobile.mcas.ui.theme.AppBackground
import com.antimobile.mcas.ui.theme.CardFill
import com.antimobile.mcas.ui.theme.Primary
import com.antimobile.mcas.ui.theme.TextPrimary
import com.antimobile.mcas.ui.theme.TextSecondary
import com.antimobile.mcas.util.DayActivity
import com.antimobile.mcas.util.DayPart
import com.antimobile.mcas.util.DirectionBreakdown
import com.antimobile.mcas.util.PhoneAnalysis
import com.antimobile.mcas.util.PhoneAnalysisResult
import com.antimobile.mcas.util.SimScope
import com.antimobile.mcas.util.TimeFormat
import com.antimobile.mcas.util.formatPhone
import java.time.DayOfWeek
import java.time.ZoneId

private val TOP_BAR_HEIGHT = 56.dp

/**
 * Màn "PHÂN TÍCH CUỘC GỌI" — thống kê CHUYÊN SÂU cho MỘT số, mở từ nút trên tay cầm bottom sheet ở màn Chi tiết
 * và Tất cả cuộc gọi. Có: thanh chọn SIM (khi máy nhiều SIM) lọc lại TOÀN BỘ số liệu; biểu đồ TRÒN đi/đến/nhỡ
 * (có animation); biểu đồ NGANG số cuộc theo ngày; thời lượng & tỉ lệ kết nối; khung giờ trong ngày & theo thứ;
 * tính năng (video/VoLTE); và mối quan hệ theo thời gian. Mọi số liệu do [PhoneAnalysis] tính (thuần, chính xác).
 */
@Composable
fun PhoneStatsScreen(vm: PhoneStatsViewModel, onBack: () -> Unit) {
    val detail = vm.detail

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .statusBarsPadding()
    ) {
        TopBar(detail = detail, onBack = onBack)

        when {
            vm.loading && detail == null ->
                Box(Modifier.fillMaxSize()) { LoadingState(text = appStrings().callDetail.loading) }

            detail == null || detail.entries.isEmpty() ->
                Box(Modifier.fillMaxSize()) { EmptyState(appStrings().phoneStats.empty) }

            else -> Content(detail)
        }
    }
}

@Composable
private fun TopBar(detail: CallNumberDetail?, onBack: () -> Unit) {
    val s = appStrings()
    Row(
        modifier = Modifier.fillMaxWidth().height(TOP_BAR_HEIGHT).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, s.common.back, tint = TextPrimary, modifier = Modifier.size(23.dp))
        }
        Spacer(Modifier.width(8.dp))
        Column {
            Text(s.phoneStats.title, style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (detail != null) {
                Text(formatPhone(detail.number), style = MaterialTheme.typography.labelMedium, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun Content(detail: CallNumberDetail) {
    val ps = appStrings().phoneStats
    // ---- Lọc theo SIM: cùng logic StatsSheetContent — khoá phạm vi toàn app → dữ liệu đã lọc tại gốc (bỏ lọc
    // cục bộ); ngược lại lọc theo lựa chọn nếu SIM còn trong danh sách. null = tất cả (khớp số tổng như sheet). ----
    val globalSim = SimScope.effectiveLabel
    val simLabels = detail.simLabels
    var statsSim by remember(detail.number) { mutableStateOf<String?>(null) }
    val effectiveStatsSim = if (globalSim != null) null else statsSim?.takeIf { it in simLabels }
    val shownEntries = remember(detail.entries, effectiveStatsSim) {
        effectiveStatsSim?.let { l -> detail.entries.filter { it.simLabel == l } } ?: detail.entries
    }
    val a = remember(shownEntries) { PhoneAnalysis.analyze(shownEntries, ZoneId.systemDefault()) }

    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 6.dp, bottom = navBottom + 28.dp)
    ) {
        if (globalSim != null || simLabels.size >= 2) {
            item("sim") {
                // Nhiều hơn 2 SIM (rất hiếm) → cho CUỘN NGANG để không ô nào bị cắt/mất vùng chạm (giống màn
                // danh sách & StatsSheetContent). 2 SIM → căn giữa như thường.
                val simRowMod = if (simLabels.size > 2) {
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp)
                } else {
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                }
                Row(
                    modifier = simRowMod,
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SimScopeControl(
                        availableLabels = simLabels,
                        localSelection = effectiveStatsSim,
                        onLocalSelect = { statsSim = it },
                        allLabel = appStrings().callList.filterAll
                    )
                }
            }
        }

        // 1) Biểu đồ TRÒN (đi / đến / nhỡ) + chú giải.
        item("donut-h") { SectionHeader(ps.breakdownTitle) }
        item("donut") {
            PanelCard(modifier = cardMod(), radius = 20.dp) {
                DonutCard(a.direction)
            }
        }

        // 2) Hàng chỉ số nhanh.
        item("metrics") {
            PanelCard(modifier = cardMod(top = 12.dp), radius = 20.dp) {
                KeyMetrics(a)
            }
        }

        // 3) Biểu đồ NGANG: số cuộc theo ngày.
        if (a.daily.isNotEmpty()) {
            item("daily-h") { SectionHeader(ps.dailyTitle) }
            item("daily-intro") {
                Text(
                    ps.dailyIntro(a.daily.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 6.dp)
                )
            }
            item("daily") {
                PanelCard(modifier = cardMod(), radius = 20.dp) { DailyBarsCard(a.daily) }
            }
        }

        // 4) Thời lượng & kết nối.
        item("talk-h") { SectionHeader(ps.talkTitle) }
        item("talk") {
            PanelCard(modifier = cardMod(), radius = 20.dp) { TalkCard(a) }
        }

        // 5) Khung giờ trong ngày (24 giờ) + buổi.
        item("hourly-h") { SectionHeader(ps.hourlyTitle) }
        item("hourly") {
            PanelCard(modifier = cardMod(), radius = 20.dp) { HourlyCard(a) }
        }

        // 6) Theo ngày trong tuần.
        item("weekday-h") { SectionHeader(ps.weekdayTitle) }
        item("weekday") {
            PanelCard(modifier = cardMod(), radius = 20.dp) { WeekdayCard(a) }
        }

        // 7) Tính năng cuộc gọi.
        item("feat-h") { SectionHeader(ps.featuresTitle) }
        item("feat") {
            PanelCard(modifier = cardMod(), radius = 20.dp) { FeaturesCard(a) }
        }

        // 8) Mối quan hệ theo thời gian.
        item("span-h") { SectionHeader(ps.spanTitle) }
        item("span") {
            PanelCard(modifier = cardMod(), radius = 20.dp) { SpanCard(a) }
        }

        // 9) Theo SIM — chỉ khi đang xem "Tất cả" (không khoá phạm vi) và có ≥2 SIM trong dữ liệu.
        if (globalSim == null && effectiveStatsSim == null && a.perSim.size >= 2) {
            item("persim-h") { SectionHeader(ps.simTitle) }
            item("persim") {
                PanelCard(modifier = cardMod(), radius = 20.dp) { SimBreakdownCard(a) }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------
// 1 — Biểu đồ TRÒN (donut) đi / đến / nhỡ, có animation quét vòng.
// ---------------------------------------------------------------------------------------------------

@Composable
private fun DonutCard(direction: DirectionBreakdown) {
    val ds = appStrings().detailedStats
    val ps = appStrings().phoneStats
    val progress = enterProgress(direction)
    val track = CardFill
    val slices = listOf(
        Triple(direction.outgoing, AccentGreen, ds.legendOutgoing),
        Triple(direction.incoming, AccentBlue, ds.legendIncoming),
        Triple(direction.missed, AccentRed, ds.legendMissed)
    )
    val total = direction.total.coerceAtLeast(1)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(148.dp), contentAlignment = Alignment.Center) {
            val strokePx = with(LocalDensity.current) { 24.dp.toPx() }
            Canvas(modifier = Modifier.fillMaxSize()) {
                val d = size.minDimension - strokePx
                val tl = Offset((size.width - d) / 2f, (size.height - d) / 2f)
                val arcSize = Size(d, d)
                drawArc(color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = tl, size = arcSize, style = Stroke(strokePx))
                var start = -90f
                for ((count, color, _) in slices) {
                    if (count <= 0) continue
                    val sweep = 360f * (count.toFloat() / total) * progress
                    drawArc(color = color, startAngle = start, sweepAngle = sweep, useCenter = false, topLeft = tl, size = arcSize, style = Stroke(strokePx, cap = StrokeCap.Butt))
                    start += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(direction.total.toString(), style = MaterialTheme.typography.headlineSmall, color = TextPrimary, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(ps.centerCallsLabel, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
            }
        }
        Spacer(Modifier.width(18.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            slices.forEach { (count, color, label) ->
                LegendRow(color, label, ps.countPercent(count, direction.percentOf(count)))
            }
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(11.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(10.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

// ---------------------------------------------------------------------------------------------------
// 2 — Hàng chỉ số nhanh: tổng cuộc · thời lượng · tỉ lệ bắt máy.
// ---------------------------------------------------------------------------------------------------

@Composable
private fun KeyMetrics(a: PhoneAnalysisResult) {
    val ds = appStrings().detailedStats
    val ps = appStrings().phoneStats
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp)) {
        MetricCell(ds.metricTotalCalls, a.totalCalls.toString(), TextPrimary, Modifier.weight(1f))
        MetricCell(ds.metricDuration, TimeFormat.durationCompact(a.talk.totalDurationSeconds), AccentGreen, Modifier.weight(1f))
        MetricCell(ps.metricAnswerRate, a.answerRate?.let { percent(it) } ?: "—", AccentBlue, Modifier.weight(1f))
    }
}

@Composable
private fun MetricCell(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = valueColor, fontWeight = FontWeight.Bold, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary, textAlign = TextAlign.Center, maxLines = 2)
    }
}

// ---------------------------------------------------------------------------------------------------
// 3 — Biểu đồ NGANG: mỗi ngày một thanh xếp chồng (đi/đến/nhỡ), dài theo tổng cuộc, có animation.
// ---------------------------------------------------------------------------------------------------

@Composable
private fun DailyBarsCard(days: List<DayActivity>) {
    val max = days.maxOf { it.total }.coerceAtLeast(1)
    val progress = enterProgress(days)
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        days.forEach { day ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    TimeFormat.date(day.startOfDayMillis),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    modifier = Modifier.width(52.dp)
                )
                Spacer(Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f).height(14.dp).clip(CircleShape).background(CardFill)) {
                    val frac = (day.total.toFloat() / max).coerceIn(0.05f, 1f) * progress
                    Row(modifier = Modifier.fillMaxWidth(frac).fillMaxHeight().clip(CircleShape)) {
                        if (day.outgoing > 0) Box(Modifier.weight(day.outgoing.toFloat()).fillMaxHeight().background(AccentGreen))
                        if (day.incoming > 0) Box(Modifier.weight(day.incoming.toFloat()).fillMaxHeight().background(AccentBlue))
                        if (day.missed > 0) Box(Modifier.weight(day.missed.toFloat()).fillMaxHeight().background(AccentRed))
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    day.total.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    modifier = Modifier.width(26.dp)
                )
            }
        }
        LegendChips()
    }
}

/** Chú giải 3 màu gọn (đi / đến / nhỡ) đặt dưới các biểu đồ. */
@Composable
private fun LegendChips() {
    val ds = appStrings().detailedStats
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LegendDot(AccentGreen, ds.legendOutgoing)
        LegendDot(AccentBlue, ds.legendIncoming)
        LegendDot(AccentRed, ds.legendMissed)
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
    }
}

// ---------------------------------------------------------------------------------------------------
// 4 — Thời lượng & kết nối.
// ---------------------------------------------------------------------------------------------------

@Composable
private fun TalkCard(a: PhoneAnalysisResult) {
    val ps = appStrings().phoneStats
    val t = a.talk
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        StatRow(Icons.Rounded.Schedule, AccentGreen, ps.talkTotal, TimeFormat.durationVerboseHours(t.totalDurationSeconds))
        StatRow(Icons.Rounded.Timer, TextSecondary, ps.talkAverage, if (t.connectedCount > 0) TimeFormat.durationCompact(t.avgDurationSeconds) else "—")
        StatRow(Icons.Rounded.Update, TextSecondary, ps.talkLongest, if (t.longestDurationSeconds > 0) TimeFormat.durationCompact(t.longestDurationSeconds) else "—")
        StatRow(Icons.Rounded.CheckCircle, AccentGreen, ps.connectRate, percent(a.connectRate))
        StatRow(Icons.AutoMirrored.Rounded.CallMissed, AccentRed, ps.missedRate, percent(a.missedRate))
        if (t.rejected + t.blocked > 0) {
            StatRow(Icons.Rounded.Block, AccentAmber, ps.rejectedBlocked, "${t.rejected} / ${t.blocked}")
        }
    }
}

// ---------------------------------------------------------------------------------------------------
// 5 — Khung giờ trong ngày (24 cột) + giờ cao điểm + 4 buổi.
// ---------------------------------------------------------------------------------------------------

@Composable
private fun HourlyCard(a: PhoneAnalysisResult) {
    val ps = appStrings().phoneStats
    val ds = appStrings().detailedStats
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp)) {
        HourlyBars(a)
        Spacer(Modifier.height(12.dp))
        // Giờ cao điểm.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Schedule, null, tint = Primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(ps.peakHourLabel, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.weight(1f))
            Text(
                text = a.peakHour?.let { "${ps.hourRange(it, (it + 1) % 24)} · ${ds.calls(a.peakHourCount)}" } ?: "—",
                style = MaterialTheme.typography.bodySmall,
                color = if (a.peakHour != null) TextPrimary else TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
        Spacer(Modifier.height(12.dp))
        // 4 buổi trong ngày.
        Row(modifier = Modifier.fillMaxWidth()) {
            a.dayParts.forEach { (part, count) ->
                MetricCell(dayPartLabel(part), count.toString(), TextPrimary, Modifier.weight(1f))
            }
        }
    }
}

/** 24 cột giờ, cao theo tổng cuộc/giờ (chuẩn hoá theo giờ đỉnh), xếp chồng nhỡ/đến/đi — có animation nâng cột. */
@Composable
private fun HourlyBars(a: PhoneAnalysisResult) {
    val progress = enterProgress(a)
    val max = (0..23).maxOf { a.hourlyTotal(it) }.coerceAtLeast(1)
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            for (h in 0..23) {
                val total = a.hourlyTotal(h)
                val frac = (total.toFloat() / max) * progress
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(if (total > 0) 0.82f else 0f)
                            .fillMaxHeight(frac.coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    ) {
                        if (a.hourlyMissed[h] > 0) Box(Modifier.weight(a.hourlyMissed[h].toFloat()).fillMaxWidth().background(AccentRed))
                        if (a.hourlyIncoming[h] > 0) Box(Modifier.weight(a.hourlyIncoming[h].toFloat()).fillMaxWidth().background(AccentBlue))
                        if (a.hourlyOutgoing[h] > 0) Box(Modifier.weight(a.hourlyOutgoing[h].toFloat()).fillMaxWidth().background(AccentGreen))
                    }
                }
            }
        }
        Spacer(Modifier.height(3.dp))
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

// ---------------------------------------------------------------------------------------------------
// 6 — Theo ngày trong tuần (7 cột) + ngày gọi nhiều nhất.
// ---------------------------------------------------------------------------------------------------

@Composable
private fun WeekdayCard(a: PhoneAnalysisResult) {
    val ps = appStrings().phoneStats
    val dt = appStrings().datetime
    val ds = appStrings().detailedStats
    val progress = enterProgress(a)
    val max = (0..6).maxOf { a.weekday[it] }.coerceAtLeast(1)
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(72.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            for (d in 0..6) {
                val count = a.weekday[d]
                val isPeak = a.peakWeekday == d && count > 0
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        if (count > 0) count.toString() else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        maxLines = 1
                    )
                    Spacer(Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .fillMaxHeight(((count.toFloat() / max) * progress).coerceIn(0f, 1f))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(if (isPeak) Primary else AccentBlue)
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (d in 0..6) {
                Text(
                    dt.weekdayShort(DayOfWeek.of(d + 1)),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Event, null, tint = Primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(ps.peakWeekdayLabel, style = MaterialTheme.typography.bodySmall, color = TextSecondary, modifier = Modifier.weight(1f))
            Text(
                text = a.peakWeekday?.let { "${dt.weekdayShort(DayOfWeek.of(it + 1))} · ${ds.calls(a.weekday[it])}" } ?: "—",
                style = MaterialTheme.typography.bodySmall,
                color = if (a.peakWeekday != null) TextPrimary else TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------------
// 7 — Tính năng cuộc gọi (video / VoLTE).
// ---------------------------------------------------------------------------------------------------

@Composable
private fun FeaturesCard(a: PhoneAnalysisResult) {
    val ps = appStrings().phoneStats
    val f = a.features
    if (!f.hasAny) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Videocam, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(ps.featuresNone, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.weight(1f))
        }
    } else {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            StatRow(Icons.Rounded.Videocam, AccentBlue, ps.featureVideo, a.features.videoCount.toString())
            StatRow(Icons.Rounded.NetworkCell, AccentGreen, ps.featureVolte, a.features.volteCount.toString())
        }
    }
}

// ---------------------------------------------------------------------------------------------------
// 8 — Mối quan hệ theo thời gian.
// ---------------------------------------------------------------------------------------------------

@Composable
private fun SpanCard(a: PhoneAnalysisResult) {
    val ps = appStrings().phoneStats
    val s = a.span
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        StatRow(Icons.Rounded.Flag, AccentGreen, ps.spanFirst, s.firstCallMillis?.let { TimeFormat.dayClock(it) } ?: "—")
        StatRow(Icons.Rounded.Update, AccentBlue, ps.spanLast, s.lastCallMillis?.let { TimeFormat.dayClock(it) } ?: "—")
        StatRow(Icons.Rounded.Event, TextSecondary, ps.spanDistinctDays, ps.distinctDaysValue(s.distinctDaysCalled))
        StatRow(Icons.Rounded.Loop, TextSecondary, ps.spanAvgGap, s.avgGapSeconds?.let { TimeFormat.gapLabel(it * 1000) } ?: "—")
    }
}

// ---------------------------------------------------------------------------------------------------
// 9 — Theo SIM.
// ---------------------------------------------------------------------------------------------------

@Composable
private fun SimBreakdownCard(a: PhoneAnalysisResult) {
    val ds = appStrings().detailedStats
    val max = a.perSim.maxOf { it.count }.coerceAtLeast(1)
    val progress = enterProgress(a.perSim)
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        a.perSim.forEach { sim ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                SimBadge(sim.label)
                Spacer(Modifier.width(12.dp))
                Box(modifier = Modifier.weight(1f).height(10.dp).clip(CircleShape).background(CardFill)) {
                    val frac = ((sim.count.toFloat() / max) * progress).coerceIn(0f, 1f)
                    if (frac > 0f) Box(Modifier.fillMaxWidth(frac).fillMaxHeight().clip(CircleShape).background(Primary))
                }
                Spacer(Modifier.width(12.dp))
                Text(ds.calls(sim.count), style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------
// Dùng chung.
// ---------------------------------------------------------------------------------------------------

@Composable
private fun StatRow(icon: ImageVector, tint: Color, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.width(10.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

private fun cardMod(top: Dp = 6.dp): Modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = top, bottom = 6.dp)

/** Phần trăm dạng "34%" từ tỉ lệ 0..1 (làm tròn). */
private fun percent(fraction: Float): String = "${Math.round(fraction * 100)}%"

@Composable
private fun dayPartLabel(part: DayPart): String = with(appStrings().phoneStats) {
    when (part) {
        DayPart.MORNING -> partMorning
        DayPart.AFTERNOON -> partAfternoon
        DayPart.EVENING -> partEvening
        DayPart.NIGHT -> partNight
    }
}

/**
 * Tiến trình vào màn 0→1 để chạy animation biểu đồ MỘT LẦN cho mỗi bộ dữ liệu ([key]).
 *
 * Cờ [played] lưu bằng [rememberSaveable] (giống mẫu once-only flashConsumed của app) nên khi ô biểu đồ bị
 * LazyColumn thu hồi lúc cuộn ra rồi cuộn lại, animation KHÔNG chạy lại (đã "settle" ở 1f). Nhưng khi [key]
 * đổi thật (đổi tab SIM → dữ liệu khác) thì cờ reset và sweep chạy lại đúng như mong muốn.
 */
@Composable
private fun enterProgress(key: Any?): Float {
    var played by rememberSaveable(key) { mutableStateOf(false) }
    val anim = remember(key) { Animatable(if (played) 1f else 0f) }
    LaunchedEffect(key) {
        if (!played) {
            anim.animateTo(1f, animationSpec = tween(durationMillis = 720, easing = FastOutSlowInEasing))
            played = true
        }
    }
    return anim.value
}

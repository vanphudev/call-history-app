package com.antimobile.callhs.ui.repeatstats

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.EventRepeat
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
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
import com.antimobile.callhs.ui.components.Avatar
import com.antimobile.callhs.ui.components.EmptyState
import com.antimobile.callhs.ui.components.LoadingState
import com.antimobile.callhs.ui.components.PanelCard
import com.antimobile.callhs.ui.components.SectionHeader
import com.antimobile.callhs.ui.theme.AccentBlue
import com.antimobile.callhs.ui.theme.AccentGreen
import com.antimobile.callhs.ui.theme.AccentRed
import com.antimobile.callhs.ui.theme.AppBackground
import com.antimobile.callhs.ui.theme.BrandSoft
import com.antimobile.callhs.ui.theme.CardFill
import com.antimobile.callhs.ui.theme.ChipBg
import com.antimobile.callhs.ui.theme.Primary
import com.antimobile.callhs.ui.theme.TextPrimary
import com.antimobile.callhs.ui.theme.TextSecondary
import com.antimobile.callhs.i18n.appStrings
import com.antimobile.callhs.util.RecallCycle
import com.antimobile.callhs.util.RecallCycleBucket
import com.antimobile.callhs.util.RepeatBucket
import com.antimobile.callhs.util.RepeatDistribution
import com.antimobile.callhs.util.RepeatFilter
import com.antimobile.callhs.util.RepeatNumber
import com.antimobile.callhs.util.RepeatSort
import com.antimobile.callhs.util.RepeatStats
import com.antimobile.callhs.util.SpecialNumbers
import com.antimobile.callhs.util.TimeFormat
import com.antimobile.callhs.util.formatPhone

@Composable
fun RepeatStatsScreen(vm: RepeatStatsViewModel, onBack: () -> Unit) {
    LaunchedEffect(Unit) { if (!vm.loaded) vm.load() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { vm.refresh() }

    val s = appStrings()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .statusBarsPadding()
    ) {
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
            Text(s.settings.repeatTitle, style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when {
                !vm.hasCallLogPermission -> EmptyState(s.repeatStats.permNeeded)
                vm.loading && !vm.loaded -> LoadingState(text = s.repeatStats.analyzing)
                vm.analysis.overview.totalCalls == 0 ->
                    EmptyState(s.repeatStats.emptyNoCalls)
                else -> RepeatContent(vm)
            }
        }
    }
}

@Composable
private fun RepeatContent(vm: RepeatStatsViewModel) {
    val a = vm.analysis
    val s = appStrings().repeatStats
    val expanded = remember { mutableStateMapOf<String, Boolean>() }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = bottomInsetPadding()) {
        item("intro") { IntroNote(a.overview.windowStartMillis, a.overview.windowEndMillis, a.windowDays) }
        item("overview") { OverviewCard(vm) }

        item("dist-title") { SectionHeader(s.distTitle) }
        item("dist") { DistributionCard(a.distribution) }

        item("cycle-title") { SectionHeader(s.cycleTitle) }
        item("cycle") { RecallCycleCard(a.recallCycles) }

        item("filter") { FilterBar(vm) }
        item("legend") { HeatLegend(a.windowDays) }

        item("list-title") { SectionHeader(s.listTitle(vm.numbers.size)) }
        if (vm.numbers.isEmpty()) {
            item("list-empty") { EmptyBlock(s.noMatch) }
        } else {
            items(vm.numbers, key = { "r_${it.number}" }) { n ->
                RepeatNumberRow(
                    number = n,
                    windowDays = a.windowDays,
                    expanded = expanded[n.number] == true,
                    onToggle = { expanded[n.number] = !(expanded[n.number] ?: false) }
                )
            }
        }
    }
}

@Composable
private fun IntroNote(startMillis: Long, endMillis: Long, windowDays: Int) {
    Text(
        text = appStrings().repeatStats.intro(windowDays, TimeFormat.date(startMillis), TimeFormat.date(endMillis)),
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 2.dp)
    )
}

@Composable
private fun OverviewCard(vm: RepeatStatsViewModel) {
    val o = vm.analysis.overview
    val s = appStrings().repeatStats
    PanelCard(modifier = cardModifier(), radius = 20.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp, horizontal = 8.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                MetricCell(s.metricTotalCalls, o.totalCalls.toString(), TextPrimary, Modifier.weight(1f))
                MetricCell(s.metricDistinctNumbers, o.distinctNumbers.toString(), TextPrimary, Modifier.weight(1f))
                MetricCell(s.metricRepeatNumbers, o.repeatNumberCount.toString(), Primary, Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                MetricCell(s.metricRepeatCalls, o.repeatCallCount.toString(), AccentBlue, Modifier.weight(1f))
                MetricCell(s.metricMultiDay, o.multiDayNumberCount.toString(), AccentGreen, Modifier.weight(1f))
                MetricCell(s.metricMaxCalls, if (o.maxRepeatCount > 0) s.timesCount(o.maxRepeatCount) else "—", AccentRed, Modifier.weight(1f))
            }
        }
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

@Composable
private fun DistributionCard(distribution: List<RepeatDistribution>) {
    val max = distribution.maxOfOrNull { it.numberCount }?.coerceAtLeast(1) ?: 1
    PanelCard(modifier = cardModifier(), radius = 20.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 14.dp)) {
            distribution.forEachIndexed { i, d ->
                if (i > 0) Spacer(Modifier.height(10.dp))
                BarRow(label = bucketLabel(d.bucket), value = d.numberCount, max = max, unit = appStrings().repeatStats.barUnitNumbers, barColor = Primary)
            }
        }
    }
}

@Composable
private fun RecallCycleCard(cycles: List<RecallCycleBucket>) {
    val total = cycles.sumOf { it.numberCount }
    PanelCard(modifier = cardModifier(), radius = 20.dp) {
        if (total == 0) {
            Text(
                appStrings().repeatStats.cycleEmpty,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp)
            )
        } else {
            val max = cycles.maxOf { it.numberCount }.coerceAtLeast(1)
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp, horizontal = 14.dp)) {
                Text(
                    appStrings().repeatStats.cycleDesc,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                cycles.forEachIndexed { i, c ->
                    if (i > 0) Spacer(Modifier.height(10.dp))
                    BarRow(label = cycleLabel(c.cycle), value = c.numberCount, max = max, unit = appStrings().repeatStats.barUnitNumbers, barColor = AccentBlue)
                }
            }
        }
    }
}

/** Một hàng biểu đồ cột ngang: nhãn cố định + thanh tô theo tỉ lệ + số đếm. */
@Composable
private fun BarRow(label: String, value: Int, max: Int, unit: String, barColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(78.dp)
        )
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier.weight(1f).height(16.dp).clip(CircleShape).background(CardFill),
            contentAlignment = Alignment.CenterStart
        ) {
            val frac = (value.toFloat() / max).coerceIn(0f, 1f)
            if (value > 0) {
                Box(Modifier.fillMaxWidth(frac.coerceAtLeast(0.04f)).fillMaxHeight().clip(CircleShape).background(barColor))
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            if (value > 0) "$value $unit" else "—",
            style = MaterialTheme.typography.labelMedium,
            color = if (value > 0) TextPrimary else TextSecondary,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.width(46.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun FilterBar(vm: RepeatStatsViewModel) {
    val filters = RepeatFilter.entries
    val sorts = RepeatSort.entries
    Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 2.dp)) {
        Segmented(
            labels = filters.map { filterLabel(it) },
            selected = filters.indexOf(vm.filter),
            onSelect = { vm.selectFilter(filters[it]) }
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(appStrings().repeatStats.sortLabel, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(Modifier.width(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                sorts.forEach { so ->
                    SortChip(label = sortName(so), selected = vm.sort == so, onClick = { vm.selectSort(so) })
                }
            }
        }
    }
}

@Composable
private fun SortChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) Color.White else TextSecondary,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        maxLines = 1,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) Primary else ChipBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@Composable
private fun HeatLegend(windowDays: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            appStrings().repeatStats.legend,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        listOf(0, 1, 2, 3, 5).forEach { c ->
            Box(Modifier.size(11.dp).clip(RoundedCornerShape(2.dp)).background(heatColor(c)))
            Spacer(Modifier.width(3.dp))
        }
    }
}

/** Bộ chọn phân đoạn (segmented) — trượt màu Primary cho mục đang chọn. */
@Composable
private fun Segmented(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().height(40.dp).clip(CircleShape).background(ChipBg).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        labels.forEachIndexed { i, label ->
            val sel = i == selected
            val interaction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(if (sel) Primary else Color.Transparent)
                    .clickable(interactionSource = interaction, indication = null) { onSelect(i) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (sel) Color.White else TextSecondary,
                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun RepeatNumberRow(number: RepeatNumber, windowDays: Int, expanded: Boolean, onToggle: () -> Unit) {
    val s = appStrings().repeatStats
    PanelCard(modifier = cardModifier(vertical = 5.dp), radius = 16.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 12.dp, vertical = 11.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatAvatar(number.number, number.cachedName, number.photoUri, 42.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        statName(number.number, number.cachedName),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        collapsedMeta(number),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text("${number.totalCalls}", style = MaterialTheme.typography.titleMedium, color = Primary, fontWeight = FontWeight.Bold)
                    Text(s.callsUnit, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
                Spacer(Modifier.width(4.dp))
                Icon(
                    if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) s.collapse else s.expand,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.height(10.dp))
            HeatStrip(number, windowDays)

            val badge = classification(number)
            if (badge != null || number.hasSameDayRepeat) {
                Spacer(Modifier.height(9.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (badge != null) TagChip(badge.first, badge.second)
                    if (number.hasSameDayRepeat) TagChip(s.maxPerDay(number.maxCallsInADay), AccentRed)
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    DetailLine(Icons.Rounded.CalendarMonth, s.detailDates(TimeFormat.date(number.firstCallMillis), TimeFormat.date(number.lastCallMillis)))
                    Spacer(Modifier.height(7.dp))
                    DetailLine(Icons.Rounded.EventRepeat, cycleLine(number))
                    Spacer(Modifier.height(7.dp))
                    DetailLine(Icons.Rounded.Repeat, breakdownLine(number))
                    Spacer(Modifier.height(10.dp))
                    Text(s.daysCalledLabel, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        number.days.asReversed().forEach { d ->
                            DayChip("${TimeFormat.date(d.startOfDayMillis)} · ${d.count}")
                        }
                    }
                }
            }
        }
    }
}

/** Dải 30 ô (mỗi ô = 1 ngày), ô đậm nhạt theo số cuộc gọi trong ngày. */
@Composable
private fun HeatStrip(number: RepeatNumber, windowDays: Int) {
    val counts = number.dailyCounts(windowDays)
    Row(
        modifier = Modifier.fillMaxWidth().height(20.dp),
        horizontalArrangement = Arrangement.spacedBy(1.5.dp)
    ) {
        for (i in 0 until windowDays) {
            Box(
                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(heatColor(counts[i]))
            )
        }
    }
}

@Composable
private fun DetailLine(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = TextPrimary, maxLines = 2)
    }
}

@Composable
private fun DayChip(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = TextPrimary,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        modifier = Modifier.clip(CircleShape).background(CardFill).padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
private fun TagChip(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = Modifier.clip(CircleShape).background(CardFill).padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

private fun heatColor(count: Int): Color = when {
    count <= 0 -> CardFill
    count == 1 -> Primary.copy(alpha = 0.28f)
    count == 2 -> Primary.copy(alpha = 0.5f)
    count <= 4 -> Primary.copy(alpha = 0.75f)
    else -> Primary
}

/** Nhãn phân loại: quay lại nhiều ngày → "Khách quay lại"; chỉ trong ngày → "Gọi lại trong ngày". */
private fun classification(n: RepeatNumber): Pair<String, Color>? = with(appStrings().repeatStats) {
    when {
        n.isMultiDay -> classReturning to AccentGreen
        n.hasSameDayRepeat -> classSameDay to AccentBlue
        else -> null
    }
}

private fun collapsedMeta(n: RepeatNumber): String {
    val parts = ArrayList<String>(4)
    parts.add(appStrings().repeatStats.daysCount(n.distinctDays))
    if (n.outgoing > 0) parts.add("↗ ${n.outgoing}")
    if (n.incoming > 0) parts.add("↘ ${n.incoming}")
    if (n.missed > 0) parts.add("✕ ${n.missed}")
    return parts.joinToString("  ·  ")
}

private fun breakdownLine(n: RepeatNumber): String = with(appStrings().repeatStats) {
    val parts = ArrayList<String>(4)
    parts.add(outgoing(n.outgoing))
    parts.add(incoming(n.incoming))
    parts.add(missed(n.missed))
    if (n.connectedCount > 0) parts.add(talkTime(TimeFormat.durationCompact(n.totalDurationSeconds)))
    parts.joinToString(" · ")
}

private fun cycleLine(n: RepeatNumber): String = with(appStrings().repeatStats) {
    val gap = n.avgRecallGapDays ?: return onlyOneDay
    avgCycle(fmtGap(gap), n.spanDays)
}

private fun fmtGap(d: Double): String {
    val rounded = Math.round(d * 10.0) / 10.0
    return if (rounded == rounded.toLong().toDouble()) rounded.toLong().toString() else rounded.toString()
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

private fun statName(number: String, cachedName: String?): String =
    cachedName?.takeIf { it.isNotBlank() } ?: SpecialNumbers.name(number) ?: formatPhone(number)

// --- Nhãn 4 enum theo ngôn ngữ hiện hành (ánh xạ enum→chuỗi ở UI để i18n không phụ thuộc tầng data) ---
private fun bucketLabel(b: RepeatBucket): String = with(appStrings().repeatStats) {
    when (b) {
        RepeatBucket.ONCE -> bucketOnce
        RepeatBucket.TWICE -> bucketTwice
        RepeatBucket.THREE_FIVE -> bucket3to5
        RepeatBucket.SIX_TEN -> bucket6to10
        RepeatBucket.OVER_TEN -> bucketOver10
    }
}

private fun cycleLabel(c: RecallCycle): String = with(appStrings().repeatStats) {
    when (c) {
        RecallCycle.WITHIN_WEEK -> cycleWithinWeek
        RecallCycle.ONE_TWO_WEEKS -> cycle1to2w
        RecallCycle.TWO_THREE_WEEKS -> cycle2to3w
        RecallCycle.OVER_THREE_WEEKS -> cycleOver3w
    }
}

private fun filterLabel(f: RepeatFilter): String = with(appStrings().repeatStats) {
    when (f) {
        RepeatFilter.REPEAT -> filterRepeat
        RepeatFilter.MULTI_DAY -> filterMultiDay
        RepeatFilter.ALL -> filterAll
    }
}

private fun sortName(so: RepeatSort): String = with(appStrings().repeatStats) {
    when (so) {
        RepeatSort.TOTAL -> sortTotal
        RepeatSort.DAYS -> sortDays
        RepeatSort.RECENT -> sortRecent
    }
}

private fun cardModifier(vertical: Dp = 6.dp): Modifier =
    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = vertical)

@Composable
private fun bottomInsetPadding(): PaddingValues {
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return PaddingValues(top = 4.dp, bottom = navBottom + 24.dp)
}

package com.antimobile.mcas.ui.callhistory

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material.icons.rounded.UnfoldLess
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.antimobile.mcas.R
import com.antimobile.mcas.data.model.CallEntry
import com.antimobile.mcas.data.model.CallNumberDetail
import com.antimobile.mcas.i18n.LanguageSettings
import com.antimobile.mcas.i18n.appStrings
import com.antimobile.mcas.ui.calldetail.CallDetailViewModel
import com.antimobile.mcas.ui.components.ActionGlyph
import com.antimobile.mcas.ui.category.AvatarCategoryBadges
import com.antimobile.mcas.ui.components.Avatar
import com.antimobile.mcas.ui.components.ContextAction
import com.antimobile.mcas.ui.components.ContextMenuOverlay
import com.antimobile.mcas.ui.components.EmptyState
import com.antimobile.mcas.ui.components.LoadingState
import com.antimobile.mcas.ui.components.PanelCard
import com.antimobile.mcas.ui.components.STATS_HANDLE_HEIGHT
import com.antimobile.mcas.ui.components.SectionHeader
import com.antimobile.mcas.ui.components.SimBadge
import com.antimobile.mcas.ui.components.SimScopeControl
import com.antimobile.mcas.ui.components.StatsSheetContent
import com.antimobile.mcas.ui.components.StatsSheetHandle
import com.antimobile.mcas.ui.components.collapsingHeaderSwap
import com.antimobile.mcas.ui.theme.AccentBlue
import com.antimobile.mcas.ui.theme.AccentGreen
import com.antimobile.mcas.ui.theme.AppBackground
import com.antimobile.mcas.ui.theme.CardFill
import com.antimobile.mcas.ui.theme.TextPrimary
import com.antimobile.mcas.ui.theme.TextSecondary
import com.antimobile.mcas.util.CallActions
import com.antimobile.mcas.util.PhoneActions
import com.antimobile.mcas.util.SimScope
import com.antimobile.mcas.util.SpecialNumbers
import com.antimobile.mcas.util.TimeFormat
import com.antimobile.mcas.util.formatPhone

private sealed interface Row2 {
    data class Header(val label: String) : Row2
    data class Item(val entry: CallEntry) : Row2
}

private fun buildSections(entries: List<CallEntry>): List<Row2> {
    val rows = ArrayList<Row2>(entries.size + 4)
    var last: String? = null
    for (e in entries) {
        val s = TimeFormat.sectionLabel(e.dateMillis)
        if (s != last) { rows.add(Row2.Header(s)); last = s }
        rows.add(Row2.Item(e))
    }
    return rows
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllCallsScreen(
    vm: CallDetailViewModel,
    onBack: () -> Unit,
    onOpenTimeline: () -> Unit,
    onOpenStats: () -> Unit
) {
    val context = LocalContext.current
    val s = appStrings()
    val d = vm.detail
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val hasDetail = d != null && d.entries.isNotEmpty()

    val listState = rememberLazyListState()
    val density = LocalDensity.current
    val collapseThresholdPx = with(density) { 92.dp.toPx() }
    val collapsed by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > collapseThresholdPx
        }
    }

    // ---- Lọc theo SIM (thanh ở đầu) — cùng logic toàn app ----
    // Khoá phạm vi 1 SIM ở Cài đặt → tab "Đang xem SIM X" (dữ liệu đã lọc sẵn); ngược lại & số này có ≥2 SIM
    // → thanh LỌC nhanh. Lọc chỉ đổi DANH SÁCH hiển thị (số liệu ở sheet vẫn theo toàn bộ như màn danh sách).
    val globalSim = SimScope.effectiveLabel
    val simLabels = remember(d?.entries) { d?.entries?.mapNotNull { it.simLabel }?.distinct()?.sorted() ?: emptyList() }
    var localSim by remember(d?.number) { mutableStateOf<String?>(null) }
    val effectiveLocal = if (globalSim != null) null else localSim?.takeIf { it in simLabels }
    val shownEntries = remember(d?.entries, effectiveLocal) {
        val es = d?.entries ?: emptyList()
        effectiveLocal?.let { l -> es.filter { it.simLabel == l } } ?: es
    }

    // Cuộc gọi cuối cùng (mới nhất) mở sẵn dropdown mặc định.
    var expandedIds by remember(d?.number) { mutableStateOf(setOfNotNull(d?.entries?.firstOrNull()?.id)) }
    val total = shownEntries.size
    val allExpanded = total > 0 && shownEntries.all { it.id in expandedIds }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded)
    )
    val peek = if (hasDetail) STATS_HANDLE_HEIGHT + navBottom else 0.dp
    // Nội dung thống kê MỜ DẦN: thu gọn = ẩn (alpha 0) → chỉ thấy handle; mở rộng = hiện (alpha 1). Chuyển mượt.
    val statsExpanded = scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded
    val statsAlpha by animateFloatAsState(targetValue = if (statsExpanded) 1f else 0f, label = "statsAlpha")

    // Context-menu khi nhấn giữ item — dựng ở GỐC (bọc NGOÀI scaffold) để nền tối phủ TRỌN màn, kể cả bottom sheet.
    val statusBarPx = WindowInsets.statusBars.getTop(density)
    val navBarPx = WindowInsets.navigationBars.getBottom(density)
    var contextTarget by remember { mutableStateOf<AllCallsContextTarget?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = peek,
        sheetContainerColor = AppBackground,
        sheetContentColor = TextPrimary,
        sheetShadowElevation = 12.dp,
        sheetTonalElevation = 0.dp,
        sheetDragHandle = { if (d != null && d.entries.isNotEmpty()) StatsSheetHandle(d, onOpenStats = onOpenStats) },
        sheetContent = { if (d != null && d.entries.isNotEmpty()) StatsSheetContent(d, navBottom, Modifier.graphicsLayer { alpha = statsAlpha }) },
        containerColor = AppBackground
    ) { _ ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .statusBarsPadding()
        ) {
            TopBar(
                onBack = onBack,
                collapsed = collapsed && hasDetail,
                detail = d,
                showToggle = hasDetail,
                allExpanded = allExpanded,
                onOpenTimeline = onOpenTimeline,
                onToggleAll = {
                    expandedIds = if (allExpanded) emptySet() else shownEntries.map { it.id }.toSet()
                }
            )
            // Thanh lọc SIM ở ĐẦU (căn giữa) — chỉ hiện khi đang khoá phạm vi 1 SIM hoặc số này có ≥2 SIM.
            if (hasDetail && (globalSim != null || simLabels.size >= 2)) {
                // Nhiều hơn 2 SIM (hiếm) → cho CUỘN ngang để không ô nào bị cắt/mất vùng chạm (giống màn danh sách).
                val simRowModifier = if (simLabels.size > 2) {
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp)
                } else {
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                }
                Row(
                    modifier = simRowModifier,
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SimScopeControl(
                        availableLabels = simLabels,
                        localSelection = effectiveLocal,   // đã chuẩn hoá → ô tô sáng luôn khớp danh sách đang hiện
                        onLocalSelect = { localSim = it },
                        allLabel = s.callList.filterAll
                    )
                }
            }
            when {
                vm.loading && d == null -> Box(Modifier.fillMaxSize()) { LoadingState(text = s.allCalls.loading) }
                d == null || d.entries.isEmpty() -> Box(Modifier.fillMaxSize()) { EmptyState(s.callDetail.emptyNoCalls) }
                else -> {
                    // LanguageSettings.lang trong khoá: đổi ngôn ngữ → dựng lại nhãn nhóm ngày (TimeFormat.sectionLabel).
                    val rows = remember(shownEntries, LanguageSettings.lang) { buildSections(shownEntries) }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = peek + 16.dp)
                    ) {
                        item { HeaderRow(d, onDoubleTapCopy = { CallActions.copy(context, d.number) }) }
                        items(
                            items = rows,
                            key = { r -> if (r is Row2.Header) "h_${r.label}" else "e_${(r as Row2.Item).entry.id}" }
                        ) { r ->
                            when (r) {
                                is Row2.Header -> SectionHeader(r.label)
                                is Row2.Item -> AllCallsCallItem(
                                    entry = r.entry,
                                    expanded = r.entry.id in expandedIds,
                                    onToggle = {
                                        expandedIds = if (r.entry.id in expandedIds) expandedIds - r.entry.id else expandedIds + r.entry.id
                                    },
                                    onLongPress = { bounds ->
                                        contextTarget = AllCallsContextTarget(r.entry, bounds, r.entry.id in expandedIds)
                                    },
                                    activeInMenu = contextTarget?.entry?.id == r.entry.id
                                )
                            }
                        }
                    }
                }
            }
        }
    }

        // Lớp phủ context-menu (nhấn giữ) — trong Box GỐC nên phủ trọn màn, trên cả bottom sheet.
        contextTarget?.let { tgt ->
            ContextMenuOverlay(
                bounds = tgt.bounds,
                actions = listOf(
                    ContextAction(ActionGlyph.Vector(Icons.AutoMirrored.Rounded.Message, AccentBlue), s.callList.menuMessage) { CallActions.message(context, tgt.entry.number) },
                    ContextAction(ActionGlyph.Vector(Icons.Rounded.Call, AccentGreen), s.callList.menuCall) { CallActions.dial(context, tgt.entry.number) },
                    ContextAction(ActionGlyph.Logo(R.drawable.ic_zalo), s.callList.menuZalo) { PhoneActions.openZalo(context, tgt.entry.number) },
                    ContextAction(ActionGlyph.Vector(Icons.Rounded.ContentCopy, TextSecondary), s.callList.menuCopy) { CallActions.copy(context, tgt.entry.number) },
                ),
                topInsetPx = statusBarPx,
                bottomInsetPx = navBarPx,
                onClosed = { contextTarget = null },
                lifted = { AllCallsCard(entry = tgt.entry, expanded = tgt.expanded) }
            )
        }
    }
}

@Composable
private fun TopBar(
    onBack: () -> Unit,
    collapsed: Boolean,
    detail: CallNumberDetail?,
    showToggle: Boolean,
    allExpanded: Boolean,
    onOpenTimeline: () -> Unit,
    onToggleAll: () -> Unit
) {
    val s = appStrings()
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, s.common.back, TextPrimary, onBack)
        Spacer(Modifier.width(12.dp))
        // Đổi giữa "tiêu đề + số" ↔ "avatar + tên + số·khu vực" bằng cú TRƯỢT DỌC đục
        // (không crossfade mờ chồng). Kích hoạt theo ngưỡng nên chuyển hẳn, có animation uyển chuyển.
        AnimatedContent(
            targetState = collapsed,
            transitionSpec = { collapsingHeaderSwap() },
            label = "topbar-title",
            modifier = Modifier.weight(1f).fillMaxHeight()
        ) { isCollapsed ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                if (isCollapsed && detail != null) {
                    CollapsedInfo(detail)
                } else {
                    Column {
                        Text(
                            s.allCalls.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (detail != null) {
                            Text(
                                formatPhone(detail.number),
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
        if (showToggle) {
            // Nút mở màn DÒNG THỜI GIAN (cùng dữ liệu, bố cục theo trục thời gian).
            RoundIconButton(Icons.Rounded.Timeline, s.timeline.openTimeline, TextSecondary, onOpenTimeline)
            RoundIconButton(
                if (allExpanded) Icons.Rounded.UnfoldLess else Icons.Rounded.UnfoldMore,
                if (allExpanded) s.allCalls.collapseAll else s.allCalls.expandAll,
                TextSecondary,
                onToggleAll
            )
        }
    }
}

@Composable
private fun CollapsedInfo(detail: CallNumberDetail) {
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

/** Thông tin số theo dạng hàng ngang: avatar, tên (nếu có), số, nhà mạng, SIM. Nhấn ĐÚP để sao chép số. */
@Composable
private fun HeaderRow(detail: CallNumberDetail, onDoubleTapCopy: () -> Unit) {
    val isNamed = !detail.cachedName.isNullOrBlank()
    PanelCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(detail.number) { detectTapGestures(onDoubleTap = { onDoubleTapCopy() }) }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(58.dp)) {
                Avatar(label = detail.nameOrNumber, photoUri = detail.photoUri, isNamed = isNamed, size = 58.dp, specialIconRes = SpecialNumbers.of(detail.number)?.iconRes)
                AvatarCategoryBadges(detail.number, modifier = Modifier.align(Alignment.BottomEnd), badgeSize = 17.dp)
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(detail.displayName, style = MaterialTheme.typography.titleLarge, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(formatPhone(detail.number), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    detail.carrier?.let { InfoChip(it) }
                    InfoChip(detail.region)
                    detail.displaySimLabels.forEach { SimBadge(it) }
                }
            }
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Box(modifier = Modifier.clip(CircleShape).background(CardFill).padding(horizontal = 12.dp, vertical = 5.dp)) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    }
}

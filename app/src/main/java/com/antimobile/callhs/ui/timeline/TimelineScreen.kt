package com.antimobile.callhs.ui.timeline

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.HourglassEmpty
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.antimobile.callhs.R
import com.antimobile.callhs.data.model.CallEntry
import com.antimobile.callhs.data.model.CallNumberDetail
import com.antimobile.callhs.data.model.CallType
import com.antimobile.callhs.i18n.LanguageSettings
import com.antimobile.callhs.i18n.appStrings
import com.antimobile.callhs.ui.calldetail.CallDetailViewModel
import com.antimobile.callhs.ui.components.ActionGlyph
import com.antimobile.callhs.ui.components.Avatar
import com.antimobile.callhs.ui.components.CallTypeIcon
import com.antimobile.callhs.ui.components.ContextAction
import com.antimobile.callhs.ui.components.ContextMenuOverlay
import com.antimobile.callhs.ui.components.EmptyState
import com.antimobile.callhs.ui.components.LoadingState
import com.antimobile.callhs.ui.components.PanelCard
import com.antimobile.callhs.ui.components.STATS_HANDLE_HEIGHT
import com.antimobile.callhs.ui.components.SimBadge
import com.antimobile.callhs.ui.components.SimScopeControl
import com.antimobile.callhs.ui.components.StatsSheetContent
import com.antimobile.callhs.ui.components.StatsSheetHandle
import com.antimobile.callhs.ui.components.collapsingHeaderSwap
import com.antimobile.callhs.ui.components.rememberPressHighlight
import com.antimobile.callhs.ui.theme.AccentAmber
import com.antimobile.callhs.ui.theme.AccentAmberBg
import com.antimobile.callhs.ui.theme.AccentBlue
import com.antimobile.callhs.ui.theme.AccentBlueBg
import com.antimobile.callhs.ui.theme.AccentGray
import com.antimobile.callhs.ui.theme.AccentGrayBg
import com.antimobile.callhs.ui.theme.AccentGreen
import com.antimobile.callhs.ui.theme.AccentGreenBg
import com.antimobile.callhs.ui.theme.AccentRed
import com.antimobile.callhs.ui.theme.AccentRedBg
import com.antimobile.callhs.ui.theme.AppBackground
import com.antimobile.callhs.ui.theme.CardFill
import com.antimobile.callhs.ui.theme.SoftGreen
import com.antimobile.callhs.ui.theme.SoftRed
import com.antimobile.callhs.ui.theme.TextPrimary
import com.antimobile.callhs.ui.theme.TextSecondary
import com.antimobile.callhs.util.CallActions
import com.antimobile.callhs.util.CallResults
import com.antimobile.callhs.util.PhoneActions
import com.antimobile.callhs.util.SimScope
import com.antimobile.callhs.util.SpecialNumbers
import com.antimobile.callhs.util.TimeFormat
import com.antimobile.callhs.util.formatPhone

/*
 * MÀN DÒNG THỜI GIAN — cùng dữ liệu & bộ lọc với màn XEM TẤT CẢ (dùng chung [CallDetailViewModel]), nhưng
 * bố cục theo TRỤC THỜI GIAN dọc:
 *  - Một "trục" (spine) chạy dọc giữa màn; mỗi CUỘC GỌI là một mốc (chấm) trên trục.
 *  - Thẻ chia HAI BÊN theo HƯỚNG: gọi ĐI ở BÊN TRÁI, gọi ĐẾN/NHỠ ở BÊN PHẢI (cảm giác khung hội thoại).
 *  - Giữa hai cuộc gọi liên tiếp là một "chip" hiển thị TỔNG THỜI GIAN cách nhau ([TimeFormat.gapLabel]):
 *    phút → giờ → ngày (không lên tháng), tối đa 2 đơn vị.
 * Mốc mới nhất ở TRÊN (entries đã sort mới→cũ). Chạm một mốc để bung/thu chi tiết; nút ở thanh đầu mở/đóng TẤT
 * CẢ; nhấn giữ để hiện context-menu (Nhắn tin/Gọi/Zalo/Sao chép) — dùng chung [ContextMenuOverlay].
 */

// ── Kích thước trục & mốc ──
private val NODE_V_GAP = 6.dp             // khoảng đệm dọc mỗi mốc (đường trục vẫn chạy xuyên qua để nối liền)
private val RAIL_W = 46.dp                // bề rộng LÀN GIỮA dành cho chấm mốc (thẻ hai bên ôm sát làn này)
private val SPINE_W = 2.dp                // độ dày đường trục
private val DOT_OUTER = 20.dp             // quầng nền quanh chấm (cắt gọn đường trục ngay tại mốc)
private val DOT = 13.dp                   // chấm mốc (tô theo kết quả cuộc gọi)
private val DOT_RING_W = 2.5.dp           // độ dày viền chấm rỗng (cuộc KHÔNG kết nối)
private val CONNECTOR_W = 2.dp            // cuống ngắn nối chấm với thẻ
// Tâm chấm cách ĐỈNH phần nội dung mốc — GHIM CỐ ĐỊNH (không đo thẻ) nên chấm/cuống KHÔNG nhảy khi bung chi
// tiết: thẻ chỉ dài THÊM xuống dưới. ~ đệm dọc thẻ (10dp) + nửa chiều cao phần thu gọn 2 dòng.
private val DOT_CENTER_FROM_TOP = 32.dp

// Đường trục: đủ rõ ở cả Sáng/Tối (dẫn xuất từ [TextSecondary] nên tự đổi theo chế độ).
private val SpineColor: Color get() = TextSecondary.copy(alpha = 0.28f)

/** Bậc khoảng cách — quyết định chiều cao hàng & kiểu chip: S < 1 giờ, M 1 giờ–< 1 ngày, L ≥ 1 ngày. */
private enum class GapTier { S, M, L }

private fun gapTier(deltaMillis: Long): GapTier {
    val hours = deltaMillis / 3_600_000L
    return when {
        hours < 1 -> GapTier.S
        hours < 24 -> GapTier.M
        else -> GapTier.L
    }
}

/** Một hàng trong dòng thời gian: MỐC cuộc gọi, hoặc KHOẢNG CÁCH giữa hai mốc liên tiếp. */
private sealed interface TLRow {
    data class Node(val entry: CallEntry, val onLeft: Boolean, val isFirst: Boolean, val isLast: Boolean) : TLRow
    data class Gap(val fromId: Long, val toId: Long, val deltaMillis: Long) : TLRow
}

/** Gọi ĐI ở bên TRÁI; mọi hướng còn lại (đến/nhỡ/từ chối/…) ở bên PHẢI. */
private fun isOutgoing(type: CallType) = type == CallType.OUTGOING

/** Dựng chuỗi hàng: Node, Gap, Node, Gap, … Gap[i] = cách giữa cuộc mới hơn (i) và cuộc cũ hơn (i+1). */
private fun buildTimeline(entries: List<CallEntry>): List<TLRow> {
    val rows = ArrayList<TLRow>(entries.size * 2)
    for (i in entries.indices) {
        val e = entries[i]
        rows.add(TLRow.Node(e, onLeft = isOutgoing(e.type), isFirst = i == 0, isLast = i == entries.lastIndex))
        if (i < entries.lastIndex) {
            val next = entries[i + 1]
            val delta = (e.dateMillis - next.dateMillis).coerceAtLeast(0)
            rows.add(TLRow.Gap(fromId = e.id, toId = next.id, deltaMillis = delta))
        }
    }
    return rows
}

/** Item màn dòng thời gian đang mở context-menu: cuộc gọi + khung THẺ (toạ độ cửa sổ, px) + trạng thái mở. */
private data class TimelineContextTarget(val entry: CallEntry, val bounds: Rect, val expanded: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    vm: CallDetailViewModel,
    onBack: () -> Unit
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

    // ── Lọc theo SIM (cùng logic toàn app như màn Xem tất cả) ──
    val globalSim = SimScope.effectiveLabel
    val simLabels = remember(d?.entries) { d?.entries?.mapNotNull { it.simLabel }?.distinct()?.sorted() ?: emptyList() }
    var localSim by remember(d?.number) { mutableStateOf<String?>(null) }
    val effectiveLocal = if (globalSim != null) null else localSim?.takeIf { it in simLabels }
    val shownEntries = remember(d?.entries, effectiveLocal) {
        val es = d?.entries ?: emptyList()
        effectiveLocal?.let { l -> es.filter { it.simLabel == l } } ?: es
    }

    // Cuộc gọi cuối cùng (mới nhất) mở sẵn chi tiết mặc định.
    var expandedIds by remember(d?.number) { mutableStateOf(setOfNotNull(d?.entries?.firstOrNull()?.id)) }
    val total = shownEntries.size
    val allExpanded = total > 0 && shownEntries.all { it.id in expandedIds }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded)
    )
    val peek = if (hasDetail) STATS_HANDLE_HEIGHT + navBottom else 0.dp
    val statsExpanded = scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded
    val statsAlpha by animateFloatAsState(targetValue = if (statsExpanded) 1f else 0f, label = "statsAlpha")

    val statusBarPx = WindowInsets.statusBars.getTop(density)
    val navBarPx = WindowInsets.navigationBars.getBottom(density)
    var contextTarget by remember { mutableStateOf<TimelineContextTarget?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        BottomSheetScaffold(
            scaffoldState = scaffoldState,
            sheetPeekHeight = peek,
            sheetContainerColor = AppBackground,
            sheetContentColor = TextPrimary,
            sheetShadowElevation = 12.dp,
            sheetTonalElevation = 0.dp,
            sheetDragHandle = { if (d != null && d.entries.isNotEmpty()) StatsSheetHandle(d) },
            sheetContent = { if (d != null && d.entries.isNotEmpty()) StatsSheetContent(d, navBottom, Modifier.graphicsLayer { alpha = statsAlpha }) },
            containerColor = AppBackground
        ) { _ ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                    .statusBarsPadding()
            ) {
                TimelineTopBar(
                    onBack = onBack,
                    collapsed = collapsed && hasDetail,
                    detail = d,
                    showToggle = hasDetail,
                    allExpanded = allExpanded,
                    onToggleAll = {
                        expandedIds = if (allExpanded) emptySet() else shownEntries.map { it.id }.toSet()
                    }
                )
                if (hasDetail && (globalSim != null || simLabels.size >= 2)) {
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
                            localSelection = effectiveLocal,
                            onLocalSelect = { localSim = it },
                            allLabel = s.callList.filterAll
                        )
                    }
                }
                // Chú thích ý nghĩa HAI BÊN: trái = gọi đi, phải = gọi đến/nhỡ (làm rõ bố cục chia đôi).
                if (hasDetail) TimelineLegend()
                when {
                    vm.loading && d == null -> Box(Modifier.fillMaxSize()) { LoadingState(text = s.allCalls.loading) }
                    d == null || d.entries.isEmpty() -> Box(Modifier.fillMaxSize()) { EmptyState(s.callDetail.emptyNoCalls) }
                    else -> {
                        // Khoá theo ngôn ngữ để dựng lại nhãn khoảng cách (đơn vị đổi theo ngôn ngữ).
                        val rows = remember(shownEntries, LanguageSettings.lang) { buildTimeline(shownEntries) }
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 4.dp, bottom = peek + 24.dp)
                        ) {
                            item(key = "header") { TimelineHeaderRow(d, onDoubleTapCopy = { CallActions.copy(context, d.number) }) }
                            items(
                                items = rows,
                                key = { r -> if (r is TLRow.Node) "n_${r.entry.id}" else "g_${(r as TLRow.Gap).fromId}_${r.toId}" }
                            ) { r ->
                                when (r) {
                                    is TLRow.Gap -> TimelineGap(r.deltaMillis)
                                    is TLRow.Node -> TimelineNode(
                                        entry = r.entry,
                                        onLeft = r.onLeft,
                                        isFirst = r.isFirst,
                                        isLast = r.isLast,
                                        expanded = r.entry.id in expandedIds,
                                        activeInMenu = contextTarget?.entry?.id == r.entry.id,
                                        onToggle = {
                                            expandedIds = if (r.entry.id in expandedIds) expandedIds - r.entry.id else expandedIds + r.entry.id
                                        },
                                        onLongPress = { bounds ->
                                            contextTarget = TimelineContextTarget(r.entry, bounds, r.entry.id in expandedIds)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Lớp phủ context-menu (nhấn giữ) — bọc NGOÀI scaffold để nền tối phủ trọn màn, kể cả bottom sheet.
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
                lifted = { NodeCard(entry = tgt.entry, expanded = tgt.expanded) }
            )
        }
    }
}

// ─────────────────────────────── Thanh đầu trang ───────────────────────────────

@Composable
private fun TimelineTopBar(
    onBack: () -> Unit,
    collapsed: Boolean,
    detail: CallNumberDetail?,
    showToggle: Boolean,
    allExpanded: Boolean,
    onToggleAll: () -> Unit
) {
    val s = appStrings()
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, s.common.back, TextPrimary, onBack)
        Spacer(Modifier.width(12.dp))
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
                            s.timeline.title,
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

/** Thẻ nhận diện số ở đầu danh sách — giống màn Xem tất cả. Nhấn ĐÚP để sao chép số. */
@Composable
private fun TimelineHeaderRow(detail: CallNumberDetail, onDoubleTapCopy: () -> Unit) {
    val isNamed = !detail.cachedName.isNullOrBlank()
    PanelCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(detail.number) { detectTapGestures(onDoubleTap = { onDoubleTapCopy() }) }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(label = detail.nameOrNumber, photoUri = detail.photoUri, isNamed = isNamed, size = 58.dp, specialIconRes = SpecialNumbers.of(detail.number)?.iconRes)
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

/** Chú thích nhỏ dưới thanh lọc: bên TRÁI = gọi đi, bên PHẢI = gọi đến/nhỡ — để người dùng đọc ngay bố cục. */
@Composable
private fun TimelineLegend() {
    val t = appStrings().timeline
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("◀  ${t.dirOutgoingSide}", style = MaterialTheme.typography.labelMedium, color = TextSecondary, maxLines = 1)
        Spacer(Modifier.weight(1f))
        Text("${t.dirIncomingSide}  ▶", style = MaterialTheme.typography.labelMedium, color = TextSecondary, maxLines = 1)
    }
}

// ─────────────────────────────── Mốc & khoảng cách ───────────────────────────────

/** Giữ [LayoutCoordinates] của thẻ ngoài snapshot state — đọc lúc nhấn giữ để lấy khung mốc hiện tại. */
private class NodeCoords(var value: LayoutCoordinates? = null)

/**
 * Một MỐC cuộc gọi trên trục: đường trục chạy dọc (nối liền các mốc), một chấm ở GIỮA, và thẻ [NodeCard] nằm
 * BÊN TRÁI (gọi đi) hoặc BÊN PHẢI (gọi đến/nhỡ). Chấm & thẻ canh giữa THEO CHIỀU DỌC nên luôn thẳng hàng dù
 * thẻ có bung chi tiết hay đổi cỡ chữ.
 */
@Composable
private fun TimelineNode(
    entry: CallEntry,
    onLeft: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    expanded: Boolean,
    activeInMenu: Boolean,
    onToggle: () -> Unit,
    onLongPress: (Rect) -> Unit
) {
    val connected = CallResults.isConnected(entry.type, entry.durationSeconds)
    val dotColor = if (connected) AccentGreen else AccentRed
    val spine = SpineColor
    val appBg = AppBackground
    val density = LocalDensity.current
    val strokePx = with(density) { SPINE_W.toPx() }
    val connPx = with(density) { CONNECTOR_W.toPx() }
    val ringPx = with(density) { DOT_RING_W.toPx() }
    val dotCy = with(density) { (NODE_V_GAP + DOT_CENTER_FROM_TOP).toPx() }
    val moatR = with(density) { DOT_OUTER.toPx() / 2f }
    val dotR = with(density) { DOT.toPx() / 2f }
    val railHalfPx = with(density) { RAIL_W.toPx() / 2f }
    val cardCoords = remember { NodeCoords() }

    // Mọi phần TRỤC (đường dọc, cuống, quầng, chấm) vẽ Ở ĐÂY theo px CHÍNH XÁC → không phụ thuộc canh dọc của
    // thẻ, nên chấm luôn ghim đúng tâm phần thu gọn dù thẻ có bung/đổi cỡ chữ.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val cx = size.width / 2f
                // 1) Đường trục dọc — dừng ở chấm với mốc ĐẦU/CUỐI; mốc DUY NHẤT thì bỏ hẳn (chỉ còn chấm).
                if (!(isFirst && isLast)) {
                    val top = if (isFirst) dotCy else 0f
                    val bottom = if (isLast) dotCy else size.height
                    drawLine(color = spine, start = Offset(cx, top), end = Offset(cx, bottom), strokeWidth = strokePx)
                }
                // 2) Cuống ngắn nối chấm → thẻ (về phía thẻ đang nằm), tô nhạt theo kết quả.
                val connColor = dotColor.copy(alpha = 0.55f)
                if (onLeft) drawLine(connColor, Offset(cx - moatR, dotCy), Offset(cx - railHalfPx, dotCy), connPx)
                else drawLine(connColor, Offset(cx + moatR, dotCy), Offset(cx + railHalfPx, dotCy), connPx)
                // 3) Quầng nền cắt gọn đường trục ngay tại mốc.
                drawCircle(color = appBg, radius = moatR, center = Offset(cx, dotCy))
                // 4) Chấm: ĐẶC khi đã kết nối, VIỀN rỗng khi không kết nối — mã hoá kép (màu + hình) cho dễ phân biệt.
                if (connected) {
                    drawCircle(color = dotColor, radius = dotR, center = Offset(cx, dotCy))
                } else {
                    drawCircle(color = appBg, radius = dotR, center = Offset(cx, dotCy))
                    drawCircle(color = dotColor, radius = dotR - ringPx / 2f, center = Offset(cx, dotCy), style = Stroke(width = ringPx))
                }
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = NODE_V_GAP),
            verticalAlignment = Alignment.Top
        ) {
            // Nửa TRÁI — thẻ (nếu gọi đi) ôm sát trục.
            Box(modifier = Modifier.weight(1f).padding(start = 8.dp), contentAlignment = Alignment.TopEnd) {
                if (onLeft) NodeCardSlot(entry, expanded, activeInMenu, cardCoords, onToggle, onLongPress)
            }
            // Làn giữa — chừa chỗ cho chấm/cuống (chấm được vẽ ở drawBehind, làn chỉ giữ khoảng trống).
            Spacer(Modifier.width(RAIL_W))
            // Nửa PHẢI — thẻ (nếu gọi đến/nhỡ) ôm sát trục.
            Box(modifier = Modifier.weight(1f).padding(end = 8.dp), contentAlignment = Alignment.TopStart) {
                if (!onLeft) NodeCardSlot(entry, expanded, activeInMenu, cardCoords, onToggle, onLongPress)
            }
        }
    }
}

/** Bọc [NodeCard] với việc bắt toạ độ (cho context-menu) và ẩn bản gốc khi item đang được "nhấc". */
@Composable
private fun NodeCardSlot(
    entry: CallEntry,
    expanded: Boolean,
    activeInMenu: Boolean,
    cardCoords: NodeCoords,
    onToggle: () -> Unit,
    onLongPress: (Rect) -> Unit
) {
    Box(
        modifier = Modifier
            .onGloballyPositioned { cardCoords.value = it }
            .graphicsLayer { alpha = if (activeInMenu) 0f else 1f }
    ) {
        NodeCard(
            entry = entry,
            expanded = expanded,
            onClick = onToggle,
            onLongClick = {
                val bounds = cardCoords.value?.takeIf { it.isAttached }?.boundsInWindow()
                if (bounds != null) onLongPress(bounds)
            }
        )
    }
}

/**
 * Thẻ một mốc — GỌN, KHÔNG lặp lại số điện thoại (mọi mốc cùng một số). Line 1: icon hướng + giờ (đậm, tô theo
 * kết quả) + ngày (nếu không phải hôm nay). Line 2: tag hướng + trạng thái/thời lượng (tô nhạt). Bung: ngày–giờ
 * đầy đủ, câu kết nối, SIM/nhà mạng/khu vực, tag VoLTE/Thường. [onClick]/[onLongClick] null = bản tĩnh (dựng lại
 * y hệt để "nhấc" trong context-menu).
 */
@Composable
private fun NodeCard(
    entry: CallEntry,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val connected = CallResults.isConnected(entry.type, entry.durationSeconds)
    val resultColor = if (connected) AccentGreen else AccentRed
    val softColor = if (connected) SoftGreen else SoftRed
    val onLeft = isOutgoing(entry.type)   // thẻ gọi đi ở bên trái → dải nhấn ở MÉP PHẢI (phía trục), và ngược lại

    PanelCard(modifier = modifier.fillMaxWidth(), radius = 16.dp) {
        val interaction = remember { MutableInteractionSource() }
        val rowModifier = if (onClick != null) {
            Modifier.fillMaxWidth().combinedClickable(
                interactionSource = interaction,
                indication = rememberPressHighlight(),
                onClick = onClick,
                onLongClick = onLongClick
            )
        } else {
            Modifier.fillMaxWidth()
        }
        // Dải nhấn vẽ bằng drawBehind (tự cao theo chiều cao THẬT của thẻ, kể cả khi bung) — tránh bẫy
        // fillMaxHeight trong Row ở cây cuộn dọc (sẽ ra 0dp). onLeft (gọi đi, thẻ bên trái) → dải ở MÉP PHẢI.
        val stripPx = with(LocalDensity.current) { 3.dp.toPx() }
        Row(
            modifier = rowModifier.drawBehind {
                val x = if (onLeft) size.width - stripPx else 0f
                drawRect(color = resultColor, topLeft = Offset(x, 0f), size = Size(stripPx, size.height))
            }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 10.dp)) {
            // Line 1: icon hướng · giờ (đậm, tô theo kết quả) · ngày (nếu không phải hôm nay).
            Row(verticalAlignment = Alignment.CenterVertically) {
                CallTypeIcon(type = entry.type, size = 16.dp, isVideo = entry.isVideo, tint = resultColor)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = TimeFormat.time(entry.dateMillis),
                    style = MaterialTheme.typography.bodyLarge,
                    color = resultColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                dateSuffix(entry.dateMillis)?.let { day ->
                    Spacer(Modifier.width(6.dp))
                    Text(day, style = MaterialTheme.typography.labelMedium, color = TextSecondary, maxLines = 1)
                }
            }
            Spacer(Modifier.height(4.dp))
            // Line 2: tag hướng · trạng thái/thời lượng (tô nhạt).
            Row(verticalAlignment = Alignment.CenterVertically) {
                DirectionTag(entry.type, connected)
                Spacer(Modifier.width(7.dp))
                Text(
                    text = if (connected) TimeFormat.durationLabel(entry.durationSeconds).ifBlank { CallResults.shortStatus(entry.type, entry.durationSeconds) }
                    else CallResults.shortStatus(entry.type, entry.durationSeconds),
                    style = MaterialTheme.typography.bodySmall,
                    color = softColor,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 9.dp)) {
                    Text(
                        text = TimeFormat.fullDateTimeWithSeconds(entry.dateMillis),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = CallResults.connectionLine(entry.type, entry.durationSeconds),
                        style = MaterialTheme.typography.bodySmall,
                        color = softColor,
                        fontWeight = FontWeight.Medium
                    )
                    val meta = detailMetaLine(entry)
                    if (meta.isNotEmpty()) {
                        Spacer(Modifier.height(5.dp))
                        Text(meta, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Spacer(Modifier.height(7.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        entry.displaySimLabel?.let {
                            SimBadge(it)
                            Spacer(Modifier.width(6.dp))
                        }
                        if (entry.isVolte) {
                            InfoTag("HD Call VoLTE", AccentBlue, AccentBlueBg)
                        } else {
                            InfoTag(appStrings().callDetail.callNormal, TextSecondary, AccentGrayBg)
                        }
                    }
                }
            }
            }
        }
    }
}

/** Nhà mạng · khu vực cho phần bung — bỏ phần trống, nối bằng dấu chấm giữa. */
private fun detailMetaLine(entry: CallEntry): String =
    listOfNotNull(entry.carrier, entry.region.takeIf { it.isNotBlank() }).joinToString("  ·  ")

/** Ngày kèm ở line 1 khi cuộc gọi KHÔNG phải hôm nay (rỗng → null để bỏ hẳn). */
private fun dateSuffix(millis: Long): String? {
    val today = TimeFormat.date(System.currentTimeMillis())
    val that = TimeFormat.date(millis)
    return if (that == today) null else that
}

/**
 * Tag hướng gọi (Đi/Đến/Nhỡ…) — nhãn theo HƯỚNG, MÀU theo KẾT QUẢ (kết nối = xanh, không phản hồi = đỏ),
 * đồng bộ với giờ ở line 1.
 */
@Composable
private fun DirectionTag(type: CallType, connected: Boolean) {
    val cd = appStrings().callDetail
    val label = when (type) {
        CallType.OUTGOING -> cd.dirOutgoing
        CallType.INCOMING, CallType.ANSWERED_EXTERNALLY -> cd.dirIncoming
        CallType.MISSED, CallType.REJECTED, CallType.BLOCKED -> cd.dirMissed
        CallType.VOICEMAIL -> cd.dirVoicemail
        CallType.UNKNOWN -> cd.dirOther
    }
    Text(
        text = label,
        color = if (connected) AccentGreen else AccentRed,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(if (connected) AccentGreenBg else AccentRedBg)
            .padding(horizontal = 6.dp, vertical = 1.dp)
    )
}

/** Tag nhỏ (pill) cho thông tin phụ như "HD Call VoLTE" / "Thường". */
@Composable
private fun InfoTag(text: String, textColor: Color, bgColor: Color) {
    Text(
        text = text,
        color = textColor,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(bgColor)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    )
}

/**
 * Hàng KHOẢNG CÁCH giữa hai mốc: đường trục chạy xuyên qua, ở giữa là một "chip" hiển thị TỔNG thời gian cách
 * nhau (đồng hồ cát + [TimeFormat.gapLabel]). Nền chip đục cắt đứt đường trục cho gọn, còn chừa đường trên/dưới.
 * Cách càng LÂU thì hàng càng CAO (khoảng cách "nhìn thấy" được); từ 1 NGÀY trở lên trục vẽ NÉT ĐỨT và chip
 * chuyển tông HỔ PHÁCH để nổi bật quãng nghỉ dài.
 */
@Composable
private fun TimelineGap(deltaMillis: Long) {
    val tier = gapTier(deltaMillis)
    val spine = SpineColor
    val density = LocalDensity.current
    val strokePx = with(density) { SPINE_W.toPx() }
    val dashPx = with(density) { 5.dp.toPx() }
    // Nét đứt CHỈ cho quãng ≥ 1 ngày; gói gọn trong 1 hàng nên không lệch pha ở mép item.
    val dash = remember(tier, dashPx) {
        if (tier == GapTier.L) PathEffect.dashPathEffect(floatArrayOf(dashPx, dashPx)) else null
    }
    val vPad = when (tier) {
        GapTier.S -> 7.dp
        GapTier.M -> 12.dp
        GapTier.L -> 18.dp
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val cx = size.width / 2f
                drawLine(color = spine, start = Offset(cx, 0f), end = Offset(cx, size.height), strokeWidth = strokePx, pathEffect = dash)
            },
        contentAlignment = Alignment.Center
    ) {
        GapChip(deltaMillis, tier, Modifier.padding(vertical = vPad))
    }
}

@Composable
private fun GapChip(deltaMillis: Long, tier: GapTier, modifier: Modifier = Modifier) {
    val amber = tier == GapTier.L
    val bg = if (amber) AccentAmberBg else CardFill
    val fg = if (amber) AccentAmber else TextSecondary
    val iconTint = if (amber) AccentAmber else AccentGray
    val label = TimeFormat.gapLabel(deltaMillis)
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.HourglassEmpty,
            contentDescription = appStrings().timeline.apart(label),   // đọc màn hình: "Cách nhau …"
            tint = iconTint,
            modifier = Modifier.size(13.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

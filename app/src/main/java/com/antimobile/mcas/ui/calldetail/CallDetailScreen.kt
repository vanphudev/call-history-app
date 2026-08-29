package com.antimobile.mcas.ui.calldetail

import android.provider.ContactsContract.CommonDataKinds.Phone
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.antimobile.mcas.R
import com.antimobile.mcas.data.model.CallEntry
import com.antimobile.mcas.data.model.CallNumberDetail
import com.antimobile.mcas.i18n.appStrings
import com.antimobile.mcas.ui.components.Avatar
import com.antimobile.mcas.ui.components.AppToastType
import com.antimobile.mcas.ui.components.EmptyState
import com.antimobile.mcas.ui.components.LoadingState
import com.antimobile.mcas.ui.components.PanelCard
import com.antimobile.mcas.ui.components.QrActionSheet
import com.antimobile.mcas.ui.components.STATS_HANDLE_HEIGHT
import com.antimobile.mcas.ui.components.SimBadge
import com.antimobile.mcas.ui.components.SimScopeControl
import com.antimobile.mcas.ui.components.StatsSheetContent
import com.antimobile.mcas.ui.components.StatsSheetHandle
import com.antimobile.mcas.ui.components.collapsingHeaderSwap
import com.antimobile.mcas.ui.theme.AccentBlue
import com.antimobile.mcas.ui.theme.AccentBlueBg
import com.antimobile.mcas.ui.theme.AccentGreen
import com.antimobile.mcas.ui.theme.AccentGreenBg
import com.antimobile.mcas.ui.theme.AppBackground
import com.antimobile.mcas.ui.theme.BrandSoft
import com.antimobile.mcas.ui.theme.CardFill
import com.antimobile.mcas.ui.theme.LinkColor
import com.antimobile.mcas.ui.theme.Primary
import com.antimobile.mcas.ui.theme.TextPrimary
import com.antimobile.mcas.ui.theme.TextSecondary
import com.antimobile.mcas.data.local.AddMemberResult
import com.antimobile.mcas.data.local.CategoryCatalog
import com.antimobile.mcas.data.local.CategoryRepository
import com.antimobile.mcas.ui.category.AddToCategorySheet
import com.antimobile.mcas.ui.category.AvatarCategoryBadges
import com.antimobile.mcas.ui.category.CategoryIconDisc
import com.antimobile.mcas.ui.category.categoryLabel
import com.antimobile.mcas.util.CallActions
import com.antimobile.mcas.util.ContactActions
import com.antimobile.mcas.util.PhoneKey
import com.antimobile.mcas.util.MessageTemplate
import com.antimobile.mcas.util.PhoneActions
import com.antimobile.mcas.util.QrContent
import com.antimobile.mcas.util.SimInfo
import com.antimobile.mcas.util.SimScope
import com.antimobile.mcas.util.SpecialNumbers
import com.antimobile.mcas.util.TemplateContext
import com.antimobile.mcas.util.TemplateFill
import com.antimobile.mcas.util.formatPhone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val TOP_BAR_HEIGHT = 56.dp

/** Chiều cao ĐỒNG NHẤT cho mọi item trong các card ở màn chi tiết (khớp item lịch sử 64dp). */
private val ITEM_HEIGHT = 64.dp

/** Đích của màn soạn mẫu tin nhắn: tạo mới hoặc sửa một mẫu có sẵn. */
private sealed interface TemplateEditorTarget {
    data object Create : TemplateEditorTarget
    data class Edit(val template: MessageTemplate) : TemplateEditorTarget
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallDetailScreen(
    vm: CallDetailViewModel,
    onBack: () -> Unit,
    onSeeAll: () -> Unit,
    onCostStats: () -> Unit,
    onOpenStats: () -> Unit,
    onCreateCategory: () -> Unit
) {
    val context = LocalContext.current
    val detail = vm.detail
    val categoryRepo = remember { CategoryRepository(context) }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    // Thu gọn theo NGƯỠNG (Boolean) thay vì crossfade alpha liên tục — chuyển HẲN
    // sang trạng thái thu gọn/mở, không kẹt ở lưng chừng nửa mờ.
    val collapseThresholdPx = with(density) { 64.dp.toPx() }
    val collapsed by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 ||
                listState.firstVisibleItemScrollOffset > collapseThresholdPx
        }
    }
    var historyExpanded by remember(detail?.number) { mutableStateOf(emptySet<Long>()) }
    var showShareSheet by remember(detail?.number) { mutableStateOf(false) }
    var showTemplateSheet by remember(detail?.number) { mutableStateOf(false) }
    // Mẫu đang chờ quét QR: != null → mở màn quét; quét xong điền {contextqr} rồi gửi.
    var pendingScan by remember(detail?.number) { mutableStateOf<MessageTemplate?>(null) }
    // Mã QR CÓ HÀNH ĐỘNG (link/tel/sms/email) vừa quét: != null → mở sheet xử lý thay vì chèn vào mẫu.
    var qrAction by remember(detail?.number) { mutableStateOf<QrContent?>(null) }
    // Màn soạn mẫu (tạo/sửa): != null → phủ toàn màn; xong → mở lại danh sách mẫu.
    var editorTarget by remember(detail?.number) { mutableStateOf<TemplateEditorTarget?>(null) }
    // Sheet "thêm số này vào nhóm phân loại".
    var showAddCategory by remember(detail?.number) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val hasDetail = detail != null && detail.entries.isNotEmpty()

    // Điền pattern trong nội dung mẫu rồi mở app nhắn tin với người nhận + nội dung soạn sẵn.
    fun sendTemplate(template: MessageTemplate, qrText: String) {
        val number = detail?.number ?: return
        scope.launch {
            val body = withContext(Dispatchers.IO) {
                val my = SimInfo.myNumbers(context)
                TemplateFill.fill(
                    template.content,
                    TemplateContext(my.bySlot, qrText, System.currentTimeMillis())
                )
            }
            CallActions.messageWithBody(context, number, body)
        }
    }

    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded)
    )
    val peek = if (hasDetail) STATS_HANDLE_HEIGHT + navBottom else 0.dp
    // Nội dung thống kê MỜ DẦN: thu gọn = ẩn (alpha 0) → chỉ thấy handle; mở rộng = hiện (alpha 1). Chuyển mượt.
    val statsExpanded = scaffoldState.bottomSheetState.targetValue == SheetValue.Expanded
    val statsAlpha by animateFloatAsState(targetValue = if (statsExpanded) 1f else 0f, label = "statsAlpha")

    // Bọc ngoài để màn soạn mẫu phủ FULL-BLEED (tự xử lý status/nav/ime inset), che cả BottomSheetScaffold.
    Box(modifier = Modifier.fillMaxSize()) {
    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = peek,
        sheetContainerColor = AppBackground,
        sheetContentColor = TextPrimary,
        sheetShadowElevation = 12.dp,
        sheetTonalElevation = 0.dp,
        sheetDragHandle = { if (hasDetail) StatsSheetHandle(detail!!, onOpenStats = onOpenStats) },
        sheetContent = { if (hasDetail) StatsSheetContent(detail!!, navBottom, Modifier.graphicsLayer { alpha = statsAlpha }) },
        containerColor = AppBackground
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .statusBarsPadding()
        ) {
            when {
                vm.loading && detail == null ->
                    Box(Modifier.fillMaxSize().padding(top = TOP_BAR_HEIGHT)) { LoadingState(text = appStrings().callDetail.loading) }

                // Số KHÔNG hợp lệ / không có gì để hiện (số rỗng) → trạng thái trống trọn màn.
                detail == null ->
                    Box(Modifier.fillMaxSize().padding(top = TOP_BAR_HEIGHT)) { EmptyState(appStrings().callDetail.emptyNoCalls) }

                // Có DANH TÍNH số (detail != null) → LUÔN hiện đầu trang + thẻ hành động (gọi/nhắn/chia sẻ/lưu),
                // kể cả khi 0 cuộc: số mở từ danh bạ hoặc đang lọc theo phạm vi SIM vẫn thao tác được. Phần LỊCH SỬ
                // rỗng thì thay bằng thông báo (nêu rõ đang lọc SIM để không tưởng là "mất" cuộc gọi).
                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = TOP_BAR_HEIGHT, bottom = peek + 16.dp)
                ) {
                    item { DetailHeader(detail, onDoubleTapCopy = { CallActions.copy(context, detail.number) }) }
                    item { Spacer(Modifier.height(16.dp)) }
                    item { ActionCard(detail, context, onTemplates = { showTemplateSheet = true }) }
                    item { Spacer(Modifier.height(22.dp)) }
                    if (detail.entries.isEmpty()) {
                        item {
                            val scopeLabel = SimScope.effectiveLabel
                            EmptyState(
                                if (scopeLabel != null) appStrings().callDetail.emptyNoCallsInScope(scopeLabel)
                                else appStrings().callDetail.emptyNoCalls
                            )
                        }
                    } else {
                        item {
                            HistorySection(
                                entries = detail.entries,
                                expanded = historyExpanded,
                                onToggle = { id ->
                                    historyExpanded = if (id in historyExpanded) historyExpanded - id else historyExpanded + id
                                },
                                onSeeAll = onSeeAll
                            )
                        }
                        item { Spacer(Modifier.height(20.dp)) }
                        item { FeatureCard(onShare = { showShareSheet = true }) }
                    }
                }
            }

            CollapsingTopBar(
                collapsed = collapsed,
                detail = detail,
                onBack = onBack,
                onCostStats = { if (hasDetail) onCostStats() },
                onCopy = { detail?.let { CallActions.copy(context, it.number) } }
            )

            if (showShareSheet && detail != null) {
                ShareContactSheet(detail = detail, onDismiss = { showShareSheet = false })
            }

            if (showTemplateSheet && detail != null) {
                MessageTemplateSheet(
                    onDismiss = { showTemplateSheet = false },
                    onSend = { template -> sendTemplate(template, "") },
                    onScan = { template -> pendingScan = template },
                    onCreate = { editorTarget = TemplateEditorTarget.Create },
                    onEdit = { template -> editorTarget = TemplateEditorTarget.Edit(template) }
                )
            }

        }
    }

        // Thanh NHÓM PHÂN LOẠI — GHIM ngay trên mép trên của BottomSheetScaffold và DI CHUYỂN theo sheet khi
        // kéo lên/xuống (requireOffset đọc ở draw-phase). Hiện icon các nhóm số đang thuộc + nút "+" thêm số
        // vào nhóm + nút "+" tạo nhóm mới. Ẩn khi có lớp phủ khác (soạn mẫu / quét QR / sheet QR).
        if (hasDetail && detail != null && editorTarget == null && pendingScan == null && qrAction == null) {
            CategorySheetBar(
                number = detail.number,
                scaffoldState = scaffoldState,
                onAdd = { showAddCategory = true },
                onCreate = onCreateCategory
            )
        }

        // Màn TẠO/SỬA mẫu — phủ full-bleed lên trên; xong (lưu hoặc quay lại) mở lại danh sách mẫu.
        if (editorTarget != null && detail != null) {
            TemplateEditorScreen(
                initial = (editorTarget as? TemplateEditorTarget.Edit)?.template,
                onExit = {
                    editorTarget = null
                    showTemplateSheet = true
                }
            )
        }

        // Màn quét QR — phủ full-bleed trên activity window (để tô thanh status/nav ĐEN). Quét/đọc xong →
        // điền {contextqr} rồi gửi.
        if (pendingScan != null && detail != null) {
            QrScannerOverlay(
                onResult = { qr ->
                    val content = QrContent.parse(qr)
                    val template = pendingScan
                    pendingScan = null
                    if (content.isActionable) {
                        // Link/tel/sms/email → KHÔNG chèn vào mẫu, mở sheet xử lý theo loại.
                        qrAction = content
                    } else {
                        // Văn bản thuần → điền {contextqr} rồi gửi như cũ.
                        template?.let { sendTemplate(it, content.raw) }
                    }
                },
                onDismiss = { pendingScan = null }
            )
        }

        // Sheet XỬ LÝ mã QR có hành động (link/tel/sms/email) — hiện nội dung + nút mở tương ứng + sao chép.
        qrAction?.let { content ->
            QrActionSheet(content = content, onDismiss = { qrAction = null })
        }

        // Sheet "Thêm vào nhóm" — bật/tắt số hiện tại khỏi các nhóm; hoặc tạo nhóm mới.
        if (showAddCategory && detail != null) {
            val number = detail.number
            AddToCategorySheet(
                number = number,
                onDismiss = { showAddCategory = false },
                onToggle = { cat, desired ->
                    scope.launch {
                        val label = categoryLabel(cat)
                        if (desired) {
                            when (categoryRepo.addMember(cat.id, number)) {
                                AddMemberResult.ADDED -> CallActions.toast(context, appStrings().category.addedTo(label), AppToastType.Success)
                                AddMemberResult.FULL -> CallActions.toast(context, appStrings().category.maxMembers, AppToastType.Warning)
                                AddMemberResult.ALREADY -> CallActions.toast(context, appStrings().category.alreadyAdded)
                                AddMemberResult.INVALID -> CallActions.toast(context, appStrings().category.invalidNumber, AppToastType.Error)
                            }
                        } else {
                            categoryRepo.removeMember(cat.id, PhoneKey.of(number))
                            CallActions.toast(context, appStrings().category.removedFrom(label), AppToastType.Success)
                        }
                    }
                },
                onCreateNew = { showAddCategory = false; onCreateCategory() }
            )
        }
    }
}

/**
 * Thanh nhóm phân loại GHIM trên đầu BottomSheetScaffold: tracking mép trên của sheet mỗi khung hình qua
 * [BottomSheetState.requireOffset] (đọc trong graphicsLayer = draw-phase, mượt). Hiện icon các nhóm mà số
 * đang thuộc + nút "+" thêm vào nhóm + nút "+" tạo nhóm mới.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoxScope.CategorySheetBar(
    number: String,
    scaffoldState: BottomSheetScaffoldState,
    onAdd: () -> Unit,
    onCreate: () -> Unit,
) {
    var rowHeightPx by remember { mutableStateOf(0f) }
    val badges = CategoryCatalog.badgesFor(number)
    Row(
        modifier = Modifier
            .align(Alignment.TopStart)
            .fillMaxWidth()
            .onSizeChanged { rowHeightPx = it.height.toFloat() }
            .graphicsLayer {
                val off = try {
                    scaffoldState.bottomSheetState.requireOffset()
                } catch (e: IllegalStateException) {
                    return@graphicsLayer
                }
                translationY = off - rowHeightPx
            }
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        badges.take(4).forEach { b ->
            CategoryIconDisc(b.iconKey, b.colorArgb, size = 34.dp)
        }
        Spacer(Modifier.weight(1f))
        SheetBarButton(Icons.Rounded.Add, appStrings().category.addToCategory, BrandSoft, Primary, onAdd)
        SheetBarButton(Icons.Rounded.CreateNewFolder, appStrings().category.newCategory, CardFill, TextSecondary, onCreate)
    }
}

@Composable
private fun SheetBarButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String,
    bg: androidx.compose.ui.graphics.Color,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = desc, tint = tint, modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun CollapsingTopBar(
    collapsed: Boolean,
    detail: CallNumberDetail?,
    onBack: () -> Unit,
    onCostStats: () -> Unit,
    onCopy: () -> Unit
) {
    val s = appStrings()
    Row(
        // Thanh vẽ ĐÈ lên LazyColumn → tự NUỐT chạm ở vùng trống để cú chạm KHÔNG lọt xuống item đang
        // cuộn phía dưới thanh (nếu không, chạm lên thanh sẽ kích hoạt onClick của item bên dưới).
        modifier = Modifier.fillMaxWidth().height(TOP_BAR_HEIGHT).background(AppBackground)
            .pointerInput(Unit) { detectTapGestures {} }
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, s.common.back, TextPrimary, onBack)
        Spacer(Modifier.width(12.dp))
        // Đổi giữa "chỉ số điện thoại" ↔ "avatar + tên + số·khu vực" bằng cú TRƯỢT DỌC đục
        // (không crossfade mờ chồng). Kích hoạt theo ngưỡng nên chuyển hẳn, có animation uyển chuyển.
        AnimatedContent(
            targetState = collapsed,
            transitionSpec = { collapsingHeaderSwap() },
            label = "detail-topbar",
            modifier = Modifier.weight(1f).fillMaxHeight()
        ) { isCollapsed ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                if (detail != null) {
                    if (isCollapsed) {
                        // Đã cuộn: avatar + TÊN (đậm) + số · khu vực.
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val isNamed = !detail.cachedName.isNullOrBlank()
                            Avatar(label = detail.nameOrNumber, photoUri = detail.photoUri, isNamed = isNamed, size = 38.dp, specialIconRes = SpecialNumbers.of(detail.number)?.iconRes)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = detail.displayName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${formatPhone(detail.number)} · ${detail.region}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        // Mặc định (chưa cuộn): chỉ hiện SỐ ĐIỆN THOẠI.
                        Text(
                            text = formatPhone(detail.number),
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        RoundIconButton(Icons.Rounded.Payments, s.callDetail.costStats, TextSecondary, onCostStats)
        RoundIconButton(Icons.Rounded.ContentCopy, s.callDetail.copyNumber, TextSecondary, onCopy)
    }
}

@Composable
private fun RoundIconButton(icon: ImageVector, desc: String, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = desc, tint = tint, modifier = Modifier.size(23.dp))
    }
}

/** Đầu trang chi tiết: avatar + tên + số + chip. Nhấn ĐÚP để sao chép số. */
@Composable
private fun DetailHeader(detail: CallNumberDetail, onDoubleTapCopy: () -> Unit) {
    val isNamed = !detail.cachedName.isNullOrBlank()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(detail.number) { detectTapGestures(onDoubleTap = { onDoubleTapCopy() }) }
            .padding(start = 20.dp, end = 20.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(74.dp)) {
            Avatar(label = detail.nameOrNumber, photoUri = detail.photoUri, isNamed = isNamed, size = 74.dp, specialIconRes = SpecialNumbers.of(detail.number)?.iconRes)
            AvatarCategoryBadges(detail.number, modifier = Modifier.align(Alignment.BottomEnd), badgeSize = 19.dp)
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = detail.displayName,
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(3.dp))
            Text(formatPhone(detail.number), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(Modifier.height(9.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isNamed) {
                    val e = detail.entries.firstOrNull()
                    PillChip(if (e != null) numberTypeLabel(e.numberType, e.numberTypeCustom) else appStrings().callDetail.numberTypeMobile)
                }
                detail.carrier?.let { PillChip(it) }
                PillChip(detail.region)
                detail.displaySimLabels.firstOrNull()?.let { SimBadge(it) }
            }
        }
    }
}

@Composable
private fun PillChip(text: String) {
    Box(modifier = Modifier.clip(CircleShape).background(CardFill).padding(horizontal = 12.dp, vertical = 5.dp)) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    }
}

/**
 * Nhãn LOẠI SỐ hiển thị theo ngôn ngữ hiện hành — resolve tại render (KHÔNG lưu ở data layer để đổi ngôn
 * ngữ là đổi ngay). [type] = CACHED_NUMBER_TYPE ([Phone.TYPE_*]); [custom] = nhãn tự đặt khi TYPE_CUSTOM.
 */
private fun numberTypeLabel(type: Int, custom: String?): String = with(appStrings().callDetail) {
    when (type) {
        Phone.TYPE_HOME -> numberTypeHome
        Phone.TYPE_MOBILE -> numberTypeMobile
        Phone.TYPE_WORK -> numberTypeWork
        Phone.TYPE_MAIN -> numberTypeMain
        Phone.TYPE_OTHER -> numberTypeOther
        Phone.TYPE_CUSTOM -> custom?.takeIf { it.isNotBlank() } ?: numberTypeGeneric
        else -> numberTypeMobile
    }
}

/**
 * Thẻ hành động (thay hàng icon cũ): số điện thoại + nút Gọi/Nhắn tin, rồi các hàng
 * Gửi tin nhắn theo mẫu, Thêm vào liên hệ, Tìm qua Zalo/Google — mỗi item là 1 dòng trong khung
 * card bo góc + đổ bóng.
 */
@Composable
private fun ActionCard(detail: CallNumberDetail, context: android.content.Context, onTemplates: () -> Unit) {
    val cd = appStrings().callDetail
    PanelCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), radius = 20.dp) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(ITEM_HEIGHT).padding(start = 18.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formatPhone(detail.number),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        text = detail.region,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(10.dp))
                CircleActionButton(Icons.Rounded.Phone, cd.call, AccentGreen, AccentGreenBg) { CallActions.dial(context, detail.number) }
                Spacer(Modifier.width(10.dp))
                CircleActionButton(Icons.AutoMirrored.Rounded.Message, cd.message, AccentBlue, AccentBlueBg) { CallActions.message(context, detail.number) }
            }
            // Nhãn chữ TỐI bên trái + nút TRÒN xanh dương (icon SMS mẫu, khác icon "Nhắn tin") để phân biệt.
            ActionCircleRow(cd.sendTemplate, Icons.Rounded.Sms, cd.sendTemplate, AccentBlue, AccentBlueBg, onClick = onTemplates)
            // Đã lưu danh bạ → "Xem trong danh bạ" (mở liên hệ); chưa lưu → "Thêm vào liên hệ" (mở màn tạo mới).
            // SỐ KHẨN CẤP (113/114/115) là danh tính CỐ ĐỊNH, không phải liên hệ cá nhân → ẨN hàng "Thêm vào liên hệ".
            if (detail.isSavedContact) {
                ActionLinkRow(R.drawable.ic_contacts, cd.viewInContacts, TextPrimary) {
                    detail.contactLookupUri?.let { ContactActions.viewContact(context, it) }
                }
            } else if (SpecialNumbers.of(detail.number) == null) {
                ActionLinkRow(R.drawable.ic_contacts, cd.addToContacts, TextPrimary) {
                    ContactActions.addToContacts(context, detail.number)
                }
            }
            ActionLinkRow(R.drawable.ic_zalo, cd.searchZalo, TextPrimary) { PhoneActions.openZalo(context, detail.number) }
            ActionLinkRow(R.drawable.ic_google, cd.searchGoogle, TextPrimary) { PhoneActions.searchNumberOnWeb(context, detail.number) }
        }
    }
}

/** Nút tròn nhỏ tô nền nhạt + icon màu, đặt cuối dòng trong thẻ hành động. */
@Composable
private fun CircleActionButton(icon: ImageVector, desc: String, tint: Color, bg: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(44.dp).clip(CircleShape).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = desc, tint = tint, modifier = Modifier.size(22.dp))
    }
}

/** Dòng: nhãn chữ TỐI bên trái + nút TRÒN tô nền màu (icon bên phải); CẢ dòng bấm được (nút chỉ để trang trí). */
@Composable
private fun ActionCircleRow(label: String, icon: ImageVector, desc: String, tint: Color, bg: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(ITEM_HEIGHT).clickable { onClick() }.padding(start = 18.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(bg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = desc, tint = tint, modifier = Modifier.size(22.dp))
        }
    }
}

/** Dòng liên kết: icon dẫn + nhãn tô màu (cả dòng bấm được). */
@Composable
private fun ActionLinkRow(icon: ImageVector, label: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(ITEM_HEIGHT).clickable { onClick() }.padding(start = 18.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = color, fontWeight = FontWeight.Medium)
    }
}

/** Biến thể dùng LOGO NHIỀU MÀU từ drawable (vd Zalo): vẽ NGUYÊN BẢN, KHÔNG tô tint; chỉ nhãn tô [color]. */
@Composable
private fun ActionLinkRow(@DrawableRes iconRes: Int, label: String, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(ITEM_HEIGHT).clickable { onClick() }.padding(start = 18.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(21.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = color, fontWeight = FontWeight.Medium)
    }
}

/**
 * Lịch sử cuộc gọi: tiêu đề (kèm thanh lọc SIM CÙNG DÒNG, bên phải) + các DÒNG cuộc gọi trong 1 card +
 * link "Hiển thị thêm".
 *
 * Thanh SIM theo cùng logic toàn app: đang khoá phạm vi 1 SIM ở Cài đặt → hiện "Đang xem SIM X" (chỉ đọc,
 * dữ liệu đã lọc sẵn); ngược lại & số này có ≥2 SIM → thanh LỌC nhanh để xem riêng cuộc của từng SIM. Lọc chỉ
 * ảnh hưởng DANH SÁCH hiển thị ở card này (giống thanh lọc ở màn danh sách).
 */
@Composable
private fun HistorySection(
    entries: List<CallEntry>,
    expanded: Set<Long>,
    onToggle: (Long) -> Unit,
    onSeeAll: () -> Unit
) {
    val globalSim = SimScope.effectiveLabel
    val simLabels = remember(entries) { entries.mapNotNull { it.simLabel }.distinct().sorted() }
    // rememberSaveable (LazyColumn giữ qua lúc item cuộn khỏi màn rồi quay lại) + KHOÁ theo số đang xem (id cuộc
    // mới nhất) → lọc SIM cục bộ KHÔNG bị reset khi cuộn xuống công cụ rồi cuộn lên; nhưng reset khi đổi sang số khác.
    var localSim by rememberSaveable(entries.firstOrNull()?.id ?: 0L) { mutableStateOf<String?>(null) }
    // Khoá phạm vi toàn app → bỏ lọc cục bộ (dữ liệu đã lọc sẵn tại gốc). Ngược lại lọc theo lựa chọn cục bộ
    // (chỉ giữ khi SIM còn trong danh sách). null = tất cả.
    val effectiveLocal = if (globalSim != null) null else localSim?.takeIf { it in simLabels }
    val shown = effectiveLocal?.let { l -> entries.filter { it.simLabel == l } } ?: entries

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                appStrings().callDetail.historyTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            SimScopeControl(
                availableLabels = simLabels,
                localSelection = effectiveLocal,   // đã chuẩn hoá → ô tô sáng luôn khớp danh sách đang hiện
                onLocalSelect = { localSim = it },
                allLabel = appStrings().callList.filterAll,
                compact = true
            )
        }
        PanelCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), radius = 20.dp) {
            Column(modifier = Modifier.fillMaxWidth()) {
                shown.take(6).forEach { entry ->
                    DetailCallItem(entry = entry, expanded = entry.id in expanded, onToggle = { onToggle(entry.id) })
                }
                Row(
                    modifier = Modifier.fillMaxWidth().height(ITEM_HEIGHT).clickable { onSeeAll() }.padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(appStrings().callDetail.showMore, style = MaterialTheme.typography.bodyMedium, color = LinkColor, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

/** Công cụ: "Chia sẻ liên hệ" — mở bottom sheet chọn CÁCH chia sẻ (văn bản / mã QR) qua ứng dụng. */
@Composable
private fun FeatureCard(onShare: () -> Unit) {
    val cd = appStrings().callDetail
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            cd.toolsTitle,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp)
        )
        PanelCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), radius = 20.dp) {
            Column(modifier = Modifier.fillMaxWidth()) {
                FeatureItem(Icons.Rounded.Share, Primary, cd.shareContact, cd.shareContactSubtitle, onShare)
            }
        }
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    tint: Color,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(ITEM_HEIGHT).clickable { onClick() }.padding(start = 18.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Spacer(Modifier.height(1.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

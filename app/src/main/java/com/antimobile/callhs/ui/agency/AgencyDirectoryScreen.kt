package com.antimobile.callhs.ui.agency

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antimobile.callhs.data.agency.Agency
import com.antimobile.callhs.data.agency.AgencyDataset
import com.antimobile.callhs.data.agency.AgencyOrigin
import com.antimobile.callhs.data.agency.Emergency
import com.antimobile.callhs.i18n.appStrings
import com.antimobile.callhs.ui.components.AppBottomSheet
import com.antimobile.callhs.ui.components.AppMessageDialog
import com.antimobile.callhs.ui.components.DialogButton
import com.antimobile.callhs.ui.components.PanelCard
import com.antimobile.callhs.ui.theme.AccentAmber
import com.antimobile.callhs.ui.theme.AccentAmberBg
import com.antimobile.callhs.ui.theme.AccentGreen
import com.antimobile.callhs.ui.theme.AccentGreenBg
import com.antimobile.callhs.ui.theme.AccentRed
import com.antimobile.callhs.ui.theme.AccentRedBg
import com.antimobile.callhs.ui.theme.AppBackground
import com.antimobile.callhs.ui.theme.CardFill
import com.antimobile.callhs.ui.theme.DividerColor
import com.antimobile.callhs.ui.theme.FieldSurface
import com.antimobile.callhs.ui.theme.LinkColor
import com.antimobile.callhs.ui.theme.Primary
import com.antimobile.callhs.ui.theme.TextPrimary
import com.antimobile.callhs.ui.theme.TextSecondary
import com.antimobile.callhs.util.AppLinks
import com.antimobile.callhs.util.CallActions
import com.antimobile.callhs.util.formatPhone

/**
 * Màn DANH BẠ CƠ QUAN của một [AgencyDataset] (tỉnh × ngành: hành chính công / công an).
 *
 * Bố cục: thanh trên (quay lại + tiêu đề tỉnh/ngành + nút LÀM MỚI + nút ⓘ) → THẺ CẢNH BÁO → ô TÌM
 * KIẾM (theo tên / phường-xã / địa chỉ) → danh sách cơ quan PHÂN TRANG 50 mục/trang. Mỗi mục: tên +
 * số điện thoại + nút GỌI; nhấn để BUNG địa chỉ, bản đồ, Facebook, các số khác, hoặc số khẩn cấp.
 *
 * Dữ liệu tải trực tiếp từ GitHub, lưu cache tại máy và làm mới tối đa 7 ngày/lần (xem
 * [com.antimobile.callhs.data.remote.RemoteFileCache]); nút LÀM MỚI buộc kiểm tra bản mới ngay.
 */
@Composable
fun AgencyDirectoryScreen(
    dataset: AgencyDataset,
    vm: AgencyDirectoryViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LaunchedEffect(dataset) { vm.load(dataset) }

    var showDisclaimer by remember { mutableStateOf(false) }

    // Lọc theo tên/phường/địa chỉ (bỏ dấu, không phân biệt hoa/thường) + phân trang — tính ở UI.
    val all = vm.data?.agencies ?: emptyList()
    val normQuery = remember(vm.query) { com.antimobile.callhs.data.agency.normalizeForSearch(vm.query) }
    val filtered = remember(all, normQuery) {
        if (normQuery.isEmpty()) all else all.filter { it.searchText.contains(normQuery) }
    }
    val pageCount = if (filtered.isEmpty()) 0 else (filtered.size + AGENCY_PAGE_SIZE - 1) / AGENCY_PAGE_SIZE
    val page = vm.page.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
    val pageItems = remember(filtered, page) {
        if (filtered.isEmpty()) emptyList()
        else filtered.subList(page * AGENCY_PAGE_SIZE, minOf((page + 1) * AGENCY_PAGE_SIZE, filtered.size))
    }

    val listState = rememberLazyListState()
    var firstScrollRun by remember { mutableStateOf(true) }
    LaunchedEffect(page, normQuery) {
        if (firstScrollRun) firstScrollRun = false else listState.scrollToItem(0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .statusBarsPadding()
            .imePadding()
    ) {
        DirectoryTopBar(
            title = dataset.province.displayName,
            subtitle = vm.data?.category ?: dataset.category,
            refreshing = vm.refreshing,
            onBack = onBack,
            onRefresh = { vm.refresh() },
            onInfo = { showDisclaimer = true }
        )

        when {
            (vm.loading || vm.refreshing) && vm.data == null -> CenterState(appStrings().agency.loading, Modifier.weight(1f))
            // Offline & chưa có dữ liệu → nền trung tính, để MODAL bên dưới dẫn dắt (không hiện ErrorState kép).
            vm.data == null && vm.noNetwork -> CenterState(appStrings().agency.needNetwork, Modifier.weight(1f))
            vm.loadError || vm.data == null -> ErrorState(onRetry = { vm.refresh() }, modifier = Modifier.weight(1f))
            else -> {
                WarningBanner(onClick = { showDisclaimer = true })
                SearchField(
                    query = vm.query,
                    onChange = vm::onQueryChange,
                    onClear = { vm.clearQuery() },
                    onSearch = { focus.clearFocus() }
                )
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = navBottom + 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (filtered.isEmpty()) {
                        item("empty") { EmptyResult(query = vm.query) }
                    } else {
                        item("count") {
                            ResultHeader(
                                category = vm.data?.category ?: dataset.category,
                                total = filtered.size,
                                page = page,
                                pageCount = pageCount,
                                origin = vm.origin,
                                updated = vm.data?.updated
                            )
                        }
                        items(pageItems, key = { dataset.key + ":" + it.id }) { agency ->
                            AgencyItemCard(
                                agency = agency,
                                emergency = vm.data?.emergency,
                                onDial = { CallActions.dial(context, it) },
                                onOpenUrl = { AppLinks.openUrl(context, it) }
                            )
                        }
                        if (pageCount > 1) {
                            item("pager") {
                                PaginationBar(
                                    page = page,
                                    pageCount = pageCount,
                                    onPrev = { vm.goToPage(page - 1, pageCount) },
                                    onNext = { vm.goToPage(page + 1, pageCount) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDisclaimer) {
        DisclaimerSheet(
            dataset = dataset,
            updated = vm.data?.updated,
            source = vm.data?.source,
            disclaimer = vm.data?.disclaimer,
            emergency = vm.data?.emergency,
            origin = vm.origin,
            onDismiss = { showDisclaimer = false }
        )
    }

    // Chưa có dữ liệu nào + KHÔNG có mạng → modal nhắc kiểm tra kết nối (thay cho việc hiện lỗi khô khan).
    if (vm.noNetwork) {
        AppMessageDialog(
            onDismissRequest = { vm.dismissNoNetwork() },
            title = appStrings().agency.noNetworkTitle,
            message = appStrings().agency.noNetworkMessage,
            dismissOnClickOutside = false,
            buttons = listOf(
                DialogButton(text = appStrings().callList.close, color = TextSecondary, onClick = { vm.dismissNoNetwork() }),
                DialogButton(text = appStrings().agency.retry, color = Primary, bold = true, onClick = {
                    vm.dismissNoNetwork()
                    vm.refresh()
                })
            )
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Thanh trên
// ---------------------------------------------------------------------------------------------

@Composable
private fun DirectoryTopBar(
    title: String,
    subtitle: String,
    refreshing: Boolean,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onInfo: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, appStrings().common.back, TextPrimary, onBack)
        Spacer(Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(4.dp))
        if (refreshing) {
            Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Primary)
            }
        } else {
            CircleIconButton(Icons.Rounded.Refresh, appStrings().agency.refresh, TextSecondary, onRefresh)
        }
        CircleIconButton(Icons.Rounded.Info, appStrings().agency.infoTitle, TextSecondary, onInfo)
    }
}

@Composable
private fun CircleIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = desc, tint = tint, modifier = Modifier.size(23.dp))
    }
}

// ---------------------------------------------------------------------------------------------
// Thẻ cảnh báo đỏ/vàng (thông tin quan trọng nhất) — nhấn để mở lưu ý đầy đủ.
// ---------------------------------------------------------------------------------------------

@Composable
private fun WarningBanner(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 2.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AccentAmberBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = AccentRed, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appStrings().agency.disclaimerShort,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AccentRed,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = appStrings().agency.disclaimerSub,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Ô tìm kiếm.
// ---------------------------------------------------------------------------------------------

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit, onClear: () -> Unit, onSearch: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
            .height(46.dp)
            .clip(RoundedCornerShape(23.dp))
            .background(FieldSurface)
            .padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(
                    text = appStrings().agency.searchHint,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier.size(34.dp).clip(CircleShape).clickable(onClick = onClear),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Close, contentDescription = appStrings().agency.clearSearch, tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Dòng tiêu đề kết quả: nhãn ngành + tổng số + trang + nguồn dữ liệu.
// ---------------------------------------------------------------------------------------------

@Composable
private fun ResultHeader(category: String, total: Int, page: Int, pageCount: Int, origin: AgencyOrigin?, updated: String?) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = appStrings().agency.categoryTotal(category, total),
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (pageCount > 1) {
                Text(
                    text = appStrings().agency.pageCompact(page + 1, pageCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }
        }
        originCaption(origin, updated)?.let {
            Text(text = it, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 11.sp)
        }
    }
}

private fun originCaption(origin: AgencyOrigin?, updated: String?): String? {
    val up = if (!updated.isNullOrBlank()) appStrings().agency.sourceUpdated(updated) else ""
    return when (origin) {
        AgencyOrigin.NETWORK -> appStrings().agency.sourceNetwork(up)
        AgencyOrigin.CACHE -> appStrings().agency.sourceCache(up)
        null -> null
    }
}

// ---------------------------------------------------------------------------------------------
// Thẻ 1 cơ quan (bung được).
// ---------------------------------------------------------------------------------------------

@Composable
private fun AgencyItemCard(
    agency: Agency,
    emergency: Emergency?,
    onDial: (String) -> Unit,
    onOpenUrl: (String) -> Unit
) {
    var expanded by rememberSaveable(agency.id) { mutableStateOf(false) }
    val chevronRot by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    val multiPhone = agency.phones.size > 1
    val fallback = agency.fallbackPhone ?: emergency?.police
    val subtitle = when {
        agency.hasPhone && multiPhone -> appStrings().agency.phonesMore(formatPhone(agency.primaryPhone!!), agency.phones.size - 1)
        agency.hasPhone -> formatPhone(agency.primaryPhone!!)
        fallback != null -> appStrings().agency.noPrivatePhoneEmergency(fallback)
        else -> appStrings().agency.noPhone
    }

    PanelCard(modifier = Modifier.fillMaxWidth(), radius = 18.dp) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = agency.name.ifBlank { appStrings().agency.unnamed },
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (agency.hasPhone) LinkColor else TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(6.dp))
                if (agency.hasPhone) {
                    CallCircleButton(onClick = { onDial(agency.primaryPhone!!) })
                }
                Icon(
                    imageVector = Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) appStrings().repeatStats.collapse else appStrings().repeatStats.expand,
                    tint = TextSecondary,
                    modifier = Modifier.size(24.dp).rotate(chevronRot)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 14.dp)) {
                    HorizontalDivider(color = DividerColor, thickness = 1.dp)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = agency.address.ifBlank { appStrings().agency.noAddress },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (multiPhone) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = appStrings().common.phoneNumberLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        agency.phones.forEach { phone -> PhoneRow(phone = phone, onDial = onDial) }
                    }

                    if (!agency.hasPhone && fallback != null) {
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = AccentRed, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = appStrings().agency.emergencyCallNote(fallback),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            CallCircleButton(onClick = { onDial(fallback) })
                        }
                    }

                    // Liên kết bản đồ / Facebook (nếu có trong dữ liệu).
                    if (!agency.maps.isNullOrBlank() || !agency.facebook.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            agency.maps?.takeIf { it.isNotBlank() }?.let { url ->
                                LinkChip(Icons.Rounded.Map, appStrings().agency.mapChip, onClick = { onOpenUrl(url) })
                                Spacer(Modifier.width(10.dp))
                            }
                            agency.facebook?.takeIf { it.isNotBlank() }?.let { url ->
                                LinkChip(Icons.Rounded.Public, "Facebook", onClick = { onOpenUrl(url) })
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Nút liên kết nhỏ (bản đồ / Facebook) — nền xám nhạt, mở URL ngoài. */
@Composable
private fun LinkChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(CardFill)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
    }
}

/** Hàng số điện thoại trong phần bung (khi cơ quan có nhiều số): số + nút gọi riêng. */
@Composable
private fun PhoneRow(phone: String, onDial: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Phone, contentDescription = null, tint = AccentGreen, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = formatPhone(phone),
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        CallCircleButton(onClick = { onDial(phone) })
    }
}

/** Nút GỌI tròn (nền xanh lá nhạt, icon điện thoại xanh) — mở trình quay số hệ thống. */
@Composable
private fun CallCircleButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(42.dp).clip(CircleShape).background(AccentGreenBg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.Phone, contentDescription = appStrings().callDetail.call, tint = AccentGreen, modifier = Modifier.size(20.dp))
    }
}

// ---------------------------------------------------------------------------------------------
// Thanh phân trang.
// ---------------------------------------------------------------------------------------------

@Composable
private fun PaginationBar(page: Int, pageCount: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        PagerButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, appStrings().agency.prevPage, enabled = page > 0, onClick = onPrev)
        Text(
            text = appStrings().agency.pageOf(page + 1, pageCount),
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 18.dp)
        )
        PagerButton(Icons.AutoMirrored.Rounded.KeyboardArrowRight, appStrings().agency.nextPage, enabled = page < pageCount - 1, onClick = onNext)
    }
}

@Composable
private fun PagerButton(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (enabled) CardFill else AppBackground)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = desc,
            tint = if (enabled) TextPrimary else DividerColor,
            modifier = Modifier.size(24.dp)
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Trạng thái trống / đang tải.
// ---------------------------------------------------------------------------------------------

@Composable
private fun EmptyResult(query: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (query.isBlank()) appStrings().agency.emptyNone else appStrings().agency.emptySearch(query),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Composable
private fun CenterState(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}

/** Trạng thái LỖI (pure remote — không có dữ liệu dự phòng): thông báo + nút Thử lại. */
@Composable
private fun ErrorState(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(12.dp))
        Text(
            text = appStrings().agency.loadFailed,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Primary)
                .clickable(onClick = onRetry)
                .padding(horizontal = 22.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(text = appStrings().agency.retry, style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Bottom sheet LƯU Ý đầy đủ (mở qua nút ⓘ hoặc thẻ cảnh báo).
// ---------------------------------------------------------------------------------------------

@Composable
private fun DisclaimerSheet(
    dataset: AgencyDataset,
    updated: String?,
    source: String?,
    disclaimer: String?,
    emergency: Emergency?,
    origin: AgencyOrigin?,
    onDismiss: () -> Unit
) {
    AppBottomSheet(onDismiss = onDismiss, title = appStrings().agency.infoTitle, showCloseButton = true) { _ ->
        Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp)) {
            DisclaimerRow(
                color = AccentRed, bg = AccentRedBg,
                title = appStrings().agency.note1Title,
                body = appStrings().agency.note1Body
            )
            DisclaimerRow(
                color = AccentAmber, bg = AccentAmberBg,
                title = appStrings().agency.note2Title,
                body = appStrings().agency.note2Body
            )
            DisclaimerRow(
                color = AccentRed, bg = AccentRedBg,
                title = appStrings().agency.note3Title,
                body = appStrings().agency.note3Body
            )
            emergency?.let { e ->
                val nums = listOfNotNull(
                    e.police?.let { appStrings().agency.emergencyPolice(it) },
                    e.fire?.let { appStrings().agency.emergencyFire(it) },
                    e.medical?.let { appStrings().agency.emergencyMedical(it) }
                )
                if (nums.isNotEmpty()) {
                    DisclaimerRow(
                        color = AccentRed, bg = AccentRedBg,
                        title = appStrings().agency.emergencyTitle,
                        body = appStrings().agency.emergencyCallBody(nums.joinToString(" · "))
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            val meta = buildString {
                append(disclaimer ?: source ?: "${dataset.category} · ${dataset.province.displayName}")
                if (!updated.isNullOrBlank()) append(appStrings().agency.metaUpdated(updated))
                originCaption(origin, null)?.let { append("\n$it") }
            }
            Text(
                text = meta,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun DisclaimerRow(color: Color, bg: Color, title: String, body: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(30.dp).clip(CircleShape).background(bg),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = color, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(text = body, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

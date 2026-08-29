package com.antimobile.mcas.ui.calldetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.antimobile.mcas.i18n.appStrings
import com.antimobile.mcas.ui.components.AppMessageDialog
import com.antimobile.mcas.ui.components.AppToastHost
import com.antimobile.mcas.ui.components.AppToastType
import com.antimobile.mcas.ui.components.DialogButton
import com.antimobile.mcas.ui.components.PanelCard
import com.antimobile.mcas.ui.components.SheetCloseButton
import com.antimobile.mcas.ui.components.SheetHandleBar
import com.antimobile.mcas.ui.theme.AccentBlue
import com.antimobile.mcas.ui.theme.AccentBlueBg
import com.antimobile.mcas.ui.theme.AccentRed
import com.antimobile.mcas.ui.theme.AccentRedBg
import com.antimobile.mcas.ui.theme.AppBackground
import com.antimobile.mcas.ui.theme.BrandSoft
import com.antimobile.mcas.ui.theme.CardFill
import com.antimobile.mcas.ui.theme.Primary
import com.antimobile.mcas.ui.theme.ProvideAppDensity
import com.antimobile.mcas.ui.theme.TextPrimary
import com.antimobile.mcas.ui.theme.TextSecondary
import com.antimobile.mcas.util.CallActions
import com.antimobile.mcas.util.MessageTemplate
import com.antimobile.mcas.util.MessageTemplateStore
import kotlinx.coroutines.launch

/** Số mẫu tin tối đa hiển thị trên MỖI trang pager (phần dư tràn sẽ cuộn dọc trong trang đó). */
private const val PAGE_SIZE = 4

/**
 * Bottom sheet "Gửi tin nhắn theo mẫu":
 *  - Cao TỐI ĐA 70% màn hình; các mẫu chia thành TRANG (pager) — vuốt NGANG để lật trang, mỗi trang tối đa
 *    [PAGE_SIZE] mẫu (nội dung trong trang vẫn cuộn dọc được nếu tràn). Chấm chỉ trang ở dưới.
 *  - Mỗi thẻ: CHẠM = điền pattern rồi mở app nhắn tin; icon QR = quét QR rồi gửi; nút Sửa / Xoá hiện SẴN
 *    trên thẻ (đã bỏ thao tác vuốt-lộ vì trùng cử chỉ vuốt-lật-trang của pager).
 *  - Nút "Tạo mẫu" (pill xanh) neo ở đáy sheet.
 *
 * Tạo/Sửa mở MÀN HÌNH riêng (không dùng dialog) qua [onCreate]/[onEdit] — sheet đóng trước rồi mở màn
 * soạn; [onSend]/[onScan] cũng chạy sau khi sheet đóng (nơi gọi điều phối: soạn nội dung / mở scanner).
 * Xoá xử lý tại chỗ (hỏi xác nhận), sheet vẫn mở.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageTemplateSheet(
    onDismiss: () -> Unit,
    onSend: (MessageTemplate) -> Unit,
    onScan: (MessageTemplate) -> Unit,
    onCreate: () -> Unit,
    onEdit: (MessageTemplate) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val config = LocalConfiguration.current
    val sheetHeight = (config.screenHeightDp * 0.70f).dp
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var templates by remember { mutableStateOf(MessageTemplateStore.load(context)) }
    var pendingDelete by remember { mutableStateOf<MessageTemplate?>(null) }

    // Đóng sheet CÓ hoạt ảnh rồi mới chạy hành động tiếp theo (gửi / quét / mở màn soạn).
    fun closeThen(after: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            after()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // Khoá kéo: vuốt lên/xuống không làm sheet dịch chuyển hay đóng; chỉ đóng bằng nút X / tap ngoài / back.
        sheetGesturesEnabled = false,
        containerColor = AppBackground,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp), contentAlignment = Alignment.Center) {
                SheetHandleBar()
            }
        }
    ) {
        // Cấp lại density theo cỡ chữ người dùng — ModalBottomSheet là cửa sổ Compose riêng (xem [ProvideAppDensity]).
        ProvideAppDensity {
        Box(Modifier.fillMaxWidth().height(sheetHeight)) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 2.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    appStrings().settings.templatesTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${templates.size}/${MessageTemplateStore.MAX}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                Spacer(Modifier.width(8.dp))
                SheetCloseButton(onClick = { closeThen { } })
            }

            if (templates.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    EmptyTemplates()
                }
            } else {
                val pageCount = (templates.size + PAGE_SIZE - 1) / PAGE_SIZE
                val pagerState = rememberPagerState(pageCount = { pageCount })
                // Xoá mẫu làm giảm số trang → nếu đang ở trang vừa biến mất thì lùi về trang cuối còn hợp lệ.
                LaunchedEffect(pageCount) {
                    if (pagerState.currentPage > pageCount - 1) pagerState.scrollToPage(pageCount - 1)
                }

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalAlignment = Alignment.Top
                ) { page ->
                    val start = page * PAGE_SIZE
                    val end = minOf(start + PAGE_SIZE, templates.size)
                    // Cuộn dọc trong trang chỉ là "phao cứu" khi thẻ cao hơn khung — cử chỉ dọc không đụng vuốt ngang.
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                    ) {
                        Spacer(Modifier.height(2.dp))
                        for (i in start until end) {
                            val template = templates[i]
                            TemplateCard(
                                template = template,
                                onSend = { closeThen { onSend(template) } },
                                onScan = { closeThen { onScan(template) } },
                                onEdit = { closeThen { onEdit(template) } },
                                onDelete = { pendingDelete = template }
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }

                if (pageCount > 1) {
                    PageDots(pagerState = pagerState, count = pageCount)
                }
            }

            CreateButton(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp, bottom = navBottom + 16.dp),
                onClick = {
                    if (templates.size >= MessageTemplateStore.MAX) {
                        CallActions.toast(context, appStrings().templates.maxReached(MessageTemplateStore.MAX), AppToastType.Warning)
                    } else {
                        closeThen { onCreate() }
                    }
                }
            )
        }
        AppToastHost(modifier = Modifier.matchParentSize())
        }
        }
    }

    pendingDelete?.let { template ->
        AppMessageDialog(
            onDismissRequest = { pendingDelete = null },
            title = appStrings().templates.deleteTitle,
            message = appStrings().templates.deleteMessage(template.title),
            buttons = listOf(
                DialogButton(appStrings().common.cancel, TextSecondary) { pendingDelete = null },
                DialogButton(appStrings().callList.delete, AccentRed, bold = true) {
                    templates = MessageTemplateStore.delete(context, template.id)
                    pendingDelete = null
                }
            )
        )
    }
}

/** Chấm chỉ trang ở đáy pager: trang đang xem = thanh xanh dài, còn lại = chấm xám nhỏ. */
@Composable
private fun PageDots(pagerState: PagerState, count: Int, modifier: Modifier = Modifier) {
    val current = pagerState.currentPage
    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { i ->
            val active = i == current
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(if (active) 18.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (active) Primary else TextSecondary.copy(alpha = 0.3f))
            )
        }
    }
}

/** Nút NỔI "Tạo mẫu" (pill xanh) đặt đáy sheet. */
@Composable
private fun CreateButton(modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Primary)
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, tint = AppBackground, modifier = Modifier.size(20.dp))
        Text(appStrings().templates.createButton, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = AppBackground)
    }
}

/** Thẻ một mẫu tin nhắn: chạm nội dung = gửi; icon QR = quét rồi gửi; nút Sửa / Xoá hiện sẵn bên phải. */
@Composable
private fun TemplateCard(
    template: MessageTemplate,
    onSend: () -> Unit,
    onScan: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    // CHỈ hiện nút quét QR khi nội dung có pattern {contextqr} (chỗ nhét kết quả quét); không có thì ẩn nút.
    val hasQr = template.content.contains("{contextqr}", ignoreCase = true)
    // Bấm CẢ dòng = gửi (hiệu ứng toả phủ full item); các nút QR/Sửa/Xoá có tap riêng nên tự "nuốt" cú chạm.
    PanelCard(modifier = Modifier.fillMaxWidth(), radius = 16.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .clickable(onClick = onSend),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    template.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (template.content.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        template.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(
                modifier = Modifier.padding(end = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasQr) {
                    ActionButton(Icons.Rounded.QrCodeScanner, appStrings().qr.scan, Primary, BrandSoft, onScan)
                }
                ActionButton(Icons.Rounded.Edit, appStrings().templates.menuEdit, AccentBlue, AccentBlueBg, onEdit)
                ActionButton(Icons.Rounded.DeleteOutline, appStrings().callList.delete, AccentRed, AccentRedBg, onDelete)
            }
        }
    }
}

/** Nút TRÒN nhỏ trên thẻ (QR / Sửa / Xoá) — icon tô màu trên nền nhạt; có clickable riêng để không kích hoạt gửi. */
@Composable
private fun ActionButton(icon: ImageVector, desc: String, tint: Color, bg: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = desc, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun EmptyTemplates() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(72.dp).clip(CircleShape).background(CardFill),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(
            appStrings().templates.emptyTitle,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            appStrings().templates.emptyBody,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

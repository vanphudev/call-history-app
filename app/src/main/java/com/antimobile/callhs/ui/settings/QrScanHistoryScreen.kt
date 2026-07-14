package com.antimobile.callhs.ui.settings

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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.antimobile.callhs.ui.calldetail.QrScannerOverlay
import com.antimobile.callhs.i18n.appStrings
import com.antimobile.callhs.ui.components.AppBottomSheet
import com.antimobile.callhs.ui.components.AppMessageDialog
import com.antimobile.callhs.ui.components.DialogButton
import com.antimobile.callhs.ui.components.PanelCard
import com.antimobile.callhs.ui.components.QrActionSheet
import com.antimobile.callhs.ui.components.qrPreviewText
import com.antimobile.callhs.ui.components.qrTypePresentation
import com.antimobile.callhs.ui.theme.AccentGray
import com.antimobile.callhs.ui.theme.AccentGrayBg
import com.antimobile.callhs.ui.theme.AccentRed
import com.antimobile.callhs.ui.theme.AccentRedBg
import com.antimobile.callhs.ui.theme.AppBackground
import com.antimobile.callhs.ui.theme.BrandSoft
import com.antimobile.callhs.ui.theme.CardFill
import com.antimobile.callhs.ui.theme.LinkColor
import com.antimobile.callhs.ui.theme.Primary
import com.antimobile.callhs.ui.theme.TextPrimary
import com.antimobile.callhs.ui.theme.TextSecondary
import com.antimobile.callhs.util.CallActions
import com.antimobile.callhs.util.MessageTemplate
import com.antimobile.callhs.util.MessageTemplateStore
import com.antimobile.callhs.util.QrContent
import com.antimobile.callhs.util.QrScanEntry
import com.antimobile.callhs.util.QrScanHistoryStore
import com.antimobile.callhs.util.SimInfo
import com.antimobile.callhs.util.TemplateContext
import com.antimobile.callhs.util.TemplateFill
import com.antimobile.callhs.util.TimeFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Token đánh dấu chỗ điền kết quả quét QR — chỉ mẫu chứa token này mới nhận được văn bản quét. */
private const val QR_TOKEN = "{contextqr}"

/**
 * Màn LỊCH SỬ QUÉT MÃ QR trong Cài đặt — xem lại tối đa [QrScanHistoryStore.MAX] mã đã quét gần đây.
 *
 *  - Mỗi mục hiện ICON theo LOẠI (văn bản / liên kết / vị trí / email / điện thoại / SMS / Wi‑Fi / danh thiếp)
 *    + nội dung xem trước + thời điểm quét. Nút XOÁ cố định ở cuối mỗi dòng.
 *  - CHẠM một mục mở bottom sheet:
 *      · Loại CÓ HÀNH ĐỘNG (link/tel/sms/email/Wi‑Fi/danh thiếp/vị trí) → [QrActionSheet] như luồng quét hiện tại.
 *      · VĂN BẢN thuần → sheet liệt kê các mẫu tin nhắn có `{contextqr}`; chọn mẫu nào thì chèn văn bản vừa quét
 *        rồi mở app nhắn tin SOẠN MỚI (không kèm người nhận).
 *  - Nút QUÉT MÃ QR trên thanh trên: quét thêm mã mới (kết quả tự vào lịch sử qua [QrScannerOverlay]).
 *
 * Lịch sử được ghi tập trung trong [QrScannerOverlay] nên MỌI luồng quét (màn chi tiết, quản lý mẫu, và ngay
 * màn này) đều xuất hiện ở đây.
 */
@Composable
fun QrScanHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var history by remember { mutableStateOf(QrScanHistoryStore.load(context)) }
    var templates by remember { mutableStateOf(MessageTemplateStore.load(context)) }
    var showScanner by remember { mutableStateOf(false) }
    // Mã QR CÓ HÀNH ĐỘNG vừa chọn/quét: != null → mở sheet xử lý theo loại.
    var qrAction by remember { mutableStateOf<QrContent?>(null) }
    // Văn bản thuần vừa chọn/quét: != null → mở sheet chọn mẫu để nhắn tin.
    var textResult by remember { mutableStateOf<String?>(null) }
    var confirmClear by remember { mutableStateOf(false) }

    val qrTemplates = remember(templates) { templates.filter { it.content.contains(QR_TOKEN, ignoreCase = true) } }

    // Mở sheet phù hợp cho một nội dung đã phân loại: văn bản → chọn mẫu; còn lại → sheet hành động.
    fun openContent(content: QrContent) {
        if (content is QrContent.Text) textResult = content.raw else qrAction = content
    }

    // Điền pattern (đọc số SIM off-main) rồi mở app nhắn tin SOẠN MỚI với nội dung soạn sẵn.
    fun sendTemplate(template: MessageTemplate, qrText: String) {
        scope.launch {
            val body = withContext(Dispatchers.IO) {
                val my = SimInfo.myNumbers(context)
                TemplateFill.fill(
                    template.content,
                    TemplateContext(my.bySlot, qrText, System.currentTimeMillis())
                )
            }
            CallActions.composeMessage(context, body)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .statusBarsPadding()
        ) {
            HistoryTopBar(
                count = history.size,
                onBack = onBack,
                onScan = { showScanner = true }
            )

            if (history.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyHistory()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(top = 4.dp, bottom = navBottom + 24.dp)
                ) {
                    item { HistoryHint(onClearAll = { confirmClear = true }) }
                    items(history, key = { it.raw }) { entry ->
                        QrHistoryRow(
                            entry = entry,
                            onClick = { openContent(QrContent.parse(entry.raw)) },
                            onDelete = { history = QrScanHistoryStore.delete(context, entry.raw) }
                        )
                    }
                }
            }
        }

        // Màn QUÉT QR — phủ full-bleed; quét xong tự vào lịch sử (ghi trong overlay), nạp lại rồi mở sheet.
        if (showScanner) {
            QrScannerOverlay(
                onResult = { qr ->
                    showScanner = false
                    history = QrScanHistoryStore.load(context)
                    templates = MessageTemplateStore.load(context)
                    openContent(QrContent.parse(qr))
                },
                onDismiss = { showScanner = false }
            )
        }
    }

    // Sheet XỬ LÝ mã QR có hành động (link/tel/sms/email/Wi‑Fi/danh thiếp/vị trí).
    qrAction?.let { content ->
        QrActionSheet(content = content, onDismiss = { qrAction = null })
    }

    // Sheet CHỌN MẪU cho văn bản thuần: chèn văn bản vừa quét vào mẫu rồi mở tin nhắn.
    textResult?.let { raw ->
        QrTextTemplateSheet(
            text = raw,
            templates = qrTemplates,
            onPick = { template ->
                textResult = null
                sendTemplate(template, raw)
            },
            onDismiss = { textResult = null }
        )
    }

    if (confirmClear) {
        AppMessageDialog(
            onDismissRequest = { confirmClear = false },
            title = appStrings().qrHistory.clearAllTitle,
            message = appStrings().qrHistory.clearAllMessage,
            buttons = listOf(
                DialogButton(appStrings().common.cancel, TextSecondary) { confirmClear = false },
                DialogButton(appStrings().callList.clearAll, AccentRed, bold = true) {
                    history = QrScanHistoryStore.clear(context)
                    confirmClear = false
                }
            )
        )
    }
}

// ---------------------------------------------------------------------------------------------------
// Thanh trên + gợi ý + trạng thái rỗng
// ---------------------------------------------------------------------------------------------------

@Composable
private fun HistoryTopBar(count: Int, onBack: () -> Unit, onScan: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, appStrings().common.back, TextPrimary, onBack)
        Spacer(Modifier.width(8.dp))
        Text(
            text = appStrings().settings.qrHistoryTitle,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$count/${QrScanHistoryStore.MAX}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(Modifier.width(4.dp))
        RoundIconButton(Icons.Rounded.QrCodeScanner, appStrings().qr.scan, Primary, onScan)
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

/** Gợi ý cách dùng + nút "Xoá tất cả", hiện ở đầu danh sách. */
@Composable
private fun HistoryHint(onClearAll: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = appStrings().qrHistory.hint,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = appStrings().callList.clearAll,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = LinkColor,
            modifier = Modifier.clip(CircleShape).clickable(onClick = onClearAll).padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun EmptyHistory() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(72.dp).clip(CircleShape).background(CardFill),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(
            appStrings().qrHistory.emptyTitle,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            appStrings().qrHistory.emptyBody,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

// ---------------------------------------------------------------------------------------------------
// Item danh sách: thẻ + nút xoá cố định
// ---------------------------------------------------------------------------------------------------

/**
 * Một mục lịch sử: CHẠM = mở sheet xử lý theo loại; nút XOÁ cố định cuối dòng (thay cử chỉ vuốt cũ). Loại mã
 * suy ra từ [QrContent.parse] để chọn icon/màu ([qrTypePresentation]) và dòng xem trước ([qrPreviewText]).
 */
@Composable
private fun QrHistoryRow(entry: QrScanEntry, onClick: () -> Unit, onDelete: () -> Unit) {
    val content = remember(entry.raw) { QrContent.parse(entry.raw) }
    val p = qrTypePresentation(content)
    // Gộp xuống-dòng & khoảng trắng liên tiếp thành 1 dấu cách để dòng tiêu đề 1 hàng đọc gọn (mã văn bản
    // nhiều dòng không bị cắt cụt ở dòng đầu).
    val preview = remember(entry.raw, appStrings().qr.empty) { qrPreviewText(content).replace(Regex("\\s+"), " ").trim().ifBlank { appStrings().qr.empty } }

    PanelCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), radius = 16.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp)
                .clickable(onClick = onClick)
                .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape).background(p.bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(p.icon, contentDescription = null, tint = p.tint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    preview,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "${p.typeLabel} · ${TimeFormat.dayClock(entry.time)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            // Nút XOÁ cố định cuối dòng (thay cử chỉ vuốt) — chạm riêng nút này để xoá, không mở nội dung.
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(AccentRedBg).clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.DeleteOutline, contentDescription = appStrings().callList.delete, tint = AccentRed, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------
// Sheet CHỌN MẪU cho văn bản thuần
// ---------------------------------------------------------------------------------------------------

/**
 * Bottom sheet cho mục VĂN BẢN: hiện nội dung đã quét (bôi/sao chép được) + liệt kê các mẫu có `{contextqr}`.
 * Chọn mẫu nào thì [onPick] (chèn văn bản vào mẫu rồi mở tin nhắn). Không có mẫu phù hợp → gợi ý tạo mẫu.
 */
@Composable
private fun QrTextTemplateSheet(
    text: String,
    templates: List<MessageTemplate>,
    onPick: (MessageTemplate) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val s = appStrings()
    AppBottomSheet(
        onDismiss = onDismiss,
        title = s.qrHistory.resultSheetTitle,
        maxHeightFraction = 0.7f,
        showCloseButton = true
    ) { _ ->
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(AccentGrayBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.TextFields, contentDescription = null, tint = AccentGray, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        s.qr.typeText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        s.qrHistory.pickTemplateForText,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            SelectionContainer {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardFill)
                        .heightIn(max = 160.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text(
                        text.ifBlank { s.qr.empty },
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            SecondaryButton(label = s.qrHistory.copyContent, icon = Icons.Rounded.ContentCopy) {
                CallActions.copyContent(context, text)
            }

            Spacer(Modifier.height(18.dp))
            if (templates.isEmpty()) {
                Text(
                    s.qrHistory.noQrTemplates(QR_TOKEN),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            } else {
                Text(
                    s.qrHistory.pickTemplate,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )
                templates.forEach { template ->
                    TemplatePickRow(template = template, onClick = { onPick(template) })
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun TemplatePickRow(template: MessageTemplate, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(CircleShape).background(BrandSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.QrCodeScanner, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                template.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (template.content.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    template.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Nút phụ nền xám chữ tối (dùng cho "Sao chép nội dung" trong sheet văn bản). */
@Composable
private fun SecondaryButton(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardFill)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}

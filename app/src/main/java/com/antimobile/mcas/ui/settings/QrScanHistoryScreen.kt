package com.antimobile.mcas.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.antimobile.mcas.i18n.appStrings
import com.antimobile.mcas.ui.components.AppMessageDialog
import com.antimobile.mcas.ui.components.DialogButton
import com.antimobile.mcas.ui.components.PanelCard
import com.antimobile.mcas.ui.components.QrScanFlow
import com.antimobile.mcas.ui.components.qrPreviewText
import com.antimobile.mcas.ui.components.qrTypePresentation
import com.antimobile.mcas.ui.components.rememberQrScanFlowState
import com.antimobile.mcas.ui.theme.AccentRed
import com.antimobile.mcas.ui.theme.AccentRedBg
import com.antimobile.mcas.ui.theme.AppBackground
import com.antimobile.mcas.ui.theme.CardFill
import com.antimobile.mcas.ui.theme.LinkColor
import com.antimobile.mcas.ui.theme.Primary
import com.antimobile.mcas.ui.theme.TextPrimary
import com.antimobile.mcas.ui.theme.TextSecondary
import com.antimobile.mcas.util.QrContent
import com.antimobile.mcas.util.QrScanEntry
import com.antimobile.mcas.util.QrScanHistoryStore
import com.antimobile.mcas.util.TimeFormat

/**
 * Màn LỊCH SỬ QUÉT MÃ QR trong Cài đặt — xem lại tối đa [QrScanHistoryStore.MAX] mã đã quét gần đây.
 *
 *  - Mỗi mục hiện ICON theo LOẠI (văn bản / liên kết / vị trí / email / điện thoại / SMS / Wi‑Fi / danh thiếp)
 *    + nội dung xem trước + thời điểm quét. Nút XOÁ cố định ở cuối mỗi dòng.
 *  - CHẠM một mục → mở lại qua [QrScanFlow] (loại CÓ HÀNH ĐỘNG → sheet hành động; VĂN BẢN → sheet chọn mẫu tin
 *    có `{contextqr}`, chèn văn bản vừa quét rồi mở app nhắn tin SOẠN MỚI không kèm người nhận).
 *  - Nút QUÉT MÃ QR trên thanh trên: mở [QrScanFlow] quét thêm mã mới (kết quả tự vào lịch sử).
 *
 * Luồng quét + xử lý kết quả dùng chung [QrScanFlow]; lịch sử được ghi tập trung nên MỌI nơi quét (màn chi tiết,
 * quản lý mẫu, màn danh sách cuộc gọi, và ngay màn này) đều xuất hiện ở đây.
 */
@Composable
fun QrScanHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    var history by remember { mutableStateOf(QrScanHistoryStore.load(context)) }
    var confirmClear by remember { mutableStateOf(false) }
    // Luồng quét QR dùng chung (camera + sheet xử lý kết quả) — cũng mở lại nội dung khi chạm một mục lịch sử.
    val qrFlow = rememberQrScanFlowState()

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
                onScan = { qrFlow.scan() }
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
                            onClick = { qrFlow.open(QrContent.parse(entry.raw)) },
                            onDelete = { history = QrScanHistoryStore.delete(context, entry.raw) }
                        )
                    }
                }
            }
        }

        // Luồng quét QR dùng chung: camera phủ full-bleed + sheet xử lý kết quả; quét xong nạp lại lịch sử.
        QrScanFlow(state = qrFlow, onScanned = { history = QrScanHistoryStore.load(context) })
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

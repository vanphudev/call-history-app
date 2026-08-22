package com.antimobile.callhs.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.antimobile.callhs.i18n.appStrings
import com.antimobile.callhs.ui.calldetail.QrScannerOverlay
import com.antimobile.callhs.ui.theme.AccentGray
import com.antimobile.callhs.ui.theme.AccentGrayBg
import com.antimobile.callhs.ui.theme.BrandSoft
import com.antimobile.callhs.ui.theme.CardFill
import com.antimobile.callhs.ui.theme.Primary
import com.antimobile.callhs.ui.theme.TextPrimary
import com.antimobile.callhs.ui.theme.TextSecondary
import com.antimobile.callhs.util.CallActions
import com.antimobile.callhs.util.MessageTemplate
import com.antimobile.callhs.util.MessageTemplateStore
import com.antimobile.callhs.util.QrContent
import com.antimobile.callhs.util.SimInfo
import com.antimobile.callhs.util.TemplateContext
import com.antimobile.callhs.util.TemplateFill
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Token đánh dấu chỗ điền kết quả quét QR — chỉ mẫu chứa token này mới nhận được văn bản quét. */
private const val QR_TOKEN = "{contextqr}"

/**
 * Bộ điều khiển luồng QUÉT MÃ QR ĐỘC LẬP (không gắn số nhận) — dùng để MỞ camera quét hoặc mở TRỰC TIẾP một
 * nội dung QR đã phân loại (vd: chạm lại một mục lịch sử). Đặt một [QrScanFlow] trong cây rồi gọi [scan]/[open].
 */
@Stable
class QrScanFlowState internal constructor() {
    /** Đang mở màn quét camera hay không (chỉ [QrScanFlow] cùng module được đặt lại). */
    internal var scanning by mutableStateOf(false)
    /** Nội dung QR đang chờ xử lý (mở sheet tương ứng); null = không có gì đang mở. */
    internal var pending by mutableStateOf<QrContent?>(null)

    /** Mở màn quét mã QR mới. */
    fun scan() { scanning = true }

    /** Mở trực tiếp sheet xử lý cho một nội dung QR đã phân loại (không qua camera). */
    fun open(content: QrContent) { pending = content }
}

/** Tạo & nhớ một [QrScanFlowState] gắn với vị trí gọi. */
@Composable
fun rememberQrScanFlowState(): QrScanFlowState = remember { QrScanFlowState() }

/**
 * Luồng QUÉT MÃ QR ĐỘC LẬP tái dùng được (giống hệt màn Lịch sử quét QR): mở camera → phân loại kết quả →
 *  - Loại CÓ HÀNH ĐỘNG (link/tel/sms/email/Wi‑Fi/danh thiếp/vị trí) → [QrActionSheet].
 *  - VĂN BẢN thuần → sheet liệt kê các mẫu tin có `{contextqr}`; chọn mẫu nào thì chèn văn bản vừa quét rồi mở
 *    app nhắn tin SOẠN MỚI (không kèm người nhận).
 *
 * Lịch sử quét được ghi tập trung trong [QrScannerOverlay] nên MỌI nơi dùng luồng này đều xuất hiện ở màn lịch sử.
 * Điều khiển qua [state]: [QrScanFlowState.scan] để quét mới, [QrScanFlowState.open] để mở lại nội dung có sẵn.
 *
 * @param onScanned gọi SAU mỗi lần quét thành công (vd: để màn gọi/lịch sử nạp lại danh sách của mình).
 */
@Composable
fun QrScanFlow(
    state: QrScanFlowState,
    onScanned: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Mẫu tin nạp một lần + nạp lại sau mỗi lần quét (người dùng có thể vừa sửa mẫu ở màn quản lý mẫu).
    var templates by remember { mutableStateOf(MessageTemplateStore.load(context)) }
    val qrTemplates = remember(templates) { templates.filter { it.content.contains(QR_TOKEN, ignoreCase = true) } }

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

    // Màn QUÉT QR — phủ full-bleed; quét xong tự vào lịch sử (ghi trong overlay), nạp lại mẫu rồi mở sheet.
    if (state.scanning) {
        QrScannerOverlay(
            onResult = { qr ->
                state.scanning = false
                templates = MessageTemplateStore.load(context)
                state.pending = QrContent.parse(qr)
                onScanned()
            },
            onDismiss = { state.scanning = false }
        )
    }

    // Sheet xử lý theo loại: văn bản → chọn mẫu; còn lại → sheet hành động.
    when (val content = state.pending) {
        null -> Unit
        is QrContent.Text -> QrTextTemplateSheet(
            text = content.raw,
            templates = qrTemplates,
            onPick = { template ->
                state.pending = null
                sendTemplate(template, content.raw)
            },
            onDismiss = { state.pending = null }
        )
        else -> QrActionSheet(content = content, onDismiss = { state.pending = null })
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

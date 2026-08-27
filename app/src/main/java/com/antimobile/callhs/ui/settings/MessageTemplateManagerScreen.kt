package com.antimobile.callhs.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Sms
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.antimobile.callhs.ui.calldetail.QrScannerOverlay
import com.antimobile.callhs.ui.calldetail.TemplateEditorScreen
import com.antimobile.callhs.ui.components.AppBottomSheet
import com.antimobile.callhs.ui.components.AppToastType
import com.antimobile.callhs.ui.components.AppMessageDialog
import com.antimobile.callhs.ui.components.DialogButton
import com.antimobile.callhs.ui.components.PanelCard
import com.antimobile.callhs.ui.components.QrActionSheet
import com.antimobile.callhs.ui.components.ActionGlyph
import com.antimobile.callhs.ui.components.ContextAction
import com.antimobile.callhs.ui.components.ContextMenuOverlay
import com.antimobile.callhs.ui.components.rememberPressHighlight
import com.antimobile.callhs.ui.theme.AccentBlue
import com.antimobile.callhs.ui.theme.AccentBlueBg
import com.antimobile.callhs.ui.theme.AccentRed
import com.antimobile.callhs.ui.theme.AppBackground
import com.antimobile.callhs.ui.theme.BrandSoft
import com.antimobile.callhs.ui.theme.CardFill
import com.antimobile.callhs.ui.theme.Primary
import com.antimobile.callhs.ui.theme.TextPrimary
import com.antimobile.callhs.ui.theme.TextSecondary
import com.antimobile.callhs.i18n.appStrings
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

/** Token đánh dấu chỗ điền kết quả quét QR — chỉ những mẫu chứa token này mới có hành động quét. */
private const val QR_TOKEN = "{contextqr}"

private fun MessageTemplate.hasQr(): Boolean = content.contains(QR_TOKEN, ignoreCase = true)

/** Đích màn soạn mẫu (tạo mới / sửa) — phủ full-bleed lên trên danh sách. */
private sealed interface EditorTarget {
    data object Create : EditorTarget
    data class Edit(val template: MessageTemplate) : EditorTarget
}

/** Lý do mở màn quét QR: quét-rồi-gửi ngay MỘT mẫu, hoặc quét-rồi-CHỌN mẫu (từ nút quét trên thanh trên). */
private sealed interface ScanIntent {
    data class ForTemplate(val template: MessageTemplate) : ScanIntent
    data object PickTemplate : ScanIntent
}

/** Item đang mở context-menu ở màn quản lý mẫu: mẫu + khung THẺ (toạ độ cửa sổ, px). */
private data class TemplateContextTarget(val template: MessageTemplate, val bounds: Rect)

/**
 * Màn QUẢN LÝ MẪU TIN NHẮN trong Cài đặt — tách khỏi màn chi tiết để xem/sửa mọi mẫu ở một nơi.
 *
 * Hoạt động tương tự [com.antimobile.callhs.ui.calldetail.MessageTemplateSheet] nhưng dạng MÀN đầy đủ:
 *  - Danh sách dọc, MỖI mẫu là 1 thẻ: CHẠM = điền pattern rồi mở app nhắn tin (KHÔNG kèm người nhận —
 *    người dùng tự chọn số); NHẤN GIỮ mở context-menu Sửa / Xoá / Quét QR (nút quét chỉ hiện khi mẫu có `{contextqr}`).
 *  - Nút QUÉT QR trên thanh trên: quét trước, xong mở bottom sheet liệt kê các mẫu có `{contextqr}`;
 *    chọn mẫu nào thì chèn mã vừa quét vào rồi mở tin nhắn.
 *  - Nút "Tạo mẫu" (pill xanh) neo đáy màn; tôn trọng giới hạn [MessageTemplateStore.MAX].
 *
 * Tái dùng [TemplateEditorScreen] (tạo/sửa) và [QrScannerOverlay] (quét) — cả hai độc lập với dữ liệu cuộc gọi.
 */
@Composable
fun MessageTemplateManagerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val density = LocalDensity.current
    val statusBarPx = WindowInsets.statusBars.getTop(density)
    val navBarPx = WindowInsets.navigationBars.getBottom(density)

    var templates by remember { mutableStateOf(MessageTemplateStore.load(context)) }
    var templateTarget by remember { mutableStateOf<TemplateContextTarget?>(null) }
    var editorTarget by remember { mutableStateOf<EditorTarget?>(null) }
    var scanIntent by remember { mutableStateOf<ScanIntent?>(null) }
    // Mã QR vừa quét ở chế độ "chọn mẫu": != null → mở bottom sheet chọn mẫu để chèn mã rồi gửi.
    var qrPickText by remember { mutableStateOf<String?>(null) }
    // Mã QR CÓ HÀNH ĐỘNG (link/tel/sms/email) vừa quét: != null → mở sheet xử lý thay vì chèn vào mẫu.
    var qrAction by remember { mutableStateOf<QrContent?>(null) }
    var pendingDelete by remember { mutableStateOf<MessageTemplate?>(null) }

    val qrTemplates = remember(templates) { templates.filter { it.hasQr() } }

    // Điền pattern (chạy off-main để đọc số SIM) rồi mở app nhắn tin SOẠN MỚI với nội dung soạn sẵn.
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
            ManagerTopBar(
                count = templates.size,
                onBack = onBack,
                onScan = {
                    if (qrTemplates.isEmpty()) {
                        CallActions.toast(context, appStrings().templates.noQrTemplatesToast(QR_TOKEN), AppToastType.Warning)
                    } else {
                        scanIntent = ScanIntent.PickTemplate
                    }
                }
            )

            if (templates.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyTemplates()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    // Chừa đáy đủ để nút "Tạo mẫu" nổi không che item cuối.
                    contentPadding = PaddingValues(top = 8.dp, bottom = navBottom + 92.dp)
                ) {
                    item { ManagerHint() }
                    items(templates, key = { it.id }) { template ->
                        TemplateManagerItem(
                            template = template,
                            onSend = { sendTemplate(template, "") },
                            onLongPress = { bounds -> templateTarget = TemplateContextTarget(template, bounds) },
                            activeInMenu = templateTarget?.template?.id == template.id
                        )
                    }
                }
            }
        }

        // Nút NỔI "Tạo mẫu" neo đáy giữa (đặt trước các lớp phủ để bị chúng che khi mở).
        CreateTemplateButton(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navBottom + 18.dp),
            onClick = {
                if (templates.size >= MessageTemplateStore.MAX) {
                    CallActions.toast(context, appStrings().templates.maxReached(MessageTemplateStore.MAX), AppToastType.Warning)
                } else {
                    editorTarget = EditorTarget.Create
                }
            }
        )

        // Màn TẠO/SỬA mẫu — phủ full-bleed; xong (lưu/quay lại) đóng lớp phủ & nạp lại danh sách.
        if (editorTarget != null) {
            TemplateEditorScreen(
                initial = (editorTarget as? EditorTarget.Edit)?.template,
                onExit = {
                    editorTarget = null
                    templates = MessageTemplateStore.load(context)
                }
            )
        }

        // Màn QUÉT QR — phủ full-bleed; quét xong tuỳ [scanIntent] mà gửi ngay hay mở sheet chọn mẫu.
        if (scanIntent != null) {
            QrScannerOverlay(
                onResult = { qr ->
                    val content = QrContent.parse(qr)
                    val intent = scanIntent
                    scanIntent = null
                    if (content.isActionable) {
                        // Link/tel/sms/email → KHÔNG chèn vào mẫu, mở sheet xử lý theo loại.
                        qrAction = content
                    } else {
                        // Văn bản thuần → theo luồng mẫu như cũ.
                        when (intent) {
                            is ScanIntent.ForTemplate -> sendTemplate(intent.template, content.raw)
                            ScanIntent.PickTemplate -> qrPickText = content.raw
                            null -> {}
                        }
                    }
                },
                onDismiss = { scanIntent = null }
            )
        }

        // Lớp phủ context-menu (nhấn giữ mẫu) — trong Box GỐC nên phủ trọn màn.
        templateTarget?.let { tgt ->
            val hasQr = tgt.template.hasQr()
            ContextMenuOverlay(
                bounds = tgt.bounds,
                actions = buildList {
                    if (hasQr) add(ContextAction(ActionGlyph.Vector(Icons.Rounded.QrCodeScanner, Primary), appStrings().qr.scan) { scanIntent = ScanIntent.ForTemplate(tgt.template) })
                    add(ContextAction(ActionGlyph.Vector(Icons.Rounded.Edit, AccentBlue), appStrings().templates.menuEdit) { editorTarget = EditorTarget.Edit(tgt.template) })
                    add(ContextAction(ActionGlyph.Vector(Icons.Rounded.DeleteOutline, AccentRed), appStrings().callList.delete) { pendingDelete = tgt.template })
                },
                topInsetPx = statusBarPx,
                bottomInsetPx = navBarPx,
                onClosed = { templateTarget = null },
                lifted = { TemplateManagerCard(template = tgt.template, hasQr = hasQr) }
            )
        }
    }

    // Sheet XỬ LÝ mã QR có hành động (link/tel/sms/email) — hiện nội dung + nút mở tương ứng + sao chép.
    qrAction?.let { content ->
        QrActionSheet(content = content, onDismiss = { qrAction = null })
    }

    // Sheet CHỌN MẪU sau khi quét từ thanh trên: chỉ liệt kê mẫu có {contextqr}; chọn → chèn mã rồi gửi.
    qrPickText?.let { qr ->
        QrTemplatePickerSheet(
            templates = qrTemplates,
            onPick = { template ->
                qrPickText = null
                sendTemplate(template, qr)
            },
            onDismiss = { qrPickText = null }
        )
    }

    // Hỏi xác nhận trước khi xoá (giống sheet ở màn chi tiết).
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

// ---------------------------------------------------------------------------------------------------
// Thanh trên + nút tạo + trạng thái rỗng
// ---------------------------------------------------------------------------------------------------

@Composable
private fun ManagerTopBar(count: Int, onBack: () -> Unit, onScan: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, appStrings().common.back, TextPrimary, onBack)
        Spacer(Modifier.width(8.dp))
        Text(
            text = appStrings().settings.templatesTitle,
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$count/${MessageTemplateStore.MAX}",
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

/** Gợi ý cách dùng, hiện ở đầu danh sách. */
@Composable
private fun ManagerHint() {
    Text(
        text = appStrings().templates.hint,
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary,
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 10.dp)
    )
}

/** Nút NỔI "Tạo mẫu" (pill xanh) — giống nút ở sheet màn chi tiết. */
@Composable
private fun CreateTemplateButton(modifier: Modifier, onClick: () -> Unit) {
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

@Composable
private fun EmptyTemplates() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
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
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

// ---------------------------------------------------------------------------------------------------
// Item danh sách: thẻ + context-menu (nhấn giữ)
// ---------------------------------------------------------------------------------------------------

/**
 * Một mẫu trong danh sách: CHẠM = gửi; NHẤN GIỮ báo [onLongPress] kèm KHUNG THẺ để màn dựng context-menu
 * (Quét QR nếu có / Sửa / Xoá). KHÔNG còn cử chỉ vuốt (đã chuyển sang context-menu dùng chung).
 */
@Composable
private fun TemplateManagerItem(
    template: MessageTemplate,
    onSend: () -> Unit,
    onLongPress: (Rect) -> Unit,
    activeInMenu: Boolean = false
) {
    val hasQr = template.hasQr()
    // Giữ toạ độ thẻ ngoài snapshot state (không recompose mỗi frame khi cuộn) — đọc lúc nhấn giữ.
    val cardCoords = remember { CoordsHolder() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .onGloballyPositioned { cardCoords.value = it }
            // Đang mở menu cho item này → ẩn hẳn bản gốc (giữ chỗ) để chỉ thấy bản sao nổi trên overlay.
            .graphicsLayer { alpha = if (activeInMenu) 0f else 1f }
    ) {
        TemplateManagerCard(
            template = template,
            hasQr = hasQr,
            onClick = onSend,
            onLongClick = {
                val bounds = cardCoords.value?.takeIf { it.isAttached }?.boundsInWindow()
                if (bounds != null) onLongPress(bounds)
            }
        )
    }
}

@Composable
private fun TemplateManagerCard(
    template: MessageTemplate,
    hasQr: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    PanelCard(modifier = modifier.fillMaxWidth(), radius = 16.dp) {
        val interaction = remember { MutableInteractionSource() }
        val rowModifier = if (onClick != null) {
            Modifier.fillMaxWidth().heightIn(min = 76.dp)
                .combinedClickable(
                    interactionSource = interaction,
                    indication = rememberPressHighlight(),
                    onClick = onClick,
                    onLongClick = onLongClick
                )
                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp)
        } else {
            Modifier.fillMaxWidth().heightIn(min = 76.dp)
                .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 12.dp)
        }
        Row(
            modifier = rowModifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape).background(if (hasQr) BrandSoft else AccentBlueBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (hasQr) Icons.Rounded.QrCodeScanner else Icons.Rounded.Sms,
                    contentDescription = null,
                    tint = if (hasQr) Primary else AccentBlue,
                    modifier = Modifier.size(22.dp)
                )
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
            Spacer(Modifier.width(10.dp))
            // Gợi ý "chạm để gửi".
            Box(
                modifier = Modifier.size(38.dp).clip(CircleShape).background(BrandSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Rounded.Message, contentDescription = appStrings().callList.menuMessage, tint = Primary, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** Giữ [LayoutCoordinates] của thẻ ngoài snapshot state — đọc lúc nhấn giữ để lấy khung item hiện tại. */
private class CoordsHolder(var value: LayoutCoordinates? = null)

// ---------------------------------------------------------------------------------------------------
// Sheet chọn mẫu sau khi quét QR từ thanh trên
// ---------------------------------------------------------------------------------------------------

/** Bottom sheet liệt kê CÁC MẪU có `{contextqr}` để chọn sau khi đã quét mã; chọn mẫu nào thì [onPick]. */
@Composable
private fun QrTemplatePickerSheet(
    templates: List<MessageTemplate>,
    onPick: (MessageTemplate) -> Unit,
    onDismiss: () -> Unit
) {
    AppBottomSheet(onDismiss = onDismiss, title = appStrings().templates.pickToSend, showCloseButton = true) { _ ->
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(
                text = appStrings().templates.qrScannedPickTemplate,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp)
            )
            templates.forEach { template ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onPick(template) }
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
            Spacer(Modifier.height(4.dp))
        }
    }
}

package com.antimobile.mcas.ui.calldetail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.antimobile.mcas.i18n.LanguageSettings
import com.antimobile.mcas.i18n.appStrings
import com.antimobile.mcas.ui.components.AppMessageDialog
import com.antimobile.mcas.ui.components.DialogButton
import com.antimobile.mcas.ui.theme.AccentBlue
import com.antimobile.mcas.ui.theme.AccentRed
import com.antimobile.mcas.ui.theme.AppBackground
import com.antimobile.mcas.ui.theme.BrandSoft
import com.antimobile.mcas.ui.theme.FieldSurface
import com.antimobile.mcas.ui.theme.LinkColor
import com.antimobile.mcas.ui.theme.Primary
import com.antimobile.mcas.ui.theme.TextPrimary
import com.antimobile.mcas.ui.theme.TextSecondary
import com.antimobile.mcas.util.MessageTemplate
import com.antimobile.mcas.util.MessageTemplateStore
import com.antimobile.mcas.util.SimInfo
import com.antimobile.mcas.util.TemplateContext
import com.antimobile.mcas.util.TemplateFill
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TITLE_MAX = 40
private const val CONTENT_MAX = 500

/** Sheet đang CHỜ mở sau khi bàn phím ẩn xong — để sheet không trượt lên trong lúc bàn phím trượt xuống (giật). */
private enum class PendingSheet { Help, Preview }

/**
 * Màn hình TẠO / SỬA mẫu tin nhắn (thay cho dialog trước đây).
 *
 * Bố cục: `Scaffold` với TOP bar (quay lại + tiêu đề) và BOTTOM bar cố định (nút Lưu/Cập nhật).
 * Xử lý IME:
 *  - Nút Lưu ở bottomBar tự NÂNG lên trên bàn phím (windowInsetsPadding theo `ime ∪ navigationBars`),
 *    đồng thời vùng nội dung Scaffold co lại nên nội dung CUỘN được và ô nhập đang focus tự trượt vào
 *    tầm nhìn phía trên bàn phím.
 *  - CHẠM RA NGOÀI ô nhập → ẩn bàn phím + thoát focus (`detectTapGestures` → `clearFocus`).
 *  - Bấm phím XÁC NHẬN (Done) ở bàn phím tiêu đề → cũng thoát focus.
 *  - Nút quay lại / phím back hệ thống → thoát màn (mở lại danh sách mẫu).
 *
 * [initial] = null → tạo mới; khác null → sửa. Lưu/Cập nhật ghi thẳng vào [MessageTemplateStore] rồi [onExit].
 */
@Composable
fun TemplateEditorScreen(initial: MessageTemplate?, onExit: () -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val isEdit = initial != null

    var title by remember(initial?.id) { mutableStateOf(TextFieldValue(initial?.title.orEmpty())) }
    var content by remember(initial?.id) { mutableStateOf(TextFieldValue(initial?.content.orEmpty())) }
    val canSave = title.text.isNotBlank() && content.text.isNotBlank()
    // Có thay đổi so với lúc mở (tạo: khác rỗng; sửa: khác nội dung gốc) → hỏi xác nhận trước khi thoát.
    val hasChanges = title.text != initial?.title.orEmpty() || content.text != initial?.content.orEmpty()
    var showDiscardConfirm by remember { mutableStateOf(false) }

    // Số SIM của máy (đọc 1 lần, off-main) để minh hoạ {phonesimN} ở phần giải thích & xem trước, và để
    // ẩn/hiện chip {phonesim2} theo số SIM đang lắp.
    val myNumbers by produceState(initialValue = SimInfo.MyNumbers(emptyList(), 0)) {
        value = withContext(Dispatchers.IO) { SimInfo.myNumbers(context) }
    }
    var showPatternHelp by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    var sheetNow by remember { mutableStateOf(0L) } // mốc thời gian lúc mở sheet (để in ví dụ/xem trước)
    // Yêu cầu mở sheet: ẩn bàn phím TRƯỚC, chờ IME về 0 rồi mới mở sheet (tránh giật do 2 hoạt ảnh ngược chiều).
    var pendingSheet by remember { mutableStateOf<PendingSheet?>(null) }
    fun requestSheet(which: PendingSheet) {
        focusManager.clearFocus()
        pendingSheet = which
    }

    fun attemptExit() {
        focusManager.clearFocus()
        if (hasChanges) showDiscardConfirm = true else onExit()
    }

    BackHandler { attemptExit() }

    fun save() {
        if (!canSave) return
        focusManager.clearFocus()
        if (isEdit) MessageTemplateStore.update(context, initial!!.id, title.text.trim(), content.text)
        else MessageTemplateStore.add(context, title.text.trim(), content.text)
        onExit()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppBackground,
        // Tự quản inset ở top/bottom bar (tránh Scaffold chèn inset trùng vào phần thân).
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { EditorTopBar(title = if (isEdit) appStrings().templateEditor.editTitle else appStrings().templateEditor.createTitle, onBack = { attemptExit() }) },
        bottomBar = {
            EditorBottomBar(
                saveLabel = if (isEdit) appStrings().templateEditor.update else appStrings().templateEditor.save,
                canSave = canSave,
                canPreview = content.text.isNotBlank(),
                onPreview = { requestSheet(PendingSheet.Preview) },
                onSave = { save() }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // Chạm ra ngoài ô nhập → ẩn bàn phím + thoát focus (chạm vào field/chip vẫn nhận vì con ưu tiên).
                .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            FieldLabel(appStrings().templateEditor.fieldTitle)
            Spacer(Modifier.height(8.dp))
            EditorField(
                value = title,
                onValueChange = { if (it.text.length <= TITLE_MAX) title = it },
                placeholder = appStrings().templateEditor.titlePlaceholder,
                singleLine = true,
                minHeight = 0.dp,
                imeAction = ImeAction.Done,
                onImeAction = { focusManager.clearFocus() }
            )

            Spacer(Modifier.height(18.dp))
            FieldLabel(appStrings().templateEditor.fieldContent)
            Spacer(Modifier.height(8.dp))
            EditorField(
                value = content,
                onValueChange = { if (it.text.length <= CONTENT_MAX) content = it },
                placeholder = appStrings().templateEditor.contentPlaceholder,
                singleLine = false,
                minHeight = 132.dp,
                imeAction = ImeAction.Default,
                onImeAction = {}
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "${content.text.length}/$CONTENT_MAX",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FieldLabel(appStrings().templateEditor.quickInsert)
                Spacer(Modifier.weight(1f))
                // Icon "?" → mở sheet giải thích + minh hoạ kết quả từng pattern.
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .clickable { requestSheet(PendingSheet.Help) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Info, contentDescription = appStrings().templateEditor.patternInfoDesc, tint = AccentBlue, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            PatternChips(simCount = myNumbers.simCount, onInsert = { token -> content = insertToken(content, token) })
            Spacer(Modifier.height(24.dp))
        }
    }

    // Có thay đổi chưa lưu → xác nhận trước khi thoát (back / nút quay lại).
    if (showDiscardConfirm) {
        AppMessageDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = appStrings().templateEditor.discardTitle,
            message = appStrings().templateEditor.discardMessage,
            buttons = listOf(
                DialogButton(appStrings().templateEditor.stay, TextSecondary) { showDiscardConfirm = false },
                DialogButton(appStrings().templateEditor.exit, AccentRed, bold = true) {
                    showDiscardConfirm = false
                    onExit()
                }
            )
        )
    }

    // Chờ bàn phím ẩn HẲN (IME height = 0) rồi mới mở sheet đang chờ → không giật.
    if (pendingSheet != null) {
        ImeHiddenGate(onHidden = {
            sheetNow = System.currentTimeMillis()
            when (pendingSheet) {
                PendingSheet.Help -> showPatternHelp = true
                PendingSheet.Preview -> showPreview = true
                null -> {}
            }
            pendingSheet = null
        })
    }

    if (showPatternHelp) {
        PatternHelpSheet(myNumbers = myNumbers, nowMillis = sheetNow, onDismiss = { showPatternHelp = false })
    }

    if (showPreview) {
        val rendered = remember(sheetNow, content.text, myNumbers, LanguageSettings.lang) {
            TemplateFill.fill(
                content.text,
                TemplateContext(myNumbers.bySlot, appStrings().templateEditor.qrPlaceholder, sheetNow)
            )
        }
        TemplatePreviewSheet(
            rendered = rendered,
            hasContextQr = content.text.contains("{contextqr}", ignoreCase = true),
            onDismiss = { showPreview = false }
        )
    }
}

@Composable
private fun EditorTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppBackground)
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = appStrings().common.back, tint = TextPrimary, modifier = Modifier.size(23.dp))
        }
        Spacer(Modifier.size(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Thanh dưới cố định: nút "Xem trước" (phụ) + "Lưu/Cập nhật" (chính); tự nâng lên trên bàn phím khi IME hiện
 * (windowInsetsPadding theo `ime ∪ navigationBars` — union lấy phần lớn hơn, không cộng dồn).
 */
@Composable
private fun EditorBottomBar(
    saveLabel: String,
    canSave: Boolean,
    canPreview: Boolean,
    onPreview: () -> Unit,
    onSave: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppBackground)
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BarButton(Modifier.weight(1f), appStrings().templateEditor.preview, Icons.Rounded.Visibility, filled = false, enabled = canPreview, onClick = onPreview)
            BarButton(Modifier.weight(1f), saveLabel, Icons.Rounded.Check, filled = true, enabled = canSave, onClick = onSave)
        }
    }
}

/** Nút thanh dưới: [filled]=nền xanh chữ trắng (chính); ngược lại nền xám chữ tối (phụ). Mờ đi khi tắt. */
@Composable
private fun BarButton(
    modifier: Modifier,
    label: String,
    icon: ImageVector,
    filled: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val bg = if (filled) (if (enabled) Primary else Primary.copy(alpha = 0.5f)) else FieldSurface
    val fg = if (filled) Color.White else if (enabled) TextPrimary else TextPrimary.copy(alpha = 0.4f)
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = fg, maxLines = 1)
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = TextSecondary)
}

/** Ô nhập bo góc nền xám nhạt (phong cách không viền của app). */
@Composable
private fun EditorField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String,
    singleLine: Boolean,
    minHeight: Dp,
    imeAction: ImeAction,
    onImeAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(FieldSurface)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        if (value.text.isEmpty()) {
            Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
            cursorBrush = SolidColor(Primary),
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(imeAction = imeAction),
            keyboardActions = KeyboardActions(
                onDone = { onImeAction() },
                onGo = { onImeAction() },
                onSend = { onImeAction() }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .then(if (minHeight > 0.dp) Modifier.heightIn(min = minHeight) else Modifier)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PatternChips(simCount: Int, onInsert: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TemplateFill.hints(simCount).forEach { (token, _) ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(BrandSoft)
                    .clickable { onInsert(token) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(token, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium, color = LinkColor)
            }
        }
    }
}

/** Chèn [token] vào vị trí con trỏ (hoặc thay đoạn đang chọn) của [tv]. */
private fun insertToken(tv: TextFieldValue, token: String): TextFieldValue {
    val start = tv.selection.start.coerceIn(0, tv.text.length)
    val end = tv.selection.end.coerceIn(0, tv.text.length)
    val newText = tv.text.substring(0, start) + token + tv.text.substring(end)
    val cursor = (start + token.length).coerceAtMost(newText.length)
    return TextFieldValue(text = newText, selection = TextRange(cursor))
}

/**
 * Composable LÁ (không con) chỉ để theo dõi chiều cao IME; gọi [onHidden] khi bàn phím đã ẩn HẲN (height = 0).
 * Tách riêng để recompose theo từng khung hình IME chỉ xảy ra ở đây, không đụng cả màn soạn.
 */
@Composable
private fun ImeHiddenGate(onHidden: () -> Unit) {
    val density = LocalDensity.current
    val imeBottom = WindowInsets.ime.getBottom(density)
    val callback by rememberUpdatedState(onHidden)
    LaunchedEffect(imeBottom) {
        if (imeBottom == 0) callback()
    }
}

package com.antimobile.callhs.ui.category

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.antimobile.callhs.i18n.appStrings
import com.antimobile.callhs.ui.components.AppMessageDialog
import com.antimobile.callhs.ui.components.Avatar
import com.antimobile.callhs.ui.components.DialogButton
import com.antimobile.callhs.ui.components.PanelCard
import com.antimobile.callhs.ui.components.Segmented
import com.antimobile.callhs.ui.theme.AccentBlue
import com.antimobile.callhs.ui.theme.AccentRed
import com.antimobile.callhs.ui.theme.AppBackground
import com.antimobile.callhs.ui.theme.FieldSurface
import com.antimobile.callhs.ui.theme.Primary
import com.antimobile.callhs.ui.theme.TextPrimary
import com.antimobile.callhs.ui.theme.TextSecondary
import com.antimobile.callhs.util.CallActions
import com.antimobile.callhs.util.TimeFormat

private const val NAME_MAX = 40
private const val DESC_MAX = 200

/**
 * Màn TẠO / SỬA nhóm phân loại — cùng recipe IME với [com.antimobile.callhs.ui.calldetail.TemplateEditorScreen]
 * (Scaffold contentWindowInsets=0 + bottomBar `ime ∪ navigationBars` + thân cuộn dọc).
 *
 * [categoryId] ≤ 0 → TẠO (chỉ tab Thông tin, icon+màu random sẵn). > 0 → SỬA (2 tab: Thông tin | Số điện thoại).
 * Nhóm mặc định: tên KHÔNG sửa được (localize theo builtInKey), chỉ đổi mô tả + icon/màu.
 */
@Composable
fun CategoryEditorScreen(
    vm: CategoryEditorViewModel,
    categoryId: Long,
    onExit: () -> Unit,
    onOpenNumber: (String) -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val s = appStrings().category
    val isEdit = categoryId > 0L

    LaunchedEffect(categoryId) { vm.bind(categoryId) }

    // rememberSaveable → khi mở lịch sử cuộc gọi của 1 số (điều hướng đi) rồi QUAY LẠI, editor giữ nguyên
    // tab đang chọn + nội dung đã nhập (KHÔNG reset về tab "Thông tin").
    var name by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var description by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var iconKey by rememberSaveable { mutableStateOf("") }
    var colorArgb by rememberSaveable { mutableStateOf(0L) }
    var initialized by rememberSaveable { mutableStateOf(false) }
    // Giá trị ban đầu để xác định "có thay đổi".
    var initName by rememberSaveable { mutableStateOf("") }
    var initDesc by rememberSaveable { mutableStateOf("") }
    var initIcon by rememberSaveable { mutableStateOf("") }
    var initColor by rememberSaveable { mutableStateOf(0L) }

    var tab by rememberSaveable { mutableStateOf(0) }
    var showIconPicker by remember { mutableStateOf(false) }
    // Đang CHỜ bàn phím ẩn xong mới mở sheet chọn icon (tránh giật do 2 hoạt ảnh ngược chiều).
    var pendingIconSheet by remember { mutableStateOf(false) }
    var showDiscard by remember { mutableStateOf(false) }

    val builtIn = vm.category?.isBuiltIn == true

    // Khởi tạo state 1 lần khi đã nạp xong (edit: từ nhóm; create: random).
    LaunchedEffect(vm.loaded) {
        if (vm.loaded && !initialized) {
            val c = vm.category
            if (c != null) {
                name = TextFieldValue(c.name)
                description = TextFieldValue(c.description)
                iconKey = c.iconKey
                colorArgb = c.colorArgb
            } else {
                iconKey = MATERIAL_ICON_KEYS.random()
                colorArgb = CATEGORY_COLORS.random()
            }
            initName = name.text; initDesc = description.text; initIcon = iconKey; initColor = colorArgb
            initialized = true
        }
    }

    val nameValid = builtIn || name.text.isNotBlank()
    val hasChanges = initialized && (name.text != initName || description.text != initDesc ||
        iconKey != initIcon || colorArgb != initColor)

    fun attemptExit() {
        if (hasChanges) showDiscard = true else onExit()
    }
    BackHandler { attemptExit() }

    fun save() {
        if (!nameValid || !initialized) return
        val finalName = if (builtIn) (vm.category?.name ?: name.text) else name.text.trim()
        vm.save(finalName, description.text, iconKey, colorArgb) { id ->
            if (id == null) CallActions.toast(context, s.maxCategories) else onExit()
        }
    }

    val showSaveBar = !isEdit || tab == 0

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { EditorTopBar(title = if (isEdit) s.editTitle else s.createTitle, onBack = { attemptExit() }) },
        bottomBar = {
            if (showSaveBar) {
                SaveBar(label = if (isEdit) s.update else s.save, enabled = nameValid, onSave = { save() })
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isEdit) {
                Segmented(
                    labels = listOf(s.tabInfo, s.tabNumbers(vm.members.size)),
                    selected = tab,
                    onSelect = { tab = it },
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 6.dp),
                )
            }
            if (!isEdit || tab == 0) {
                InfoTab(
                    modifier = Modifier.weight(1f),
                    builtIn = builtIn,
                    displayName = vm.category?.let { categoryLabel(it) }.orEmpty(),
                    name = name,
                    onName = { if (it.text.length <= NAME_MAX) name = it },
                    description = description,
                    onDescription = { if (it.text.length <= DESC_MAX) description = it },
                    iconKey = iconKey,
                    colorArgb = colorArgb,
                    onPickIcon = { focusManager.clearFocus(); pendingIconSheet = true },
                )
            } else {
                NumbersTab(
                    modifier = Modifier.weight(1f),
                    members = vm.members,
                    onOpenNumber = onOpenNumber,
                    onRemove = { row ->
                        vm.removeMember(row.phoneKey)
                        CallActions.toast(context, s.removedFrom(vm.category?.let { categoryLabel(it) }.orEmpty()))
                    },
                )
            }
        }
    }

    // Chờ bàn phím ẩn HẲN (IME height = 0) rồi mới mở sheet chọn icon → không giật (theo TemplateEditorScreen).
    if (pendingIconSheet) {
        ImeHiddenGate(onHidden = { showIconPicker = true; pendingIconSheet = false })
    }

    if (showIconPicker) {
        IconPickerSheet(
            onDismiss = { showIconPicker = false },
            onPick = { key, argb -> iconKey = key; colorArgb = argb; showIconPicker = false },
        )
    }

    if (showDiscard) {
        AppMessageDialog(
            onDismissRequest = { showDiscard = false },
            title = s.discardTitle,
            message = s.discardMessage,
            buttons = listOf(
                DialogButton(s.discardStay, TextSecondary) { showDiscard = false },
                DialogButton(s.discardExit, AccentRed, bold = true) { showDiscard = false; onExit() },
            )
        )
    }
}

// -------------------------------------------------------------------------------------------------

@Composable
private fun InfoTab(
    modifier: Modifier,
    builtIn: Boolean,
    displayName: String,
    name: TextFieldValue,
    onName: (TextFieldValue) -> Unit,
    description: TextFieldValue,
    onDescription: (TextFieldValue) -> Unit,
    iconKey: String,
    colorArgb: Long,
    onPickIcon: () -> Unit,
) {
    val focus = LocalFocusManager.current
    val s = appStrings().category
    Column(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) { detectTapGestures { focus.clearFocus() } }
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Biểu tượng & màu
        FieldLabel(s.iconLabel)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(FieldSurface)
                .clickable(onClick = onPickIcon)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryIconDisc(iconKey, colorArgb, size = 48.dp)
            Spacer(Modifier.width(14.dp))
            Text(s.pickIconTitle, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(22.dp))
        }

        Spacer(Modifier.height(18.dp))
        FieldLabel(s.nameLabel)
        Spacer(Modifier.height(8.dp))
        if (builtIn) {
            // Nhóm mặc định: tên cố định (không sửa) → hiện read-only + ghi chú giải thích rõ cho người dùng.
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(FieldSurface).padding(14.dp)
            ) {
                Text(displayName, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Info, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    s.builtinLocked,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            EditorField(
                value = name,
                onValueChange = onName,
                placeholder = s.nameHint,
                singleLine = true,
                minHeight = 0.dp,
                imeAction = ImeAction.Done,
                onImeAction = { focus.clearFocus() },
            )
        }

        Spacer(Modifier.height(18.dp))
        FieldLabel(s.descLabel)
        Spacer(Modifier.height(8.dp))
        EditorField(
            value = description,
            onValueChange = onDescription,
            placeholder = s.descHint,
            singleLine = false,
            minHeight = 96.dp,
            imeAction = ImeAction.Default,
            onImeAction = {},
        )
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun NumbersTab(
    modifier: Modifier,
    members: List<MemberRow>,
    onOpenNumber: (String) -> Unit,
    onRemove: (MemberRow) -> Unit,
) {
    val s = appStrings().category
    if (members.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(s.emptyMembers, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, modifier = Modifier.padding(40.dp))
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize().navigationBarsPadding(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 8.dp, bottom = 8.dp),
    ) {
        items(members, key = { it.phoneKey }) { row ->
            MemberItem(row = row, onClick = { onOpenNumber(row.rawNumber) }, onRemove = { onRemove(row) })
        }
    }
}

@Composable
private fun MemberItem(row: MemberRow, onClick: () -> Unit, onRemove: () -> Unit) {
    val s = appStrings().category
    PanelCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        radius = 16.dp,
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(label = row.displayLabel, photoUri = row.photoUri, isNamed = row.isNamed, size = 44.dp)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = row.displayLabel,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (row.isNamed) "${row.rawNumber} · ${s.addedAt(TimeFormat.dayClock(row.addedAt))}"
                else s.addedAt(TimeFormat.dayClock(row.addedAt)),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = s.removeMember, tint = AccentRed, modifier = Modifier.size(22.dp))
        }
    }
    }
}

// -------------------------------------------------------------------------------------------------
// Chrome dùng lại từ TemplateEditor (bản sao cục bộ để không đụng file gốc)
// -------------------------------------------------------------------------------------------------

@Composable
private fun EditorTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppBackground)
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = appStrings().common.back, tint = TextPrimary, modifier = Modifier.size(23.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SaveBar(label: String, enabled: Boolean, onSave: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppBackground)
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 12.dp)
    ) {
        val bg = if (enabled) Primary else Primary.copy(alpha = 0.5f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(bg)
                .clickable(enabled = enabled, onClick = onSave),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = TextSecondary)
}

@Composable
private fun EditorField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String,
    singleLine: Boolean,
    minHeight: Dp,
    imeAction: ImeAction,
    onImeAction: () -> Unit,
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
            keyboardActions = KeyboardActions(onDone = { onImeAction() }, onGo = { onImeAction() }, onSend = { onImeAction() }),
            modifier = Modifier
                .fillMaxWidth()
                .then(if (minHeight > 0.dp) Modifier.heightIn(min = minHeight) else Modifier)
        )
    }
}

/**
 * Composable LÁ chỉ theo dõi chiều cao IME; gọi [onHidden] khi bàn phím đã ẩn HẲN (height = 0). Dùng để
 * mở bottom sheet SAU khi bàn phím đóng xong, tránh 2 hoạt ảnh ngược chiều gây giật (theo TemplateEditorScreen).
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

package com.antimobile.callhs.ui.contacts

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import com.antimobile.callhs.ui.category.AvatarCategoryBadges
import com.antimobile.callhs.ui.category.CategoryIconDisc
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Message
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CopyAll
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.MailOutline
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.antimobile.callhs.R
import com.antimobile.callhs.data.contacts.Contact
import com.antimobile.callhs.data.contacts.ContactIndex
import com.antimobile.callhs.data.contacts.ContactListing
import com.antimobile.callhs.data.contacts.ContactPhone
import com.antimobile.callhs.data.contacts.ContactPhoneType
import com.antimobile.callhs.data.contacts.ContactRowItem
import com.antimobile.callhs.data.contacts.ContactSearch
import com.antimobile.callhs.data.model.CallNumberDetail
import com.antimobile.callhs.i18n.appStrings
import com.antimobile.callhs.ui.calldetail.MessageTemplateSheet
import com.antimobile.callhs.ui.calldetail.QrScannerOverlay
import com.antimobile.callhs.ui.calldetail.ShareContactSheet
import com.antimobile.callhs.ui.calldetail.TemplateEditorScreen
import com.antimobile.callhs.ui.components.ActionGlyph
import com.antimobile.callhs.ui.components.AppBottomSheet
import com.antimobile.callhs.ui.components.ContextAction
import com.antimobile.callhs.ui.components.ContextMenuOverlay
import com.antimobile.callhs.ui.components.LoadingState
import com.antimobile.callhs.ui.components.PanelCard
import com.antimobile.callhs.ui.components.QrActionSheet
import com.antimobile.callhs.ui.components.SearchEntry
import com.antimobile.callhs.ui.components.SearchHistoryView
import com.antimobile.callhs.ui.components.loadSearchHistory
import com.antimobile.callhs.ui.components.pushSearch
import com.antimobile.callhs.ui.components.rememberContactPhoto
import com.antimobile.callhs.ui.components.rememberPressHighlight
import com.antimobile.callhs.ui.components.saveSearchHistory
import com.antimobile.callhs.ui.theme.AccentBlue
import com.antimobile.callhs.ui.theme.AccentRed
import com.antimobile.callhs.ui.theme.AccentGreenBg
import com.antimobile.callhs.ui.theme.AppBackground
import com.antimobile.callhs.ui.theme.CardFill
import com.antimobile.callhs.ui.theme.CardShadow
import com.antimobile.callhs.ui.theme.CardSurface
import com.antimobile.callhs.ui.theme.FieldSurface
import com.antimobile.callhs.ui.theme.Primary
import com.antimobile.callhs.ui.theme.TextPrimary
import com.antimobile.callhs.ui.theme.TextSecondary
import com.antimobile.callhs.data.local.AddMemberResult
import com.antimobile.callhs.data.local.CategoryCatalog
import com.antimobile.callhs.data.local.CategoryRepository
import com.antimobile.callhs.ui.category.AddToCategorySheet
import com.antimobile.callhs.ui.category.categoryLabel
import com.antimobile.callhs.util.CallActions
import com.antimobile.callhs.util.PhoneKey
import com.antimobile.callhs.util.PhoneActions
import com.antimobile.callhs.util.ContactActions
import com.antimobile.callhs.util.MessageTemplate
import com.antimobile.callhs.util.QrContent
import com.antimobile.callhs.util.SimInfo
import com.antimobile.callhs.util.SliderTick
import com.antimobile.callhs.util.TemplateContext
import com.antimobile.callhs.util.TemplateFill
import com.antimobile.callhs.util.formatPhone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

/** Đích của màn soạn mẫu tin nhắn: tạo mới hoặc sửa một mẫu có sẵn (giống CallDetailScreen). */
private sealed interface TemplateEditorTarget {
    data object Create : TemplateEditorTarget
    data class Edit(val template: MessageTemplate) : TemplateEditorTarget
}

/** Liên hệ đang mở context-menu (nhấn giữ) + KHUNG thẻ (toạ độ cửa sổ, px) để dựng lớp phủ đúng vị trí. */
private data class ContactContextTarget(val contact: Contact, val bounds: Rect)

/**
 * Màn DANH BẠ: tìm kiếm toàn văn (tên/số/email) + DANH SÁCH hàng ngang, mỗi liên hệ là MỘT thẻ nền XANH LÁ
 * chủ đạo, bo góc + đổ bóng nhẹ. Chạm thẻ → bottom sheet chi tiết (có tiêu đề); "Xem cuộc gọi" của một số
 * điều hướng sang màn nhật ký của số đó ([onOpenNumber]) — KHÔNG tạo màn chi tiết mới.
 */
@Composable
fun ContactsScreen(
    vm: ContactsViewModel,
    onBack: () -> Unit,
    onOpenNumber: (String) -> Unit,
    onCreateCategory: () -> Unit
) {
    LaunchedEffect(Unit) { if (!vm.loaded) vm.load() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { vm.refresh() }

    val s = appStrings()
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Contact?>(null) }
    // Số đang mở sheet "thêm vào nhóm phân loại" (chồng lên sheet thông tin liên hệ).
    var addCategoryNumber by remember { mutableStateOf<String?>(null) }

    // ---- Trạng thái luồng "Gửi tin nhắn theo mẫu" (sao chép nguyên cơ chế từ CallDetailScreen) ----
    // Số đích của mẫu = số CHÍNH của liên hệ; GIỮ suốt luồng: chọn mẫu → quét QR → soạn → gửi.
    var templateNumber by remember { mutableStateOf<String?>(null) }
    var showTemplateSheet by remember { mutableStateOf(false) }
    // Mẫu đang chờ quét QR: != null → mở màn quét; quét xong điền {contextqr} rồi gửi.
    var pendingScan by remember { mutableStateOf<MessageTemplate?>(null) }
    // Mã QR CÓ HÀNH ĐỘNG (link/tel/sms/email) vừa quét: != null → mở sheet xử lý thay vì chèn vào mẫu.
    var qrAction by remember { mutableStateOf<QrContent?>(null) }
    // Màn soạn mẫu (tạo/sửa): != null → phủ toàn màn; xong → mở lại danh sách mẫu.
    var editorTarget by remember { mutableStateOf<TemplateEditorTarget?>(null) }
    // Liên hệ đang mở sheet "Chia sẻ liên hệ".
    var shareContact by remember { mutableStateOf<Contact?>(null) }

    // ---- Context-menu (NHẤN GIỮ item) hiện 2 nút Sửa / Xoá — dựng ở GỐC (ngoài inset) để nền tối phủ trọn màn.
    val density = LocalDensity.current
    val statusBarPx = WindowInsets.statusBars.getTop(density)
    val navBarPx = WindowInsets.navigationBars.getBottom(density)
    var contextTarget by remember { mutableStateOf<ContactContextTarget?>(null) }

    val filtered = remember(vm.contacts, query) { ContactSearch.filter(vm.contacts, query) }
    // Danh bạ ĐÃ nhóm A→Z (chỉ dùng khi KHÔNG tìm kiếm) + trạng thái cuộn cho thanh mục lục.
    val listing = remember(vm.contacts) { ContactIndex.build(vm.contacts) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    // ---- Tìm kiếm nâng cao: focus (ẩn bàn phím khi chạm ngoài / phím ✓) + LỊCH SỬ tìm kiếm (tối đa 5) ----
    val context = LocalContext.current
    val categoryRepo = remember { CategoryRepository(context) }
    // Lịch sử tìm kiếm của DANH BẠ dùng file prefs RIÊNG → không lẫn với lịch sử ở nhật ký cuộc gọi.
    val prefs = remember { context.getSharedPreferences("contacts_prefs", Context.MODE_PRIVATE) }
    val focusManager = LocalFocusManager.current
    var searchFocused by remember { mutableStateOf(false) }
    var searchHistory by remember { mutableStateOf(prefs.loadSearchHistory()) }
    val commitSearch: (String) -> Unit = { q ->
        val updated = pushSearch(searchHistory, q, System.currentTimeMillis())
        searchHistory = updated
        prefs.saveSearchHistory(updated)
    }
    val deleteSearch: (SearchEntry) -> Unit = { entry ->
        val updated = searchHistory.filterNot { it.query == entry.query }
        searchHistory = updated
        prefs.saveSearchHistory(updated)
    }
    // Phím ✓/Tìm trên bàn phím → NHẢ FOCUS hẳn (như chạm ngoài) + lưu lịch sử nếu có từ khoá.
    val onImeSearch: () -> Unit = {
        if (query.isNotBlank()) commitSearch(query)
        focusManager.clearFocus()
    }
    // Điền pattern trong nội dung mẫu rồi mở app nhắn tin với SỐ + nội dung soạn sẵn (giống CallDetailScreen).
    fun sendTemplate(number: String, template: MessageTemplate, qrText: String) {
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

    // Đang focus ô tìm → nút BACK chỉ NHẢ FOCUS (ẩn lịch sử/bàn phím) thay vì thoát màn.
    BackHandler(enabled = searchFocused) { focusManager.clearFocus() }

    // Bọc Box để các lớp phủ FULL-MÀN (soạn mẫu / quét QR) đè lên trên danh sách.
    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .statusBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, s.common.back, tint = TextPrimary, modifier = Modifier.size(23.dp))
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(s.common.contacts, style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
                if (vm.hasContactsPermission && vm.loaded) {
                    Text(
                        text = if (query.isBlank()) s.contacts.count(vm.contacts.size) else s.contacts.countFiltered(filtered.size, vm.contacts.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            // Nút TẠO liên hệ mới — mở màn tạo danh bạ của ứng dụng mặc định (không cần quyền ghi).
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).clickable { ContactActions.createContact(context) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.PersonAdd, s.contacts.createContact, tint = Primary, modifier = Modifier.size(24.dp))
            }
        }

        if (vm.hasContactsPermission) {
            ContactSearchBar(
                query = query,
                onQueryChange = { query = it },
                onFocusChanged = { searchFocused = it },
                onImeSearch = onImeSearch
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                // Chạm ra vùng trống (ngoài ô nhập) → ẩn bàn phím + nhả focus; item con vẫn nhận chạm.
                .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
        ) {
            when {
                !vm.hasContactsPermission -> ContactsPermissionGate(onGranted = { vm.refresh() })

                // Focus vào ô tìm & CHƯA gõ gì → hiện LỊCH SỬ tìm kiếm gần đây (tối đa 5).
                searchFocused && query.isBlank() -> SearchHistoryView(
                    history = searchHistory,
                    onTap = { entry -> query = entry.query; commitSearch(entry.query); focusManager.clearFocus() },
                    onDelete = deleteSearch,
                    onClearAll = { searchHistory = emptyList(); prefs.saveSearchHistory(emptyList()) }
                )

                vm.loading && !vm.loaded -> LoadingState(text = s.contacts.loading)

                vm.contacts.isEmpty() -> CenterMessage(s.contacts.emptyNoContacts)

                filtered.isEmpty() -> CenterMessage(s.contacts.emptyNoResults)

                else -> {
                    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                    if (query.isNotBlank()) {
                        // Đang tìm kiếm → danh sách PHẲNG (không mục lục A–Z).
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(top = 4.dp, bottom = navBottom + 24.dp)
                        ) {
                            items(filtered, key = { "c_${it.id}" }) { contact ->
                                ContactRow(
                                    contact = contact,
                                    onOpen = {
                                        // Mở một kết quả tìm kiếm → lưu từ khoá vào lịch sử.
                                        if (query.isNotBlank()) commitSearch(query)
                                        selected = contact
                                    },
                                    onLongPress = { bounds -> if (contact.lookupKey != null) contextTarget = ContactContextTarget(contact, bounds) },
                                    activeInMenu = contextTarget?.contact?.id == contact.id
                                )
                            }
                        }
                    } else {
                        // Duyệt A→Z: danh sách theo nhóm chữ cái + thanh mục lục kéo trượt (phóng to như bánh răng).
                        Box(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                // Item chiếm FULL chiều rộng; thanh mục lục nổi ĐÈ lên trên (không chừa chỗ end nữa).
                                contentPadding = PaddingValues(top = 4.dp, bottom = navBottom + 24.dp)
                            ) {
                                items(listing.rows, key = { rowKey(it) }) { row ->
                                    when (row) {
                                        is ContactRowItem.Header -> SectionLetterHeader(row.letter)
                                        is ContactRowItem.Item -> ContactRow(
                                            contact = row.contact,
                                            onOpen = { selected = row.contact },
                                            onLongPress = { bounds -> if (row.contact.lookupKey != null) contextTarget = ContactContextTarget(row.contact, bounds) },
                                            activeInMenu = contextTarget?.contact?.id == row.contact.id
                                        )
                                    }
                                }
                            }
                            if (listing.letters.size > 1) {
                                // Chữ cái đang xem — LUÔN đồng bộ với vị trí cuộn, kể cả ở đoạn CUỐI:
                                //  • Chạm đáy tuyệt đối → tô chữ CUỐI (Z/#) vì mục cuối không thể trồi lên đầu khung.
                                //  • Đang trong màn CUỐI (mục cuối đã hiện) → lấy chữ của mục ở GIỮA khung nên chữ
                                //    chạy MƯỢT U→V→…→Z thay vì kẹt ở "T" rồi nhảy phựt sang "Z".
                                //  • Còn lại → theo mục ở ĐẦU khung (trực giác "đang ở nhóm nào").
                                val currentLetter by remember(listing) {
                                    derivedStateOf {
                                        val info = listState.layoutInfo
                                        val vis = info.visibleItemsInfo
                                        when {
                                            vis.isEmpty() -> ContactIndex.currentLetter(0, listing)
                                            listState.canScrollBackward && !listState.canScrollForward ->
                                                listing.letters.lastOrNull()
                                            listState.canScrollBackward && vis.last().index >= listing.rows.lastIndex -> {
                                                val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
                                                val ref = vis.firstOrNull { center >= it.offset && center < it.offset + it.size } ?: vis.last()
                                                ContactIndex.currentLetter(ref.index, listing)
                                            }
                                            else -> ContactIndex.currentLetter(listState.firstVisibleItemIndex, listing)
                                        }
                                    }
                                }
                                AlphabetIndexBar(
                                    letters = listing.letters,
                                    currentLetter = currentLetter,
                                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp, top = 6.dp, bottom = 6.dp),
                                    onFocusLetter = { letter ->
                                        listing.letterToRowIndex[letter]?.let { idx ->
                                            scope.launch { listState.scrollToItem(idx) }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

        // Lớp phủ FULL-MÀN: màn TẠO/SỬA mẫu — xong (lưu hoặc quay lại) mở lại danh sách mẫu.
        if (editorTarget != null) {
            TemplateEditorScreen(
                initial = (editorTarget as? TemplateEditorTarget.Edit)?.template,
                onExit = { editorTarget = null; showTemplateSheet = true }
            )
        }

        // Lớp phủ FULL-MÀN: màn QUÉT QR — mã có hành động (link/tel/sms/email) thì mở sheet xử lý;
        // văn bản thuần thì điền {contextqr} rồi gửi mẫu.
        if (pendingScan != null) {
            QrScannerOverlay(
                onResult = { qr ->
                    val content = QrContent.parse(qr)
                    val template = pendingScan
                    pendingScan = null
                    if (content.isActionable) {
                        qrAction = content
                    } else {
                        val number = templateNumber
                        if (template != null && number != null) sendTemplate(number, template, content.raw)
                    }
                },
                onDismiss = { pendingScan = null }
            )
        }

        // Lớp phủ CONTEXT-MENU (nhấn giữ liên hệ) — nằm trong Box GỐC nên nền tối phủ TRỌN màn (kể cả 2 thanh
        // hệ thống). 2 nút Sửa / Xoá đều chuyển sang Danh bạ mặc định để thao tác.
        contextTarget?.let { tgt ->
            val cd = appStrings().contacts
            ContextMenuOverlay(
                bounds = tgt.bounds,
                actions = listOf(
                    // Sao chép SỐ (số chính) — toast "Đã sao chép số".
                    ContextAction(ActionGlyph.Vector(Icons.Rounded.ContentCopy, TextSecondary), appStrings().callDetail.copyNumber) {
                        tgt.contact.primaryPhone?.let { CallActions.copy(context, it.number) }
                    },
                    // Sao chép CẢ thông tin (tên + mọi số + mọi email) — toast trung tính.
                    ContextAction(ActionGlyph.Vector(Icons.Rounded.CopyAll, Primary), cd.copyContactInfo) {
                        CallActions.copyContent(context, tgt.contact.copyInfoText())
                    },
                    ContextAction(ActionGlyph.Vector(Icons.Rounded.Edit, AccentBlue), cd.editContact) {
                        tgt.contact.lookupUri()?.let { ContactActions.editContact(context, it) }
                    },
                    ContextAction(ActionGlyph.Vector(Icons.Rounded.DeleteOutline, AccentRed), cd.deleteContact) {
                        tgt.contact.lookupUri()?.let { ContactActions.deleteContact(context, it) }
                    },
                ),
                topInsetPx = statusBarPx,
                bottomInsetPx = navBarPx,
                onClosed = { contextTarget = null },
                lifted = { ContactRowCard(contact = tgt.contact) },
            )
        }
    }

    selected?.let { contact ->
        ContactDetailSheet(
            contact = contact,
            onOpenNumber = onOpenNumber,
            onAddToCategory = { number -> addCategoryNumber = number },
            onCreateCategory = { selected = null; onCreateCategory() },
            // Gửi mẫu / chia sẻ dùng số CHÍNH — đóng sheet liên hệ trước (lớp phủ soạn mẫu không được nằm sau sheet modal).
            onSendTemplate = {
                contact.primaryPhone?.let { p ->
                    selected = null
                    templateNumber = p.number
                    showTemplateSheet = true
                }
            },
            onShareContact = { selected = null; shareContact = contact },
            onDismiss = { selected = null }
        )
    }

    // Sheet "Thêm vào nhóm" — CHỒNG lên sheet thông tin liên hệ; đóng thì quay lại sheet liên hệ.
    addCategoryNumber?.let { number ->
        AddToCategorySheet(
            number = number,
            onDismiss = { addCategoryNumber = null },
            onToggle = { cat, desired ->
                scope.launch {
                    val label = categoryLabel(cat)
                    if (desired) {
                        when (categoryRepo.addMember(cat.id, number)) {
                            AddMemberResult.ADDED -> CallActions.toast(context, s.category.addedTo(label))
                            AddMemberResult.FULL -> CallActions.toast(context, s.category.maxMembers)
                            AddMemberResult.ALREADY -> CallActions.toast(context, s.category.alreadyAdded)
                            AddMemberResult.INVALID -> {}
                        }
                    } else {
                        categoryRepo.removeMember(cat.id, PhoneKey.of(number))
                        CallActions.toast(context, s.category.removedFrom(label))
                    }
                }
            },
            onCreateNew = { addCategoryNumber = null; selected = null; onCreateCategory() }
        )
    }

    // Sheet "Gửi tin nhắn theo mẫu" — chọn mẫu để gửi / quét QR / tạo / sửa (tái dùng nguyên từ CallDetailScreen).
    if (showTemplateSheet) {
        val number = templateNumber
        if (number != null) {
            MessageTemplateSheet(
                onDismiss = { showTemplateSheet = false },
                onSend = { template -> sendTemplate(number, template, "") },
                onScan = { template -> pendingScan = template },
                onCreate = { editorTarget = TemplateEditorTarget.Create },
                onEdit = { template -> editorTarget = TemplateEditorTarget.Edit(template) }
            )
        }
    }

    // Sheet "Chia sẻ liên hệ" — dựng CallNumberDetail tối giản từ liên hệ (số CHÍNH) để tái dùng nguyên sheet.
    shareContact?.let { contact ->
        ShareContactSheet(
            detail = contact.toShareDetail(),
            onDismiss = { shareContact = null }
        )
    }

    // Sheet XỬ LÝ mã QR có hành động (link/tel/sms/email) — hiện nội dung + nút mở tương ứng + sao chép.
    qrAction?.let { content ->
        QrActionSheet(content = content, onDismiss = { qrAction = null })
    }
}

// ---------------------------------------------------------------------------------------------------
// Thẻ liên hệ — HÀNG NGANG, nền TRẮNG (như nhật ký), bo góc + đổ bóng nhẹ; chỉ AVATAR xanh lá.
// ---------------------------------------------------------------------------------------------------

@Composable
private fun ContactRow(
    contact: Contact,
    onOpen: () -> Unit,
    onLongPress: (Rect) -> Unit,
    activeInMenu: Boolean = false
) {
    // Giữ toạ độ thẻ ngoài snapshot state (không recompose mỗi frame khi cuộn) — đọc lúc nhấn giữ.
    val cardCoords = remember { ContactCoordsHolder() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 5.dp)
            .onGloballyPositioned { cardCoords.value = it }
            // Item đang mở menu → ẩn HẲN bản gốc (giữ chỗ) để chỉ thấy bản sao nổi trên overlay.
            .graphicsLayer { alpha = if (activeInMenu) 0f else 1f }
    ) {
        ContactRowCard(
            contact = contact,
            onClick = onOpen,
            onLongClick = {
                // Chốt khung thẻ hiện tại (toạ độ cửa sổ) rồi báo lên màn để dựng context-menu.
                val bounds = cardCoords.value?.takeIf { it.isAttached }?.boundsInWindow()
                if (bounds != null) onLongPress(bounds)
            }
        )
    }
}

/**
 * Phần THẺ hiển thị của một liên hệ — tách riêng để context-menu (nhấn giữ) tái dùng bản sao Y HỆT khi
 * "nhấc" thẻ lên nền tối. [onClick]/[onLongClick] = null (bản sao ở overlay) → thẻ chỉ hiển thị, không nhận chạm.
 */
@Composable
private fun ContactRowCard(
    contact: Contact,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    PanelCard(modifier = Modifier.fillMaxWidth(), radius = 16.dp) {
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
        Row(
            modifier = rowModifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar XANH LÁ chủ đạo, chữ cái trắng (ảnh thì hiện ảnh) — đồng bộ nhật ký cuộc gọi.
            Box(modifier = Modifier.size(46.dp)) {
                ContactAvatar(contact.displayName, contact.photoUri, 46.dp, circleColor = Primary, textColor = Color.White)
                contact.primaryPhone?.let {
                    AvatarCategoryBadges(it.number, modifier = Modifier.align(Alignment.BottomEnd))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.displayNameOrNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 2,               // tên dài xuống dòng (tối đa 2 dòng)
                    overflow = TextOverflow.Ellipsis
                )
                contact.primaryPhone?.let { phone ->
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = formatPhone(phone.number),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val hasChips = contact.primaryPhone?.carrier != null || contact.phones.size > 1
                if (hasChips) {
                    Spacer(Modifier.height(7.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        contact.primaryPhone?.carrier?.let { GreenChip(it) }
                        if (contact.phones.size > 1) GrayChip(appStrings().contacts.morePhones(contact.phones.size - 1))
                    }
                }
            }
        }
    }
}

/** Giữ [LayoutCoordinates] của thẻ ngoài snapshot state — đọc lúc nhấn giữ để lấy khung item hiện tại. */
private class ContactCoordsHolder(var value: LayoutCoordinates? = null)

/** Chip nhỏ tone XÁM trung tính (nền CardFill, chữ xám). */
@Composable
private fun GrayChip(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = TextSecondary,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        modifier = Modifier.clip(CircleShape).background(CardFill).padding(horizontal = 10.dp, vertical = 3.dp)
    )
}

/** Avatar liên hệ: ảnh (nếu có) hoặc VÒNG [circleColor] + chữ cái đầu màu [textColor]. */
@Composable
private fun ContactAvatar(name: String, photoUri: String?, size: Dp, circleColor: Color, textColor: Color) {
    val photo = rememberContactPhoto(photoUri).bitmap
    when {
        photo != null -> Image(
            bitmap = photo,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(size).clip(CircleShape)
        )
        else -> Box(
            modifier = Modifier.size(size).clip(CircleShape).background(circleColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials(name),
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.38f).sp
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------------
// Bottom sheet chi tiết liên hệ (có TIÊU ĐỀ)
// ---------------------------------------------------------------------------------------------------

@Composable
private fun ContactDetailSheet(
    contact: Contact,
    onOpenNumber: (String) -> Unit,
    onAddToCategory: (String) -> Unit,
    onCreateCategory: () -> Unit,
    onSendTemplate: () -> Unit,
    onShareContact: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val s = appStrings()
    AppBottomSheet(
        onDismiss = onDismiss,
        title = s.contacts.sheetTitle,
        showCloseButton = true,
        // Thanh nhóm GHIM CỨNG trên sheet (di chuyển theo sheet) — icon các nhóm liên hệ đang thuộc + nút tạo nhóm.
        pinnedHeader = { ContactCategoryBar(contact = contact, onCreateCategory = onCreateCategory) },
    ) { close ->
        // Header: avatar (vòng xanh, chữ trắng) + tên/tổ chức.
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ContactAvatar(contact.displayName, contact.photoUri, 58.dp, circleColor = Primary, textColor = Color.White)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    contact.displayNameOrNumber,
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                contact.organization?.let {
                    Spacer(Modifier.height(2.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }

        SheetSectionLabel(s.contacts.phonesSection)
        contact.phones.forEach { phone ->
            PhoneRow(
                phone = phone,
                onOpenCallLog = { onOpenNumber(phone.number); close() },
                onCall = { CallActions.dial(context, phone.number) },
                onMessage = { CallActions.message(context, phone.number) },
                onAddCategory = { onAddToCategory(phone.number) }
            )
        }

        if (contact.emails.isNotEmpty()) {
            SheetSectionLabel("Email")
            contact.emails.forEach { EmailRow(it) }
        }

        // Các hành động cấp liên hệ (dùng số CHÍNH) — cùng tính năng như màn chi tiết cuộc gọi.
        ContactActionRow(Icons.Rounded.Sms, s.callDetail.sendTemplate, onSendTemplate)
        ContactActionRow(Icons.Rounded.Share, s.callDetail.shareContact, onShareContact)
        contact.primaryPhone?.let { phone ->
            ContactActionRow(R.drawable.ic_zalo, s.callDetail.searchZalo) { PhoneActions.openZalo(context, phone.number) }
            ContactActionRow(R.drawable.ic_google, s.callDetail.searchGoogle) { PhoneActions.searchNumberOnWeb(context, phone.number) }
            ContactActionRow(Icons.Rounded.ContentCopy, s.callDetail.copyNumber) { CallActions.copy(context, phone.number) }
        }

        contact.lookupKey?.let { key ->
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val uri = ContactsContract.Contacts.getLookupUri(contact.id, key)
                        if (uri != null) ContactActions.viewContact(context, uri.toString())
                    }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, tint = Primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(14.dp))
                Text(s.contacts.openInContactsApp, style = MaterialTheme.typography.bodyLarge, color = Primary, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/**
 * Thanh nhóm phân loại của liên hệ — GỘP các nhóm của MỌI số (khử trùng) hiển thị icon, + nút TẠO nhóm mới.
 * Đặt ở slot GHIM của [AppBottomSheet] nên cố định trên sheet và di chuyển theo sheet (như thanh nhóm ở CallDetail).
 */
@Composable
private fun ContactCategoryBar(contact: Contact, onCreateCategory: () -> Unit) {
    val s = appStrings().category
    val badges = contact.phones.flatMap { CategoryCatalog.badgesFor(it.number) }.distinctBy { it.categoryId }
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 2.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (badges.isEmpty()) {
            Text(
                s.noCategories,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.weight(1f),
            )
        } else {
            Row(
                modifier = Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                badges.forEach { b -> CategoryIconDisc(b.iconKey, b.colorArgb, size = 34.dp) }
            }
        }
        Spacer(Modifier.width(8.dp))
        SheetActionButton(Icons.Rounded.CreateNewFolder, s.newCategory, onCreateCategory)
    }
}

@Composable
private fun SheetSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp)
    )
}

/** Nhãn LOẠI số theo ngôn ngữ hiện hành (ánh xạ [ContactPhoneType] → chuỗi để i18n không phụ thuộc tầng data). */
private fun phoneTypeLabel(phone: ContactPhone): String = with(appStrings().contacts) {
    when (phone.type) {
        ContactPhoneType.MOBILE -> phoneTypeMobile
        ContactPhoneType.HOME -> phoneTypeHome
        ContactPhoneType.WORK -> phoneTypeWork
        ContactPhoneType.MAIN -> phoneTypeMain
        ContactPhoneType.WORK_MOBILE -> phoneTypeWorkMobile
        ContactPhoneType.FAX -> phoneTypeFax
        ContactPhoneType.PAGER -> phoneTypePager
        ContactPhoneType.CUSTOM -> phone.customLabel?.takeIf { it.isNotBlank() } ?: phoneTypeOther
        ContactPhoneType.OTHER -> phoneTypeOther
    }
}

@Composable
private fun PhoneRow(
    phone: ContactPhone,
    onOpenCallLog: () -> Unit,
    onCall: () -> Unit,
    onMessage: () -> Unit,
    onAddCategory: () -> Unit,
) {
    val s = appStrings()
    // Nhóm phân loại CỦA RIÊNG số này (badge hiển thị dưới số).
    val badges = CategoryCatalog.badgesFor(phone.number)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thân hàng: chạm → xem NHẬT KÝ CUỘC GỌI của số này.
        Row(
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).clickable(onClick = onOpenCallLog).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(formatPhone(phone.number), style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(phoneTypeLabel(phone), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    phone.carrier?.let {
                        Spacer(Modifier.width(8.dp))
                        GreenChip(it)
                    }
                }
                if (badges.isNotEmpty()) {
                    Spacer(Modifier.height(5.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        badges.take(5).forEach { b -> CategoryIconDisc(b.iconKey, b.colorArgb, size = 22.dp) }
                    }
                }
            }
        }
        // Nút THÊM riêng số này vào nhóm.
        SheetActionButton(Icons.Rounded.Add, s.category.addToCategory, onAddCategory)
        Spacer(Modifier.width(4.dp))
        SheetActionButton(Icons.AutoMirrored.Rounded.Message, s.callDetail.message, onMessage)
        Spacer(Modifier.width(4.dp))
        SheetActionButton(Icons.Rounded.Phone, s.callDetail.call, onCall)
    }
}

/** Chip xanh nhạt (nền BrandSoft/AccentGreenBg, chữ xanh) cho tone sáng trong bottom sheet. */
@Composable
private fun GreenChip(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = Primary,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = Modifier.clip(CircleShape).background(AccentGreenBg).padding(horizontal = 10.dp, vertical = 3.dp)
    )
}

@Composable
private fun SheetActionButton(icon: ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(42.dp).clip(CircleShape).background(AccentGreenBg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, desc, tint = Primary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun EmailRow(email: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.MailOutline, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(email, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Dòng hành động trong sheet danh bạ: icon xanh + nhãn tối; cả dòng bấm được (đồng bộ dòng "Mở trong Danh bạ"). */
@Composable
private fun ContactActionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = Primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}

/** Biến thể dùng LOGO nhiều màu từ drawable (Zalo/Google): vẽ NGUYÊN BẢN, KHÔNG tô tint; chỉ nhãn tối. */
@Composable
private fun ContactActionRow(@DrawableRes iconRes: Int, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.Medium)
    }
}

// ---------------------------------------------------------------------------------------------------
// Thanh tìm kiếm + trạng thái
// ---------------------------------------------------------------------------------------------------

@Composable
private fun ContactSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onImeSearch: () -> Unit
) {
    val s = appStrings()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)
            .height(46.dp)
            .clip(CircleShape)
            .background(FieldSurface)
            .padding(start = 14.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(s.contacts.searchHint, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                cursorBrush = SolidColor(Primary),
                // Phím ✓ = biểu tượng TÌM; nhấn → nhả focus hẳn (con trỏ tắt) như chạm ngoài.
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onImeSearch() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { onFocusChanged(it.isFocused) }
            )
        }
        if (query.isNotEmpty()) {
            Box(
                modifier = Modifier.size(30.dp).clip(CircleShape).clickable { onQueryChange("") },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Close, s.callList.delete, tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun CenterMessage(text: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Rounded.Contacts, null, tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
    }
}

/** Màn mời cấp quyền Danh bạ — chưa cho phép thì không xem được danh sách. */
@Composable
private fun ContactsPermissionGate(onGranted: () -> Unit) {
    val context = LocalContext.current
    val s = appStrings()
    // rememberSaveable → giữ đúng nhãn "Mở Cài đặt" khi xoay màn / tái tạo tiến trình (như CallListScreen).
    var permanentlyDenied by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            onGranted()
        } else {
            val activity = context.findActivity()
            permanentlyDenied = activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_CONTACTS)
        }
    }
    val request: () -> Unit = {
        if (permanentlyDenied) {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        } else {
            launcher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.size(96.dp).clip(CircleShape).background(AccentGreenBg), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Person, null, tint = Primary, modifier = Modifier.size(46.dp))
        }
        Spacer(Modifier.height(22.dp))
        Text(s.contacts.permTitle, style = MaterialTheme.typography.headlineSmall, color = TextPrimary, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            s.contacts.permBody,
            style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier.clip(CircleShape).background(Primary).clickable(onClick = request).padding(horizontal = 28.dp, vertical = 13.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (permanentlyDenied) s.common.openSettings else s.common.allowAccess,
                color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ---------------------------------------------------------------------------------------------------
// Nhóm A→Z: tiêu đề chữ cái + thanh mục lục kéo trượt (phóng to như bánh răng)
// ---------------------------------------------------------------------------------------------------

private fun rowKey(row: ContactRowItem): String = when (row) {
    is ContactRowItem.Header -> "h_${row.letter}"
    is ContactRowItem.Item -> "c_${row.contact.id}"
}

/** Tiêu đề chữ cái nhóm (A, B, C…) — xanh lá, đậm. */
@Composable
private fun SectionLetterHeader(letter: String) {
    Text(
        text = letter,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = Primary,
        modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 2.dp)
    )
}

/**
 * Thanh MỤC LỤC A→Z dọc bên phải. Chạm & KÉO trượt: chữ cái gần ngón tay PHÓNG TO dần (như bánh răng),
 * chữ cái đang chọn to nhất + đậm + xanh + bong ra trái; danh sách cuộn theo tới nhóm đó, kèm rung nhẹ.
 * Nhả tay → hiệu ứng phóng to thu về mượt (animate). Khi KHÔNG kéo, chữ cái của nhóm đang xem được tô xanh.
 */
@Composable
private fun AlphabetIndexBar(
    letters: List<String>,
    currentLetter: String?,
    modifier: Modifier = Modifier,
    onFocusLetter: (String) -> Unit
) {
    if (letters.isEmpty()) return
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    // Âm "tạch" như lẫy bánh răng: mỗi lần trượt sang chữ mới phát một tiếng. Giữ theo vòng đời màn,
    // nhả tài nguyên audio khi rời (đồng hành với rung ở cùng điểm đổi chữ bên dưới).
    val context = LocalContext.current
    val tick = remember { SliderTick(context) }
    DisposableEffect(Unit) { onDispose { tick.release() } }
    val n = letters.size
    // Ô CAO lý tưởng cho mỗi chữ (đủ rộng để chạm trúng ngay); ÍT chữ → dải NGẮN, nằm GIỮA;
    // NHIỀU chữ → co ô lại cho vừa chiều cao (dải dài kín khung). → luôn cân đối theo số lượng.
    val preferredSlotPx = with(density) { 26.dp.toPx() }

    var fullHpx by remember { mutableStateOf(0f) }     // chiều cao VÙNG CHẠM (px) = cả cột dọc
    var dragging by remember { mutableStateOf(false) } // đang chạm/kéo?
    var lastTouchY by remember { mutableStateOf(0f) }  // vị trí ngón tay QUY VỀ trong DẢI (px, 0..stripHpx)
    var lastFocused by remember { mutableStateOf<String?>(null) }

    // 0 khi rảnh → 1 khi đang kéo; nhả tay animate về 0 để chữ thu nhỏ MƯỢT.
    val expansion by animateFloatAsState(
        targetValue = if (dragging) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "alphaIndexExpand"
    )

    // Hình học DẢI (đồng bộ giữa VẼ & CHẠM): ít chữ → dải NGẮN nằm GIỮA; nhiều → co ô cho kín khung.
    val slotPx = if (fullHpx > 0f) minOf(preferredSlotPx, fullHpx / n) else preferredSlotPx
    val stripHpx = slotPx * n
    val stripH = with(density) { stripHpx.toDp() }

    // VÙNG CHẠM = TOÀN chiều cao × 44dp (không chỉ phần dải chữ) → chạm bất kỳ đâu trên rãnh đều BẮT chữ ngay.
    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(44.dp)
            .onSizeChanged { fullHpx = it.height.toFloat() }
            .pointerInput(letters) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val pointerId = down.id // BÁM đúng ngón đã chạm (bỏ ngón thứ 2)
                        lastFocused = null      // reset bộ-phát-hiện-đổi-chữ cho lượt kéo mới
                        dragging = true
                        // Đọc hình học TỨC THỜI từ kích thước thật của vùng chạm → quy toạ độ chạm về trong DẢI.
                        val h = size.height.toFloat()
                        val slot = if (h > 0f) minOf(preferredSlotPx, h / n) else preferredSlotPx
                        val sHpx = slot * n
                        val top = ((h - sHpx) / 2f).coerceAtLeast(0f) // dải canh GIỮA vùng chạm
                        val focus: (Float) -> Unit = focus@{ rawY ->
                            if (slot <= 0f) return@focus
                            val local = (rawY - top).coerceIn(0f, sHpx) // trên dải→chữ đầu, dưới→chữ cuối
                            lastTouchY = local
                            val idx = (local / slot).toInt().coerceIn(0, n - 1)
                            val letter = letters[idx]
                            if (letter != lastFocused) {
                                lastFocused = letter
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                tick.tick() // tiếng "tạch" đi kèm rung, đúng mỗi nấc chữ cái
                                onFocusLetter(letter)
                            }
                        }
                        focus(down.position.y)  // BẮT chữ + cuộn NGAY khi vừa chạm (không ngưỡng, không chờ)
                        down.consume()
                        var pressed = true
                        while (pressed) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId }
                            if (change == null || !change.pressed) {
                                pressed = false
                            } else {
                                focus(change.position.y)
                                change.consume()
                            }
                        }
                        // Nhả tay: GIỮ lastFocused để bong bóng + chữ thu theo expansion (mượt), không tắt phựt.
                        dragging = false
                    }
                }
            }
    ) {
        if (fullHpx > 0f) {
            // DẢI chữ cao [stripH], canh GIỮA theo chiều dọc trong vùng chạm.
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .height(stripH)
                    .fillMaxWidth()
            ) {
                // Nền PILL MỜ (frosted) — LUÔN hiện, BO TRÒN hai đầu, nổi (đổ bóng nhẹ) và ĐÈ cố định lên danh
                // sách full-rộng bên dưới. Dùng CardSurface (theo chế độ): ở SÁNG ra pill gần-trắng như cũ, ở TỐI
                // ra pill TỐI mờ → chữ chỉ mục (TextSecondary/Primary) vẫn đọc rõ, KHÔNG còn thanh trắng chói.
                // Bán trong suốt nên vẫn THẤY nội dung phía sau; kéo tay → tô thêm chút tone xanh cho rõ đang thao tác.
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .shadow(elevation = 3.dp, shape = RoundedCornerShape(percent = 50), clip = false, ambientColor = CardShadow, spotColor = CardShadow)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(CardSurface.copy(alpha = 0.7f))
                        .background(Primary.copy(alpha = 0.06f * expansion))
                )
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    letters.forEachIndexed { i, letter ->
                        val center = (i + 0.5f) * slotPx
                        val dist = abs(lastTouchY - center)
                        val radius = slotPx * 3.5f
                        val prox = (1f - dist / radius).coerceIn(0f, 1f)
                        val bump = prox * prox                      // ease-in để đỉnh nhọn hơn
                        val scale = 1f + bump * 1.4f * expansion
                        val tx = -bump * 30f * expansion            // bong ra TRÁI về phía ngón tay
                        // Đang kéo → tô chữ đang BẮT (lastFocused); rảnh → tô chữ nhóm đang xem (currentLetter).
                        // Một điều kiện liền mạch → KHÔNG có "vùng chết" gây nháy khi nhả tay.
                        val selected = if (expansion > 0.02f) letter == lastFocused else letter == currentLetter
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = tx
                                    transformOrigin = TransformOrigin(1f, 0.5f)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = letter,
                                color = if (selected) Primary else TextSecondary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                // Bong bóng chữ cái LỚN nổi bên trái ngón tay khi đang kéo (tràn ra ngoài dải — không bị cắt).
                if (lastFocused != null && expansion > 0.02f) {
                    val bubble = 58.dp
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset {
                                val bpx = bubble.toPx()
                                IntOffset(
                                    x = -(bpx + 14.dp.toPx()).roundToInt(),
                                    // giữ bong bóng NẰM TRONG dải theo chiều dọc (không trồi lên che danh sách/ô tìm kiếm)
                                    y = (lastTouchY - bpx / 2f).coerceIn(0f, (stripHpx - bpx).coerceAtLeast(0f)).roundToInt()
                                )
                            }
                            .size(bubble)
                            .graphicsLayer {
                                alpha = expansion
                                val s = 0.7f + 0.3f * expansion
                                scaleX = s; scaleY = s
                                transformOrigin = TransformOrigin(1f, 0.5f)
                            }
                            .clip(CircleShape)
                            .background(Primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(lastFocused ?: "", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 26.sp)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------
// Tiện ích
// ---------------------------------------------------------------------------------------------------

/**
 * Dựng [CallNumberDetail] TỐI GIẢN từ một liên hệ (dùng số CHÍNH) để TÁI DÙNG nguyên [ShareContactSheet]:
 * sheet chỉ đọc tên/số/ảnh nên các trường lịch sử/thống kê để rỗng.
 */
/** Lookup URI (chuỗi) của liên hệ để mở/sửa/xoá trong Danh bạ mặc định; null nếu thiếu lookupKey. */
private fun Contact.lookupUri(): String? {
    val key = lookupKey ?: return null
    return ContactsContract.Contacts.getLookupUri(id, key)?.toString()
}

/** Khối văn bản để SAO CHÉP cả liên hệ: tên (nếu có) + MỌI số (định dạng dễ đọc) + MỌI email, mỗi thứ một dòng. */
private fun Contact.copyInfoText(): String = buildList {
    displayName.takeIf { it.isNotBlank() }?.let { add(it) }
    phones.forEach { add(formatPhone(it.number)) }
    emails.forEach { add(it) }
}.joinToString("\n")

private fun Contact.toShareDetail(): CallNumberDetail {
    val phone = primaryPhone
    return CallNumberDetail(
        number = phone?.number ?: "",
        cachedName = displayName.takeIf { it.isNotBlank() },
        photoUri = photoUri,
        geocodedLocation = null,
        carrier = phone?.carrier,
        simLabels = emptyList(),
        entries = emptyList(),
        totalCalls = 0,
        incomingCount = 0,
        outgoingCount = 0,
        missedCount = 0,
        totalDurationSeconds = 0L,
        contactLookupUri = null
    )
}

private fun initials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "#"
        parts.size == 1 -> parts[0].take(1).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}

private fun Context.findActivity(): Activity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

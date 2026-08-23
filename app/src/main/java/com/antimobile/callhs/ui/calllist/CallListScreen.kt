package com.antimobile.callhs.ui.calllist

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallMissed
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.EditCalendar
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.antimobile.callhs.data.local.Category
import com.antimobile.callhs.data.local.CategoryCatalog
import com.antimobile.callhs.data.agency.normalizeForSearch
import com.antimobile.callhs.data.model.CallEntry
import com.antimobile.callhs.ui.category.CategoryGlyph
import com.antimobile.callhs.ui.category.categoryGlyph
import com.antimobile.callhs.ui.category.categoryLabel
import com.antimobile.callhs.util.PhoneKey
import com.antimobile.callhs.data.model.CallType
import com.antimobile.callhs.i18n.LanguageSettings
import com.antimobile.callhs.i18n.appStrings
import com.antimobile.callhs.ui.components.AppBottomSheet
import com.antimobile.callhs.ui.components.ContactsPermissionBanner
import com.antimobile.callhs.ui.components.EmptyState
import com.antimobile.callhs.ui.components.FilterOptionRow
import com.antimobile.callhs.ui.components.LoadingState
import com.antimobile.callhs.ui.components.PermissionState
import com.antimobile.callhs.ui.components.QrScanFlow
import com.antimobile.callhs.ui.components.SearchEntry
import com.antimobile.callhs.ui.components.SearchHistoryView
import com.antimobile.callhs.ui.components.SectionHeader
import com.antimobile.callhs.ui.components.SimScopeStatusPill
import com.antimobile.callhs.ui.components.SimSegmentedToggle
import com.antimobile.callhs.ui.components.loadSearchHistory
import com.antimobile.callhs.ui.components.pushSearch
import com.antimobile.callhs.ui.components.rememberQrScanFlowState
import com.antimobile.callhs.ui.components.saveSearchHistory
import com.antimobile.callhs.ui.theme.AppBackground
import com.antimobile.callhs.ui.theme.BrandSoft
import com.antimobile.callhs.ui.theme.CardSurface
import com.antimobile.callhs.ui.theme.ChipBg
import com.antimobile.callhs.ui.theme.ChipSelectedBg
import com.antimobile.callhs.ui.theme.ChipSelectedText
import com.antimobile.callhs.ui.theme.ChipText
import com.antimobile.callhs.ui.theme.FieldSurface
import com.antimobile.callhs.ui.theme.LinkColor
import com.antimobile.callhs.ui.theme.Primary
import com.antimobile.callhs.ui.theme.ScrollTopScrim
import com.antimobile.callhs.ui.theme.ScrollTopSolid
import com.antimobile.callhs.ui.theme.TextPrimary
import com.antimobile.callhs.ui.theme.TextSecondary
import com.antimobile.callhs.util.CallActions
import com.antimobile.callhs.util.PhoneActions
import com.antimobile.callhs.util.SimScope
import com.antimobile.callhs.util.TimeFormat
import com.antimobile.callhs.util.hasPermission
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

internal enum class TypeFilter { ALL, MISSED, OUTGOING, INCOMING }
internal enum class DateFilter { NONE, TODAY, YESTERDAY, WEEK, MONTH, CUSTOM }

/** Cuộc gọi KẾT THÚC trong khoảng này (ms) so với hiện tại → item của nó LOÉ SÁNG báo "vừa mới". */
private const val RECENT_CALL_FLASH_MS = 40_000L
/** Dung sai lệch đồng hồ: mốc kết thúc "ở tương lai" trong khoảng này vẫn coi là vừa xong (không bỏ sót). */
private const val RECENT_CALL_SKEW_MS = 10_000L

/** Kiểu hiển thị danh sách: theo THỜI GIAN (mỗi cuộc 1 dòng, như cũ) hay theo SỐ (gộp mỗi số 1 dòng). */
internal enum class ViewMode { BY_TIME, BY_PHONE }

internal sealed interface Row2 {
    data class Header(val label: String) : Row2
    /** [missedStreak] > 0 chỉ ở chế độ theo SỐ (gộp) — số cuộc nhỡ liên tiếp cuối cùng; theo THỜI GIAN luôn 0. */
    data class Item(val entry: CallEntry, val missedStreak: Int) : Row2
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallListScreen(
    vm: CallListViewModel,
    onOpenNumber: (String) -> Unit,
    onOpenContacts: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenCreateCategory: () -> Unit
) {
    val context = LocalContext.current
    val s = appStrings()
    var hasPerm by remember {
        mutableStateOf(hasPermission(context, Manifest.permission.READ_CALL_LOG))
    }
    // Quyền đã bị TỪ CHỐI VĨNH VIỄN chưa — xác định trong callback SAU khi người dùng phản hồi.
    // Từ Android 11 (API 30) không còn ô "Không hỏi lại": lần từ chối thứ 2 là vĩnh viễn; khi đó
    // shouldShowRequestPermissionRationale = false VÀ permLauncher.launch() không hiện hộp thoại nữa
    // (trả về denied ngay) → nếu CTA vẫn chỉ gọi launch() thì bấm mãi không có gì xảy ra, kẹt cứng.
    var permPermanentlyDenied by rememberSaveable { mutableStateOf(false) }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPerm = granted
        if (granted) {
            permPermanentlyDenied = false
            vm.load()
        } else {
            val activity = context.findActivity()
            permPermanentlyDenied = activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_CALL_LOG)
        }
    }
    LaunchedEffect(hasPerm) { if (hasPerm && !vm.loaded) vm.load() }
    // Đổi PHẠM VI SIM toàn app (ở Cài đặt) → nạp lại danh sách để khớp SIM đang xem. Chỉ nạp khi phạm vi THẬT
    // SỰ đổi so với lần nạp trước (loadedScope lưu qua điều hướng) → không nạp thừa mỗi lần quay về màn.
    var loadedScope by rememberSaveable { mutableStateOf(SimScope.scope) }
    LaunchedEffect(SimScope.scope) {
        if (vm.loaded && SimScope.scope != loadedScope) {
            loadedScope = SimScope.scope
            if (hasPerm) vm.load()
        }
    }
    // Quyền DANH BẠ (TUỲ CHỌN): có thì hiện tên/ảnh liên hệ ĐÃ lưu; thiếu thì fallback CACHED_NAME.
    // KHÔNG tự bật hộp thoại — hiện BANNER mời cấp quyền để người dùng chủ động bấm.
    var contactsGranted by remember { mutableStateOf(hasPermission(context, Manifest.permission.READ_CONTACTS)) }
    var contactsPermanentlyDenied by rememberSaveable { mutableStateOf(false) }
    var contactsBannerDismissed by rememberSaveable { mutableStateOf(false) }
    val contactsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        contactsGranted = granted
        if (granted) {
            contactsPermanentlyDenied = false
            vm.load()   // nạp lại để bổ sung tên/ảnh danh bạ sống
        } else {
            val activity = context.findActivity()
            contactsPermanentlyDenied = activity != null &&
                !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_CONTACTS)
        }
    }
    val requestContactsPermission: () -> Unit = {
        if (contactsPermanentlyDenied) {
            runCatching {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }.onFailure { CallActions.toast(context, appStrings().common.appSettingsOpenFailed) }
        } else {
            contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }
    // Quay lại màn (mở app lại / vừa cấp quyền trong Cài đặt) → cập nhật lại trạng thái quyền.
    // Nếu vừa được cấp, LaunchedEffect(hasPerm) ở trên sẽ tự nạp dữ liệu.
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        hasPerm = hasPermission(context, Manifest.permission.READ_CALL_LOG)
        // Vừa bật quyền Danh bạ trong Cài đặt → cập nhật trạng thái + nạp lại để bổ sung tên liên hệ.
        val nowContacts = hasPermission(context, Manifest.permission.READ_CONTACTS)
        if (nowContacts && !contactsGranted) vm.load()
        contactsGranted = nowContacts
    }

    // Nút CTA: chưa hỏi / từ chối tạm → hiện hộp thoại hệ thống; đã từ chối vĩnh viễn → mở thẳng
    // trang Cài đặt app để người dùng tự cấp (tránh kẹt cứng). ON_START ở trên tự nạp lại khi quay về.
    val requestCallLogPermission: () -> Unit = {
        if (permPermanentlyDenied) {
            runCatching {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }.onFailure { CallActions.toast(context, appStrings().common.appSettingsOpenFailed) }
        } else {
            permLauncher.launch(Manifest.permission.READ_CALL_LOG)
        }
    }

    var query by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    // Bộ lọc + kiểu hiển thị được LƯU LẠI (SharedPreferences) → mở app lần sau giữ nguyên lựa chọn.
    val prefs = remember { context.getSharedPreferences("call_list_prefs", Context.MODE_PRIVATE) }
    var typeFilter by remember { mutableStateOf(prefs.loadEnum("type", TypeFilter.ALL)) }
    // Bộ lọc NGÀY + ngày cụ thể (DateFilter.CUSTOM, lưu theo EPOCH-DAY; 0 = chưa chọn) — giữ lại khi mở app lần sau.
    // Khung "3 tháng gần nhất" TRÔI theo thời gian thực: ngày đã lưu có thể rơi RA NGOÀI khung sau nhiều ngày
    // không mở app. Chuẩn hoá NGAY khi nạp → quá hạn thì HUỶ lọc (NONE/0) để nhãn chip, danh sách và lịch luôn
    // khớp nhau (tránh "Áp dụng" rỗng bị nhảy sang ngày biên ngoài ý muốn — xem DayFilterSheet).
    val (initialDateFilter, initialCustomDate) = remember {
        val f = prefs.loadEnum("date", DateFilter.NONE)
        val c = prefs.getLong("date_custom", 0L)
        if (f == DateFilter.CUSTOM) {
            val today = LocalDate.now(ZoneId.systemDefault())
            val inWindow = c > 0L &&
                LocalDate.ofEpochDay(c).let { !it.isBefore(today.minusMonths(3)) && !it.isAfter(today) }
            if (inWindow) f to c else DateFilter.NONE to 0L
        } else f to c
    }
    var dateFilter by remember { mutableStateOf(initialDateFilter) }
    var customDate by remember { mutableStateOf(initialCustomDate) }
    var viewMode by remember { mutableStateOf(prefs.loadEnum("view", ViewMode.BY_TIME)) }
    // Bộ lọc theo SIM: null = tất cả SIM, còn lại là NHÃN sim ("SIM 1"/"SIM 2"). Nhãn suy từ nhật ký
    // (CallEntry.simLabel, theo PHONE_ACCOUNT_ID) nên KHÔNG cần READ_PHONE_STATE và vẫn lọc được cuộc gọi
    // cũ của SIM ĐÃ THÁO. Chỉ đổi qua thanh lọc (chỉ hiện khi có ≥2 SIM) → giá trị lưu luôn hợp lệ lúc chọn.
    // LƯU Ý: nhãn SIM đánh theo THỨ TỰ tài khoản điện thoại (xem assignSimLabels), KHÔNG khoá cứng theo khe
    // vật lý. Nếu người dùng THAY SIM khiến thứ tự id đổi thì "SIM 2" đã lưu có thể trỏ sang SIM khác — chấp
    // nhận được (chỉ xem, bấm lại là đúng) và nhất quán với cách app dùng nhãn SIM ở mọi nơi (danh sách/chi tiết).
    var simFilter by remember { mutableStateOf(prefs.getString("sim", null)) }
    // Bộ lọc theo NHÓM PHÂN LOẠI: null = không lọc; id nhóm = chỉ hiện cuộc gọi của số trong nhóm. Lưu 0 = null.
    var categoryFilter by remember { mutableStateOf(prefs.getLong("category", 0L).takeIf { it > 0L }) }
    LaunchedEffect(categoryFilter) { prefs.edit().putLong("category", categoryFilter ?: 0L).apply() }
    LaunchedEffect(typeFilter) { prefs.edit().putString("type", typeFilter.name).apply() }
    LaunchedEffect(dateFilter) { prefs.edit().putString("date", dateFilter.name).apply() }
    LaunchedEffect(customDate) { prefs.edit().putLong("date_custom", customDate).apply() }
    LaunchedEffect(viewMode) { prefs.edit().putString("view", viewMode.name).apply() }
    LaunchedEffect(simFilter) { prefs.edit().putString("sim", simFilter).apply() }
    var showTypeSheet by remember { mutableStateOf(false) }
    var showDateSheet by remember { mutableStateOf(false) }
    // Nhóm phân loại (phản ứng): chip lọc + tập số để lọc danh sách. effectiveCategory bỏ lọc nếu nhóm đã bị xoá.
    val categories = CategoryCatalog.categories
    val effectiveCategory = categoryFilter?.takeIf { id -> categories.any { it.id == id } }
    val categoryNumbers = effectiveCategory?.let { CategoryCatalog.membersOf(it) }

    // ---- LOÉ SÁNG cuộc gọi VỪA XONG (chỉ ở màn danh sách này) ----
    // Khi nhận dữ liệu mới (mở app ngay sau khi vừa gọi, hoặc có cuộc mới lúc đang xem), nếu cuộc MỚI NHẤT
    // vừa kết thúc trong [RECENT_CALL_FLASH_MS] thì item của nó LOÉ nền xanh lá rồi tắt — báo "log này vừa mới".
    // Xét theo id nên mỗi cuộc mới nhất chỉ loé MỘT lần (nạp lại/đổi bộ lọc không loé lại). Mốc "kết thúc" =
    // dateMillis + thời lượng (cuộc dài vừa gác máy vẫn tính là mới); quá 40s thì không làm gì.
    // Đặt id cần loé; item tự giới hạn loé đúng MỘT lần (xem [ListCallItem]) nên KHÔNG cần hẹn giờ bỏ cờ. Nút
    // "mới nhất" = cuộc đầu danh sách (mới nhất theo giờ bắt đầu); mốc "vừa xong" tính theo giờ KẾT THÚC bên dưới.
    var flashEntryId by remember { mutableStateOf<Long?>(null) }
    var lastNewestId by rememberSaveable { mutableStateOf(0L) }
    LaunchedEffect(vm.entries) {
        val newest = vm.entries.firstOrNull() ?: return@LaunchedEffect
        if (newest.id == lastNewestId) return@LaunchedEffect
        lastNewestId = newest.id
        val endedAt = newest.dateMillis + newest.durationSeconds * 1000L
        val age = System.currentTimeMillis() - endedAt
        if (age <= RECENT_CALL_FLASH_MS && age >= -RECENT_CALL_SKEW_MS) flashEntryId = newest.id
    }

    // ---- Tìm kiếm: focus (ẩn bàn phím khi chạm ngoài) + lịch sử tìm kiếm (lưu tối đa 5) ----
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
    // Hoist trạng thái cuộn của danh sách RA NGOÀI khối `when` → giữ NGUYÊN vị trí cuộn khi swap qua lại
    // giữa danh sách và lịch sử tìm kiếm (LazyColumn bị dispose lúc hiện lịch sử vẫn không mất vị trí).
    val listState = rememberLazyListState()
    // ---- Nút "LÊN ĐẦU" (kính mờ) — hiện khi đã cuộn xuống, đặt ngay TRÊN nút FAB ----
    val scrollScope = rememberCoroutineScope()
    // Ảnh chụp (record) nội dung danh sách vào lớp đồ hoạ này để nút vẽ lại vùng nền BÊN DƯỚI ở dạng BLUR.
    val backdropLayer = rememberGraphicsLayer()
    // Toạ độ của cột nội dung (gốc của backdropLayer) — để tính lệch vẽ nền mờ cho đúng vùng dưới nút.
    var contentCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    // Chỉ hiện nút khi đã cuộn qua vài dòng đầu (tránh nhấp nháy khi ở gần đỉnh).
    val showScrollTop by remember { derivedStateOf { listState.firstVisibleItemIndex > 4 } }
    // Đóng màn tìm kiếm (dùng cho CẢ nút X lẫn nút BACK): CHỈ reset, KHÔNG lưu lịch sử — tránh nhét truy vấn
    // gõ dở mà người dùng CHỦ ĐỘNG huỷ vào "tìm gần đây". Lịch sử chỉ lưu khi xác nhận thật (phím ✓ / chạm kết quả).
    val closeSearch: () -> Unit = {
        searchActive = false; searchFocused = false; query = ""; focusManager.clearFocus()
    }
    // Nút xác nhận trên bàn phím → NHẢ FOCUS HẲN (như chạm ngoài, con trỏ tắt) + lưu lịch sử nếu có từ khoá.
    val onImeAction: () -> Unit = {
        if (query.isNotBlank()) commitSearch(query)
        focusManager.clearFocus()
    }
    // Đang tìm kiếm → nút BACK ĐÓNG search (quay về màn chính) thay vì thoát app.
    BackHandler(enabled = searchActive) { closeSearch() }

    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) query = spoken
    }

    // Context-menu khi nhấn giữ item — dựng ở GỐC (ngoài inset) để nền tối phủ TRỌN màn hình.
    val density = LocalDensity.current
    val statusBarPx = WindowInsets.statusBars.getTop(density)
    val navBarPx = WindowInsets.navigationBars.getBottom(density)
    var contextTarget by remember { mutableStateOf<CallContextTarget?>(null) }

    // Luồng QUÉT MÃ QR dùng chung (giống màn Lịch sử quét QR): mở camera → xử lý kết quả theo loại.
    val qrFlow = rememberQrScanFlowState()

    // ---- SIM: phạm vi TOÀN APP (Cài đặt) + lọc nhanh CỤC BỘ ----
    // Phạm vi SIM toàn app: khi bật một SIM cụ thể, dữ liệu ĐÃ được lọc TẠI GỐC (repo) nên danh sách chỉ còn
    // SIM đó → thanh lọc cục bộ nhường chỗ cho tab TRẠNG THÁI "Đang xem SIM X" và filter cục bộ TẮT (tránh
    // lọc chồng ra rỗng). Đang xem "tất cả" → thanh LỌC nhanh hoạt động như thường.
    val globalSim = SimScope.effectiveLabel
    // Các SIM đang xuất hiện trong danh sách: nhãn "SIM n" do repo gán theo SIM ĐANG LẮP (SimRegistry/SimInfo),
    // nên cuộc của SIM ĐÃ THÁO có simLabel = null → tự bị loại (không hiện thành SIM lạ). ≥2 SIM lắp thật →
    // cho lọc nhanh. Khi phạm vi toàn app đang bật, dữ liệu đã lọc còn 1 nhãn → thanh lọc cục bộ tự ẩn (đúng).
    val simLabels = remember(vm.entries) {
        vm.entries.mapNotNull { it.simLabel }.distinct().sorted()
    }
    // Chỉ cho LỌC NHANH cục bộ khi đang xem "tất cả" toàn app VÀ đang lắp ≥2 SIM.
    val showSimFilter = globalSim == null && simLabels.size >= 2
    // Giá trị lọc SIM cục bộ CÓ HIỆU LỰC: null khi đang khoá phạm vi toàn app; ngược lại chỉ giữ khi SIM đó
    // còn trong nhật ký (tránh kẹt lọc ngầm ra danh sách rỗng khi SIM đã tháo + hết lịch sử).
    val effectiveSim = if (globalSim != null) null else simFilter?.takeIf { simLabels.size >= 2 && it in simLabels }

    Box(modifier = Modifier.fillMaxSize()) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .statusBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { contentCoords = it }
                // Khi nút "lên đầu" đang hiện: GHI nội dung vào backdropLayer rồi vẽ lại → nút có nền để làm mờ.
                // Khi ở gần đỉnh (nút ẩn): vẽ thẳng, không tốn thêm lớp đồ hoạ.
                .drawWithContent {
                    if (showScrollTop) {
                        backdropLayer.record { this@drawWithContent.drawContent() }
                        drawLayer(backdropLayer)
                    } else {
                        drawContent()
                    }
                }
        ) {

            TopBar(
                searchActive = searchActive,
                query = query,
                onQueryChange = { query = it },
                onOpenSearch = { searchActive = true },
                onCloseSearch = closeSearch,
                onFocusChanged = { searchFocused = it },
                onImeAction = onImeAction,
                onVoice = {
                    runCatching {
                        speechLauncher.launch(
                            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, LanguageSettings.lang.tag)
                                .putExtra(RecognizerIntent.EXTRA_PROMPT, s.callList.voicePrompt)
                        )
                    }.onFailure { CallActions.toast(context, s.callList.voiceUnsupported) }
                },
                onScanQr = { qrFlow.scan() },
                onContacts = onOpenContacts,
                onSettings = onOpenSettings
            )

            if (hasPerm && !searchActive) {
                // Hàng 1: NÚT LỌC (mở bottom sheet chọn loại) + THANH chế độ xem (text, cùng cỡ/bo góc).
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TypeFilterButton(current = typeFilter, onClick = { showTypeSheet = true })
                    Spacer(Modifier.weight(1f))
                    ViewModeToggle(selected = viewMode, onSelect = { viewMode = it })
                }
                ChipFilterRow(
                    dateFilter = dateFilter,
                    customEpochDay = customDate,
                    onDate = { dateFilter = if (dateFilter == it) DateFilter.NONE else it },
                    onPickDate = { showDateSheet = true },
                    onDemo = { CallActions.demo(context) },
                    categories = categories,
                    selectedCategory = effectiveCategory,
                    onSelectCategory = { id -> categoryFilter = if (categoryFilter == id) null else id },
                    onCreateCategory = onOpenCreateCategory
                )
                // Thanh SIM (căn GIỮA, ngay dưới hàng chip ngày): đang khoá phạm vi 1 SIM toàn app → tab TRẠNG
                // THÁI "Đang xem SIM X" (dữ liệu đã lọc sẵn); ngược lại, nhật ký ≥2 SIM → thanh LỌC nhanh.
                if (globalSim != null) {
                    SimScopeStatusTab(globalSim)
                } else if (showSimFilter) {
                    // 2 SIM (case chính) → 3 ô, vừa khít mọi máy → căn GIỮA, không cuộn. Nhiều SIM hơn (hiếm,
                    // do lịch sử đổi SIM nhiều lần) → tổng bề ngang có thể vượt máy hẹp nên cho CUỘN ngang
                    // (giống ChipFilterRow) để không ô nào bị cắt/mất vùng chạm; khi cuộn thì tự căn trái.
                    val simRowModifier = if (simLabels.size > 2) {
                        Modifier.fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 2.dp)
                    } else {
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)
                    }
                    Row(
                        modifier = simRowModifier,
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SimSegmentedToggle(
                            allLabel = s.callList.filterAll,
                            labels = simLabels,
                            selected = effectiveSim,
                            onSelect = { simFilter = it }
                        )
                    }
                }
                if (!contactsGranted && !contactsBannerDismissed) {
                    ContactsPermissionBanner(
                        onAllow = requestContactsPermission,
                        onDismiss = { contactsBannerDismissed = true },
                        allowLabel = if (contactsPermanentlyDenied) s.common.openSettings else s.common.grantPermission
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    // Chạm ra vùng trống (ngoài ô nhập) → ẩn bàn phím; item con vẫn nhận chạm bình thường.
                    .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
            ) {
                when {
                    !hasPerm -> PermissionState(
                        onRequest = requestCallLogPermission,
                        buttonLabel = if (permPermanentlyDenied) s.common.openSettings else s.common.allowAccess
                    )

                    // Đang gõ tìm kiếm & ô rỗng → hiện LỊCH SỬ tìm kiếm (thay vì danh sách).
                    searchFocused && query.isBlank() -> SearchHistoryView(
                        history = searchHistory,
                        onTap = { entry -> query = entry.query; commitSearch(entry.query); focusManager.clearFocus() },
                        onDelete = deleteSearch,
                        onClearAll = { searchHistory = emptyList(); prefs.saveSearchHistory(emptyList()) }
                    )

                    vm.loading && vm.entries.isEmpty() -> LoadingState()

                    else -> {
                        // LanguageSettings.lang trong khoá: đổi ngôn ngữ → dựng lại nhãn nhóm ngày (TimeFormat.sectionLabel).
                        val rows = remember(vm.entries, query, typeFilter, dateFilter, customDate, effectiveSim, viewMode, categoryNumbers, LanguageSettings.lang) {
                            buildRows(vm.entries, query, typeFilter, dateFilter, customDate, effectiveSim, viewMode, categoryNumbers)
                        }
                        // VUỐT XUỐNG để làm mới — thay cho nút "Làm mới" cũ. Vòng xoay xanh lá chủ đạo hiện
                        // trong lúc vm.refresh() nạp lại (song song với tự nạp ngầm khi nhật ký máy thay đổi).
                        val pullState = rememberPullToRefreshState()
                        PullToRefreshBox(
                            isRefreshing = vm.refreshing,
                            onRefresh = { vm.refresh() },
                            state = pullState,
                            modifier = Modifier.fillMaxSize(),
                            indicator = {
                                PullToRefreshDefaults.Indicator(
                                    state = pullState,
                                    isRefreshing = vm.refreshing,
                                    modifier = Modifier.align(Alignment.TopCenter),
                                    color = Primary,
                                    containerColor = CardSurface
                                )
                            }
                        ) {
                            if (rows.isEmpty()) {
                                // Bọc trong LazyColumn 1 item đầy khung → cử chỉ vuốt vẫn kích hoạt làm mới
                                // khi danh sách rỗng (Column thường không sinh sự kiện cuộn lồng).
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    item { EmptyState(emptyMessage(query), modifier = Modifier.fillParentMaxSize()) }
                                }
                            } else {
                                val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(top = 4.dp, bottom = navBottom + 96.dp)
                                ) {
                                    items(
                                        items = rows,
                                        key = { row ->
                                            when (row) {
                                                is Row2.Header -> "h_${row.label}"
                                                is Row2.Item -> "e_${row.entry.id}"
                                            }
                                        }
                                    ) { row ->
                                        when (row) {
                                            is Row2.Header -> SectionHeader(row.label)
                                            is Row2.Item -> ListCallItem(
                                                entry = row.entry,
                                                onOpen = {
                                                    // Mở kết quả từ tìm kiếm → lưu từ khoá vào lịch sử.
                                                    if (searchActive && query.isNotBlank()) commitSearch(query)
                                                    onOpenNumber(row.entry.number)
                                                },
                                                onLongPress = { bounds ->
                                                    contextTarget = CallContextTarget(row.entry, bounds, row.missedStreak)
                                                },
                                                activeInMenu = contextTarget?.entry?.id == row.entry.id,
                                                missedStreak = row.missedStreak,
                                                flash = flashEntryId == row.entry.id
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // FAB ẩn khi đang TÌM KIẾM: tránh bị bàn phím đẩy lên (cộng dồn cả nav bar nhìn xa/xấu) & gọn màn tìm.
        if (hasPerm && !searchActive) {
            val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

            // Nút "LÊN ĐẦU" — kính mờ xám, icon trắng; NGAY TRÊN FAB (căn giữa theo trục FAB, cách 12.dp).
            // Hiện/ẩn mượt bằng fade + scale khi cuộn xuống/lên. Nền mờ lấy từ backdropLayer đã ghi ở Column.
            AnimatedVisibility(
                visible = showScrollTop,
                enter = fadeIn() + scaleIn(initialScale = 0.7f),
                exit = fadeOut() + scaleOut(targetScale = 0.7f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    // FAB: end 20 + rộng 60 → tâm cách phải 50; nút rộng 48 → end 26 để cùng trục.
                    // bottom: trên FAB (cao 60) + đáy FAB (navBottom+24) + khoảng hở 12.
                    .padding(end = 26.dp, bottom = navBottom + 24.dp + 60.dp + 12.dp)
            ) {
                ScrollToTopButton(
                    backdropLayer = backdropLayer,
                    contentCoords = contentCoords,
                    onClick = { scrollScope.launch { listState.animateScrollToItem(0) } }
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = navBottom + 24.dp)
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Primary)
                    .clickable { CallActions.openDialer(context) },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Dialpad, contentDescription = s.callList.dialpad, tint = Color.White, modifier = Modifier.size(26.dp))
            }
        }
    }

        // Lớp phủ context-menu (nhấn giữ) — nằm trong Box GỐC nên phủ trọn màn, trên cả FAB.
        contextTarget?.let { tgt ->
            CallContextMenuOverlay(
                target = tgt,
                topInsetPx = statusBarPx,
                bottomInsetPx = navBarPx,
                onMessage = { CallActions.message(context, tgt.entry.number) },
                onCall = { CallActions.dial(context, tgt.entry.number) },
                onCopy = { CallActions.copy(context, tgt.entry.number) },
                onZalo = { PhoneActions.openZalo(context, tgt.entry.number) },
                onClosed = { contextTarget = null }
            )
        }

        if (showTypeSheet) {
            TypeFilterSheet(
                selected = typeFilter,
                onSelect = { typeFilter = it },
                onDismiss = { showTypeSheet = false }
            )
        }

        if (showDateSheet) {
            DayFilterSheet(
                initialEpochDay = if (dateFilter == DateFilter.CUSTOM) customDate else 0L,
                isActive = dateFilter == DateFilter.CUSTOM,
                onApply = { epochDay -> customDate = epochDay; dateFilter = DateFilter.CUSTOM },
                onClear = { dateFilter = DateFilter.NONE },
                onDismiss = { showDateSheet = false }
            )
        }

        // Luồng quét QR (camera phủ full-bleed + sheet xử lý kết quả) — mở bằng nút QR ở thanh trên.
        QrScanFlow(state = qrFlow)
    }
}

private fun emptyMessage(query: String): String = with(appStrings().callList) {
    if (query.isNotBlank()) emptyNoResults else emptyNoCalls
}

/** Đọc một enum đã lưu (theo tên) từ SharedPreferences; tên không hợp lệ / chưa lưu → [default]. */
private inline fun <reified T : Enum<T>> SharedPreferences.loadEnum(key: String, default: T): T {
    val name = getString(key, null) ?: return default
    return runCatching { enumValueOf<T>(name) }.getOrDefault(default)
}

/**
 * Số cuộc NHỠ (type MISSED) LIÊN TIẾP MỚI NHẤT trong [calls] (mới → cũ): nếu cuộc mới nhất là nhỡ thì đếm
 * chuỗi nhỡ liền mạch từ đầu; trả về N nếu >= 2, ngược lại 0 (nhỡ đơn lẻ / mới nhất không-nhỡ → 0).
 * [calls] là các cuộc CỦA MỘT SỐ đã qua bộ lọc → số đếm tự khớp bộ lọc ngày/loại đang chọn.
 */
private fun leadingMissedRun(calls: List<CallEntry>): Int {
    if (calls.firstOrNull()?.type != CallType.MISSED) return 0
    var n = 0
    while (n < calls.size && calls[n].type == CallType.MISSED) n++
    return if (n >= 2) n else 0
}

internal fun buildRows(
    entries: List<CallEntry>,
    query: String,
    typeFilter: TypeFilter,
    dateFilter: DateFilter,
    customEpochDay: Long,
    simFilter: String?,
    viewMode: ViewMode,
    categoryNumbers: Set<String>?
): List<Row2> {
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    // Ngày cụ thể đang lọc (chỉ dùng khi DateFilter.CUSTOM); null nếu chưa chọn → không áp lọc ngày.
    val customDay = if (customEpochDay > 0L) LocalDate.ofEpochDay(customEpochDay) else null
    val q = query.trim()
    // Tính MỘT LẦN cho cả truy vấn (không lặp mỗi dòng): chuỗi tìm đã bỏ dấu + truy vấn có "giống số" không.
    val nq = normalizeForSearch(q)
    val phoneLike = PhoneKey.isPhoneLikeQuery(q)
    val filtered = entries.filter { e ->
        // Phạm vi SIM + NHÓM luôn áp dụng (kể cả khi tìm kiếm) → tìm ĐÚNG trong những gì đang xem, và đóng tìm
        // kiếm không bị "nhảy" danh sách. null = không thu hẹp.
        val matchScope = (simFilter == null || e.simLabel == simFilter) &&
            (categoryNumbers == null || PhoneKey.of(e.number) in categoryNumbers)
        if (q.isNotBlank()) {
            // ĐANG TÌM KIẾM = tìm theo tên/số trong PHẠM VI SIM/nhóm hiện tại, **KHÔNG áp bộ lọc loại/ngày**.
            // Tên: bỏ dấu 2 phía; Số: khớp 0 ↔ +84 (chỉ khi truy vấn giống số → không kéo nhầm khi gõ chữ).
            matchScope && (
                (nq.isNotEmpty() && (normalizeForSearch(e.nameOrNumber).contains(nq) ||
                    (e.cachedName != null && normalizeForSearch(e.cachedName).contains(nq)))) ||
                    (phoneLike && PhoneKey.matchesQuery(e.number, q))
                )
        } else {
            // KHÔNG tìm kiếm → áp bộ lọc LOẠI + NGÀY như thường.
            val matchType = when (typeFilter) {
                TypeFilter.ALL -> true
                TypeFilter.MISSED -> e.type == CallType.MISSED || e.type == CallType.REJECTED || e.type == CallType.BLOCKED
                TypeFilter.INCOMING -> e.type == CallType.INCOMING || e.type == CallType.ANSWERED_EXTERNALLY
                TypeFilter.OUTGOING -> e.type == CallType.OUTGOING
            }
            val date = Instant.ofEpochMilli(e.dateMillis).atZone(zone).toLocalDate()
            val matchDate = when (dateFilter) {
                DateFilter.NONE -> true
                DateFilter.TODAY -> date.isEqual(today)
                DateFilter.YESTERDAY -> date.isEqual(today.minusDays(1))
                DateFilter.WEEK -> ChronoUnit.DAYS.between(date, today) < 7
                DateFilter.MONTH -> ChronoUnit.DAYS.between(date, today) < 30
                // Lọc đúng MỘT NGÀY cụ thể; chưa chọn ngày (customDay null) → không thu hẹp.
                DateFilter.CUSTOM -> customDay == null || date.isEqual(customDay)
            }
            // SIM + nhóm: dùng chung matchScope (đã tính ở trên) để không lặp logic.
            matchType && matchDate && matchScope
        }
    }

    // Danh sách (đại diện, số cuộc nhỡ) theo chế độ, GIỮ thứ tự mới → cũ.
    val items: List<Pair<CallEntry, Int>> = when (viewMode) {
        // Theo THỜI GIAN: mỗi cuộc 1 dòng, KHÔNG hiện số cuộc nhỡ.
        ViewMode.BY_TIME -> filtered.map { it to 0 }
        // Theo SỐ: gộp mỗi số 1 dòng (đại diện = cuộc mới nhất đã lọc), kèm số cuộc nhỡ liên tiếp cuối cùng.
        ViewMode.BY_PHONE -> {
            // Gộp theo KHOÁ CHUẨN (không phải chuỗi số thô) → cùng người dù nhật ký lưu lẫn "+84…"/"0…" chỉ
            // còn MỘT dòng; đại diện = cuộc mới nhất (chuỗi số thật để dial/hiển thị đúng).
            val byNumber = LinkedHashMap<String, MutableList<CallEntry>>()
            for (e in filtered) byNumber.getOrPut(PhoneKey.of(e.number).ifEmpty { e.number }) { ArrayList() }.add(e)
            byNumber.values.map { calls -> calls.first() to leadingMissedRun(calls) }
        }
    }

    val rows = ArrayList<Row2>(items.size + 4)
    var lastSection: String? = null
    for ((entry, streak) in items) {
        val section = TimeFormat.sectionLabel(entry.dateMillis)
        if (section != lastSection) {
            rows.add(Row2.Header(section))
            lastSection = section
        }
        rows.add(Row2.Item(entry, streak))
    }
    return rows
}

@Composable
private fun TopBar(
    searchActive: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onCloseSearch: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onImeAction: () -> Unit,
    onVoice: () -> Unit,
    onScanQr: () -> Unit,
    onContacts: () -> Unit,
    onSettings: () -> Unit
) {
    val s = appStrings()
    if (searchActive) {
        // Mở tìm kiếm → tự focus ô nhập (bật bàn phím) + hiện lịch sử.
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 16.dp, bottom = 8.dp)
                .height(48.dp)
                .clip(CircleShape)
                .background(FieldSurface)
                .padding(start = 8.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundIconButton(Icons.Rounded.Search, s.common.search, TextSecondary) {}
            Box(modifier = Modifier.weight(1f).padding(start = 4.dp), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(
                        s.callList.searchHint,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextSecondary,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                    cursorBrush = SolidColor(Primary),
                    // Phím ✓ mặc định (Done); nhấn → nhả focus hẳn (không còn con trỏ nhấp nháy).
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onImeAction() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { onFocusChanged(it.isFocused) }
                )
            }
            RoundIconButton(Icons.Rounded.Mic, s.callList.voice, TextSecondary, onVoice)
            RoundIconButton(Icons.Rounded.Close, s.callList.close, TextSecondary, onCloseSearch)
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 6.dp, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = s.callList.title,
                style = MaterialTheme.typography.headlineSmall,
                color = TextPrimary,
                // Tiêu đề 1 dòng; khi thêm nút QR làm bề ngang không đủ hiển thị đủ chữ thì CHẠY vòng (phải→trái)
                // liên tục thay vì bị cắt cụt. Đủ chỗ thì đứng yên như thường.
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .weight(1f)
                    .basicMarquee(iterations = Int.MAX_VALUE)
            )
            RoundIconButton(Icons.Rounded.QrCodeScanner, s.qr.scan, TextSecondary, onScanQr)
            RoundIconButton(Icons.Rounded.Contacts, s.common.contacts, TextSecondary, onContacts)
            RoundIconButton(Icons.Rounded.Search, s.common.search, TextSecondary, onOpenSearch)
            RoundIconButton(Icons.Rounded.Settings, s.common.settings, TextSecondary, onSettings)
        }
    }
}

@Composable
private fun RoundIconButton(icon: ImageVector, desc: String, tint: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = desc, tint = tint, modifier = Modifier.size(24.dp))
    }
}

/**
 * Nút tròn "LÊN ĐẦU DANH SÁCH" dạng KÍNH MỜ (frosted glass): nền BLUR nội dung danh sách bên dưới +
 * lớp kính XÁM rất nhạt phủ lên để vẫn nhìn xuyên nội dung bên dưới. Đặt ngay trên FAB.
 *
 * Cách blur nền THẬT (backdrop blur) không cần thư viện ngoài:
 *  1) [Column] danh sách đã GHI (record) toàn bộ nội dung vào [backdropLayer].
 *  2) Ở đây vẽ LẠI đúng vùng dưới nút: dịch backdropLayer theo lệch toạ độ (nút ↔ gốc nội dung), rồi
 *     cho qua [BlurEffect] trong một lớp phụ [blurLayer]; phần thừa bị [clip] hình tròn cắt gọn.
 * Dưới API 31 (Android 12) BlurEffect tự bỏ qua (không crash) — lúc đó chỉ còn lớp kính xám, nút vẫn đẹp.
 */
@Composable
private fun ScrollToTopButton(
    backdropLayer: GraphicsLayer,
    contentCoords: LayoutCoordinates?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val blurLayer = rememberGraphicsLayer()
    val blurRadiusPx = with(LocalDensity.current) { 24.dp.toPx() }
    var selfCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    Box(
        modifier = modifier
            .size(48.dp)
            .onGloballyPositioned { selfCoords = it }
            .clip(CircleShape)
            .drawBehind {
                val content = contentCoords
                val self = selfCoords
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    content != null && self != null && content.isAttached && self.isAttached
                ) {
                    // Lệch của góc trên–trái nút TRONG hệ toạ độ nội dung = vị trí cần lấy từ backdropLayer.
                    val topLeft = content.localPositionOf(self, Offset.Zero)
                    blurLayer.renderEffect = BlurEffect(blurRadiusPx, blurRadiusPx, TileMode.Decal)
                    blurLayer.record {
                        translate(left = -topLeft.x, top = -topLeft.y) {
                            drawLayer(backdropLayer)
                        }
                    }
                    drawLayer(blurLayer)
                    // Lớp kính rất nhạt phủ lên nền đã blur → vẫn thấy rõ nội dung mờ phía sau.
                    drawRect(color = ScrollTopScrim)
                } else {
                    // < Android 12 không blur được: dùng fallback bán trong suốt, không tạo mảng xám đặc.
                    drawRect(color = ScrollTopSolid)
                }
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Rounded.KeyboardArrowUp,
            contentDescription = appStrings().callList.scrollToTop,
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

/** Chiều cao chung cho nút lọc + thanh chế độ xem (để chúng đồng bộ cỡ/bo góc). */
private val FilterBarHeight = 38.dp

/** Nhãn hiển thị cho từng loại lọc (ĐA NGÔN NGỮ qua [appStrings]). */
private fun typeFilterLabel(f: TypeFilter): String = with(appStrings().callList) {
    when (f) {
        TypeFilter.ALL -> filterAll
        TypeFilter.MISSED -> filterMissed
        TypeFilter.OUTGOING -> filterOutgoing
        TypeFilter.INCOMING -> filterIncoming
    }
}

/**
 * Nút mở bottom sheet chọn LOẠI cuộc gọi — hiện nhãn loại đang chọn + MŨI TÊN xuống. Đang lọc (khác "Tất cả")
 * thì tô xanh nhạt (chip chọn); mặc định thì nền xám.
 */
@Composable
internal fun TypeFilterButton(current: TypeFilter, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val active = current != TypeFilter.ALL
    val bg = if (active) ChipSelectedBg else ChipBg
    val fg = if (active) ChipSelectedText else ChipText
    Row(
        modifier = modifier
            .height(FilterBarHeight)
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = typeFilterLabel(current),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = fg,
            maxLines = 1
        )
        Icon(Icons.Rounded.ExpandMore, contentDescription = appStrings().callList.chooseFilter, tint = fg, modifier = Modifier.size(18.dp))
    }
}

/**
 * Thanh chế độ xem dạng TEXT (cùng chiều cao / bo góc với nút lọc): "Thời gian" | "Số điện thoại".
 * Con trượt XANH trượt mượt (spring); chữ tab chọn TRẮNG, tab kia xám.
 */
@Composable
private fun ViewModeToggle(selected: ViewMode, onSelect: (ViewMode) -> Unit, modifier: Modifier = Modifier) {
    val cl = appStrings().callList
    val items = listOf(ViewMode.BY_TIME to cl.viewByTime, ViewMode.BY_PHONE to cl.viewByPhone)
    val selectedIndex = items.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    val segWidth = 104.dp
    Box(
        modifier = modifier
            .height(FilterBarHeight)
            .width(segWidth * items.size)
            .clip(CircleShape)
            .background(ChipBg)
    ) {
        val thumbX by animateDpAsState(
            targetValue = segWidth * selectedIndex.toFloat(),
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 520f),
            label = "viewThumb"
        )
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbX.roundToPx(), 0) }
                .width(segWidth)
                .fillMaxHeight()
                .padding(3.dp)
                .clip(CircleShape)
                .background(Primary)
        )
        Row(modifier = Modifier.fillMaxSize()) {
            items.forEachIndexed { i, (mode, label) ->
                val isSel = i == selectedIndex
                val interaction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .width(segWidth)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(interactionSource = interaction, indication = null) { onSelect(mode) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSel) Color.White else TextSecondary,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center,
                        // Cỡ chữ lớn làm nhãn dài ("Số điện thoại") vượt bề rộng segment cố định (104.dp) → thay vì
                        // bị CẮT, cho chữ CHẠY vòng lặp (phải→trái). Nhãn ngắn/vừa vẫn đứng yên & căn giữa như cũ.
                        modifier = Modifier
                            .fillMaxWidth()
                            .basicMarquee(iterations = Int.MAX_VALUE)
                    )
                }
            }
        }
    }
}

/**
 * Tab TRẠNG THÁI "Đang xem SIM X" (chỉ đọc, căn GIỮA) — hiện thay cho thanh lọc SIM khi PHẠM VI SIM TOÀN APP
 * đang khoá một SIM (chọn ở Cài đặt). Nội dung pill dùng chung [SimScopeStatusPill] (đồng bộ với chi tiết/tất cả).
 */
@Composable
private fun SimScopeStatusTab(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SimScopeStatusPill(label)
    }
}

/**
 * Bottom sheet chọn LOẠI cuộc gọi — dựng trên [AppBottomSheet] dùng chung (đồng bộ với sheet chi tiết).
 * Tone XÁM–TRẮNG, KHÔNG icon màu: mỗi loại = icon xám trong nền tròn xám + nhãn; loại đang chọn có
 * nền xám nhạt + DẤU TICK màu xám; chọn xong tự đóng.
 */
@Composable
internal fun TypeFilterSheet(
    selected: TypeFilter,
    onSelect: (TypeFilter) -> Unit,
    onDismiss: () -> Unit
) {
    AppBottomSheet(
        onDismiss = onDismiss,
        title = appStrings().callList.typeSheetTitle,
        sheetGesturesEnabled = false,
        showCloseButton = true
    ) { close ->
        TypeFilter.entries.forEach { option ->
            FilterOptionRow(
                icon = typeFilterIcon(option),
                label = typeFilterLabel(option),
                selected = option == selected,
                onClick = { onSelect(option); close() }
            )
        }
    }
}

private fun typeFilterIcon(f: TypeFilter): ImageVector = when (f) {
    TypeFilter.ALL -> Icons.Rounded.FilterList
    TypeFilter.MISSED -> Icons.AutoMirrored.Rounded.CallMissed
    TypeFilter.OUTGOING -> Icons.AutoMirrored.Rounded.CallMade
    TypeFilter.INCOMING -> Icons.AutoMirrored.Rounded.CallReceived
}

@Composable
private fun ChipFilterRow(
    dateFilter: DateFilter,
    customEpochDay: Long,
    onDate: (DateFilter) -> Unit,
    onPickDate: () -> Unit,
    onDemo: () -> Unit,
    categories: List<Category>,
    selectedCategory: Long?,
    onSelectCategory: (Long?) -> Unit,
    onCreateCategory: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val cl = appStrings().callList
        // Nút "+" ở ĐẦU → mở màn tạo nhóm phân loại.
        AddCategoryChip(onClick = onCreateCategory)
        IconChip(cl.dateToday, Icons.Rounded.Today, dateFilter == DateFilter.TODAY) { onDate(DateFilter.TODAY) }
        IconChip(cl.dateYesterday, Icons.Rounded.History, dateFilter == DateFilter.YESTERDAY) { onDate(DateFilter.YESTERDAY) }
        IconChip(cl.dateWeek, Icons.Rounded.DateRange, dateFilter == DateFilter.WEEK) { onDate(DateFilter.WEEK) }
        IconChip(cl.dateMonth, Icons.Rounded.CalendarMonth, dateFilter == DateFilter.MONTH) { onDate(DateFilter.MONTH) }
        // Chọn MỘT ngày cụ thể (mở lịch modal). Đang lọc → chip xanh + hiện ngày đã chọn thay cho nhãn.
        IconChip(
            text = customChipLabel(dateFilter, customEpochDay),
            icon = Icons.Rounded.EditCalendar,
            selected = dateFilter == DateFilter.CUSTOM,
            onClick = onPickDate
        )
        // Chip theo NHÓM PHÂN LOẠI (Công việc / Yêu thích / … do người dùng tạo) — chạm để lọc, chạm lại bỏ lọc.
        categories.forEach { cat ->
            CategoryChip(
                text = categoryLabel(cat),
                iconKey = cat.iconKey,
                selected = selectedCategory == cat.id,
                onClick = { onSelectCategory(cat.id) }
            )
        }
    }
}

/** Chip nhóm phân loại ở hàng lọc — hỗ trợ CẢ icon material (tô màu chip) LẪN logo app (ảnh, giữ màu). */
@Composable
private fun CategoryChip(text: String, iconKey: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(if (selected) ChipSelectedBg else ChipBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        when (val g = categoryGlyph(iconKey)) {
            is CategoryGlyph.Res -> Image(
                painter = painterResource(g.id),
                contentDescription = null,
                modifier = Modifier.size(18.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            is CategoryGlyph.Vec -> Icon(
                g.icon, null,
                tint = if (selected) ChipSelectedText else ChipText,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = if (selected) ChipSelectedText else ChipText,
        )
    }
}

/** Nút chip TRÒN chỉ có dấu "+" ở đầu hàng lọc — mở màn tạo nhóm phân loại. */
@Composable
private fun AddCategoryChip(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(ChipBg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.Add, contentDescription = appStrings().category.newCategory, tint = ChipText, modifier = Modifier.size(20.dp))
    }
}

/** Nhãn chip "Chọn ngày": đang lọc theo ngày cụ thể → hiện "dd/MM"; ngược lại → "Chọn ngày". */
private fun customChipLabel(dateFilter: DateFilter, epochDay: Long): String {
    if (dateFilter != DateFilter.CUSTOM || epochDay <= 0L) return appStrings().callList.pickDate
    val d = LocalDate.ofEpochDay(epochDay)
    return "%02d/%02d".format(d.dayOfMonth, d.monthValue)
}

@Composable
private fun IconChip(text: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(36.dp)
            .clip(CircleShape)
            .background(if (selected) ChipSelectedBg else ChipBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, tint = if (selected) ChipSelectedText else ChipText, modifier = Modifier.size(18.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = if (selected) ChipSelectedText else ChipText
        )
    }
}

/**
 * MODAL LỊCH chọn MỘT ngày cụ thể — dựng trên [AppBottomSheet] dùng chung (đồng bộ với các sheet khác:
 * nền trắng, bo góc trên, tay cầm, nút X). Lịch TỰ VẼ (không dùng DatePicker mặc định của Material) để
 * khớp tone xám–trắng + xanh lá của app: đầu tuần THỨ 2, ô ngày vuông bo tròn, ngày chọn = tròn xanh đặc.
 *
 * GIỚI HẠN: chỉ chọn được trong **3 THÁNG GẦN NHẤT** (từ hôm nay lùi 3 tháng) → hôm nay; tháng/ngày TƯƠNG LAI
 * và ngày quá 3 tháng bị KHOÁ (mờ, không bấm) và mũi tên chuyển tháng tự tắt khi chạm biên.
 *
 * @param initialEpochDay ngày đang lọc (epoch-day) để mở lại đúng chỗ; 0 = chưa chọn → mặc định HÔM NAY.
 * @param isActive đang lọc theo ngày cụ thể chưa (để hiện nút "Bỏ lọc theo ngày").
 * @param minimumDate biên ngày cũ nhất tuỳ chọn; null giữ hành vi 3 tháng của bộ lọc cuộc gọi.
 * @param rangeNote ghi chú giới hạn tuỳ chọn để màn tái sử dụng lịch có thể mô tả đúng phạm vi của mình.
 */
@Composable
internal fun DayFilterSheet(
    initialEpochDay: Long,
    isActive: Boolean,
    onApply: (Long) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
    minimumDate: LocalDate? = null,
    rangeNote: String? = null,
) {
    val cl = appStrings().callList
    val zone = remember { ZoneId.systemDefault() }
    val today = remember { LocalDate.now(zone) }
    val minDate = remember(today, minimumDate) { minimumDate?.coerceAtMost(today) ?: today.minusMonths(3) }
    val maxDate = today                                // biên TRÊN: hôm nay (ẩn tương lai)
    val minMonth = remember { YearMonth.from(minDate) }
    val maxMonth = remember { YearMonth.from(today) }

    // Ngày chọn ban đầu: kẹp về [minDate, maxDate] nếu ngày lưu đã rơi ngoài khung 3 tháng.
    val initialDate = remember {
        when {
            initialEpochDay <= 0L -> today
            else -> LocalDate.ofEpochDay(initialEpochDay).let { d ->
                when {
                    d.isBefore(minDate) -> minDate
                    d.isAfter(maxDate) -> maxDate
                    else -> d
                }
            }
        }
    }
    var selected by remember { mutableStateOf(initialDate) }
    var displayMonth by remember { mutableStateOf(YearMonth.from(initialDate)) }

    AppBottomSheet(
        onDismiss = onDismiss,
        title = cl.pickDate,
        sheetGesturesEnabled = false,
        showCloseButton = true
    ) { close ->
        // ---- Hàng chuyển THÁNG: ‹  Tháng M, yyyy  › (mũi tên tự tắt khi chạm biên 3 tháng / hôm nay) ----
        val canPrev = displayMonth.isAfter(minMonth)
        val canNext = displayMonth.isBefore(maxMonth)
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CalNavButton(Icons.Rounded.ChevronLeft, cl.prevMonth, canPrev) {
                if (canPrev) displayMonth = displayMonth.minusMonths(1)
            }
            Text(
                text = cl.monthYear(displayMonth.monthValue, displayMonth.year),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            CalNavButton(Icons.Rounded.ChevronRight, cl.nextMonth, canNext) {
                if (canNext) displayMonth = displayMonth.plusMonths(1)
            }
        }

        // ---- Hàng thứ trong tuần (đầu tuần THỨ 2) ----
        Row(modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 2.dp)) {
            cl.weekdayHeaders.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // ---- Lưới NGÀY: mỗi ô vuông (aspectRatio 1) trong hàng 7 cột ----
        val weeks = remember(displayMonth) { buildMonthGrid(displayMonth) }
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 2.dp)) {
            weeks.forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        Box(
                            modifier = Modifier.weight(1f).aspectRatio(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            if (date != null) {
                                DayCell(
                                    day = date.dayOfMonth,
                                    selected = date.isEqual(selected),
                                    isToday = date.isEqual(today),
                                    enabled = !date.isBefore(minDate) && !date.isAfter(maxDate),
                                    onClick = { selected = date }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ---- Tóm tắt ngày đang chọn (tái dùng nhãn nhóm: "Hôm nay" / "Thứ X · dd/MM/yyyy") ----
        val selectedMillis = remember(selected) { selected.atStartOfDay(zone).toInstant().toEpochMilli() }
        Spacer(Modifier.height(10.dp))
        Text(
            text = cl.selectedDay(TimeFormat.sectionLabel(selectedMillis)),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = rangeNote ?: cl.dateRangeNote,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        )

        // ---- Nút ÁP DỤNG (pill xanh lá) + tuỳ chọn BỎ LỌC khi đang lọc theo ngày ----
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(50.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Primary)
                .clickable { onApply(selected.toEpochDay()); close() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = cl.apply,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (isActive) {
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onClear(); close() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = cl.clearDateFilter,
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            Spacer(Modifier.height(6.dp))
        }
    }
}

/** Nút chuyển tháng (‹ ›) trong [DayFilterSheet]: bật → đậm bấm được; tắt (chạm biên) → mờ, không bấm. */
@Composable
private fun CalNavButton(icon: ImageVector, desc: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = desc,
            tint = if (enabled) TextPrimary else TextSecondary.copy(alpha = 0.3f),
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Một ô NGÀY trong lưới lịch: chọn → tròn xanh đặc chữ trắng; hôm nay (chưa chọn) → nền xanh nhạt + chữ xanh;
 * quá khứ/tương lai ngoài khung 3 tháng ([enabled] = false) → mờ, không bấm; còn lại → chữ đen bấm được.
 */
@Composable
private fun DayCell(day: Int, selected: Boolean, isToday: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val bg = when {
        selected -> Primary
        isToday -> BrandSoft
        else -> Color.Transparent
    }
    val fg = when {
        selected -> Color.White
        !enabled -> TextSecondary.copy(alpha = 0.28f)
        isToday -> LinkColor
        else -> TextPrimary
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(3.dp)
            .clip(CircleShape)
            .background(bg)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected || isToday) FontWeight.SemiBold else FontWeight.Normal,
            color = fg
        )
    }
}

/**
 * Dựng lưới ngày của [ym] theo TUẦN (đầu tuần THỨ 2): trả về danh sách các hàng, mỗi hàng 7 ô;
 * ô `null` là chỗ trống đệm đầu/cuối tháng. DayOfWeek.value: T2=1..CN=7 → chỉ số Thứ-2-đầu = (value+6)%7.
 */
private fun buildMonthGrid(ym: YearMonth): List<List<LocalDate?>> {
    val lead = (ym.atDay(1).dayOfWeek.value + 6) % 7
    val cells = ArrayList<LocalDate?>(42)
    repeat(lead) { cells.add(null) }
    for (d in 1..ym.lengthOfMonth()) cells.add(ym.atDay(d))
    while (cells.size % 7 != 0) cells.add(null)
    return cells.chunked(7)
}

/** Lần ngược chuỗi ContextWrapper để lấy Activity chủ (cho shouldShowRequestPermissionRationale). */
private fun Context.findActivity(): Activity? {
    var ctx: Context = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

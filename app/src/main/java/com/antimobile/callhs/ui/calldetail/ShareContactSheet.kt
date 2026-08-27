package com.antimobile.callhs.ui.calldetail

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.antimobile.callhs.data.model.CallNumberDetail
import com.antimobile.callhs.i18n.appStrings
import com.antimobile.callhs.ui.components.AppBottomSheet
import com.antimobile.callhs.ui.components.AppToastType
import com.antimobile.callhs.ui.components.Avatar
import com.antimobile.callhs.ui.components.PanelCard
import com.antimobile.callhs.ui.components.SheetCloseButton
import com.antimobile.callhs.ui.theme.AccentBlue
import com.antimobile.callhs.ui.theme.AccentBlueBg
import com.antimobile.callhs.ui.theme.AccentGray
import com.antimobile.callhs.ui.theme.AccentGrayBg
import com.antimobile.callhs.ui.theme.BrandSoft
import com.antimobile.callhs.ui.theme.FieldSurface
import com.antimobile.callhs.ui.theme.Primary
import com.antimobile.callhs.ui.theme.TextPrimary
import com.antimobile.callhs.ui.theme.TextSecondary
import com.antimobile.callhs.util.CallActions
import com.antimobile.callhs.util.QrShare
import com.antimobile.callhs.util.ShareContact
import com.antimobile.callhs.util.SimInfo
import com.antimobile.callhs.util.SpecialNumbers
import com.antimobile.callhs.util.formatPhone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Cạnh (px) ảnh QR khi mã hoá — đủ lớn để tải/chia sẻ quét lại rõ, hiển thị thì thu nhỏ theo bề ngang. */
private const val QR_ENCODE_PX = 800

/** Các bước trong sheet chia sẻ: chọn cách chia sẻ → xem QR liên hệ / xem QR của tôi. */
private enum class ShareStep { Chooser, ContactQr, MyQr }

/** Trạng thái tạo mã QR: đang tạo / xong / không tạo được (số ẩn/marker/lỗi mã hoá). */
private sealed interface QrState {
    data object Loading : QrState
    data class Ready(val bitmap: Bitmap) : QrState
    data object Unavailable : QrState
}

/**
 * Bottom sheet "Chia sẻ liên hệ" — 3 lựa chọn:
 *  1. Dạng văn bản (Tên + số) → Android tự chọn ứng dụng gửi.
 *  2. Mã QR liên hệ → HIỂN THỊ mã QR của số này + Tải về / Chia sẻ.
 *  3. QR của Tôi → đọc SIM (nhiều SIM thì cho chọn), hiện mã QR SỐ CỦA BẠN kèm chú thích
 *     "Di động: …" / "Nhà mạng: …" bên dưới (có sẵn trong ảnh tải/chia sẻ) + Tải về / Chia sẻ.
 */
@Composable
fun ShareContactSheet(detail: CallNumberDetail, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var step by remember(detail.number) { mutableStateOf(ShareStep.Chooser) }

    AppBottomSheet(onDismiss = onDismiss, sheetGesturesEnabled = false) { close ->
        when (step) {
            ShareStep.Chooser -> ChooserStep(
                detail = detail,
                onText = {
                    // Tên chia sẻ: tên danh bạ; số khẩn cấp chưa lưu thì dùng tên mặc định (Cảnh sát/Cứu hoả/Cấp cứu).
                    val shareName = detail.cachedName ?: SpecialNumbers.name(detail.number)
                    val ok = ShareContact.shareText(context, shareName, detail.number)
                    if (!ok) CallActions.toast(context, appStrings().shareSheet.invalidNumber, AppToastType.Error)
                    close()
                },
                onContactQr = { step = ShareStep.ContactQr },
                onMyQr = { step = ShareStep.MyQr },
                onClose = close
            )
            ShareStep.ContactQr -> ContactQrStep(detail = detail, onBack = { step = ShareStep.Chooser }, onClose = close)
            ShareStep.MyQr -> MyQrStep(onBackToChooser = { step = ShareStep.Chooser }, onClose = close)
        }
    }
}

// ---------------------------------------------------------------------------------------------------
// Bước 1 — Chọn cách chia sẻ
// ---------------------------------------------------------------------------------------------------

@Composable
private fun ChooserStep(
    detail: CallNumberDetail,
    onText: () -> Unit,
    onContactQr: () -> Unit,
    onMyQr: () -> Unit,
    onClose: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        SheetHeader(title = appStrings().callDetail.shareContact, onBack = null, onClose = onClose)
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            IdentityHeader(detail)
            Spacer(Modifier.height(18.dp))
            ShareOptionCard(
                icon = Icons.Rounded.Description,
                tint = AccentGray,
                iconBg = AccentGrayBg,
                title = appStrings().shareSheet.asTextTitle,
                subtitle = appStrings().shareSheet.asTextSubtitle,
                onClick = onText
            )
            Spacer(Modifier.height(12.dp))
            ShareOptionCard(
                icon = Icons.Rounded.QrCode2,
                tint = Primary,
                iconBg = BrandSoft,
                title = appStrings().shareSheet.asQrTitle,
                subtitle = appStrings().shareSheet.asQrSubtitle,
                onClick = onContactQr
            )
            Spacer(Modifier.height(12.dp))
            ShareOptionCard(
                icon = Icons.Rounded.QrCode2,
                tint = AccentBlue,
                iconBg = AccentBlueBg,
                title = appStrings().shareSheet.myQrTitle,
                subtitle = appStrings().shareSheet.myQrSubtitle,
                onClick = onMyQr
            )
            Spacer(Modifier.height(6.dp))
        }
    }
}

// ---------------------------------------------------------------------------------------------------
// Bước 2 — Mã QR của LIÊN HỆ (số đang xem)
// ---------------------------------------------------------------------------------------------------

@Composable
private fun ContactQrStep(detail: CallNumberDetail, onBack: () -> Unit, onClose: () -> Unit) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    BoostScreenBrightness(context)
    val payload = remember(detail.number) { QrShare.telPayload(detail.number) }
    val qrState by produceState<QrState>(QrState.Loading, payload) {
        value = if (payload.isEmpty()) QrState.Unavailable
        else withContext(Dispatchers.Default) {
            runCatching { QrShare.encode(payload, QR_ENCODE_PX) }
                .fold(onSuccess = { QrState.Ready(it) }, onFailure = { QrState.Unavailable })
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        SheetHeader(title = appStrings().shareSheet.contactQrTitle, onBack = onBack, onClose = onClose)
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IdentityHeader(detail)
            Spacer(Modifier.height(16.dp))
            QrCardView(qrState, unavailableText = appStrings().shareSheet.qrUnavailable)
            Spacer(Modifier.height(16.dp))
            val ready = qrState as? QrState.Ready
            if (ready != null) {
                HelperText(appStrings().shareSheet.contactQrHelper(formatPhone(detail.number)))
                Spacer(Modifier.height(16.dp))
                QrActionRow(bitmap = ready.bitmap, accent = Primary)
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

// ---------------------------------------------------------------------------------------------------
// Bước 3 — QR của TÔI (theo SIM): chọn SIM → mã QR số của bạn + chú thích
// ---------------------------------------------------------------------------------------------------

@Composable
private fun MyQrStep(onBackToChooser: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    // Đọc SIM + số đã lưu ở luồng nền (IPC viễn thông + SharedPreferences) để KHÔNG giật khung hình lúc mở
    // sheet. null = đang đọc. Số của mỗi khe = ưu tiên tự đọc, thiếu thì lấy số người dùng nhập trong Cài đặt.
    val device by produceState<SimInfo.DeviceSims?>(initialValue = null) {
        value = withContext(Dispatchers.IO) { SimInfo.readDeviceSims(context) }
    }
    var manualSelected by remember { mutableStateOf<SimInfo.SlotInfo?>(null) }
    val slots = device?.slots
    val multiple = (slots?.size ?: 0) > 1
    // Máy chỉ 1 khe → tự chọn NGAY TRONG composition (không qua LaunchedEffect) để KHÔNG nháy một khung
    // "Chọn SIM" trước khi vào QR. Máy nhiều khe → chờ người dùng chọn (manualSelected).
    val selected = manualSelected ?: slots?.singleOrNull()
    // Back: đang xem QR ở máy nhiều SIM → quay lại danh sách chọn SIM; còn lại → về màn chọn cách chia sẻ.
    val onBack: () -> Unit = { if (manualSelected != null && multiple) manualSelected = null else onBackToChooser() }
    BackHandler(onBack = onBack)

    Column(modifier = Modifier.fillMaxWidth()) {
        SheetHeader(title = if (selected != null) appStrings().shareSheet.myQr else appStrings().shareSheet.chooseSim, onBack = onBack, onClose = onClose)
        when {
            slots == null -> LoadingCenter()
            slots.isEmpty() -> EmptyInfo(appStrings().shareSheet.noSim)
            selected == null -> SimPicker(slots = slots, onSelect = { manualSelected = it })
            else -> MyQrShow(slot = selected)
        }
    }
}

@Composable
private fun SimPicker(slots: List<SimInfo.SlotInfo>, onSelect: (SimInfo.SlotInfo) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text(
            text = appStrings().shareSheet.chooseSimHint,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        slots.forEach { slot ->
            val subtitle = listOfNotNull(
                slot.carrier,
                slot.resolved.takeIf { it.isNotBlank() }?.let { formatPhone(it) }
            ).joinToString(" · ").ifBlank { if (slot.present) appStrings().shareSheet.noNumberEnter else appStrings().myNumber.simAbsent }
            PanelCard(modifier = Modifier.fillMaxWidth(), radius = 16.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onSelect(slot) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(AccentBlueBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.SimCard, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = slot.displayName?.let { "${slot.label} · $it" } ?: slot.label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun MyQrShow(slot: SimInfo.SlotInfo) {
    val context = LocalContext.current
    BoostScreenBrightness(context)
    // Mã hoá QR (tel: số của khe) rồi GHÉP chú thích "Di động: …" / "Nhà mạng: …" vào ảnh — cùng một ảnh
    // dùng cho cả hiển thị, tải về và chia sẻ. Chưa có số (tự đọc rỗng & chưa nhập tay) → Unavailable.
    val qrState by produceState<QrState>(QrState.Loading, slot) {
        val number = slot.resolved.trim()
        val payload = QrShare.telPayload(number)
        value = if (payload.isEmpty()) QrState.Unavailable
        else withContext(Dispatchers.Default) {
            runCatching {
                val qr = QrShare.encode(payload, QR_ENCODE_PX)
                QrShare.renderCaptioned(
                    qr = qr,
                    lines = listOf(
                        appStrings().shareSheet.mobileLine(formatPhone(number)),
                        appStrings().shareSheet.carrierLine(slot.carrier ?: appStrings().shareSheet.unknown)
                    )
                )
            }.fold(onSuccess = { QrState.Ready(it) }, onFailure = { QrState.Unavailable })
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = slot.displayName?.let { "${slot.label} · $it" } ?: slot.label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(14.dp))
        QrCardView(qrState, unavailableText = appStrings().shareSheet.noNumberForSlot(slot.label))
        Spacer(Modifier.height(16.dp))
        val ready = qrState as? QrState.Ready
        if (ready != null) {
            HelperText(appStrings().shareSheet.myQrHelper)
            Spacer(Modifier.height(16.dp))
            QrActionRow(bitmap = ready.bitmap, accent = AccentBlue)
        }
        Spacer(Modifier.height(6.dp))
    }
}

// ---------------------------------------------------------------------------------------------------
// Thành phần dùng chung
// ---------------------------------------------------------------------------------------------------

/** Tiêu đề sheet + nút quay lại tuỳ chọn (các bước con dùng để về bước trước). */
@Composable
private fun SheetHeader(title: String, onBack: (() -> Unit)?, onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (onBack != null) 6.dp else 20.dp, end = 8.dp, top = 2.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = appStrings().common.back, tint = TextPrimary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(4.dp))
        }
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        // Nút đóng (X) góc phải — đóng cả sheet ở mọi bước (khác nút back trái: back chỉ lùi 1 bước).
        Spacer(Modifier.weight(1f))
        SheetCloseButton(onClick = onClose)
    }
}

/** Hàng danh tính: avatar + tên + số của liên hệ. */
@Composable
private fun IdentityHeader(detail: CallNumberDetail) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Avatar(
            label = detail.nameOrNumber,
            photoUri = detail.photoUri,
            isNamed = !detail.cachedName.isNullOrBlank(),
            size = 48.dp,
            specialIconRes = SpecialNumbers.of(detail.number)?.iconRes
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = detail.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatPhone(detail.number),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Thẻ lựa chọn cách chia sẻ: icon tròn tô nền nhạt + tiêu đề + mô tả; cả thẻ bấm được. */
@Composable
private fun ShareOptionCard(
    icon: ImageVector,
    tint: Color,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    PanelCard(modifier = Modifier.fillMaxWidth(), radius = 18.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Khung trắng bo góc chứa mã QR (hoặc spinner / thông báo không tạo được). */
@Composable
private fun QrCardView(state: QrState, unavailableText: String) {
    PanelCard(modifier = Modifier.fillMaxWidth(), radius = 24.dp) {
        Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp).padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                is QrState.Ready -> Image(
                    bitmap = state.bitmap.asImageBitmap(),
                    contentDescription = appStrings().shareSheet.qrImageDesc,
                    filterQuality = FilterQuality.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
                QrState.Loading -> CircularProgressIndicator(color = Primary, strokeWidth = 3.dp)
                QrState.Unavailable -> UnavailableContent(unavailableText)
            }
        }
    }
}

/** Nội dung khi không tạo được mã: icon mờ + dòng giải thích ngắn. */
@Composable
private fun UnavailableContent(text: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Rounded.QrCode2,
            contentDescription = null,
            tint = TextSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
    }
}

/** Spinner căn giữa trong lúc đọc thông tin SIM ở luồng nền. */
@Composable
private fun LoadingCenter() {
    Box(
        modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp).padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Primary, strokeWidth = 3.dp)
    }
}

/** Thông báo giữa màn (vd không có SIM). */
@Composable
private fun EmptyInfo(text: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Rounded.SimCard, contentDescription = null, tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
    }
}

/** Dòng hướng dẫn ngắn dưới mã QR. */
@Composable
private fun HelperText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary,
        textAlign = TextAlign.Center
    )
}

/** Hàng 2 nút: TẢI ảnh về máy (trung tính) + CHIA SẺ (tô [accent]). */
@Composable
private fun QrActionRow(bitmap: Bitmap, accent: Color) {
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var sharing by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ActionPillButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.Download,
            label = if (saving) appStrings().shareSheet.saving else appStrings().shareSheet.saveToDevice,
            filled = false,
            accent = accent,
            enabled = !saving
        ) {
            saving = true
            // NonCancellable + applicationContext: đóng sheet lúc đang lưu vẫn ghi xong ảnh + hiện toast.
            scope.launch {
                withContext(NonCancellable) {
                    val uri = withContext(Dispatchers.IO) {
                        QrShare.saveToGallery(appContext, bitmap, "CallHS_QR_${System.currentTimeMillis()}.png")
                    }
                    saving = false
                    CallActions.toast(
                        appContext,
                        if (uri != null) appStrings().shareSheet.savedToGallery else appStrings().shareSheet.saveFailed,
                        if (uri != null) AppToastType.Success else AppToastType.Error
                    )
                }
            }
        }
        ActionPillButton(
            modifier = Modifier.weight(1f),
            icon = Icons.Rounded.Share,
            label = if (sharing) appStrings().shareSheet.opening else appStrings().shareSheet.share,
            filled = true,
            accent = accent,
            enabled = !sharing
        ) {
            sharing = true
            scope.launch {
                val ok = ShareContact.shareBitmap(context, bitmap)
                sharing = false
                if (!ok) CallActions.toast(appContext, appStrings().shareSheet.shareFailed, AppToastType.Error)
            }
        }
    }
}

/** Nút pill: [filled]=nền [accent] chữ trắng; ngược lại nền xám nhạt chữ tối. Mờ đi khi tắt. */
@Composable
private fun ActionPillButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    filled: Boolean,
    accent: Color,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val bg = if (filled) (if (enabled) accent else accent.copy(alpha = 0.5f)) else FieldSurface
    val fg = if (filled) Color.White else TextPrimary
    Box(
        modifier = modifier
            .height(50.dp)
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
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = fg, maxLines = 1)
        }
    }
}

// ---------------------------------------------------------------------------------------------------
// Tăng độ sáng màn hình khi hiển thị mã QR (dễ cho người khác quét), trả lại mức cũ khi rời màn.
// ---------------------------------------------------------------------------------------------------

@Composable
private fun BoostScreenBrightness(context: Context) {
    DisposableEffect(context) {
        val window = context.findActivity()?.window
        val previous = window?.attributes?.screenBrightness
        window?.let { w -> w.attributes = w.attributes.apply { screenBrightness = 1f } }
        onDispose {
            window?.let { w ->
                w.attributes = w.attributes.apply {
                    // BRIGHTNESS_OVERRIDE_NONE (-1f) = bỏ ghi đè, quay lại độ sáng hệ thống/tự động.
                    screenBrightness = previous ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                }
            }
        }
    }
}

/** Truy ngược Activity từ Context (qua chuỗi ContextWrapper). */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

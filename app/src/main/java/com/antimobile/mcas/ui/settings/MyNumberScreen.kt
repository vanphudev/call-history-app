package com.antimobile.mcas.ui.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.antimobile.mcas.ui.theme.AccentBlue
import com.antimobile.mcas.ui.theme.AccentBlueBg
import com.antimobile.mcas.ui.theme.AccentRed
import com.antimobile.mcas.ui.theme.AppBackground
import com.antimobile.mcas.ui.theme.BrandSoft
import com.antimobile.mcas.ui.theme.CardFill
import com.antimobile.mcas.ui.theme.FieldSurface
import com.antimobile.mcas.ui.theme.Primary
import com.antimobile.mcas.ui.theme.TextPrimary
import com.antimobile.mcas.ui.theme.TextSecondary
import com.antimobile.mcas.i18n.appStrings
import com.antimobile.mcas.ui.components.AppToastType
import com.antimobile.mcas.util.CallActions
import com.antimobile.mcas.util.Carrier
import com.antimobile.mcas.util.MyNumberStore
import com.antimobile.mcas.util.SimInfo
import com.antimobile.mcas.util.formatPhone
import com.antimobile.mcas.util.hasPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Màn "SỐ ĐIỆN THOẠI CỦA TÔI" (trong Cài đặt).
 *
 * Vì sao có màn này: Android/nhà mạng VN hầu như KHÔNG cung cấp số thuê bao từ SIM, nên pattern
 * {phonesim1}/{phonesim2} trong mẫu tin nhắn và "QR của tôi" không có số để điền. Ở đây:
 *  - Nếu máy/nhà mạng ĐỌC ĐƯỢC số tự động (đã cấp quyền) → hiển thị CHỈ XEM, KHÔNG cho sửa.
 *  - Nếu KHÔNG đọc được → cho người dùng NHẬP TAY (chỉ chữ số), lưu tại máy ([MyNumberStore]).
 *
 * Quy tắc hiển thị khe: máy đang có 2 SIM → hiện SIM 1 + SIM 2; còn lại → chỉ SIM 1 (xem
 * [SimInfo.readDeviceSims]). Xử lý bàn phím: nội dung CUỘN được, nút Lưu tự nâng trên bàn phím, chạm
 * ra ngoài để ẩn bàn phím.
 */
@Composable
fun MyNumberScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val s = appStrings()
    val focusManager = LocalFocusManager.current

    // Xin quyền READ_PHONE_NUMBERS (để THỬ đọc số tự động). Cấp/không cấp đều nạp lại danh sách khe:
    // đọc được → khoá ô; không đọc được → cho nhập tay. Không cấp cũng không sao (nhập tay vẫn chạy).
    var reload by remember { mutableIntStateOf(0) }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { reload++ }
    LaunchedEffect(Unit) {
        if (!hasPermission(context, Manifest.permission.READ_PHONE_NUMBERS)) {
            permLauncher.launch(Manifest.permission.READ_PHONE_NUMBERS)
        }
    }
    val device by produceState<SimInfo.DeviceSims?>(initialValue = null, reload) {
        value = withContext(Dispatchers.IO) { SimInfo.readDeviceSims(context) }
    }

    // Ô nhập theo khe (CHỈ cho khe không đọc tự động được), gieo từ số đã lưu. Tạo lại khi [device] đổi
    // (chỉ xảy ra sau khi cấp quyền) — lúc đó người dùng chưa gõ gì đáng kể nên không mất dữ liệu.
    val inputs = remember(device) {
        mutableStateMapOf<Int, TextFieldValue>().apply {
            device?.slots?.filter { !it.canAutoRead }?.forEach { slot ->
                put(slot.slotIndex, TextFieldValue(slot.manualNumber))
            }
        }
    }

    val slots = device?.slots.orEmpty()
    val editableSlots = slots.filter { !it.canAutoRead }
    // Số hợp lệ để LƯU: rỗng (chưa nhập) HOẶC 9–11 chữ số (số VN: nội địa 10 chữ số đầu 0, dạng 84 là 11).
    // Sai → chặn Lưu + hiện lỗi dưới ô.
    fun errorFor(slotIndex: Int): String? {
        val digits = inputs[slotIndex]?.text?.filter { it.isDigit() }.orEmpty()
        return when {
            digits.isEmpty() -> null
            digits.length !in 9..11 -> s.myNumber.errorInvalid
            else -> null
        }
    }
    val hasEditable = editableSlots.isNotEmpty()
    val canSave = editableSlots.all { errorFor(it.slotIndex) == null }

    fun save() {
        if (!canSave) return
        focusManager.clearFocus()
        editableSlots.forEach { slot ->
            val digits = inputs[slot.slotIndex]?.text?.filter { it.isDigit() }.orEmpty()
            MyNumberStore.set(context, slot.slotIndex, digits)
        }
        if (hasEditable) CallActions.toast(context, s.myNumber.saved, AppToastType.Success)
        onBack()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopBar(onBack = { focusManager.clearFocus(); onBack() }) },
        bottomBar = {
            if (device != null) {
                SaveBar(
                    label = if (hasEditable) s.myNumber.save else s.myNumber.done,
                    enabled = canSave,
                    onClick = { if (hasEditable) save() else onBack() }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            IntroCard()
            Spacer(Modifier.height(18.dp))

            if (device == null) {
                LoadingRow()
            } else {
                slots.forEachIndexed { index, slot ->
                    if (index > 0) Spacer(Modifier.height(18.dp))
                    SlotBlock(
                        slot = slot,
                        value = inputs[slot.slotIndex] ?: TextFieldValue(""),
                        onValueChange = { tv ->
                            // CHỈ giữ chữ số (yêu cầu: chỉ cho nhập số).
                            inputs[slot.slotIndex] = tv.copy(text = tv.text.filter { it.isDigit() })
                        },
                        error = errorFor(slot.slotIndex),
                        onImeDone = { focusManager.clearFocus() }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    val s = appStrings()
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
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = s.common.back, tint = TextPrimary, modifier = Modifier.size(23.dp))
        }
        Spacer(Modifier.size(8.dp))
        Text(
            s.settings.myNumberTitle,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Thẻ giải thích: vì sao cần số này & dùng vào đâu. */
@Composable
private fun IntroCard() {
    val s = appStrings().myNumber
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BrandSoft)
            .padding(14.dp)
    ) {
        Icon(Icons.Rounded.Info, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = s.introTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = s.introBody,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

/** Một khe SIM: tiêu đề (nhãn + nhà mạng) rồi ô CHỈ XEM (nếu đọc tự động được) hoặc ô NHẬP TAY. */
@Composable
private fun SlotBlock(
    slot: SimInfo.SlotInfo,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    error: String?,
    onImeDone: () -> Unit
) {
    val s = appStrings().myNumber
    val statusText = when {
        slot.present && slot.carrier != null -> slot.carrier
        slot.present -> s.simPresent
        else -> s.simAbsent
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(AccentBlueBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.SimCard, contentDescription = null, tint = AccentBlue, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(slot.label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        Spacer(Modifier.width(8.dp))
        Text("· $statusText", style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    Spacer(Modifier.height(8.dp))

    if (slot.canAutoRead) {
        // Đọc tự động được → CHỈ XEM (khoá), không cho sửa.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CardFill)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatPhone(slot.autoNumber),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Primary, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(s.autoRead, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    } else {
        // Không đọc được → cho NHẬP TAY (chỉ số).
        val invalid = error != null
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(FieldSurface)
                .then(if (invalid) Modifier.border(1.dp, AccentRed, RoundedCornerShape(12.dp)) else Modifier)
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            if (value.text.isEmpty()) {
                Text(s.inputHint, style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
                cursorBrush = SolidColor(Primary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onImeDone() }),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(6.dp))
        // Nhà mạng suy TỪ SỐ đang gõ (live) — cùng nguồn Carrier.of với màn Cước → nhất quán "số này dùng tính cước".
        val liveCarrier = Carrier.of(value.text)
        Text(
            text = when {
                error != null -> error
                liveCarrier != null -> s.carrierHint(liveCarrier)
                else -> s.enterHint(slot.label)
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (invalid) AccentRed else TextSecondary
        )
    }
}

@Composable
private fun LoadingRow() {
    Text(
        text = appStrings().myNumber.checkingSim,
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary,
        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
    )
}

/** Thanh dưới cố định với nút Lưu/Xong — tự nâng trên bàn phím khi IME hiện (ime ∪ navigationBars). */
@Composable
private fun SaveBar(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppBackground)
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))
            .padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (enabled) Primary else Primary.copy(alpha = 0.5f))
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

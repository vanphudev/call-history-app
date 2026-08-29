package com.antimobile.mcas.ui.donate

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antimobile.mcas.data.donate.BankApp
import com.antimobile.mcas.i18n.DonateStrings
import com.antimobile.mcas.i18n.appStrings
import com.antimobile.mcas.ui.components.PanelCard
import com.antimobile.mcas.ui.components.AppToastType
import com.antimobile.mcas.ui.components.RemoteImage
import com.antimobile.mcas.ui.components.RemoteImageState
import com.antimobile.mcas.ui.components.rememberRemoteImage
import com.antimobile.mcas.ui.components.shimmer
import com.antimobile.mcas.ui.theme.AccentGreen
import com.antimobile.mcas.ui.theme.AccentRed
import com.antimobile.mcas.ui.theme.AppBackground
import com.antimobile.mcas.ui.theme.BrandSoft
import com.antimobile.mcas.ui.theme.CardFill
import com.antimobile.mcas.ui.theme.DividerColor
import com.antimobile.mcas.ui.theme.HairlineBorder
import com.antimobile.mcas.ui.theme.Primary
import com.antimobile.mcas.ui.theme.TextPrimary
import com.antimobile.mcas.ui.theme.TextSecondary
import com.antimobile.mcas.util.CallActions
import com.antimobile.mcas.util.Donate
import com.antimobile.mcas.util.NetworkUtil
import com.antimobile.mcas.util.QrShare
import com.antimobile.mcas.util.formatDong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Màn ỦNG HỘ NHÀ PHÁT TRIỂN — hoàn toàn TỰ NGUYỆN, minh bạch. Bố cục (hiện tuần tự có hoạt ảnh):
 *  1. Hero: icon trái tim đập, lời nhắn tự nguyện.
 *  2. Chọn số tiền: 2k…20k + "Tuỳ tâm" (mã mở) + "Số khác…".
 *  3. Mã QR VietQR (đổi theo số tiền, crossfade + vệt quét); ngoại tuyến → mã dự phòng dựng tại máy. Lưu/Chia sẻ.
 *  4. Thông tin chuyển khoản: ngân hàng / số TK / chủ TK / số tiền / nội dung — bấm để sao chép.
 *  5. Mở nhanh app ngân hàng (deeplink điền sẵn thông tin nếu ngân hàng hỗ trợ).
 *  6. Chân trang: nhấn mạnh tinh thần tự nguyện + lời cảm ơn.
 */
@Composable
fun DonateScreen(vm: DonateViewModel, onBack: () -> Unit) {
    val s = appStrings().donate
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    var shown by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    LaunchedEffect(Unit) { shown = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            .statusBarsPadding()
            // Khi bàn phím hiện, vùng nội dung CO LẠI đúng phần bị che → vùng cuộn ngắn lại, ô nhập (TextField
            // tự kéo mình vào tầm nhìn khi có tiêu điểm) được đẩy lên TRÊN bàn phím. (Cùng cách CallListScreen.)
            .imePadding()
            // Chạm ra NGOÀI ô nhập (vùng không bấm được) → bỏ tiêu điểm & ẩn bàn phím. Chạm lên phần tử bấm
            // được (nút/viên/dòng) đã bị chúng "tiêu thụ" nên onTap không chạy → không phá thao tác của chúng.
            .pointerInput(Unit) { detectTapGestures(onTap = { focusManager.clearFocus() }) }
    ) {
        DonateTopBar(title = s.screenTitle, onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = navBottom + 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            RevealItem(shown, 0) { DonateHero(s) }
            RevealItem(shown, 1) {
                Column {
                    SectionLabel(s.amountSection)
                    Spacer(Modifier.height(10.dp))
                    AmountPicker(selected = vm.selectedAmount, onSelect = vm::select)
                }
            }
            RevealItem(shown, 2) { QrCard(s, vm.selectedAmount) }
            RevealItem(shown, 3) { AccountCard(s, vm.selectedAmount) }
            RevealItem(shown, 4) { BankAppsCard(s, vm.bankApps, vm.selectedAmount) }
            RevealItem(shown, 5) { VoluntaryFooter(s) }
        }
    }
}

/** Thanh trên: nút quay lại + tiêu đề (đồng bộ với các màn khác). */
@Composable
private fun DonateTopBar(title: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = appStrings().common.back,
                tint = TextPrimary,
                modifier = Modifier.size(23.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
    }
}

/** Bọc mỗi khối trong hiệu ứng HIỆN DẦN có ĐỘ TRỄ tăng dần theo [index] → hiệu ứng đổ tầng khi mở màn. */
@Composable
private fun RevealItem(visible: Boolean, index: Int, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(420, delayMillis = index * 70)) +
            slideInVertically(tween(420, delayMillis = index * 70), initialOffsetY = { it / 6 })
    ) { content() }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary,
        modifier = Modifier.padding(start = 4.dp)
    )
}

/** Hero: nền gradient xanh lá→teal, icon trái tim đập nhịp, tiêu đề + lời nhắn tự nguyện (chữ trắng). */
@Composable
private fun DonateHero(s: DonateStrings) {
    val infinite = rememberInfiniteTransition(label = "hero")
    val beat by infinite.animateFloat(
        initialValue = 1f, targetValue = 1.16f,
        animationSpec = infiniteRepeatable(tween(820, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "beat"
    )
    val glow by infinite.animateFloat(
        initialValue = 0.16f, targetValue = 0.30f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF2FA35B), Color(0xFF12A79E))))
    ) {
        Box(Modifier.align(Alignment.TopEnd).offset(x = 34.dp, y = (-34).dp).size(150.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)))
        Box(Modifier.align(Alignment.BottomStart).offset(x = (-26).dp, y = 30.dp).size(120.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.06f)))
        Column(Modifier.padding(22.dp)) {
            Box(
                // Đọc [glow] ở PHA VẼ (drawBehind) chứ không phải pha dựng → chỉ vẽ lại, KHÔNG recompose mỗi khung
                // hình (giống cách [beat] dùng graphicsLayer). Tránh "bão recompose" cả hero khi màn đang mở.
                modifier = Modifier.size(66.dp).clip(CircleShape).drawBehind { drawCircle(Color.White.copy(alpha = glow)) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Favorite,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(34.dp).graphicsLayer { scaleX = beat; scaleY = beat }
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(s.heroTitle, style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(s.heroMessage, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.94f), lineHeight = 20.sp)
        }
    }
}

/**
 * Khối CHỌN SỐ TIỀN: bình thường hiện các "viên" chọn nhanh; bấm "Số khác…" → CẢ khối biến thành ô NHẬP
 * (không dùng hộp thoại), có HOẠT ẢNH chuyển đổi qua lại (fade + phóng nhẹ + đổi cỡ mượt). Nhập tối đa
 * [Donate.MAX_AMOUNT]; vượt trần → báo đỏ, khoá nút "Xong".
 */
@Composable
private fun AmountPicker(selected: Long, onSelect: (Long) -> Unit) {
    var editing by rememberSaveable { mutableStateOf(false) }
    AnimatedContent(
        targetState = editing,
        transitionSpec = {
            (fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.96f))
                .togetherWith(fadeOut(tween(140)) + scaleOut(tween(140), targetScale = 0.96f))
                .using(SizeTransform(clip = false))
        },
        label = "amountMode"
    ) { isEditing ->
        if (isEditing) {
            AmountInput(
                initial = if (selected > 0L && selected !in Donate.PRESET_AMOUNTS) selected else 0L,
                onCancel = { editing = false },
                onConfirm = { amt -> onSelect(amt); editing = false }
            )
        } else {
            AmountSelector(selected = selected, onSelect = onSelect, onCustom = { editing = true })
        }
    }
}

/**
 * Ô NHẬP số tiền tuỳ ý (thay cho khối chọn nhanh khi bấm "Số khác…"): ô nhập nằm TRÊN, dưới là hai nút
 * [Huỷ] · [Xong] chia ĐỀU. Chỉ nhận chữ số; vượt [Donate.MAX_AMOUNT] → viền + chữ ĐỎ, khoá "Xong".
 */
@Composable
private fun AmountInput(initial: Long, onCancel: () -> Unit, onConfirm: (Long) -> Unit) {
    val s = appStrings().donate
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var text by remember { mutableStateOf(if (initial > 0L) initial.toString() else "") }
    val parsed = text.toLongOrNull() ?: 0L
    val tooBig = parsed > Donate.MAX_AMOUNT
    val valid = parsed in 1L..Donate.MAX_AMOUNT
    // Bỏ tiêu điểm (ẩn bàn phím) TRƯỚC khi rời ô nhập — dùng cho Huỷ / Xong / phím "Done".
    val done: (Long?) -> Unit = { amt -> focusManager.clearFocus(); if (amt != null) onConfirm(amt) else onCancel() }

    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Column(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = text,
            onValueChange = { new -> text = new.filter { it.isDigit() }.trimStart('0').take(9) },
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
            singleLine = true,
            isError = tooBig,
            textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            placeholder = { Text(s.customFieldLabel, style = MaterialTheme.typography.bodyLarge) },
            suffix = { Text("đ", fontWeight = FontWeight.Bold) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (valid) done(parsed) else focusManager.clearFocus() }),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (tooBig) AccentRed else Primary,
                unfocusedBorderColor = if (tooBig) AccentRed else HairlineBorder,
                errorBorderColor = AccentRed,
                cursorColor = Primary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )
        Spacer(Modifier.height(8.dp))
        // Dòng phản hồi: hợp lệ → xem trước có định dạng (xanh); còn lại → nhắc trần, ĐỎ khi vượt.
        Text(
            text = if (valid) formatDong(parsed) else s.customMax,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (tooBig) AccentRed else if (valid) Primary else TextSecondary,
            modifier = Modifier.padding(start = 4.dp)
        )
        Spacer(Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            WideButton(text = appStrings().common.cancel, filled = false, enabled = true, onClick = { done(null) })
            WideButton(text = s.confirm, filled = true, enabled = valid, onClick = { if (valid) done(parsed) })
        }
    }
}

/** Nút rộng chia đều (weight) trong một hàng: [filled] = nút chính (nền xanh, chữ trắng) / phụ (nền nhạt). */
@Composable
private fun RowScope.WideButton(text: String, filled: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val bg = if (filled) (if (enabled) Primary else Primary.copy(alpha = 0.35f)) else CardFill
    val fg = if (filled) Color.White else TextPrimary
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 13.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = fg, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    }
}

/** Hàng chọn số tiền: các mức gợi ý + "Tuỳ tâm" (mã mở) + "Số khác…" (nhập tuỳ ý). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AmountSelector(selected: Long, onSelect: (Long) -> Unit, onCustom: () -> Unit) {
    val s = appStrings().donate
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Donate.PRESET_AMOUNTS.forEach { amt ->
            AmountChip(text = formatDong(amt), selected = selected == amt, icon = null, onClick = { onSelect(amt) })
        }
        AmountChip(text = s.amountOpen, selected = selected == 0L, icon = Icons.Rounded.Favorite, onClick = { onSelect(0L) })
        val custom = selected > 0L && selected !in Donate.PRESET_AMOUNTS
        AmountChip(
            text = if (custom) formatDong(selected) else s.amountCustom,
            selected = custom,
            icon = Icons.Rounded.Edit,
            onClick = onCustom
        )
    }
}

/** Một "viên" chọn số tiền: đổi màu + phóng nhẹ + hiện dấu tích khi được chọn (đều có hoạt ảnh). */
@Composable
private fun AmountChip(text: String, selected: Boolean, icon: ImageVector?, onClick: () -> Unit) {
    val bg by animateColorAsState(if (selected) Primary else CardFill, tween(220), label = "bg")
    val fg by animateColorAsState(if (selected) Color.White else TextPrimary, tween(220), label = "fg")
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
        label = "scale"
    )
    Row(
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedVisibility(visible = selected) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Check, null, tint = fg, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(5.dp))
            }
        }
        if (icon != null && !selected) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
        }
        Text(text, color = fg, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

/** Thẻ MÃ QR: ảnh VietQR theo số tiền (crossfade + vệt quét); ngoại tuyến → mã dự phòng; Lưu/Chia sẻ. */
@Composable
private fun QrCard(s: DonateStrings, amount: Long) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val url = remember(amount) { Donate.imageUrl(amount) }
    var reload by remember { mutableStateOf(0) }
    val state = rememberRemoteImage(url, reload)
    val isError = state is RemoteImageState.Error

    // Mã dự phòng dựng TẠI MÁY — chỉ tạo khi ảnh mạng lỗi (rẻ, chạy ở luồng nền).
    val offlineQr by produceState<Bitmap?>(null, amount, isError) {
        value = if (isError) withContext(Dispatchers.Default) {
            runCatching { QrShare.encode(Donate.offlinePayload(amount), 512) }.getOrNull()
        } else null
    }
    val displayBitmap: Bitmap? = (state as? RemoteImageState.Success)?.bitmap ?: offlineQr

    PanelCard(modifier = Modifier.fillMaxWidth(), radius = 24.dp) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                s.qrSection,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (amount > 0L) s.qrHint else s.qrOpenAmountNote,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier.size(248.dp).clip(RoundedCornerShape(20.dp)).background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Crossfade(targetState = state, animationSpec = tween(320), label = "qr") { st ->
                    when (st) {
                        is RemoteImageState.Success -> QrImageWithScan(st.image)
                        is RemoteImageState.Loading -> QrLoading(s)
                        is RemoteImageState.Error -> {
                            val fb = offlineQr
                            if (fb != null) QrImageWithScan(fb.asImageBitmap(), caption = s.qrOfflineFallback)
                            else QrLoading(s)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            AnimatedContent(
                targetState = amount,
                transitionSpec = {
                    (slideInVertically(tween(320)) { h -> h / 2 } + fadeIn(tween(320))) togetherWith
                        (slideOutVertically(tween(320)) { h -> -h / 2 } + fadeOut(tween(320)))
                },
                label = "amount"
            ) { amt ->
                Text(
                    text = if (amt > 0L) formatDong(amt) else s.amountOpen,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Primary
                )
            }

            AnimatedVisibility(visible = isError) {
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.CloudOff, null, tint = TextSecondary, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(s.qrRetry, color = Primary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { reload++ }.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QrActionButton(Icons.Rounded.SaveAlt, s.saveQr, enabled = displayBitmap != null) {
                    val bmp = displayBitmap ?: return@QrActionButton
                    scope.launch {
                        val name = "mcas_ung_ho_" + (if (amount > 0L) amount.toString() else "mo") + ".png"
                        val uri = withContext(Dispatchers.IO) { QrShare.saveToGallery(context, bmp, name) }
                        CallActions.toast(
                            context,
                            if (uri != null) s.qrSaved else s.qrSaveFailed,
                            if (uri != null) AppToastType.Success else AppToastType.Error
                        )
                    }
                }
                QrActionButton(Icons.Rounded.Share, s.shareQr, enabled = displayBitmap != null) {
                    val bmp = displayBitmap ?: return@QrActionButton
                    scope.launch { shareQr(context, bmp, amount, s) }
                }
            }
        }
    }
}

/** Ảnh QR + lớp vệt quét chuyển động; [caption] hiện nhãn "mã dự phòng ngoại tuyến" khi có. */
@Composable
private fun QrImageWithScan(image: ImageBitmap, caption: String? = null) {
    Box(Modifier.fillMaxSize().padding(14.dp)) {
        Image(
            bitmap = image,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        ScanLineOverlay(Modifier.fillMaxSize())
        if (caption != null) {
            Text(
                caption,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF3C4043),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFEDEFF2))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

/** Vệt quét xanh mềm chạy lên xuống trên mã QR — thuần trang trí (ảnh lưu/chia sẻ vẫn sạch). */
@Composable
private fun ScanLineOverlay(modifier: Modifier) {
    val infinite = rememberInfiniteTransition(label = "scan")
    val y by infinite.animateFloat(
        initialValue = 0.08f, targetValue = 0.92f,
        animationSpec = infiniteRepeatable(tween(1900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scanY"
    )
    val green = Primary
    Canvas(modifier) {
        val cy = size.height * y
        val band = size.height * 0.09f
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color.Transparent, green.copy(alpha = 0.22f), Color.Transparent),
                startY = cy - band, endY = cy + band
            ),
            topLeft = Offset(0f, cy - band),
            size = Size(size.width, band * 2)
        )
        drawLine(green.copy(alpha = 0.5f), Offset(0f, cy), Offset(size.width, cy), strokeWidth = 2f)
    }
}

@Composable
private fun QrLoading(s: DonateStrings) {
    Box(Modifier.fillMaxSize().shimmer(RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Primary, strokeWidth = 3.dp, modifier = Modifier.size(34.dp))
            Spacer(Modifier.height(10.dp))
            Text(s.qrLoading, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Composable
private fun RowScope.QrActionButton(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val tint = if (enabled) Primary else TextSecondary
    Row(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(BrandSoft)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = tint, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

/** Thẻ THÔNG TIN CHUYỂN KHOẢN — mỗi dòng bấm để sao chép (có dấu tích phản hồi). */
@Composable
private fun AccountCard(s: DonateStrings, amount: Long) {
    PanelCard(modifier = Modifier.fillMaxWidth(), radius = 24.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Text(
                s.accountSection,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp)
            )
            CopyRow(s.bankLabel, Donate.BANK_NAME_VI, copyValue = null)
            RowDivider()
            CopyRow(s.accountNoLabel, Donate.ACCOUNT_NO, copyValue = Donate.ACCOUNT_NO, highlight = true)
            RowDivider()
            CopyRow(s.accountNameLabel, Donate.ACCOUNT_NAME, copyValue = Donate.ACCOUNT_NAME)
            RowDivider()
            CopyRow(
                s.amountRowLabel,
                if (amount > 0L) formatDong(amount) else s.amountOpenValue,
                copyValue = if (amount > 0L) amount.toString() else null
            )
            RowDivider()
            CopyRow(s.messageLabel, Donate.ADD_INFO, copyValue = Donate.ADD_INFO)
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(color = DividerColor, thickness = 1.dp, modifier = Modifier.padding(horizontal = 20.dp))
}

@Composable
private fun CopyRow(label: String, value: String, copyValue: String?, highlight: Boolean = false) {
    val context = LocalContext.current
    val s = appStrings().donate
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) { if (copied) { delay(1400); copied = false } }

    val clickable = if (copyValue != null) {
        Modifier.clickable {
            copyToClipboard(context, label, copyValue)
            copied = true
            CallActions.toast(context, s.copied, AppToastType.Success)
        }
    } else Modifier

    Row(
        modifier = Modifier.fillMaxWidth().then(clickable).padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(2.dp))
            Text(
                value,
                style = if (highlight) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                color = if (highlight) Primary else TextPrimary,
                fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium
            )
        }
        if (copyValue != null) {
            Spacer(Modifier.width(10.dp))
            Crossfade(targetState = copied, animationSpec = tween(200), label = "copy") { c ->
                Box(
                    modifier = Modifier.size(38.dp).clip(CircleShape).background(BrandSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (c) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                        contentDescription = null,
                        tint = if (c) AccentGreen else Primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/** Thẻ MỞ NHANH APP NGÂN HÀNG — lưới 4 cột logo; bấm mở deeplink (điền sẵn nếu ngân hàng hỗ trợ). */
@Composable
private fun BankAppsCard(s: DonateStrings, apps: List<BankApp>, amount: Long) {
    var expanded by remember { mutableStateOf(false) }
    val collapsed = 8
    val shownApps = if (expanded || apps.size <= collapsed) apps else apps.take(collapsed)

    PanelCard(modifier = Modifier.fillMaxWidth(), radius = 24.dp) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp).animateContentSize()) {
            Text(s.bankAppsSection, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(s.bankAppsHint, style = MaterialTheme.typography.bodySmall, color = TextSecondary, lineHeight = 18.sp)
            Spacer(Modifier.height(14.dp))

            shownApps.chunked(4).forEach { rowApps ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rowApps.forEach { app -> BankAppItem(app, amount, Modifier.weight(1f)) }
                    repeat(4 - rowApps.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(8.dp))
            }

            if (apps.size > collapsed) {
                Text(
                    text = if (expanded) s.bankAppsShowLess else s.bankAppsShowAll,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { expanded = !expanded }
                        .padding(vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun BankAppItem(app: BankApp, amount: Long, modifier: Modifier) {
    val context = LocalContext.current
    val s = appStrings().donate
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable {
                // Deeplink đi qua trang chuyển hướng https của VietQR → cần mạng. Ngoại tuyến thì trình duyệt
                // vẫn mở nhưng trang không tải được (app ngân hàng không bật) → báo rõ thay vì im lặng.
                if (!NetworkUtil.isOnline(context)) {
                    CallActions.toast(context, s.bankAppNeedNetwork, AppToastType.Warning)
                    return@clickable
                }
                val ok = runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(Donate.bankAppDeeplink(app.appId, amount)))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }.isSuccess
                if (!ok) CallActions.toast(context, s.bankAppOpenFailed, AppToastType.Error)
            }
            .padding(vertical = 10.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Box(
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                if (app.logoUrl.isNotBlank()) {
                    RemoteImage(
                        url = app.logoUrl,
                        contentDescription = app.appName,
                        modifier = Modifier.fillMaxSize().padding(6.dp),
                        contentScale = ContentScale.Fit,
                        reqSizePx = 220, // logo hiện trong ô ~52dp → giảm mẫu, khỏi giữ bitmap nguyên cỡ ~512px
                        fallback = { BankFallbackIcon() }
                    )
                } else {
                    BankFallbackIcon()
                }
            }
            if (app.autofill) {
                Box(
                    modifier = Modifier
                        .offset(x = 3.dp, y = (-3).dp)
                        .size(13.dp)
                        .clip(CircleShape)
                        // Xanh lá đậm CỐ ĐỊNH cho cả 2 chế độ: dấu tích trắng trên nền này luôn đủ tương phản
                        // (token AccentGreen sáng lên ở chế độ tối → trắng trên nền sáng sẽ mờ).
                        .background(Color(0xFF1E8E3E))
                        .semantics { contentDescription = s.bankAppPrefill },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(9.dp))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            app.appName,
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BankFallbackIcon() {
    Box(
        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp)).background(BrandSoft),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Rounded.AccountBalance, null, tint = Primary, modifier = Modifier.size(24.dp))
    }
}

/** Chân trang: nhấn mạnh tinh thần TỰ NGUYỆN + lời cảm ơn. */
@Composable
private fun VoluntaryFooter(s: DonateStrings) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(BrandSoft).padding(18.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.VolunteerActivism, null, tint = Primary, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(s.footerTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Spacer(Modifier.height(8.dp))
                Text(s.footerMessage, style = MaterialTheme.typography.bodySmall, color = TextSecondary, lineHeight = 19.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            s.thankYou,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Primary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun copyToClipboard(context: Context, label: String, value: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cm?.setPrimaryClip(ClipData.newPlainText(label, value))
}

/** Ghi ảnh QR ra cache rồi chia sẻ qua ACTION_SEND kèm thông tin chuyển khoản dạng chữ. */
private suspend fun shareQr(context: Context, bmp: Bitmap, amount: Long, s: DonateStrings) {
    val uri = withContext(Dispatchers.IO) { QrShare.cacheForShare(context, bmp) }
    if (uri == null) {
        CallActions.toast(context, s.qrSaveFailed, AppToastType.Error)
        return
    }
    val body = buildString {
        append(Donate.BANK_SHORT).append(" · ").append(Donate.ACCOUNT_NO).append(" · ").append(Donate.ACCOUNT_NAME)
        append('\n').append(if (amount > 0L) formatDong(amount) else s.amountOpenValue)
        append('\n').append(Donate.ADD_INFO)
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, s.shareSubject)
        putExtra(Intent.EXTRA_TEXT, body)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, s.shareQr).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

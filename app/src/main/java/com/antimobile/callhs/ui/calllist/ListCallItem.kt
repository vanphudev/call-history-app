package com.antimobile.callhs.ui.calllist

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.antimobile.callhs.data.model.CallEntry
import com.antimobile.callhs.ui.category.AvatarCategoryBadges
import com.antimobile.callhs.ui.components.Avatar
import com.antimobile.callhs.ui.components.CallTypeIcon
import com.antimobile.callhs.ui.components.PanelCard
import com.antimobile.callhs.ui.components.rememberPressHighlight
import com.antimobile.callhs.ui.theme.AccentBlue
import com.antimobile.callhs.ui.theme.AccentBlueBg
import com.antimobile.callhs.ui.theme.AccentGray
import com.antimobile.callhs.ui.theme.AccentGrayBg
import com.antimobile.callhs.ui.theme.AccentGreen
import com.antimobile.callhs.ui.theme.AccentRed
import com.antimobile.callhs.ui.theme.Primary
import com.antimobile.callhs.ui.theme.SoftGreen
import com.antimobile.callhs.ui.theme.SoftRed
import com.antimobile.callhs.ui.theme.TextPrimary
import com.antimobile.callhs.ui.theme.TextSecondary
import com.antimobile.callhs.util.CallResults
import com.antimobile.callhs.util.SpecialNumbers
import com.antimobile.callhs.util.TimeFormat
import com.antimobile.callhs.util.formatPhone

/**
 * Item cho màn DANH SÁCH: thẻ trắng riêng + bóng; chạm MỞ chi tiết; NHẤN GIỮ báo [onLongPress] kèm KHUNG
 * THẺ (px, toạ độ cửa sổ) để màn dựng context-menu phủ nền — các hành động Nhắn tin/Gọi/Zalo/Sao chép nằm
 * trong menu nhấn-giữ. KHÔNG còn cử chỉ vuốt (đã gỡ để cuộn dọc mượt, không dừng/kẹt vì nhận nhầm vuốt ngang).
 */
/** Đỉnh độ mờ của lớp phủ LOÉ SÁNG (xanh lá) khi một cuộc gọi vừa xong — đủ "đậm" mà chữ vẫn đọc được. */
private const val FLASH_PEAK_ALPHA = 0.7f

@Composable
fun ListCallItem(
    entry: CallEntry,
    onOpen: () -> Unit,
    onLongPress: (Rect) -> Unit,
    activeInMenu: Boolean = false,
    missedStreak: Int = 0,
    flash: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Giữ toạ độ thẻ ngoài snapshot state (không recompose mỗi frame khi cuộn) — đọc lúc nhấn giữ.
    val cardCoords = remember { CoordsHolder() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 7.dp)
            .onGloballyPositioned { cardCoords.value = it }
            // Khi item này đang được mở menu: ẩn HẲN bản gốc (giữ chỗ) để không lộ ripple/viền dưới bản
            // sao nổi. Bản sao ở overlay khi ĐÓNG đã về đúng scale gốc nên tráo lại liền mạch.
            .graphicsLayer { alpha = if (activeInMenu) 0f else 1f }
    ) {
        ListCallCard(
            entry = entry,
            missedStreak = missedStreak,
            flash = flash,
            onClick = onOpen,
            onLongClick = {
                // Chốt khung thẻ hiện tại rồi báo lên màn để dựng menu.
                val bounds = cardCoords.value?.takeIf { it.isAttached }?.boundsInWindow()
                if (bounds != null) {
                    onLongPress(bounds)
                }
            }
        )
    }
}

/**
 * Phần THẺ hiển thị của một item — tách riêng để context-menu tái dùng bản sao y hệt khi "nhấc" item
 * lên trên nền tối. Truyền [onClick]/[onLongClick] để gắn cử chỉ; khi null (bản sao ở overlay) thẻ chỉ
 * hiển thị, không nhận chạm. Cử chỉ đặt BÊN TRONG vùng bo góc; hiệu ứng nhấn là lớp phủ PHẲNG mờ toàn thẻ
 * ([rememberPressHighlight]) — không lan toả — tự bo theo góc thẻ nhờ PanelCard đã clip.
 */
@Composable
fun ListCallCard(
    entry: CallEntry,
    modifier: Modifier = Modifier,
    missedStreak: Int = 0,
    flash: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
) {
    val connected = CallResults.isConnected(entry.type, entry.durationSeconds)
    val resultColor = if (connected) AccentGreen else AccentRed   // line 1: xanh = kết nối, đỏ = không phản hồi

    PanelCard(modifier = modifier.fillMaxWidth(), radius = 18.dp) {
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
        // Cuộc gọi vừa kết thúc → nền xanh lá LOÉ lên nhanh rồi TẮT dần (đậm → nhạt → tắt), mượt. Vẽ ở tầng
        // DRAW (drawBehind) nên mỗi khung hình CHỈ vẽ lại, không recompose cả thẻ; nằm SAU nội dung Row (chữ/
        // ảnh vẫn hiện trên nền loé) và bo góc theo PanelCard đã clip. flash=false → alpha 0 → không vẽ gì.
        val flashAlpha = remember { Animatable(0f) }
        // Mỗi cuộc gọi chỉ LOÉ MỘT LẦN — kể cả khi item bị cuộn ra rồi vào lại (LazyColumn giữ state saveable
        // theo khoá item). Một id chỉ là "mới nhất" đúng một lần nên đánh dấu "đã loé" là đủ, không loé lại.
        val flashConsumed = rememberSaveable(entry.id) { mutableStateOf(false) }
        LaunchedEffect(flash) {
            if (flash) {
                if (flashConsumed.value) return@LaunchedEffect
                flashConsumed.value = true
                flashAlpha.snapTo(0f)
                flashAlpha.animateTo(1f, animationSpec = tween(180, easing = FastOutSlowInEasing))
                flashAlpha.animateTo(0f, animationSpec = tween(1400, easing = FastOutSlowInEasing))
            } else if (flashAlpha.value > 0f) {
                // Bị cuộc mới hơn CHIẾM lượt loé giữa chừng → tắt MƯỢT nền còn lại (tránh kẹt màu xanh).
                flashAlpha.animateTo(0f, animationSpec = tween(250, easing = FastOutSlowInEasing))
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val a = flashAlpha.value
                    if (a > 0f) drawRect(Primary.copy(alpha = a * FLASH_PEAK_ALPHA))
                }
        ) {
            Row(
                modifier = rowModifier.padding(horizontal = 12.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(46.dp)) {
                    Avatar(
                        label = entry.nameOrNumber,
                        photoUri = entry.photoUri,
                        isNamed = !entry.cachedName.isNullOrBlank(),
                        size = 46.dp,
                        specialIconRes = SpecialNumbers.of(entry.number)?.iconRes
                    )
                    AvatarCategoryBadges(entry.number, modifier = Modifier.align(Alignment.BottomEnd))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    // Line 1: icon hướng gọi + "TÊN danh bạ · SỐ" (nếu có tên) — dài thì CHẠY marquee lặp vô
                    // hạn để đọc được cả tên lẫn số. Tên = TextPrimary, số = màu theo kết quả; đều in đậm.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CallTypeIcon(type = entry.type, size = 18.dp, isVideo = entry.isVideo, tint = resultColor)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = listPrimaryLine(entry, resultColor, missedStreak),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier
                                .weight(1f)
                                .basicMarquee(iterations = Int.MAX_VALUE)
                        )
                        if (entry.isVolte) {
                            Spacer(Modifier.width(6.dp))
                            VolteBadge()
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = listInfoLine(entry),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(6.dp))
                Box(
                    modifier = Modifier.size(30.dp).clip(CircleShape).background(AccentGrayBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = AccentGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/** Giữ [LayoutCoordinates] của thẻ ngoài snapshot state — đọc lúc nhấn giữ để lấy khung item hiện tại. */
private class CoordsHolder(var value: LayoutCoordinates? = null)

/**
 * Line 1: "TÊN danh bạ  ·  SỐ" khi số ĐÃ lưu danh bạ; chưa lưu thì chỉ hiện SỐ.
 * Tên tô [TextPrimary] (trung tính), số tô [resultColor] theo kết quả cuộc gọi; đều để phần in đậm cho Text.
 * Nếu [missedStreak] >= 2 (chuỗi cuộc NHỠ liên tiếp) → thêm " (N)" đỏ ngay sau số.
 */
private fun listPrimaryLine(entry: CallEntry, resultColor: Color, missedStreak: Int): AnnotatedString = buildAnnotatedString {
    // Tên danh bạ; chưa lưu thì dùng TÊN KHẨN CẤP (113/114/115) → hiện "Cảnh sát  ·  113".
    val name = entry.cachedName?.takeIf { it.isNotBlank() } ?: SpecialNumbers.name(entry.number)
    if (name != null) {
        withStyle(SpanStyle(color = TextPrimary)) { append(name) }
        withStyle(SpanStyle(color = TextSecondary)) { append("  ·  ") }
    }
    withStyle(SpanStyle(color = resultColor)) { append(formatPhone(entry.number)) }
    if (missedStreak >= 2) {
        withStyle(SpanStyle(color = AccentRed, fontWeight = FontWeight.Bold)) { append("  ($missedStreak)") }
    }
}

/**
 * Line 2: "SIM · giờ · vùng · nhà mạng · <trạng thái tô màu NHẠT>".
 * Trạng thái: đã kết nối/đã nhận -> hiện THỜI LƯỢNG nghe máy; còn lại -> shortStatus theo loại cuộc gọi.
 */
private fun listInfoLine(entry: CallEntry): AnnotatedString = buildAnnotatedString {
    val info = listOfNotNull(
        entry.displaySimLabel,
        TimeFormat.itemClock(entry.dateMillis),
        entry.region.takeIf { it.isNotBlank() },
        entry.carrier
    ).joinToString("  ·  ")
    val connected = CallResults.isConnected(entry.type, entry.durationSeconds)
    withStyle(SpanStyle(color = TextSecondary)) {
        append(info)
        append("  ·  ")
    }
    withStyle(SpanStyle(color = if (connected) SoftGreen else SoftRed, fontWeight = FontWeight.SemiBold)) {
        append(
            if (connected) TimeFormat.durationLabel(entry.durationSeconds)
            else CallResults.shortStatus(entry.type, entry.durationSeconds)
        )
    }
}

@Composable
private fun VolteBadge() {
    Box(modifier = Modifier.clip(RoundedCornerShape(5.dp)).background(AccentBlueBg).padding(horizontal = 6.dp, vertical = 1.dp)) {
        Text("VoLTE", color = AccentBlue, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}

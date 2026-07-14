package com.antimobile.callhs.ui.calldetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.antimobile.callhs.data.model.CallEntry
import com.antimobile.callhs.data.model.CallType
import com.antimobile.callhs.i18n.appStrings
import com.antimobile.callhs.ui.components.CallTypeIcon
import com.antimobile.callhs.ui.theme.AccentBlue
import com.antimobile.callhs.ui.theme.AccentBlueBg
import com.antimobile.callhs.ui.theme.AccentGrayBg
import com.antimobile.callhs.ui.theme.AccentGreen
import com.antimobile.callhs.ui.theme.AccentGreenBg
import com.antimobile.callhs.ui.theme.AccentRed
import com.antimobile.callhs.ui.theme.AccentRedBg
import com.antimobile.callhs.ui.theme.SoftGreen
import com.antimobile.callhs.ui.theme.SoftRed
import com.antimobile.callhs.ui.theme.TextPrimary
import com.antimobile.callhs.ui.theme.TextSecondary
import com.antimobile.callhs.util.CallResults
import com.antimobile.callhs.util.TimeFormat
import com.antimobile.callhs.util.formatPhone

/** Chiều cao đồng nhất cho phần thu gọn của mọi item. */
private val ROW_HEIGHT = 64.dp

/** Icon hướng cuộc gọi (đi/đến) + khoảng cách sau nó — dùng để thụt phần chi tiết cho thẳng với text line 1. */
private val DIR_ICON_SIZE = 18.dp
private val DIR_ICON_GAP = 7.dp

// Tone kết quả ở line 2 + phần chi tiết — NHẠT hơn line 1 (line1 dùng AccentGreen/AccentRed đậm): dùng token
// dùng chung [SoftGreen]/[SoftRed] (theo chế độ Sáng/Tối) thay cho hằng số cục bộ trước đây.

/**
 * Item lịch sử cho màn CHI TIẾT (xem phone): 1 DÒNG trong card gộp, KHÔNG divider, KHÔNG vuốt.
 * Chạm để bung ngày-giờ-tới-giây + kết quả kết nối. Layout riêng để dễ chỉnh về sau.
 */
@Composable
fun DetailCallItem(
    entry: CallEntry,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val connected = CallResults.isConnected(entry.type, entry.durationSeconds)
    val resultColor = if (connected) AccentGreen else AccentRed   // line 1: tone ĐẬM
    val softColor = if (connected) SoftGreen else SoftRed         // line 2 + chi tiết: tone NHẠT
    Column(modifier = modifier.fillMaxWidth().clickable { onToggle() }.padding(horizontal = 16.dp)) {
        // Vùng thu gọn — CHIỀU CAO ĐỒNG NHẤT, canh giữa theo chiều dọc.
        Column(modifier = Modifier.fillMaxWidth().height(ROW_HEIGHT), verticalArrangement = Arrangement.Center) {
            // Line 1: icon đi/đến · TAG hướng (Đi/Đến/Nhỡ) · giờ · số — giờ/số in đậm & tô màu theo kết quả (đỏ / xanh).
            Row(verticalAlignment = Alignment.CenterVertically) {
                CallTypeIcon(type = entry.type, size = DIR_ICON_SIZE, isVideo = entry.isVideo, tint = resultColor)
                Spacer(Modifier.width(DIR_ICON_GAP))
                DirectionTag(entry.type, connected)
                Spacer(Modifier.width(DIR_ICON_GAP))
                Text(
                    text = TimeFormat.time(entry.dateMillis),
                    style = MaterialTheme.typography.bodyLarge,
                    color = resultColor,
                    fontWeight = FontWeight.Bold
                )
                Text("  ·  ", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                Text(
                    text = formatPhone(entry.number),
                    style = MaterialTheme.typography.bodyLarge,
                    color = resultColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(3.dp))
            // Line 2: giờ 24h · SIM · nhà mạng · <kết quả tô màu>.
            Text(
                text = callInfoLine(entry, connected),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            // Thụt vào bằng bề rộng icon đi/đến (+ khoảng cách) để thẳng hàng với text ở line 1.
            Column(modifier = Modifier.fillMaxWidth().padding(start = DIR_ICON_SIZE + DIR_ICON_GAP, bottom = 12.dp)) {
                Text(
                    text = TimeFormat.fullDateTimeWithSeconds(entry.dateMillis),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = CallResults.connectionLine(entry.type, entry.durationSeconds),
                    style = MaterialTheme.typography.bodySmall,
                    color = softColor,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val carrierRegion = listOfNotNull(
                        entry.carrier,
                        entry.region.takeIf { it.isNotBlank() }
                    ).joinToString("  ·  ")
                    if (carrierRegion.isNotEmpty()) {
                        Text(carrierRegion, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Spacer(Modifier.width(8.dp))
                    }
                    if (entry.isVolte) {
                        InfoTag("HD Call VoLTE", AccentBlue, AccentBlueBg)
                    } else {
                        InfoTag(appStrings().callDetail.callNormal, TextSecondary, AccentGrayBg)
                    }
                }
            }
        }
    }
}

/** Line 2: "giờ 24h · SIM · nhà mạng · <kết quả tô màu>". */
private fun callInfoLine(entry: CallEntry, connected: Boolean): AnnotatedString = buildAnnotatedString {
    val info = listOfNotNull(
        TimeFormat.itemClock(entry.dateMillis),
        entry.displaySimLabel,
        entry.carrier
    ).joinToString("  ·  ")
    withStyle(SpanStyle(color = TextSecondary)) {
        append(info)
        append("  ·  ")
    }
    withStyle(SpanStyle(color = if (connected) SoftGreen else SoftRed, fontWeight = FontWeight.SemiBold)) {
        append(if (connected) TimeFormat.durationLabel(entry.durationSeconds) else appStrings().callStatus.noResponse)
    }
}

/**
 * Tag hướng gọi (Đi/Đến/Nhỡ) chèn giữa icon và giờ ở line 1.
 * Nhãn theo HƯỚNG cuộc gọi; MÀU theo KẾT QUẢ (kết nối = xanh, không phản hồi = đỏ) — đồng bộ với giờ/số ở line 1.
 */
@Composable
private fun DirectionTag(type: CallType, connected: Boolean) {
    val cd = appStrings().callDetail
    val label = when (type) {
        CallType.OUTGOING -> cd.dirOutgoing
        CallType.INCOMING, CallType.ANSWERED_EXTERNALLY -> cd.dirIncoming
        CallType.MISSED, CallType.REJECTED, CallType.BLOCKED -> cd.dirMissed
        CallType.VOICEMAIL -> cd.dirVoicemail
        CallType.UNKNOWN -> cd.dirOther
    }
    Text(
        text = label,
        color = if (connected) AccentGreen else AccentRed,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(if (connected) AccentGreenBg else AccentRedBg)
            .padding(horizontal = 6.dp, vertical = 1.dp)
    )
}

/** Tag nhỏ (pill) cho thông tin phụ như "HD Call VoLTE" / "Thường". */
@Composable
private fun InfoTag(text: String, textColor: Color, bgColor: Color) {
    Text(
        text = text,
        color = textColor,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(5.dp))
            .background(bgColor)
            .padding(horizontal = 7.dp, vertical = 2.dp)
    )
}

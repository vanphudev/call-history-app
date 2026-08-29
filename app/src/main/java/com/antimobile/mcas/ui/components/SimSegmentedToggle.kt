package com.antimobile.mcas.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.antimobile.mcas.i18n.appStrings
import com.antimobile.mcas.ui.theme.ChipBg
import com.antimobile.mcas.ui.theme.ChipSelectedBg
import com.antimobile.mcas.ui.theme.ChipSelectedText
import com.antimobile.mcas.ui.theme.Primary
import com.antimobile.mcas.ui.theme.TextSecondary
import com.antimobile.mcas.util.SimRegistry
import com.antimobile.mcas.util.SimScope

/**
 * Thanh chọn SIM dạng SEGMENTED (con trượt XANH trượt mượt) DÙNG CHUNG ở nhiều màn (danh sách, chi tiết,
 * tất cả cuộc gọi, Cài đặt).
 *
 * Bố cục theo yêu cầu: đúng 2 SIM → `[SIM 1] [Tất cả] [SIM 2]` ("Tất cả" ở GIỮA). Nhiều hơn 2 SIM (hiếm) →
 * `[Tất cả] [SIM 1] [SIM 2] …` (nơi gọi tự cho cuộn ngang nếu tràn).
 *
 * Giá trị: `null` = tất cả SIM; còn lại là NHÃN sim ("SIM 1"/"SIM 2") khớp đúng [com.antimobile.mcas.data.model.CallEntry.simLabel].
 * [compact] = bản NHỎ để đặt cùng dòng với tiêu đề (màn chi tiết): ô hẹp + chữ nhỏ + thấp hơn.
 */
@Composable
fun SimSegmentedToggle(
    allLabel: String,
    labels: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val barHeight = if (compact) 32.dp else 38.dp
    val segmentWidth = if (compact) 54.dp else 84.dp
    val textStyle = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge
    // (value, nhãn hiển thị): value null = "Tất cả".
    val segments: List<Pair<String?, String>> = if (labels.size == 2) {
        listOf(labels[0] to labels[0], null to allLabel, labels[1] to labels[1])
    } else {
        listOf<Pair<String?, String>>(null to allLabel) + labels.map { it to it }
    }
    val selectedIndex = segments.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    Box(
        modifier = modifier
            .height(barHeight)
            .width(segmentWidth * segments.size)
            .clip(CircleShape)
            .background(ChipBg)
    ) {
        val thumbX by animateDpAsState(
            targetValue = segmentWidth * selectedIndex.toFloat(),
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 520f),
            label = "simThumb"
        )
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbX.roundToPx(), 0) }
                .width(segmentWidth)
                .fillMaxHeight()
                .padding(3.dp)
                .clip(CircleShape)
                .background(Primary)
        )
        Row(modifier = Modifier.fillMaxSize()) {
            segments.forEachIndexed { i, (value, label) ->
                val isSel = i == selectedIndex
                val interaction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .width(segmentWidth)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .clickable(interactionSource = interaction, indication = null) { onSelect(value) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = textStyle,
                        fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSel) Color.White else TextSecondary,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center,
                        // Nhãn dài (tên mạng dịch dài) có thể vượt bề rộng segment cố định → chạy chữ thay vì bị
                        // cắt. Nhãn ngắn ("SIM 1"/"Tất cả") vẫn đứng yên & căn giữa như thường.
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
 * Nhãn TRẠNG THÁI "Đang xem SIM X · nhà mạng" (chỉ đọc) — hiện thay cho thanh lọc khi PHẠM VI SIM TOÀN APP
 * đang khoá một SIM (chọn ở Cài đặt). Dữ liệu đã lọc tại gốc nên đây chỉ báo hiệu, KHÔNG bấm được. Kèm nhà
 * mạng khi biết; SIM đã tháo / không có quyền đọc → chỉ hiện nhãn trơn.
 */
@Composable
fun SimScopeStatusPill(label: String, modifier: Modifier = Modifier, compact: Boolean = false) {
    // Nhà mạng của SIM đang khoá (từ SIM đang lắp). Đọc snapshot-state → tự cập nhật khi SIM đổi.
    val carrier = SimRegistry.carrierForLabel(label)
    val shown = carrier?.let { "$label · $it" } ?: label
    Row(
        modifier = modifier
            .height(if (compact) 32.dp else 38.dp)
            .clip(CircleShape)
            .background(ChipSelectedBg)
            .padding(horizontal = if (compact) 12.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            Icons.Rounded.SimCard,
            contentDescription = null,
            tint = ChipSelectedText,
            modifier = Modifier.size(if (compact) 15.dp else 16.dp)
        )
        Text(
            text = appStrings().callList.simViewing(shown),
            style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = ChipSelectedText,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Điều khiển SIM MODE-AWARE dùng chung cho màn chi tiết / tất cả cuộc gọi:
 *  - Đang khoá phạm vi 1 SIM toàn app → [SimScopeStatusPill] "Đang xem SIM X" (dữ liệu đã lọc sẵn).
 *  - Đang xem "tất cả" & dữ liệu có ≥2 SIM → [SimSegmentedToggle] để LỌC nhanh hiển thị.
 *  - Chỉ 1 SIM (khi xem tất cả) → KHÔNG hiện gì (không có gì để lọc).
 *
 * [availableLabels] = các nhãn SIM CÓ trong dữ liệu đang xem (quyết định có hiện thanh lọc). [localSelection]
 * là lựa chọn lọc nhanh cục bộ (chỉ dùng khi xem "tất cả"). Nơi gọi tự bỏ qua [localSelection] khi phạm vi
 * toàn app đang bật (dữ liệu đã lọc tại gốc).
 */
@Composable
fun SimScopeControl(
    availableLabels: List<String>,
    localSelection: String?,
    onLocalSelect: (String?) -> Unit,
    allLabel: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val globalSim = SimScope.effectiveLabel
    when {
        globalSim != null -> SimScopeStatusPill(globalSim, modifier = modifier, compact = compact)
        availableLabels.size >= 2 -> SimSegmentedToggle(
            allLabel = allLabel,
            labels = availableLabels,
            selected = localSelection,
            onSelect = onLocalSelect,
            modifier = modifier,
            compact = compact
        )
        else -> {}
    }
}

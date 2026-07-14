package com.antimobile.callhs.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.antimobile.callhs.ui.theme.ChipBg
import com.antimobile.callhs.ui.theme.Primary
import com.antimobile.callhs.ui.theme.TextSecondary

private val SEG_GAP = 4.dp

/**
 * Bộ chọn phân đoạn (segmented tab) DÙNG CHUNG toàn dự án — nền [ChipBg] bo tròn, con trượt [Primary] TRƯỢT
 * MƯỢT (animate) sang ô đang chọn, chữ ô chọn trắng, ô còn lại [TextSecondary]. Không có hiệu ứng toả.
 * Kiểu thanh tab của màn THỐNG KÊ; tách ra để mọi màn (thống kê, sửa nhóm phân loại…) dùng chung, đồng bộ UI.
 * Hoạt động với số ô bất kỳ (2, 3, …).
 */
@Composable
fun Segmented(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val n = labels.size
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth().height(40.dp).clip(CircleShape).background(ChipBg).padding(4.dp)
    ) {
        // Bề rộng mỗi ô = (bề rộng trong - tổng khe) / số ô. Con trượt trượt theo (ô + khe) × chỉ số đang chọn.
        val segW = (maxWidth - SEG_GAP * (n - 1)) / n
        val pillX by animateDpAsState(
            targetValue = (segW + SEG_GAP) * selected.coerceIn(0, (n - 1).coerceAtLeast(0)),
            animationSpec = spring(dampingRatio = 0.85f, stiffness = 700f),
            label = "segmentPill",
        )

        // Con trượt (pill xanh) — vẽ TRƯỚC (nằm dưới) nhãn; trượt animate sang ô chọn.
        Box(
            modifier = Modifier
                .offset(x = pillX)
                .width(segW)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(Primary)
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(SEG_GAP),
        ) {
            labels.forEachIndexed { i, label ->
                val sel = i == selected
                val color by animateColorAsState(if (sel) Color.White else TextSecondary, label = "segmentText")
                val interaction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(interactionSource = interaction, indication = null) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        color = color,
                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

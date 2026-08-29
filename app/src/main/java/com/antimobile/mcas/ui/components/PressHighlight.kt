package com.antimobile.mcas.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import com.antimobile.mcas.ui.theme.ThemeSettings
import kotlinx.coroutines.launch

/**
 * Hiệu ứng NHẤN PHẲNG dùng CHUNG — thay cho ripple lan toả của Material.
 *
 * Khi nhấn: NGUYÊN vùng nhấn (cả item) tô NHANH một lớp màu [color] mờ (~[maxAlpha]) rồi GIỮ tới khi nhả;
 * nhả xong lớp phủ mờ đi GỌN. KHÔNG có sóng lan toả từ điểm chạm, không "đuôi" mượt kéo dài.
 *
 * Lớp phủ vẽ trong biên của node clickable. Các item đều bọc trong [PanelCard] (đã `.clip` bo góc) nên
 * lớp phủ tự bo theo góc thẻ. Cách dùng:
 * `combinedClickable(interactionSource = remember { MutableInteractionSource() }, indication = rememberPressHighlight(), …)`.
 */

// Màu + độ mờ mặc định: lớp "state layer" mờ khi nhấn. THEO CHẾ ĐỘ (getter, không hằng số): ở SÁNG dùng ĐEN
// (trên nền sáng → vệt xám nhạt); ở TỐI dùng TRẮNG (trên nền tối → vệt sáng nhẹ) — nếu để đen thì lớp phủ
// gần như VÔ HÌNH trên nền tối, mất phản hồi khi nhấn. Đọc snapshot-state nên đổi chế độ là cập nhật ngay.
private val DefaultHighlightColor: Color get() = if (ThemeSettings.dark) Color.White else Color.Black
private const val DefaultHighlightAlpha = 0.10f

// Nhịp: HIỆN gần như tức thì; nhả thì xoá nhanh-gọn (không phải "mượt" kiểu ripple).
private const val AppearMs = 45
private const val FadeMs = 170

/** Tạo (và nhớ) hiệu ứng nhấn phẳng để truyền vào `combinedClickable(indication = …)`. */
@Composable
fun rememberPressHighlight(
    color: Color = DefaultHighlightColor,
    maxAlpha: Float = DefaultHighlightAlpha,
): IndicationNodeFactory = remember(color, maxAlpha) { PressHighlightIndication(color, maxAlpha) }

private data class PressHighlightIndication(
    val color: Color,
    val maxAlpha: Float,
) : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode =
        PressHighlightNode(interactionSource, color, maxAlpha)
}

private class PressHighlightNode(
    private val interactionSource: InteractionSource,
    private val color: Color,
    private val maxAlpha: Float,
) : Modifier.Node(), DrawModifierNode {

    private val alpha = Animatable(0f)

    override fun onAttach() {
        coroutineScope.launch {
            // Xử lý TUẦN TỰ: cú tô-vào chạy xong mới tới lượt nhả → tap CỰC NHANH vẫn kịp thấy lớp phủ.
            interactionSource.interactions.collect { interaction ->
                when (interaction) {
                    is PressInteraction.Press -> alpha.animateTo(maxAlpha, tween(AppearMs, easing = LinearEasing))
                    is PressInteraction.Release -> alpha.animateTo(0f, tween(FadeMs, easing = LinearEasing))
                    is PressInteraction.Cancel -> alpha.animateTo(0f, tween(FadeMs, easing = LinearEasing))
                }
            }
        }
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        val a = alpha.value
        if (a > 0f) drawRect(color = color, alpha = a)
    }
}

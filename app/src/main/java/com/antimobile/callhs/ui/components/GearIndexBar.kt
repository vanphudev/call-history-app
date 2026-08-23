package com.antimobile.callhs.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antimobile.callhs.ui.theme.CardShadow
import com.antimobile.callhs.ui.theme.CardSurface
import com.antimobile.callhs.ui.theme.Primary
import com.antimobile.callhs.ui.theme.TextSecondary
import com.antimobile.callhs.util.SliderTick
import kotlin.math.abs
import kotlin.math.roundToInt

/** Một nấc của thanh mục lục kiểu bánh răng. [key] phải duy nhất trong thanh. */
data class GearIndexItem(
    val key: String,
    val label: String,
    val bubbleLabel: String = label,
)

/**
 * Phần nền nhô ra khỏi thanh theo đúng silhouette vẽ tay: bầu tròn ở trái, cổ thắt ở giữa và hai
 * cung lõm nhập mềm vào cạnh trái của pill. Path chồng vào nửa pill để đường ghép luôn kín. Phần
 * pill được kéo dài qua nấc đầu/cuối theo đúng bán kính nắp, nên lớp chồng này không thể lộ ra ngoài
 * nắp bo tròn khi chọn phần tử đầu hoặc cuối.
 */
private object GearBubbleShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val width = size.width
        val height = size.height
        // Bubble 86dp chìm 14dp (nửa pill 28dp) vào thanh; cổ được giữ ngắn để bầu nằm sát pill.
        val railJoinX = width * 0.83721f
        val bulbRadius = height * 0.40f
        val bulbCenterX = bulbRadius + height * 0.03f
        val bulbCenterY = height * 0.50f
        val attachCos = 0.76604444f
        val attachSin = 0.64278764f
        val upperAttachX = bulbCenterX + bulbRadius * attachCos
        val upperAttachY = bulbCenterY - bulbRadius * attachSin
        val lowerAttachX = bulbCenterX + bulbRadius * attachCos
        val lowerAttachY = bulbCenterY + bulbRadius * attachSin
        // Cổ có một eo thật sự ở giữa: từ eo này, silhouette loe mềm về cả bầu tròn lẫn pill.
        // Không đặt phần hẹp nhất ngay sát pill vì cách đó khiến đầu nối trông như một cục vuông.
        val neckX = upperAttachX + (railJoinX - upperAttachX) * 0.58f
        val upperNeckY = height * 0.41f
        val lowerNeckY = height * 0.59f
        val upperRailY = height * 0.25f
        val lowerRailY = height * 0.75f
        val neckTangent = (railJoinX - upperAttachX) * 0.15f
        val railVerticalTangent = height * 0.10f
        val bulbTangent = bulbRadius * 0.28f
        val bulbBounds = Rect(
            left = bulbCenterX - bulbRadius,
            top = bulbCenterY - bulbRadius,
            right = bulbCenterX + bulbRadius,
            bottom = bulbCenterY + bulbRadius,
        )
        val path = Path().apply {
            // Phía pill mở rộng trước khi thắt vào eo. Tại miệng pill, đường cong giữ tiếp tuyến
            // dọc theo cạnh thanh rồi mới hõm vào trong; nếu dùng tiếp tuyến ngang ở đây, gốc nối
            // sẽ phồng lồi ra ngoài dù phần eo vẫn đúng.
            moveTo(width, upperRailY)
            lineTo(railJoinX, upperRailY)
            cubicTo(
                railJoinX,
                upperRailY + railVerticalTangent,
                neckX + neckTangent,
                upperNeckY,
                neckX,
                upperNeckY,
            )
            // Từ eo lại mở dần vào bầu; control point cuối cùng đi theo tiếp tuyến vòng tròn
            // nên phần giao với bầu không có nếp gãy.
            cubicTo(
                neckX - neckTangent,
                upperNeckY,
                upperAttachX + bulbTangent * attachSin,
                upperAttachY + bulbTangent * attachCos,
                upperAttachX,
                upperAttachY,
            )
            // Cung tròn chuẩn 280°; miệng nối 80° cho chuyển tiếp rộng và mềm như ảnh mẫu.
            arcTo(
                rect = bulbBounds,
                startAngleDegrees = -40f,
                sweepAngleDegrees = -280f,
                forceMoveTo = false,
            )
            // Nửa dưới phản chiếu chính xác nửa trên: bầu -> eo -> mở dần vào pill.
            cubicTo(
                lowerAttachX + bulbTangent * attachSin,
                lowerAttachY - bulbTangent * attachCos,
                neckX - neckTangent,
                lowerNeckY,
                neckX,
                lowerNeckY,
            )
            cubicTo(
                neckX + neckTangent,
                lowerNeckY,
                railJoinX,
                lowerRailY - railVerticalTangent,
                railJoinX,
                lowerRailY,
            )
            lineTo(width, lowerRailY)
            close()
        }
        return Outline.Generic(path)
    }
}

/**
 * Thanh mục lục dọc dùng cùng chuyển động với mục lục A-Z trong Danh bạ.
 *
 * Chạm/kéo qua một nấc sẽ gọi [onFocusKey], rung nhẹ và phát một tiếng tạch. Toàn bộ nấc luôn hiện
 * trong chiều cao khả dụng; chiều cao nấc và cỡ chữ tự co giãn theo số lượng. Bong bóng dạng giọt
 * nước nối liền vào thanh và thay thế chữ tại nấc đang chọn để không có hai lớp chữ đè lên nhau.
 */
@Composable
fun GearIndexBar(
    items: List<GearIndexItem>,
    currentKey: String?,
    modifier: Modifier = Modifier,
    contentTopPadding: Dp = 36.dp,
    contentBottomPadding: Dp = 36.dp,
    onFocusKey: (String) -> Unit,
) {
    if (items.isEmpty()) return
    val haptic = LocalHapticFeedback.current
    val focusKey by rememberUpdatedState(onFocusKey)
    val density = LocalDensity.current
    val context = LocalContext.current
    val tick = remember { SliderTick(context) }
    DisposableEffect(Unit) { onDispose { tick.release() } }

    val count = items.size
    var availableHeightPx by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    var lastTouchY by remember { mutableFloatStateOf(0f) }
    var lastFocused by remember { mutableStateOf<GearIndexItem?>(null) }

    // Luôn chừa một khoảng thở ở hai mép của vùng nội dung. Với ít nấc, mỗi nấc cao tối đa 32dp;
    // với A-Z/31 ngày, toàn bộ nấc được thu đều để vẫn hiện trọn vẹn mà không tràn màn hình.
    val navigationBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val safeTopPx = with(density) { contentTopPadding.toPx() }
    val safeBottomPx = with(density) { (contentBottomPadding + navigationBottom).toPx() }
    val usableHeightPx = (availableHeightPx - safeTopPx - safeBottomPx).coerceAtLeast(0f)
    val comfortableSlotPx = with(density) { 32.dp.toPx() }
    val slotPx = if (count > 0 && usableHeightPx > 0f) {
        minOf(comfortableSlotPx, usableHeightPx / count)
    } else {
        0f
    }
    val contentHeightPx = slotPx * count
    val contentTopPx = safeTopPx + (usableHeightPx - contentHeightPx).coerceAtLeast(0f) / 2f
    val contentHeight = with(density) { contentHeightPx.toDp() }
    val slotHeight = with(density) { slotPx.toDp() }
    val slotDp = slotHeight.value
    val labelFontSize = (slotDp * 0.43f).coerceIn(8.5f, 13f).sp
    val gearTranslationPx = with(density) { 7.dp.toPx() }
    val railWidth = 28.dp
    val bubbleWidth = 86.dp
    val bubbleHeight = 62.dp
    // Tâm nấc đầu/cuối cách mép danh sách slotHeight / 2. Kéo pill thêm đúng phần còn thiếu để
    // miệng cổ (cao bubbleHeight / 2) luôn nhập vào đoạn cạnh dọc sau bán kính nắp railWidth / 2.
    // Nhờ vậy path vẫn được chồng vào pill để kín đường ghép mà không vượt ra ngoài hai nắp.
    val railEndGuard = maxOf(
        (
            bubbleHeight * 0.25f + railWidth / 2f - slotHeight / 2f
            ).coerceAtLeast(0.dp),
        // Trường hợp danh bạ chỉ có một nhóm: khung pill vẫn phải đủ cao để Compose không ép
        // bubble 62dp và tự căn giữa phần overflow, làm lệch tâm giọt nước khỏi nhãn duy nhất.
        ((bubbleHeight - contentHeight) / 2f).coerceAtLeast(0.dp),
    )
    val railEndGuardPx = with(density) { railEndGuard.toPx() }
    val railHeight = contentHeight + railEndGuard * 2f
    val expansion by animateFloatAsState(
        targetValue = if (dragging) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessMediumLow),
        label = "gearIndexExpand",
    )
    // Nền luôn bán trong suốt để vẫn nhìn thấy danh sách bên dưới. Khi kéo, toàn khối chuyển sang
    // xanh rất nhạt nhưng không trở thành màu đặc.
    val indexSurface = lerp(CardSurface, Primary, 0.16f * expansion)
    val indexSurfaceAlpha = 0.68f + 0.06f * expansion
    // Lớp compositing phải phủ cả phần bubble tràn sang trái. Alpha được áp một lần sau khi rail và
    // bubble đã ghép, nhờ vậy vùng chồng để chống hở khử răng cưa không bị đậm màu gấp đôi.
    val backgroundGroupWidth = bubbleWidth + railWidth / 2f
    val backgroundOverflowCompensation = (backgroundGroupWidth - railWidth) / 2f

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(48.dp)
            .onSizeChanged { availableHeightPx = it.height.toFloat() }
            .pointerInput(items, contentHeightPx, contentTopPx, slotPx) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)

                        // Vùng trống trên/dưới pill không chặn thao tác cuộn của danh sách bên dưới.
                        if (
                            slotPx <= 0f ||
                            down.position.y !in contentTopPx..(contentTopPx + contentHeightPx)
                        ) {
                            continue
                        }

                        val pointerId = down.id
                        lastFocused = null
                        dragging = true
                        val focus: (rawY: Float) -> Unit = { rawY ->
                            val localY = (rawY - contentTopPx)
                                .coerceIn(0f, (contentHeightPx - 0.5f).coerceAtLeast(0f))
                            val index = (localY / slotPx).toInt().coerceIn(0, count - 1)
                            val item = items[index]
                            // Neo bong bóng vào tâm nấc, không chạy lệch theo điểm chạm trong cùng nấc.
                            lastTouchY = (index + 0.5f) * slotPx
                            if (item.key != lastFocused?.key) {
                                lastFocused = item
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                tick.tick()
                                focusKey(item.key)
                            }
                        }

                        focus(down.position.y)
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
                        dragging = false
                    }
                }
            },
    ) {
        if (contentHeightPx <= 0f) return@Box
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                // Khung của pill phải mang luôn phần nới ở hai đầu. Nếu chỉ đặt một Box cao hơn
                // bên trong khung contentHeight, Compose sẽ ép nó về contentHeight rồi offset lên:
                // đầu trên có guard nhưng đầu dưới bị hụt và không còn ôm trọn nhãn cuối.
                .offset {
                    IntOffset(0, (contentTopPx - railEndGuardPx).roundToInt())
                }
                .height(railHeight)
                .width(railWidth),
        ) {
            val focusedItem = lastFocused
            val showBubble = focusedItem != null && expansion > 0.02f

            // Bóng giữ độc lập với lớp nền bán trong suốt để không bị texture offscreen cắt ở mép.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(
                        elevation = 3.dp,
                        shape = RoundedCornerShape(percent = 50),
                        clip = false,
                        ambientColor = CardShadow,
                        spotColor = CardShadow,
                    ),
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    // requiredSize rộng hơn parent sẽ được Compose căn giữa phần overflow; bù thêm
                    // nửa độ chênh để cạnh phải của texture vẫn trùng cạnh phải của rail.
                    .offset(x = -backgroundOverflowCompensation)
                    .requiredSize(width = backgroundGroupWidth, height = railHeight)
                    .graphicsLayer {
                        alpha = indexSurfaceAlpha
                        compositingStrategy = CompositingStrategy.Offscreen
                    },
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .fillMaxHeight()
                        .width(railWidth)
                        .clip(RoundedCornerShape(percent = 50))
                        .background(indexSurface),
                )

                if (showBubble) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset {
                                IntOffset(
                                    x = 0,
                                    y = (
                                        railEndGuardPx + lastTouchY - bubbleHeight.toPx() / 2f
                                        ).roundToInt(),
                                )
                            }
                            .requiredSize(width = bubbleWidth, height = bubbleHeight)
                            .graphicsLayer {
                                alpha = expansion
                                val bubbleScale = 0.7f + 0.3f * expansion
                                scaleX = bubbleScale
                                scaleY = bubbleScale
                                transformOrigin = TransformOrigin(1f, 0.5f)
                            }
                            .clip(GearBubbleShape)
                            .background(indexSurface),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .offset(y = railEndGuard)
                    .height(contentHeight)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items.forEachIndexed { index, item ->
                    val visibleCenter = (index + 0.5f) * slotPx
                    val distance = abs(lastTouchY - visibleCenter)
                    val proximity = (1f - distance / (slotPx * 3.5f)).coerceIn(0f, 1f)
                    val bump = proximity * proximity
                    val scale = 1f + bump * 0.35f * expansion
                    val translationX = -bump * gearTranslationPx * expansion
                    val selected = if (expansion > 0.02f) {
                        item.key == lastFocused?.key
                    } else {
                        item.key == currentKey
                    }
                    Box(
                        modifier = Modifier
                            .height(slotHeight)
                            .fillMaxWidth()
                            .graphicsLayer {
                                // Chữ đang chọn được thể hiện trong giọt nước; ẩn dần bản sao trên thanh
                                // để hai text không bao giờ chồng lên nhau.
                                alpha = if (item.key == lastFocused?.key) 1f - expansion else 1f
                                scaleX = scale
                                scaleY = scale
                                this.translationX = translationX
                                transformOrigin = TransformOrigin(1f, 0.5f)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = item.label,
                            color = if (selected) Primary else TextSecondary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = labelFontSize,
                            maxLines = 1,
                        )
                    }
                }
            }

            if (focusedItem != null && showBubble) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset {
                            val bubbleWidthPx = bubbleWidth.toPx()
                            val bubbleHeightPx = bubbleHeight.toPx()
                            // requiredSize rộng hơn parent 28dp bị Compose tự căn giữa phần overflow.
                            // Bù nửa độ chênh để railJoinX thực sự nằm đúng cạnh trái pill, thay vì
                            // để lộ đoạn chữ nhật đóng path ở bên ngoài thanh.
                            val overflowCenteringCompensation =
                                ((bubbleWidth - railWidth) / 2f).toPx()
                            IntOffset(
                                x = (
                                    -(bubbleWidthPx - railWidth.toPx() / 2f) +
                                        overflowCenteringCompensation
                                    ).roundToInt(),
                                // Vùng an toàn của component đã chừa đủ nửa chiều cao giọt nước,
                                // vì vậy đuôi luôn trỏ đúng tâm cả nấc đầu và nấc cuối.
                                y = (
                                    railEndGuardPx + lastTouchY - bubbleHeightPx / 2f
                                    ).roundToInt(),
                            )
                        }
                        .requiredSize(width = bubbleWidth, height = bubbleHeight)
                        .graphicsLayer {
                            alpha = expansion
                            val bubbleScale = 0.7f + 0.3f * expansion
                            scaleX = bubbleScale
                            scaleY = bubbleScale
                            transformOrigin = TransformOrigin(1f, 0.5f)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(end = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = focusedItem.bubbleLabel,
                            color = Primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = when {
                                focusedItem.bubbleLabel.length <= 2 -> 25.sp
                                focusedItem.bubbleLabel.length <= 3 -> 22.sp
                                focusedItem.bubbleLabel.length <= 5 -> 18.sp
                                else -> 13.sp
                            },
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }

}

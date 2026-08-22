package com.antimobile.callhs.ui.components

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.antimobile.callhs.ui.theme.CardSurface
import kotlin.math.roundToInt

/**
 * Glyph của một nút hành động trong [ContextMenuOverlay]:
 *  - [Vector]: icon material, tô [tint].
 *  - [Logo]: logo drawable NHIỀU MÀU (vd Zalo) — vẽ nguyên bản, không tô tint.
 */
sealed interface ActionGlyph {
    data class Vector(val icon: ImageVector, val tint: Color) : ActionGlyph
    data class Logo(@param:DrawableRes val res: Int) : ActionGlyph
}

/** Một hành động trong [ContextMenuOverlay]: glyph + mô tả (accessibility) + việc làm khi bấm. */
data class ContextAction(
    val glyph: ActionGlyph,
    val desc: String,
    val onClick: () -> Unit,
)

/** Hành động dạng chữ: hiển thị thành nút pill ở hàng riêng bên dưới hàng icon. */
data class ContextTextAction(
    val label: String,
    val tint: Color,
    val onClick: () -> Unit,
)

// Nền tối phủ toàn màn — đủ đậm để làm nổi item + hàng icon, vẫn thấy mờ danh sách phía sau.
private val ScrimColor = Color(0f, 0f, 0f, 0.55f)

// Item khi "nhấc" lên thu nhỏ còn tỉ lệ này (quanh TÂM). Dùng chung cho cả diễn hoạt lẫn canh vị trí hàng icon.
private const val ITEM_REST_SCALE = 0.96f

// Hàng icon hành động: nút TRÒN nền TRẮNG, mỗi icon một màu riêng, cách ĐỀU nhau.
private val ACTION_SIZE = 48.dp        // đường kính nút tròn
private val ACTION_GLYPH = 24.dp       // cỡ icon material bên trong nút
private val ACTION_LOGO_GLYPH = 30.dp  // logo nhiều màu (vd Zalo) hiển thị to hơn 1 tí cho cân
private val ACTION_GAP = 12.dp         // khoảng cách ĐỀU giữa các nút
private val TEXT_ACTION_HEIGHT = 56.dp // đủ cho nhãn dài xuống tối đa hai dòng mà vẫn giữ dáng pill
private val STACKED_TEXT_ACTION_WIDTH = 248.dp

/**
 * Lớp phủ CONTEXT-MENU kiểu Zalo dùng CHUNG toàn dự án — thay cho cử chỉ vuốt-lộ-hành-động.
 *
 * Đặt ở GỐC màn (cửa sổ edge-to-edge) nên nền tối phủ TRỌN màn hình, kể cả sau thanh trạng thái & thanh
 * điều hướng (khác Popup vốn chừa lại 2 thanh này). Cách dùng: màn giữ 1 state "đang mở menu cho item nào"
 * (khung [bounds] tính từ `boundsInWindow()` lúc nhấn giữ); khi != null thì render overlay này ở gốc.
 *
 * Diễn hoạt: nền tối mờ dần vào; item được "nhấc" lên (bản sao [lifted] tại đúng vị trí cũ) và THU NHẸ lại;
 * các hàng hành động bung ra mượt cạnh item. Đóng thì mọi thứ chạy ngược rồi mới gọi [onClosed] gỡ overlay.
 *
 * Vị trí hàng icon tự tính để không bị che: canh PHẢI theo item, ưu tiên xổ DƯỚI, thiếu chỗ thì lật TRÊN,
 * luôn nằm trong vùng an toàn (chừa [topInsetPx]/[bottomInsetPx]).
 *
 * @param bounds khung THẺ gốc (toạ độ cửa sổ, px) — dùng để đặt bản sao [lifted] đúng chỗ & canh hàng icon.
 * @param actions danh sách hành động; bấm nút nào cũng tự ĐÓNG menu sau khi chạy [ContextAction.onClick].
 * @param textActions hành động chữ dạng pill, nằm thành hàng riêng bên dưới [actions] và có bề rộng bằng nhau.
 * @param stackTextActions xếp mỗi hành động chữ thành một hàng, canh phải và dùng bề mặt kính trong suốt.
 * @param lifted bản sao THẺ (vẽ y hệt item gốc) — được đo theo bề rộng của [bounds].
 */
@Composable
fun ContextMenuOverlay(
    bounds: Rect,
    actions: List<ContextAction>,
    textActions: List<ContextTextAction> = emptyList(),
    stackTextActions: Boolean = false,
    topInsetPx: Int,
    bottomInsetPx: Int,
    onClosed: () -> Unit,
    lifted: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val gapPx = with(density) { 10.dp.roundToPx() }
    val marginPx = with(density) { 12.dp.roundToPx() }

    // Điều khiển vào/ra bằng transition; khi ĐÓNG xong mới gọi onClosed để gỡ overlay.
    val visibleState = remember { MutableTransitionState(false) }
    var closing by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visibleState.targetState = true }
    LaunchedEffect(visibleState.isIdle) {
        if (closing && visibleState.isIdle) onClosed()
    }
    val requestClose: () -> Unit = {
        if (!closing) {
            closing = true
            visibleState.targetState = false
        }
    }
    BackHandler(enabled = !closing) { requestClose() }

    val transition = rememberTransition(visibleState, label = "ctxMenu")
    val dim by transition.animateFloat(
        // VÀO 200ms cho mượt; RA nhanh & DỨT KHOÁT (tween ngắn) để overlay gỡ NGAY.
        transitionSpec = {
            if (targetState) tween(durationMillis = 200, easing = FastOutSlowInEasing)
            else tween(durationMillis = 140, easing = FastOutSlowInEasing)
        }, label = "dim"
    ) { if (it) 1f else 0f }
    val appear by transition.animateFloat(
        // VÀO: nảy nhẹ (spring) cho sinh động. RA: tween NGẮN thay vì spring — spring có đuôi rung
        // mất ~1s mới coi là "idle", giữ overlay (scrim) còn bám nuốt chạm nên NHẤN GIỮ lại phải chờ.
        // Đổi lối RA sang tween 140ms → isIdle sớm → target=null NGAY → nhấn giữ lại được liền.
        transitionSpec = {
            if (targetState) spring(dampingRatio = 0.82f, stiffness = 340f)
            else tween(durationMillis = 140, easing = FastOutSlowInEasing)
        }, label = "appear"
    ) { if (it) 1f else 0f }

    Box(Modifier.fillMaxSize()) {
        // 1) Nền tối phủ toàn màn. Chạm nền để đóng; chặn kéo lọt xuống danh sách phía sau.
        //    KHI bắt đầu ĐÓNG (closing) → GỠ NGAY mọi bắt-chạm: từ giây phút đó overlay chỉ còn là lớp mờ
        //    dần THUẦN HÌNH ẢNH (~140ms), KHÔNG còn "nuốt" chạm. Nhờ vậy cú NHẤN GIỮ kế tiếp rơi THẲNG xuống
        //    item bên dưới → combinedClickable khởi động bộ đếm long-press ngay, không phải chờ overlay biến
        //    mất mới nhận được. (Compose chốt đích chạm lúc DOWN; scrim còn pointerInput = còn chặn item.)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = dim }
                .background(ScrimColor)
                .then(
                    if (closing) Modifier
                    else Modifier
                        .pointerInput(Unit) { detectTapGestures { requestClose() } }
                        .pointerInput(Unit) { detectDragGestures { change, _ -> change.consume() } }
                )
        )
        // 2) Item nổi (bản sao) + hàng icon — đặt theo toạ độ đã chốt.
        Layout(
            modifier = Modifier.fillMaxSize(),
            content = {
                // [0] Item được nhấc lên, thu nhẹ để tạo cảm giác tách khỏi danh sách.
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            val s = 1f - (1f - ITEM_REST_SCALE) * appear
                            scaleX = s
                            scaleY = s
                        }
                        // Bản sao nổi nằm ĐÈ đúng item gốc → cũng phải GỠ bắt-chạm khi đang đóng, nếu không
                        // nó chặn cú nhấn giữ LẠI CHÍNH item đó trong lúc mờ dần.
                        .then(
                            if (closing) Modifier
                            else Modifier.pointerInput(Unit) { detectTapGestures { requestClose() } }
                        )
                ) {
                    lifted()
                }
                // [1] Hàng icon và (nếu có) hàng nút chữ bên dưới, bung ra từ mép PHẢI gần item.
                Box(
                    modifier = Modifier.graphicsLayer {
                        alpha = appear
                        val s = 0.88f + 0.12f * appear
                        scaleX = s
                        scaleY = s
                        transformOrigin = TransformOrigin(1f, 0f)
                    }
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(ACTION_GAP),
                    ) {
                        Row(
                            modifier = if (textActions.isNotEmpty() && !stackTextActions) {
                                Modifier.fillMaxWidth()
                            } else {
                                Modifier
                            },
                            horizontalArrangement = if (textActions.isNotEmpty() && !stackTextActions) {
                                Arrangement.SpaceEvenly
                            } else {
                                Arrangement.spacedBy(ACTION_GAP)
                            },
                        ) {
                            actions.forEach { action ->
                                // Chặn bấm nút hành động khi ĐANG đóng (nút đang mờ dần) để không lỡ chạy 2 lần
                                // (vd gọi/sao chép) do một cú chạm lạc vào lúc chuyển cảnh.
                                CircleAction(action) { if (!closing) { action.onClick(); requestClose() } }
                            }
                        }
                        if (textActions.isNotEmpty()) {
                            if (stackTextActions) {
                                textActions.forEach { action ->
                                    TextAction(
                                        action = action,
                                        modifier = Modifier.width(STACKED_TEXT_ACTION_WIDTH),
                                        glass = true,
                                    ) {
                                        if (!closing) {
                                            action.onClick()
                                            requestClose()
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(ACTION_GAP),
                                ) {
                                    textActions.forEach { action ->
                                        TextAction(
                                            action = action,
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            if (!closing) {
                                                action.onClick()
                                                requestClose()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { measurables, constraints ->
            val fullW = constraints.maxWidth
            val fullH = constraints.maxHeight
            val cardW = bounds.width.roundToInt().coerceIn(1, fullW)
            val card = measurables[0].measure(
                Constraints(minWidth = cardW, maxWidth = cardW, minHeight = 0, maxHeight = fullH)
            )
            val menu = measurables[1].measure(
                Constraints(minWidth = 0, maxWidth = (fullW - 2 * marginPx).coerceAtLeast(1), minHeight = 0, maxHeight = fullH)
            )

            val cardX = bounds.left.roundToInt().coerceIn(0, (fullW - card.width).coerceAtLeast(0))
            val cardY = bounds.top.roundToInt()

            // Item thu nhỏ quanh TÂM khi hiện → các mép dịch vào trong; canh hàng icon theo khung ĐÃ thu nhỏ
            // để icon & item canh PHẢI đều nhau và khoảng cách dọc ôm sát item.
            val insetX = ((1f - ITEM_REST_SCALE) / 2f * card.width).roundToInt()
            val insetY = ((1f - ITEM_REST_SCALE) / 2f * card.height).roundToInt()
            val itemRight = cardX + card.width - insetX
            val itemTop = cardY + insetY
            val itemBottom = cardY + card.height - insetY

            // Hàng icon canh PHẢI theo mép phải đã thu nhỏ của item, kẹp trong lề màn hình.
            val menuX = (itemRight - menu.width).coerceIn(marginPx, (fullW - menu.width - marginPx).coerceAtLeast(marginPx))
            val minY = topInsetPx + marginPx
            val maxY = fullH - bottomInsetPx - marginPx - menu.height
            val belowY = itemBottom + gapPx
            val aboveY = itemTop - gapPx - menu.height
            val menuY = when {
                maxY < minY -> minY
                belowY <= maxY -> belowY
                aboveY >= minY -> aboveY
                else -> maxY
            }

            layout(fullW, fullH) {
                card.place(cardX, cardY)
                menu.place(menuX, menuY)
            }
        }
    }
}

/**
 * Nút TRÒN chứa glyph (icon material tô màu, hoặc logo drawable nhiều màu vẽ nguyên bản). Nền dùng [CardSurface]
 * (theo chế độ): SÁNG = trắng như cũ; TỐI = bề mặt thẻ tối → icon nhấn (đã làm sáng cho bản tối) GIỮ đủ tương
 * phản, không bị "chữ sáng trên nền trắng" mờ tịt, và không còn đốm trắng chói trên nền menu tối. Logo Zalo
 * (app-icon tự chứa nền) vẫn hiện đúng trên nền tối.
 */
@Composable
private fun CircleAction(action: ContextAction, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(ACTION_SIZE)
            .shadow(6.dp, CircleShape)
            .clip(CircleShape)
            .background(CardSurface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when (val g = action.glyph) {
            is ActionGlyph.Vector -> Icon(g.icon, contentDescription = action.desc, tint = g.tint, modifier = Modifier.size(ACTION_GLYPH))
            is ActionGlyph.Logo -> Image(painterResource(g.res), contentDescription = action.desc, modifier = Modifier.size(ACTION_LOGO_GLYPH))
        }
    }
}

/** Nút chữ bo tròn hai đầu; các nút cùng hàng nhận cùng trọng số nên luôn canh và rộng đều nhau. */
@Composable
private fun TextAction(
    action: ContextTextAction,
    modifier: Modifier = Modifier,
    glass: Boolean = false,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    val surface = if (glass) {
        Modifier
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.22f),
                        Color.White.copy(alpha = 0.10f),
                    )
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.30f), shape)
    } else {
        Modifier.background(CardSurface)
    }
    Box(
        modifier = modifier
            .height(TEXT_ACTION_HEIGHT)
            .shadow(6.dp, shape)
            .clip(shape)
            .then(surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = action.label,
            style = MaterialTheme.typography.labelLarge,
            color = if (glass) Color.White else action.tint,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

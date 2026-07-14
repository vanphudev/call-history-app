package com.antimobile.callhs.ui.category

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antimobile.callhs.data.local.CategoryBadge
import com.antimobile.callhs.data.local.CategoryCatalog
import com.antimobile.callhs.ui.theme.AppBackground
import com.antimobile.callhs.ui.theme.TextSecondary

/**
 * Cụm badge nhóm phân loại ở góc DƯỚI–PHẢI avatar: tối đa [max] icon đặc (mỗi cái vòng tròn màu của nhóm
 * + vành trắng) xếp CHỒNG nhẹ; nếu nhiều hơn thì hiện [max] icon + 1 badge "+N".
 *
 * Đọc [CategoryCatalog.badgesFor] (snapshot-state) → tự cập nhật khi số được thêm/gỡ khỏi nhóm ở bất kỳ đâu.
 * Caller đặt `Modifier.align(Alignment.BottomEnd)` trên avatar-Box (KHÔNG clip Box) để cụm nằm đúng góc.
 */
@Composable
fun AvatarCategoryBadges(
    number: String,
    modifier: Modifier = Modifier,
    max: Int = 3,
    badgeSize: Dp = 15.dp,
) {
    val badges = CategoryCatalog.badgesFor(number)
    if (badges.isEmpty()) return
    val shown = badges.take(max)
    val extra = badges.size - shown.size

    // Layout tự tính bề rộng ĐÃ chồng để BottomEnd canh đúng (badge phải cùng cạnh phải avatar).
    Layout(
        modifier = modifier,
        content = {
            shown.forEach { b -> Badge(b, badgeSize) }
            if (extra > 0) MoreBadge(extra, badgeSize)
        }
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints) }
        if (placeables.isEmpty()) return@Layout layout(0, 0) {}
        val d = placeables.first().width
        val step = (d * 0.66f).toInt()                 // chồng ~34%
        val totalW = d + step * (placeables.size - 1)
        val h = placeables.maxOf { it.height }
        layout(totalW, h) {
            var x = 0
            placeables.forEach { p -> p.placeRelative(x, 0); x += step }  // vẽ sau nằm trên
        }
    }
}

@Composable
private fun Badge(badge: CategoryBadge, size: Dp) {
    when (val g = categoryGlyph(badge.iconKey)) {
        // Logo app: vành trắng + ảnh logo lấp kín vòng tròn (giữ màu thương hiệu).
        is CategoryGlyph.Res -> Box(
            modifier = Modifier.size(size).clip(CircleShape).background(AppBackground),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(g.id),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(1.5.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
        // Icon material: đĩa màu nhóm + icon trắng.
        is CategoryGlyph.Vec -> RingDisc(fill = Color(badge.colorArgb), size = size) {
            Icon(
                imageVector = g.icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.56f),
            )
        }
    }
}

@Composable
private fun MoreBadge(extra: Int, size: Dp) {
    RingDisc(fill = TextSecondary, size = size) {
        Text(
            text = "+$extra",
            color = Color.White,
            fontSize = (size.value * 0.44f).sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** Vành ngoài trắng (theo nền app) + đĩa màu đặc bên trong → tách bạch khi các badge chồng lên nhau. */
@Composable
private fun RingDisc(fill: Color, size: Dp, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(size).clip(CircleShape).background(AppBackground),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(1.5.dp).clip(CircleShape).background(fill),
            contentAlignment = Alignment.Center,
            content = { content() },
        )
    }
}

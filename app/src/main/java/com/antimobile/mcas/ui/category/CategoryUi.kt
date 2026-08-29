package com.antimobile.mcas.ui.category

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Ô icon nhóm dùng chung cho hàng danh sách / sheet / editor.
 *  - Icon material: nền vuông bo mềm tô màu nhạt của nhóm + icon đặc tô màu nhóm.
 *  - LOGO app (giao hàng): ảnh logo LẤP KÍN ô vuông bo góc (giữ nguyên màu thương hiệu, không tô màu).
 */
@Composable
fun CategoryIconDisc(
    iconKey: String,
    colorArgb: Long,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
) {
    val shape = RoundedCornerShape(size * 0.30f)
    when (val g = categoryGlyph(iconKey)) {
        is CategoryGlyph.Res -> Box(
            modifier = modifier.size(size).clip(shape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(g.id),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        is CategoryGlyph.Vec -> {
            val color = Color(colorArgb)
            Box(
                modifier = modifier
                    .size(size)
                    .clip(shape)
                    .background(color.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = g.icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(size * 0.52f),
                )
            }
        }
    }
}

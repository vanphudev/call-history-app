package com.antimobile.callhs.ui.components

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.antimobile.callhs.ui.theme.ScrollTopScrim
import com.antimobile.callhs.ui.theme.ScrollTopSolid

/**
 * Nút cuộn tròn dạng kính mờ, dùng cùng kỹ thuật backdrop blur với nút "Lên đầu" ở danh sách cuộc gọi.
 * [backdropLayer] phải là ảnh chụp lớp nội dung nằm dưới nút và [contentCoords] là toạ độ của lớp đó.
 */
@Composable
fun FrostedScrollButton(
    icon: ImageVector,
    contentDescription: String,
    backdropLayer: GraphicsLayer,
    contentCoords: LayoutCoordinates?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    buttonSize: Dp = 48.dp,
    iconSize: Dp = 28.dp,
) {
    val blurLayer = rememberGraphicsLayer()
    val blurRadiusPx = with(LocalDensity.current) { 24.dp.toPx() }
    var selfCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val iconAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.26f,
        animationSpec = tween(durationMillis = 220),
        label = "frostedScrollButtonAlpha",
    )
    Box(
        modifier = modifier
            .size(buttonSize)
            .onGloballyPositioned { selfCoords = it }
            .clip(CircleShape)
            .drawBehind {
                val content = contentCoords
                val self = selfCoords
                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    content != null && self != null && content.isAttached && self.isAttached
                ) {
                    val topLeft = content.localPositionOf(self, Offset.Zero)
                    blurLayer.renderEffect = BlurEffect(blurRadiusPx, blurRadiusPx, TileMode.Decal)
                    blurLayer.record {
                        translate(left = -topLeft.x, top = -topLeft.y) {
                            drawLayer(backdropLayer)
                        }
                    }
                    drawLayer(blurLayer)
                    drawRect(color = ScrollTopScrim)
                } else {
                    drawRect(color = ScrollTopSolid)
                }
            }
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(iconSize).alpha(iconAlpha),
        )
    }
}

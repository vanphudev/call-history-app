package com.antimobile.mcas.ui.components

import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.antimobile.mcas.ui.theme.CardSurface
import com.antimobile.mcas.ui.theme.HairlineBorder

/**
 * Bề mặt kính mờ dùng cho các cụm nội dung nổi trên một lớp đã được ghi vào [backdropLayer].
 * Android 12+ dùng backdrop blur thật; các bản cũ dùng nền bán trong suốt có độ tương phản tương đương.
 */
@Composable
fun FrostedSurface(
    backdropLayer: GraphicsLayer,
    contentCoords: LayoutCoordinates?,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val blurLayer = rememberGraphicsLayer()
    val blurRadiusPx = with(LocalDensity.current) { 24.dp.toPx() }
    var selfCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val glassTint = CardSurface.copy(alpha = 0.64f)
    val fallbackTint = CardSurface.copy(alpha = 0.94f)

    Box(
        modifier = modifier
            .shadow(8.dp, shape)
            .onGloballyPositioned { selfCoords = it }
            .clip(shape)
            .drawBehind {
                val backdropCoords = contentCoords
                val surfaceCoords = selfCoords
                if (
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    backdropCoords != null && surfaceCoords != null &&
                    backdropCoords.isAttached && surfaceCoords.isAttached
                ) {
                    val topLeft = backdropCoords.localPositionOf(surfaceCoords, Offset.Zero)
                    blurLayer.renderEffect = BlurEffect(blurRadiusPx, blurRadiusPx, TileMode.Decal)
                    blurLayer.record {
                        translate(left = -topLeft.x, top = -topLeft.y) {
                            drawLayer(backdropLayer)
                        }
                    }
                    drawLayer(blurLayer)
                    drawRect(glassTint)
                } else {
                    drawRect(fallbackTint)
                }
            }
            .border(1.dp, HairlineBorder.copy(alpha = 0.78f), shape),
        contentAlignment = contentAlignment,
        content = content,
    )
}

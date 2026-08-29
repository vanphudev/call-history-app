package com.antimobile.mcas.ui.theme

import androidx.compose.foundation.LocalIndication
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.Density
import com.antimobile.mcas.ui.components.rememberPressHighlight
import com.antimobile.mcas.util.FontScaleSettings

/*
 * Giao diện app hỗ trợ 2 chế độ SÁNG / TỐI (mặc định theo hệ thống). Màu do design-token điều khiển
 * ([Color.kt] + [Palette.kt]); Material chỉ lo typography + ripple + màu của vài thành phần Material
 * (Switch/RadioButton/ProgressIndicator) qua [ColorScheme]. Chọn chế độ ở [ThemeSettings] (đọc snapshot-state
 * nên đổi là tô lại ngay, không khởi động lại).
 */

/**
 * Dựng [ColorScheme] Material từ một [Palette] cụ thể (KHÔNG đọc token động → luôn đúng chế độ được truyền).
 * Chỉ đặt các trường app thực sự dùng (màu do design-token lo); phần còn lại giữ mặc định của factory
 * light/dark cho khớp chế độ. Dùng factory (không `copy()`) để không phụ thuộc phiên bản Material3.
 */
private fun schemeFor(palette: Palette, dark: Boolean): ColorScheme {
    val primary = palette.primary
    val onPrimary = Color.White
    val primaryContainer = palette.brandSoft
    val onPrimaryContainer = palette.onPrimaryContainer
    val secondaryContainer = palette.accentGrayBg
    val onSecondaryContainer = palette.textPrimary
    val background = palette.appBackground
    val onBackground = palette.textPrimary
    val surface = palette.cardSurface
    val onSurface = palette.textPrimary
    val surfaceVariant = palette.cardFill
    val onSurfaceVariant = palette.textSecondary
    val outline = palette.hairlineBorder
    return if (dark) {
        darkColorScheme(
            primary = primary, onPrimary = onPrimary,
            primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
            secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
            background = background, onBackground = onBackground,
            surface = surface, onSurface = onSurface,
            surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
            outline = outline, outlineVariant = outline
        )
    } else {
        lightColorScheme(
            primary = primary, onPrimary = onPrimary,
            primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
            secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
            background = background, onBackground = onBackground,
            surface = surface, onSurface = onSurface,
            surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
            outline = outline, outlineVariant = outline
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MCASTheme(
    content: @Composable () -> Unit
) {
    // Đọc [ThemeSettings.dark] (snapshot-state) → đổi chế độ là recompose cả cây, chọn lại palette + scheme.
    val dark = ThemeSettings.dark
    val palette = if (dark) DarkPalette else LightPalette
    val colorScheme = remember(dark) { schemeFor(palette, dark) }
    ProvideAppDensity {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography
        ) {
            // BỎ ripple TOÀN APP (dự án dùng hiệu ứng nhấn PHẲNG riêng):
            // - LocalIndication = rememberPressHighlight() → mọi Modifier.clickable/combinedClickable/
            //   toggleable/selectable TRẦN (không tự truyền indication) dùng lớp phủ phẳng thay ripple.
            //   Site nào truyền indication tường minh (vd tab dùng `indication = null`) vẫn giữ nguyên.
            // - LocalRippleConfiguration = null → tắt ripple của thành phần Material (Switch, RadioButton).
            // - LocalViewConfiguration = rút NGẮN ngưỡng NHẤN GIỮ (xem [FastLongPressViewConfiguration]) →
            //   mọi combinedClickable onLongClick (item danh sách/xem-tất-cả/mẫu tin) bật context-menu NHANH
            //   hơn, đỡ cảm giác "giữ hơi lâu" so với app thường. Chỉ đổi riêng ngưỡng long-press.
            val fastLongPress = LocalViewConfiguration.current.let { base ->
                remember(base) { FastLongPressViewConfiguration(base) }
            }
            CompositionLocalProvider(
                LocalIndication provides rememberPressHighlight(),
                LocalRippleConfiguration provides null,
                LocalViewConfiguration provides fastLongPress
            ) {
                content()
            }
        }
    }
}

/**
 * Cấp [LocalDensity] theo CỠ CHỮ người dùng chọn ([FontScaleSettings.scale]): CHỈ chữ (sp) phóng theo hệ số,
 * còn dp (icon, khoảng cách) GIỮ NGUYÊN → không vỡ layout. Đọc snapshot-state nên đổi cỡ chữ là cập nhật ngay.
 *
 * QUAN TRỌNG — phải gọi LẠI helper này BÊN TRONG mỗi "cửa sổ Compose" riêng (ModalBottomSheet, Dialog,
 * Popup…). Lý do: mỗi cửa sổ là một Compose root độc lập, tự cấp lại LocalDensity từ context của nó (đã bị
 * ghim fontScale = 1.0 ở [com.antimobile.mcas.MainActivity]) → ĐÈ mất override ở gốc app. Nếu không bọc
 * lại thì chữ trong sheet/dialog sẽ KHÔNG đổi cỡ (kẹt ở 100%). Nội dung màn hình thường (cùng cửa sổ activity)
 * đã được [MCASTheme] bọc sẵn nên không cần gọi thêm.
 *
 * (Màu KHÔNG gặp vấn đề này: token màu đọc từ singleton toàn cục [ThemeSettings] nên tự đúng ở mọi cửa sổ,
 * không cần cấp lại theo từng cửa sổ.)
 */
@Composable
fun ProvideAppDensity(content: @Composable () -> Unit) {
    val scale = FontScaleSettings.scale
    val base = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density = base.density, fontScale = scale),
        content = content
    )
}

/**
 * Ngưỡng NHẤN GIỮ (ms) để bật context-menu — hạ xuống dưới mặc định nền tảng (~400ms) cho cảm giác nhấn-giữ
 * NHẠY hơn, ngang các app thường. Đủ dài để KHÔNG nhầm với chạm nhanh (tap ~<150ms) và vì cuộn/di ngón vượt
 * touch-slop luôn HUỶ đếm long-press nên không sợ bật menu ngoài ý khi bắt đầu cuộn. Chỉnh 1 chỗ này là đổi
 * toàn app. Tăng nếu thấy dễ bung nhầm; giảm nếu vẫn thấy chậm.
 */
private const val LONG_PRESS_TIMEOUT_MS = 250L

/**
 * Cấu hình chạm CHỈ ĐỔI ngưỡng [ViewConfiguration.longPressTimeoutMillis]; mọi thứ khác (touchSlop,
 * double-tap, kích thước vùng chạm tối thiểu…) UỶ QUYỀN nguyên vẹn cho cấu hình gốc `by base` → không ảnh
 * hưởng hành vi chạm/cuộn/nhận-đúp, cũng không vỡ khi Compose thêm thuộc tính mới vào interface.
 */
private class FastLongPressViewConfiguration(
    base: ViewConfiguration
) : ViewConfiguration by base {
    override val longPressTimeoutMillis: Long = LONG_PRESS_TIMEOUT_MS
}

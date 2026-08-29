package com.antimobile.mcas.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * TOKEN MÀU của dự án — nay ĐỘNG theo chế độ Sáng/Tối.
 *
 * Mỗi token là một property getter trả về trường tương ứng của palette ĐANG áp ([ThemeSettings.colors]).
 * Nhờ vậy ~278 chỗ dùng token (33 file) KHÔNG phải sửa gì: đọc token TRONG @Composable/khi vẽ sẽ subscribe
 * snapshot-state ở [ThemeSettings] → đổi chế độ là cả app tự tô lại NGAY (không khởi động lại).
 *
 * Giá trị thật của từng token cho mỗi chế độ nằm ở [Palette.kt] ([LightPalette] / [DarkPalette]); phân tích
 * thiết kế bản tối (đối lập & hài hoà, kiểm tương phản WCAG) cũng ở đó.
 *
 * LƯU Ý: vì là getter đọc state, ĐỪNG gán token vào một `val` top-level/`remember{}` không khoá (sẽ "đóng băng"
 * giá trị của chế độ lúc khởi tạo). Cần dẫn xuất thì dùng getter (vd `val X get() = TextSecondary.copy(...)`).
 */

// ---- Nền & bề mặt (không viền) ----
val AppBackground: Color get() = ThemeSettings.colors.appBackground
val CardSurface: Color get() = ThemeSettings.colors.cardSurface
val CardFill: Color get() = ThemeSettings.colors.cardFill
val CardShadow: Color get() = ThemeSettings.colors.cardShadow
val FieldSurface: Color get() = ThemeSettings.colors.fieldSurface
val DividerColor: Color get() = ThemeSettings.colors.dividerColor
val ScrollTopScrim: Color get() = ThemeSettings.colors.scrollTopScrim
val ScrollTopSolid: Color get() = ThemeSettings.colors.scrollTopSolid
val HairlineBorder: Color get() = ThemeSettings.colors.hairlineBorder

// ---- Chữ ----
val TextPrimary: Color get() = ThemeSettings.colors.textPrimary
val TextSecondary: Color get() = ThemeSettings.colors.textSecondary

// ---- Màu chủ đạo (XANH LÁ) ----
val Primary: Color get() = ThemeSettings.colors.primary
val LinkColor: Color get() = ThemeSettings.colors.linkColor
val BrandSoft: Color get() = ThemeSettings.colors.brandSoft

// ---- Màu loại cuộc gọi ----
val CallOutgoing: Color get() = ThemeSettings.colors.callOutgoing
val CallIncoming: Color get() = ThemeSettings.colors.callIncoming
val CallMissed: Color get() = ThemeSettings.colors.callMissed

// ---- Accent + nền nhạt cho icon hành động / trạng thái ----
val AccentGreen: Color get() = ThemeSettings.colors.accentGreen
val AccentGreenBg: Color get() = ThemeSettings.colors.accentGreenBg
val AccentBlue: Color get() = ThemeSettings.colors.accentBlue
val AccentBlueBg: Color get() = ThemeSettings.colors.accentBlueBg
val AccentRed: Color get() = ThemeSettings.colors.accentRed
val AccentRedBg: Color get() = ThemeSettings.colors.accentRedBg
val AccentAmber: Color get() = ThemeSettings.colors.accentAmber
val AccentAmberBg: Color get() = ThemeSettings.colors.accentAmberBg
val AccentGray: Color get() = ThemeSettings.colors.accentGray
val AccentGrayBg: Color get() = ThemeSettings.colors.accentGrayBg

// ---- Tone NHẠT cho trạng thái ở line 2 / phần chi tiết ----
val SoftGreen: Color get() = ThemeSettings.colors.softGreen
val SoftRed: Color get() = ThemeSettings.colors.softRed

// ---- Huy hiệu SIM ----
val SimBadgeBg: Color get() = ThemeSettings.colors.simBadgeBg
val SimBadgeText: Color get() = ThemeSettings.colors.simBadgeText

// ---- Tab lọc hàng 1 ----
val TabSelectedBg: Color get() = ThemeSettings.colors.tabSelectedBg
val TabSelectedText: Color get() = ThemeSettings.colors.tabSelectedText
val TabText: Color get() = ThemeSettings.colors.tabText

// ---- Chip lọc hàng 2 ----
val ChipSelectedBg: Color get() = ThemeSettings.colors.chipSelectedBg
val ChipSelectedText: Color get() = ThemeSettings.colors.chipSelectedText
val ChipBg: Color get() = ThemeSettings.colors.chipBg
val ChipText: Color get() = ThemeSettings.colors.chipText

// ---- Bong bóng SMS ----
val MessageOutgoingBubble: Color get() = ThemeSettings.colors.messageOutgoingBubble
val MessageIncomingBubble: Color get() = ThemeSettings.colors.messageIncomingBubble
val MessageOutgoingText: Color get() = ThemeSettings.colors.messageOutgoingText
val MessageIncomingText: Color get() = ThemeSettings.colors.messageIncomingText
